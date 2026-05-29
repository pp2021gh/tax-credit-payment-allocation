# Final Video Creation Steps

This document lists the exact steps, scripts, inputs, and commands used to produce [`demo_presentation_AGPR.mp4`](demo_presentation_AGPR.mp4).

## Inputs

- PowerPoint source: [`FodFin AGPR.pptx`](FodFin AGPR.pptx)
- Existing demo video: [`Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4`](Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4)

## Configuration

### Environment Variables

The project uses a [`.env`](.env) file to store API credentials and configuration. This file contains:

- **IBM Watson Speech-to-Text credentials:**
  - `WATSON_STT_URL`: Watson Speech-to-Text service endpoint
  - `WATSON_STT_API_KEY`: API key for Speech-to-Text authentication

- **IBM Watson Text-to-Speech credentials:**
  - `WATSON_TTS_URL`: Watson Text-to-Speech service endpoint
  - `WATSON_TTS_API_KEY`: API key for Text-to-Speech authentication

- **FFmpeg configuration:**
  - `FFMPEG_PATH`: Path to FFmpeg executable (default: `ffmpeg.exe`)
  - `FFPROBE_PATH`: Path to FFprobe executable (default: `ffprobe.exe`)

**Example `.env` file:**

```env
WATSON_STT_URL=https://api.eu-de.speech-to-text.watson.cloud.ibm.com/instances/YOUR_STT_INSTANCE_ID
WATSON_STT_API_KEY=your_stt_api_key_here
WATSON_TTS_URL=https://api.eu-de.text-to-speech.watson.cloud.ibm.com/instances/YOUR_TTS_INSTANCE_ID
WATSON_TTS_API_KEY=your_tts_api_key_here
FFMPEG_PATH=ffmpeg.exe
FFPROBE_PATH=ffprobe.exe
```

**Important:** The `.env` file should never be committed to version control as it contains sensitive credentials. Ensure it's listed in [`.gitignore`](../.gitignore).

### Voice Configuration

The Text-to-Speech voice used is: `en-GB_GeorgeNatural`

This can be modified in [`generate_tts.py`](generate_tts.py) if a different voice is desired.

## Prerequisites

### 1. Create a Python virtual environment

```powershell
python -m venv .venv
```

### 2. Install Python dependencies

```powershell
.\.venv\Scripts\python -m pip install python-pptx requests lxml comtypes pywin32
```

### 3. Install FFmpeg

FFmpeg was installed via `winget`:

```powershell
winget install --id Gyan.FFmpeg --accept-source-agreements --accept-package-agreements
```

Installed executable used in the final command:

```text
C:\Users\008424624\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe
```

### 4. Microsoft PowerPoint

Microsoft PowerPoint was required for embedding media and exporting the slideshow to video through COM automation.

Detected executable:

```text
C:\Program Files\Microsoft Office\root\Office16\POWERPNT.EXE
```

---

## Step 1: Extract slide speaker notes

Script used: [`extract_notes.py`](extract_notes.py)

Command:

```powershell
.\.venv\Scripts\python .\extract_notes.py
```

Output produced:

- [`slide_notes.json`](slide_notes.json)

Purpose:

- Opens [`FodFin AGPR.pptx`](FodFin AGPR.pptx)
- Reads notes from each slide
- Saves them as structured JSON in [`slide_notes.json`](slide_notes.json)

---

## Step 2: Generate audio from notes with IBM Watson TTS

Script used: [`generate_tts.py`](generate_tts.py)

Command:

```powershell
.\.venv\Scripts\python .\generate_tts.py
```

Outputs produced:

- [`slide_audio/slide_01.mp3`](slide_audio/slide_01.mp3)
- [`slide_audio/slide_02.mp3`](slide_audio/slide_02.mp3)
- [`slide_audio/slide_04.mp3`](slide_audio/slide_04.mp3)
- [`slide_audio/slide_05.mp3`](slide_audio/slide_05.mp3)
- [`slide_audio/slide_06.mp3`](slide_audio/slide_06.mp3)
- [`slide_audio/slide_07.mp3`](slide_audio/slide_07.mp3)
- [`slide_audio/slide_08.mp3`](slide_audio/slide_08.mp3)
- [`slide_audio/slide_09.mp3`](slide_audio/slide_09.mp3)
- [`slide_audio/slide_10.mp3`](slide_audio/slide_10.mp3)
- [`slide_audio/slide_11.mp3`](slide_audio/slide_11.mp3)
- [`slide_audio/slide_12.mp3`](slide_audio/slide_12.mp3)
- [`slide_audio/slide_13.mp3`](slide_audio/slide_13.mp3)

Purpose:

- Reads notes from [`slide_notes.json`](slide_notes.json)
- Calls the IBM Watson `/v1/synthesize` API
- Saves one MP3 per slide in [`slide_audio`](slide_audio)

