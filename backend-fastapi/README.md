# FastAPI Backend

Migration backend for the Android/KMP app.

## Run locally

```bash
cd backend-fastapi
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
export YOUVERSION_API_KEY="your-key"
export LIVEKIT_API_KEY="your-livekit-api-key"
export LIVEKIT_API_SECRET="your-livekit-api-secret"
export LIVEKIT_URL="wss://your-livekit-host"
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

For Android debug builds, set `BASE_URL=http://10.0.2.2:8000` in `local.properties`
when running on the Android emulator.

## Implemented endpoints

- `GET /api/health.php`
- `GET /api/csrf-token.php`
- `POST /api/reports/report-video.php`
- `GET /api/bible/youversion/passage.php?bibleId=206&passageId=JHN.3.16`
- `POST /api/livekit/token.php`

## Environment

- `YOUVERSION_API_KEY`: server-side YouVersion API key.
- `LIVEKIT_API_KEY`: server-side LiveKit API key used to sign room tokens.
- `LIVEKIT_API_SECRET`: server-side LiveKit secret used to sign room tokens.
- `LIVEKIT_URL`: LiveKit websocket URL returned to the app.
- `CSRF_TOKEN`: static development CSRF token until a real session store is added.
- `APP_ENV`: environment label returned by health checks.
