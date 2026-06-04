# FastAPI Backend

Migration backend for the Android/KMP app.

## Run locally

```bash
cd backend-fastapi
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
export YOUVERSION_API_KEY="your-key"
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

For Android debug builds, set `BASE_URL=http://10.0.2.2:8000` in `local.properties`
when running on the Android emulator.

## Implemented endpoints

- `GET /api/health.php`
- `GET /api/csrf-token.php`
- `POST /api/reports/report-video.php`
- `GET /api/bible/youversion/passage.php?bibleId=206&passageId=JHN.3.16`
  Uses `YOUVERSION_API_KEY` when configured and falls back to bible-api.com
  WEB text when the key is missing or YouVersion returns an error.

## Environment

- `YOUVERSION_API_KEY`: server-side YouVersion API key.
- `LEGACY_BASE_URL`: fallback PHP backend for endpoints not yet migrated to
  FastAPI. Defaults to `https://tmbv-hms.com/aYOUTUBEocjenivanje5`.
- `CSRF_TOKEN`: static development CSRF token until a real session store is added.
- `APP_ENV`: environment label returned by health checks.