---

## Step 3: Embed audio into the PowerPoint and configure auto-advance

Script used: [`build_presentation.py`](build_presentation.py)

Command:

```powershell
.\.venv\Scripts\python .\build_presentation.py
```

Outputs produced:

- [`FodFin AGPR_with_audio.pptx`](FodFin AGPR_with_audio.pptx)
- Initial video target path: [`generated_presentation.mp4`](generated_presentation.mp4)

Purpose:

- Opens [`FodFin AGPR.pptx`](FodFin AGPR.pptx) in PowerPoint
- Inserts each corresponding MP3 from [`slide_audio`](slide_audio)
- Configures playback in the slideshow timeline
- Enables slide transition on timing
- Saves the updated deck as [`FodFin AGPR_with_audio.pptx`](FodFin AGPR_with_audio.pptx)

---

## Step 4: Export the updated PowerPoint to MP4

Script used: [`export_mp4.py`](export_mp4.py)

Command:

```powershell
.\.venv\Scripts\python .\export_mp4.py
```

Output produced:

- [`generated_presentation.mp4`](generated_presentation.mp4)

Purpose:

- Opens [`FodFin AGPR_with_audio.pptx`](FodFin AGPR_with_audio.pptx)
- Calls PowerPoint video export via `CreateVideo`
- Polls PowerPoint until the MP4 is fully rendered

---

## Step 5: Concatenate the generated presentation video with the existing demo video

Final working command used:

```powershell
$ffmpeg="C:\Users\008424624\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe"; & $ffmpeg -y -i .\generated_presentation.mp4 -i ".\Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4" -filter_complex "[0:v]fps=30,scale=2276:1280:force_original_aspect_ratio=decrease,pad=2276:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v0];[1:v]fps=30,scale=2276:1280:force_original_aspect_ratio=decrease,pad=2276:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v1];[0:a]aresample=44100[a0];[1:a]aresample=44100[a1];[v0][a0][v1][a1]concat=n=2:v=1:a=1[v][a]" -map "[v]" -map "[a]" -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p -c:a aac -b:a 192k .\demo_presentation_AGPR.mp4
```

Final output produced:

- [`demo_presentation_AGPR.mp4`](demo_presentation_AGPR.mp4)

Purpose:

- Loads [`generated_presentation.mp4`](generated_presentation.mp4)
- Loads [`Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4`](Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4)
- Normalizes frame rate, size, aspect ratio, and audio sample rate
- Concatenates both videos into one output file
- Re-encodes to a stable final MP4

---

## Intermediate files created

- [`slide_notes.json`](slide_notes.json)
- [`slide_audio`](slide_audio)
- [`FodFin AGPR_with_audio.pptx`](FodFin AGPR_with_audio.pptx)
- [`generated_presentation.mp4`](generated_presentation.mp4)
- [`concat_list.txt`](concat_list.txt) (created during an earlier concat attempt)

## Final deliverable

- [`demo_presentation_AGPR.mp4`](demo_presentation_AGPR.mp4)

## Script summary

- [`extract_notes.py`](extract_notes.py): extracts speaker notes from PowerPoint
- [`generate_tts.py`](generate_tts.py): converts notes to MP3 with IBM Watson TTS
- [`build_presentation.py`](build_presentation.py): embeds MP3 files into the deck
- [`export_mp4.py`](export_mp4.py): exports the updated deck to MP4

## End-to-end command sequence

Run these commands in order from the project root:

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install python-pptx requests lxml comtypes pywin32
winget install --id Gyan.FFmpeg --accept-source-agreements --accept-package-agreements
.\.venv\Scripts\python .\extract_notes.py
.\.venv\Scripts\python .\generate_tts.py
.\.venv\Scripts\python .\build_presentation.py
.\.venv\Scripts\python .\export_mp4.py
$ffmpeg="C:\Users\008424624\AppData\Local\Microsoft\WinGet\Packages\Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe\ffmpeg-8.1.1-full_build\bin\ffmpeg.exe"; & $ffmpeg -y -i .\generated_presentation.mp4 -i ".\Bob-a-Thon Team Belgium FOD Finance AGPR Application Demo.mp4" -filter_complex "[0:v]fps=30,scale=2276:1280:force_original_aspect_ratio=decrease,pad=2276:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v0];[1:v]fps=30,scale=2276:1280:force_original_aspect_ratio=decrease,pad=2276:1280:(ow-iw)/2:(oh-ih)/2,setsar=1[v1];[0:a]aresample=44100[a0];[1:a]aresample=44100[a1];[v0][a0][v1][a1]concat=n=2:v=1:a=1[v][a]" -map "[v]" -map "[a]" -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p -c:a aac -b:a 192k .\demo_presentation_AGPR.mp4