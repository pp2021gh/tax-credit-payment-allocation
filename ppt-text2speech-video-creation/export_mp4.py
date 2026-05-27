import time
from pathlib import Path

import comtypes.client


base = Path.cwd()
ppt = base / "FodFin AGPR_with_audio.pptx"
mp4 = base / "generated_presentation.mp4"

app = comtypes.client.CreateObject("PowerPoint.Application")
app.Visible = 1
presentation = app.Presentations.Open(str(ppt), WithWindow=False)

try:
    presentation.CreateVideo(str(mp4), True, 5, 1280, 30, 85)

    for _ in range(900):
        try:
            status = presentation.CreateVideoStatus
        except Exception:
            status = None

        size = mp4.stat().st_size if mp4.exists() else 0
        print(f"status={status} size={size}")

        if status == 3 and size > 0:
            print(f"mp4_ready={mp4} size={size}")
            break

        time.sleep(2)
    else:
        print("mp4_not_ready")
finally:
    presentation.Close()
    app.Quit()

# Made with Bob
