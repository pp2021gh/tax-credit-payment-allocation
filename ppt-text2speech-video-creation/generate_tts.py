from __future__ import annotations

import json
import os
from pathlib import Path

import requests
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

API_KEY = os.getenv("WATSON_TTS_API_KEY")
SERVICE_URL = os.getenv("WATSON_TTS_URL")
VOICE = "en-GB_GeorgeNatural"

if not API_KEY or not SERVICE_URL:
    raise ValueError("WATSON_TTS_API_KEY and WATSON_TTS_URL must be set in .env file")

notes_path = Path("slide_notes.json")
audio_dir = Path("slide_audio")
audio_dir.mkdir(exist_ok=True)

slides = json.loads(notes_path.read_text(encoding="utf-8"))

session = requests.Session()
session.auth = ("apikey", API_KEY)

for item in slides:
    slide_number = item["slide"]
    text = (item.get("notes") or "").strip()
    if not text:
        print(f"slide {slide_number}: skipped (no notes)")
        continue

    output_path = audio_dir / f"slide_{slide_number:02d}.mp3"
    response = session.post(
        f"{SERVICE_URL}/v1/synthesize",
        params={
            "voice": VOICE,
            "accept": "audio/mp3",
        },
        headers={
            "Content-Type": "application/json",
        },
        json={
            "text": text,
        },
        timeout=120,
    )
    response.raise_for_status()
    output_path.write_bytes(response.content)
    print(f"slide {slide_number}: wrote {output_path}")

# Made with Bob
