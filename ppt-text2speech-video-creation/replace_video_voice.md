# Video Voice Replacement with Watson Speech to Text and Text to Speech

This workflow replaces the spoken audio track in [`Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4`](ppt-text2speech-video-creation/Bob-a-Thon%20Team%20Belgium%20FOD%20Finance%20AGPR%20Application%20Demo.mp4) by:

1. extracting the original audio with FFmpeg,
2. transcribing it with IBM Watson Speech to Text,
3. synthesizing a new voice with IBM Watson Text to Speech,
4. adjusting the generated narration duration,
5. muxing the new audio back into the original video.

## Script

Use [`replace_video_voice.py`](ppt-text2speech-video-creation/replace_video_voice.py).

## Prerequisites

- Python 3.10+
- FFmpeg and FFprobe available on `PATH`, or set [`FFMPEG_PATH`](ppt-text2speech-video-creation/replace_video_voice.py) and [`FFPROBE_PATH`](ppt-text2speech-video-creation/replace_video_voice.py)
- IBM Watson Speech to Text instance and API key
- IBM Watson Text to Speech instance and API key

## Install dependencies

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install requests
```

## Configuration with `.env`

Yes — the script loads a local [`.env`](ppt-text2speech-video-creation/.env) file automatically from the same folder as [`replace_video_voice.py`](ppt-text2speech-video-creation/replace_video_voice.py:22).

Example [`.env`](ppt-text2speech-video-creation/.env):

```dotenv
WATSON_STT_URL=https://api.eu-de.speech-to-text.watson.cloud.ibm.com/instances/<your-stt-instance>
WATSON_STT_API_KEY=<your-stt-api-key>
WATSON_TTS_URL=https://api.eu-de.text-to-speech.watson.cloud.ibm.com/instances/<your-tts-instance>
WATSON_TTS_API_KEY=<your-tts-api-key>
FFMPEG_PATH=ffmpeg
FFPROBE_PATH=ffprobe
```

You can still set environment variables directly in PowerShell if preferred:

If you get a `Executable not found` error, [`replace_video_voice.py`](ppt-text2speech-video-creation/replace_video_voice.py:48) could not find [`ffmpeg`](ppt-text2speech-video-creation/replace_video_voice.md) or [`ffprobe`](ppt-text2speech-video-creation/replace_video_voice.md). In that case, set absolute paths in [`.env`](ppt-text2speech-video-creation/.env), for example:

```dotenv
FFMPEG_PATH=C:\Users\008424624\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe
FFPROBE_PATH=C:\Users\008424624\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffprobe.exe
```

Then rerun [`replace_video_voice.py`](ppt-text2speech-video-creation/replace_video_voice.py:196).

```powershell
$env:WATSON_STT_URL="https://api.eu-de.speech-to-text.watson.cloud.ibm.com/instances/<your-stt-instance>"
$env:WATSON_STT_API_KEY="<your-stt-api-key>"
$env:WATSON_TTS_URL="https://api.eu-de.text-to-speech.watson.cloud.ibm.com/instances/<your-tts-instance>"
$env:WATSON_TTS_API_KEY="<your-tts-api-key>"
$env:FFMPEG_PATH="ffmpeg"
$env:FFPROBE_PATH="ffprobe"
```

## Run

From [`ppt-text2speech-video-creation`](ppt-text2speech-video-creation):

```powershell
.\.venv\Scripts\python .\replace_video_voice.py
```

Or from the repository root:

```powershell
.\.venv\Scripts\python .\ppt-text2speech-video-creation\replace_video_voice.py
```

## Optional arguments

- [`--input-video`](ppt-text2speech-video-creation/replace_video_voice.py): input video file name
- [`--output-video`](ppt-text2speech-video-creation/replace_video_voice.py): output video file name
- [`--transcript-json`](ppt-text2speech-video-creation/replace_video_voice.py): raw Watson STT response file
- [`--transcript-text`](ppt-text2speech-video-creation/replace_video_voice.py): flattened transcript text file
- [`--stt-model`](ppt-text2speech-video-creation/replace_video_voice.py): STT model, default `en-US_Multimedia`
- [`--tts-voice`](ppt-text2speech-video-creation/replace_video_voice.py): TTS voice, default `en-GB_GeorgeNatural`
- [`--max-speed-change-ratio`](ppt-text2speech-video-creation/replace_video_voice.py): limits how much the synthesized speech may be sped up or slowed down to stay aligned, default `1.15`
- [`--keep-temp`](ppt-text2speech-video-creation/replace_video_voice.py): keep extracted and generated temporary audio files

Example:

```powershell
.\.venv\Scripts\python .\ppt-text2speech-video-creation\replace_video_voice.py `
  --input-video "Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4" `
  --output-video "Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo_revoiced.mp4" `
  --stt-model "en-US_Multimedia" `
  --tts-voice "en-GB_GeorgeNatural" `
  --max-speed-change-ratio 1.10 `
  --keep-temp
```

## Outputs

The script produces:

- [`Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo_revoiced.mp4`](ppt-text2speech-video-creation/Bob-a-Thon%20Team%20Belgium%20FOD%20Finance%20AGPR%20Application%20Demo_revoiced.mp4) by default
- [`watson_stt_transcript.json`](ppt-text2speech-video-creation/watson_stt_transcript.json)
- [`watson_stt_transcript.txt`](ppt-text2speech-video-creation/watson_stt_transcript.txt)

## Notes

- The script currently replaces the full audio track with synthesized speech and keeps the original video stream unchanged.
- [`align_audio_to_duration()`](ppt-text2speech-video-creation/replace_video_voice.py:181) now uses a hybrid strategy: it allows a limited speed adjustment to improve sync, then pads any remaining gap with silence.
- This avoids the two extremes you observed: fully stretched speech that sounds unnatural, or fully natural speech that finishes far too early.
- Tune [`--max-speed-change-ratio`](ppt-text2speech-video-creation/replace_video_voice.py) if needed. Lower values preserve a more natural voice, while higher values improve sync at the cost of more audible stretching.
- If the source video contains background music or sound effects mixed into the speech track, those will also be removed because the audio track is fully replaced.

# Made with Bob