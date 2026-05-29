from __future__ import annotations

import json
import subprocess
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
    """Get audio duration using ffprobe for accurate results."""
    try:
        result = subprocess.run(
            [
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                str(path)
            ],
            capture_output=True,
            text=True,
            check=True
        )
        duration = float(result.stdout.strip())
        return duration
    except (subprocess.CalledProcessError, ValueError, FileNotFoundError) as e:
        raise ValueError(f"Unable to determine MP3 duration for {path}: {e}")


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
