import base64
import json

from app.config import Settings
from app.routers import livekit
from fastapi.testclient import TestClient

from app.main import create_app


def _decode_payload(token: str) -> dict:
    payload = token.split(".")[1]
    padded = payload + "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(padded.encode("ascii")))


def test_livekit_token_uses_backend_secret_without_returning_it(monkeypatch):
    monkeypatch.setattr(
        livekit,
        "settings",
        Settings(
            livekit_api_key="server-api-key",
            livekit_api_secret="server-api-secret",
            livekit_url="wss://livekit.example.com",
        ),
    )

    client = TestClient(create_app())
    response = client.post(
        "/api/livekit/token.php",
        json={
            "userToken": "user-token",
            "room": "room-1",
            "identity": "user-1",
            "name": "User One",
        },
    )

    body = response.json()
    assert body["success"] is True
    assert body["url"] == "wss://livekit.example.com"
    assert "server-api-secret" not in json.dumps(body)

    payload = _decode_payload(body["token"])
    assert payload["iss"] == "server-api-key"
    assert payload["sub"] == "user-1"
    assert payload["video"]["room"] == "room-1"
    assert payload["video"]["roomJoin"] is True


def test_livekit_token_requires_backend_configuration(monkeypatch):
    monkeypatch.setattr(livekit, "settings", Settings())

    client = TestClient(create_app())
    response = client.post(
        "/api/livekit/token.php",
        json={
            "userToken": "user-token",
            "room": "room-1",
            "identity": "user-1",
            "name": "User One",
        },
    )

    assert response.json() == {
        "success": False,
        "message": "LiveKit is not configured",
    }
