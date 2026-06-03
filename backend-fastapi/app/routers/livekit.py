import base64
import hmac
import json
import time
from hashlib import sha256
from typing import Any

from fastapi import APIRouter
from pydantic import BaseModel, Field

from app.config import settings

router = APIRouter()


class LiveKitTokenRequest(BaseModel):
    user_token: str = Field(alias="userToken")
    room: str
    identity: str
    name: str


def _base64_url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _jwt_hs256(payload: dict[str, Any], secret: str) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    encoded_header = _base64_url_encode(
        json.dumps(header, separators=(",", ":")).encode("utf-8")
    )
    encoded_payload = _base64_url_encode(
        json.dumps(payload, separators=(",", ":")).encode("utf-8")
    )
    signing_input = f"{encoded_header}.{encoded_payload}".encode("ascii")
    signature = hmac.new(secret.encode("utf-8"), signing_input, sha256).digest()
    return f"{encoded_header}.{encoded_payload}.{_base64_url_encode(signature)}"


@router.post("/api/livekit/token.php")
@router.post("/api/livekit/token")
async def livekit_token(request: LiveKitTokenRequest) -> dict:
    if not settings.livekit_api_key or not settings.livekit_api_secret:
        return {"success": False, "message": "LiveKit is not configured"}
    if not settings.livekit_url:
        return {"success": False, "message": "LIVEKIT_URL is not configured"}
    if not request.user_token.strip():
        return {"success": False, "message": "Missing userToken"}
    if not request.room.strip():
        return {"success": False, "message": "Missing room"}
    if not request.identity.strip():
        return {"success": False, "message": "Missing identity"}

    now = int(time.time())
    payload = {
        "iss": settings.livekit_api_key,
        "sub": request.identity.strip(),
        "name": request.name.strip() or request.identity.strip(),
        "nbf": now - 5,
        "iat": now,
        "exp": now + 3600,
        "video": {
            "roomJoin": True,
            "room": request.room.strip(),
            "canPublish": True,
            "canSubscribe": True,
        },
    }
    token = _jwt_hs256(payload=payload, secret=settings.livekit_api_secret)
    return {
        "success": True,
        "message": "LiveKit token issued",
        "token": token,
        "url": settings.livekit_url,
    }
