from __future__ import annotations

import json
from pathlib import Path

import comtypes.client


BASE_DIR = Path(__file__).resolve().parent
PPT_PATH = BASE_DIR / "FodFin AGPR.pptx"
NOTES_PATH = BASE_DIR / "slide_notes.json"
AUDIO_DIR = BASE_DIR / "slide_audio"
OUTPUT_PPTX = BASE_DIR / "FodFin AGPR_with_audio.pptx"

MsoTriStateTrue = -1
ppAdvanceOnTime = 2
msoAnimTriggerAfterPrevious = 3
AUDIO_EFFECT_ID = 83
BUFFER_SECONDS = 2.0

slides_data = json.loads(NOTES_PATH.read_text(encoding="utf-8"))


def mp3_duration_seconds(path: Path) -> float:
    data = path.read_bytes()
    offset = 0
    total_samples = None
    sample_rate = None
    bitrate = None

    while offset + 10 <= len(data):
        if data[offset:offset + 3] == b"ID3":
            tag_size = (
                ((data[offset + 6] & 0x7F) << 21)
                | ((data[offset + 7] & 0x7F) << 14)
                | ((data[offset + 8] & 0x7F) << 7)
                | (data[offset + 9] & 0x7F)
            )
            offset += 10 + tag_size
            continue

        if data[offset] != 0xFF or (data[offset + 1] & 0xE0) != 0xE0:
            offset += 1
            continue

        version_bits = (data[offset + 1] >> 3) & 0x03
        layer_bits = (data[offset + 1] >> 1) & 0x03
        bitrate_index = (data[offset + 2] >> 4) & 0x0F
        sample_rate_index = (data[offset + 2] >> 2) & 0x03
        padding_bit = (data[offset + 2] >> 1) & 0x01

        if version_bits == 1 or layer_bits != 1 or bitrate_index in (0, 15) or sample_rate_index == 3:
            offset += 1
            continue

        version_lookup = {3: "mpeg1", 2: "mpeg2", 0: "mpeg25"}
        sample_rate_table = {
            "mpeg1": [44100, 48000, 32000],
            "mpeg2": [22050, 24000, 16000],
            "mpeg25": [11025, 12000, 8000],
        }
        bitrate_table = {
            "mpeg1": [0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0],
            "mpeg2": [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0],
            "mpeg25": [0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0],
        }

        version = version_lookup[version_bits]
        sample_rate = sample_rate_table[version][sample_rate_index]
        bitrate = bitrate_table[version][bitrate_index] * 1000
        if bitrate == 0:
            offset += 1
            continue

        frame_length = ((144 if version == "mpeg1" else 72) * bitrate) // sample_rate + padding_bit
        total_samples = 1152 if version == "mpeg1" else 576

        if frame_length <= 0:
            offset += 1
            continue

        offset += frame_length
        break

    if total_samples is None or sample_rate is None or bitrate is None:
        raise ValueError(f"Unable to determine MP3 duration for {path}")

    audio_size = len(data) - offset
    bitrate_bps = bitrate
    duration = (audio_size * 8) / bitrate_bps
    return max(duration, total_samples / sample_rate)


powerpoint = comtypes.client.CreateObject("PowerPoint.Application")
powerpoint.Visible = 1
presentation = powerpoint.Presentations.Open(str(PPT_PATH), WithWindow=False)

try:
    for item in slides_data:
        slide_index = int(item["slide"])
        slide = presentation.Slides(slide_index)
        audio_path = AUDIO_DIR / f"slide_{slide_index:02d}.mp3"

        for shape_index in range(slide.Shapes.Count, 0, -1):
            shape = slide.Shapes(shape_index)
            try:
                if shape.Type == 16:
                    shape.Delete()
            except Exception:
                continue

        if audio_path.exists():
            duration_seconds = mp3_duration_seconds(audio_path)

            shape = slide.Shapes.AddMediaObject2(
                str(audio_path),
                MsoTriStateTrue,
                MsoTriStateTrue,
                0,
                0,
            )
            shape.Left = 0
            shape.Top = 0
            shape.Width = 32
            shape.Height = 32

            effect = slide.TimeLine.MainSequence.AddEffect(shape, AUDIO_EFFECT_ID)
            effect.Timing.TriggerType = msoAnimTriggerAfterPrevious

            transition = slide.SlideShowTransition
            transition.AdvanceOnTime = MsoTriStateTrue
            transition.AdvanceTime = round(duration_seconds + BUFFER_SECONDS, 2)
            print(f"slide={slide_index} duration={duration_seconds:.2f} advance={transition.AdvanceTime:.2f}")
        else:
            transition = slide.SlideShowTransition
            transition.AdvanceOnTime = MsoTriStateTrue
            transition.AdvanceTime = 2.0
            print(f"slide={slide_index} duration=0.00 advance=2.00 (no audio)")

    presentation.SaveAs(str(OUTPUT_PPTX))
    print(f"saved_pptx={OUTPUT_PPTX}")
finally:
    presentation.Close()
    powerpoint.Quit()

# Made with Bob
