import time

from fastapi import APIRouter

from app.config import settings

router = APIRouter()


@router.get("/api/health.php")
@router.get("/api/health")
async def health() -> dict:
    return {
        "success": True,
        "status": "ok",
        "timestamp": int(time.time()),
        "environment": settings.environment,
        "checks": {
            "api": "ok",
            "youversion_key_configured": bool(settings.youversion_api_key),
        },
    }
