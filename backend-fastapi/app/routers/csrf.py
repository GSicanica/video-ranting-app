from fastapi import APIRouter

from app.config import settings

router = APIRouter()


@router.get("/api/csrf-token.php")
@router.get("/api/csrf-token")
async def csrf_token() -> dict:
    return {
        "success": True,
        "message": "CSRF token issued",
        "data": settings.csrf_token,
    }
