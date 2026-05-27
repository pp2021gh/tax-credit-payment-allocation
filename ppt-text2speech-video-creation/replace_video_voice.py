from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

import requests


DEFAULT_STT_MODEL = "en-US_Multimedia"
DEFAULT_TTS_VOICE = "en-GB_GeorgeNatural"
DEFAULT_TTS_ACCEPT = "audio/mp3"
ENV_FILE_NAME = ".env"


def load_dotenv(dotenv_path: Path) -> None:
    if not dotenv_path.exists():
        return

    for raw_line in dotenv_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()

        if value and len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]

        os.environ.setdefault(key, value)


def require_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def run_command(command: list[str]) -> None:
    print("Running:", subprocess.list2cmdline(command))
    try:
        subprocess.run(command, check=True)
    except FileNotFoundError as exc:
        executable = command[0] if command else "<unknown>"
        raise RuntimeError(
            f"Executable not found: {executable}. Install it or set the matching path in .env "
            f"using FFMPEG_PATH or FFPROBE_PATH."
        ) from exc


def ffmpeg_executable() -> str:
    return os.getenv("FFMPEG_PATH", "ffmpeg")


def ffprobe_executable() -> str:
    return os.getenv("FFPROBE_PATH", "ffprobe")


def extract_audio_from_video(video_path: Path, audio_path: Path) -> None:
    run_command(
        [
            ffmpeg_executable(),
            "-y",
            "-i",
            str(video_path),
            "-vn",
            "-acodec",
            "mp3",
            str(audio_path),
        ]
    )


def probe_duration_seconds(media_path: Path) -> float:
    command = [
        ffprobe_executable(),
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        str(media_path),
    ]
    try:
        result = subprocess.run(command, check=True, capture_output=True, text=True)
    except FileNotFoundError as exc:
        executable = command[0] if command else "<unknown>"
        raise RuntimeError(
            f"Executable not found: {executable}. Install it or set the matching path in .env "
            f"using FFMPEG_PATH or FFPROBE_PATH."
        ) from exc
    return float(result.stdout.strip())


def transcribe_audio(audio_path: Path, stt_url: str, stt_api_key: str, model: str) -> dict[str, Any]:
    with audio_path.open("rb") as audio_file:
        response = requests.post(
            f"{stt_url.rstrip('/')}/v1/recognize",
            params={
                "model": model,
                "timestamps": "true",
                "speaker_labels": "false",
                "smart_formatting": "true",
            },
            headers={
                "Content-Type": "audio/mp3",
            },
            auth=("apikey", stt_api_key),
            data=audio_file,
            timeout=600,
        )
    response.raise_for_status()
    return response.json()


def flatten_transcript(stt_result: dict[str, Any]) -> str:
    parts: list[str] = []
    for result in stt_result.get("results", []):
        alternatives = result.get("alternatives", [])
        if not alternatives:
            continue
        transcript = alternatives[0].get("transcript", "").strip()
        if transcript:
            parts.append(transcript)
    return " ".join(parts).strip()


def synthesize_speech(
    text: str,
    output_audio_path: Path,
    tts_url: str,
    tts_api_key: str,
    voice: str,
    accept: str,
) -> None:
    response = requests.post(
        f"{tts_url.rstrip('/')}/v1/synthesize",
        params={
            "voice": voice,
            "accept": accept,
        },
        headers={
            "Content-Type": "application/json",
        },
        auth=("apikey", tts_api_key),
        json={
            "text": text,
        },
        timeout=600,
    )
    response.raise_for_status()
    output_audio_path.write_bytes(response.content)


def build_atempo_filter(speed_ratio: float) -> str:
    if speed_ratio <= 0:
        raise RuntimeError(f"Invalid speed ratio: {speed_ratio}")

    factors: list[float] = []
    remaining = speed_ratio

    while remaining > 2.0:
        factors.append(2.0)
        remaining /= 2.0

    while remaining < 0.5:
        factors.append(0.5)
        remaining /= 0.5

    factors.append(remaining)
    return ",".join(f"atempo={factor:.6f}" for factor in factors)


def align_audio_to_duration(
    input_audio_path: Path,
    output_audio_path: Path,
    target_duration: float,
    max_speed_change_ratio: float,
) -> None:
    source_duration = probe_duration_seconds(input_audio_path)
    if source_duration <= 0:
        raise RuntimeError(f"Invalid synthesized audio duration for {input_audio_path}")
    if target_duration <= 0:
        raise RuntimeError(f"Invalid target duration: {target_duration}")

    speed_ratio = source_duration / target_duration
    min_ratio = 1.0 / max_speed_change_ratio
    max_ratio = max_speed_change_ratio
    adjusted_ratio = min(max(speed_ratio, min_ratio), max_ratio)

    audio_filter_parts = [build_atempo_filter(adjusted_ratio)]
    adjusted_duration = source_duration / adjusted_ratio

    if adjusted_duration < target_duration:
        pad_duration = target_duration - adjusted_duration
        audio_filter_parts.append(f"apad=pad_dur={pad_duration:.6f}")

    run_command(
        [
            ffmpeg_executable(),
            "-y",
            "-i",
            str(input_audio_path),
            "-af",
            ",".join(audio_filter_parts),
            "-t",
            f"{target_duration:.6f}",
            "-vn",
            str(output_audio_path),
        ]
    )


def replace_video_audio(video_path: Path, audio_path: Path, output_video_path: Path) -> None:
    run_command(
        [
            ffmpeg_executable(),
            "-y",
            "-i",
            str(video_path),
            "-i",
            str(audio_path),
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-shortest",
            str(output_video_path),
        ]
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Replace speech in a video by transcribing with Watson STT and synthesizing with Watson TTS."
    )
    parser.add_argument(
        "--input-video",
        default="Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4",
        help="Input video path",
    )
    parser.add_argument(
        "--output-video",
        default="Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo_revoiced.mp4",
        help="Output video path",
    )
    parser.add_argument(
        "--transcript-json",
        default="watson_stt_transcript.json",
        help="Path to save the raw STT response JSON",
    )
    parser.add_argument(
        "--transcript-text",
        default="watson_stt_transcript.txt",
        help="Path to save the flattened transcript text",
    )
    parser.add_argument(
        "--stt-model",
        default=DEFAULT_STT_MODEL,
        help="Watson Speech to Text model",
    )
    parser.add_argument(
        "--tts-voice",
        default=DEFAULT_TTS_VOICE,
        help="Watson Text to Speech voice",
    )
    parser.add_argument(
        "--max-speed-change-ratio",
        type=float,
        default=1.15,
        help="Maximum allowed speech speed adjustment ratio before padding the remainder with silence",
    )
    parser.add_argument(
        "--keep-temp",
        action="store_true",
        help="Keep temporary extracted and synthesized audio files",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    base_dir = Path(__file__).resolve().parent

    input_video = (base_dir / args.input_video).resolve()
    output_video = (base_dir / args.output_video).resolve()
    transcript_json_path = (base_dir / args.transcript_json).resolve()
    transcript_text_path = (base_dir / args.transcript_text).resolve()

    if not input_video.exists():
        raise FileNotFoundError(f"Input video not found: {input_video}")

    load_dotenv(base_dir / ENV_FILE_NAME)

    stt_url = require_env("WATSON_STT_URL")
    stt_api_key = require_env("WATSON_STT_API_KEY")
    tts_url = require_env("WATSON_TTS_URL")
    tts_api_key = require_env("WATSON_TTS_API_KEY")

    temp_dir_path = Path(tempfile.mkdtemp(prefix="video-revoice-", dir=str(base_dir)))
    extracted_audio = temp_dir_path / "original_audio.mp3"
    synthesized_audio = temp_dir_path / "synthesized_audio.mp3"
    adjusted_audio = temp_dir_path / "adjusted_audio.mp3"

    try:
        print(f"Input video: {input_video}")
        extract_audio_from_video(input_video, extracted_audio)

        print("Submitting audio to Watson Speech to Text...")
        stt_result = transcribe_audio(extracted_audio, stt_url, stt_api_key, args.stt_model)
        transcript_json_path.write_text(json.dumps(stt_result, ensure_ascii=False, indent=2), encoding="utf-8")

        transcript_text = flatten_transcript(stt_result)
        if not transcript_text:
            raise RuntimeError("Speech to Text returned an empty transcript.")
        transcript_text_path.write_text(transcript_text, encoding="utf-8")
        print(f"Transcript saved to {transcript_text_path}")

        print("Submitting transcript to Watson Text to Speech...")
        synthesize_speech(
            transcript_text,
            synthesized_audio,
            tts_url,
            tts_api_key,
            args.tts_voice,
            DEFAULT_TTS_ACCEPT,
        )

        original_duration = probe_duration_seconds(extracted_audio)
        print(f"Original audio duration: {original_duration:.2f}s")
        align_audio_to_duration(
            synthesized_audio,
            adjusted_audio,
            original_duration,
            args.max_speed_change_ratio,
        )

        print("Muxing synthesized audio back into the video...")
        replace_video_audio(input_video, adjusted_audio, output_video)
        print(f"Output video written to {output_video}")
        print(f"Raw STT JSON written to {transcript_json_path}")
        return 0
    finally:
        if args.keep_temp:
            print(f"Temporary files kept in {temp_dir_path}")
        else:
            shutil.rmtree(temp_dir_path, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())

# Made with Bob