from app.config import Settings
from app.routers import bible
from fastapi.testclient import TestClient

from app.main import create_app


class _FakeResponse:
    status_code = 200

    def json(self) -> dict:
        return {
            "content": "<p>For God so loved the world</p>",
            "reference": "John 3:16",
        }


class _FakeAsyncClient:
    def __init__(self, *args, **kwargs):
        self.request_headers = None

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, traceback):
        return None

    async def get(self, url, params, headers):
        self.request_headers = headers
        assert headers["X-YVP-App-Key"] == "server-youversion-key"
        return _FakeResponse()


def test_youversion_proxy_uses_backend_key_without_returning_it(monkeypatch):
    monkeypatch.setattr(
        bible,
        "settings",
        Settings(youversion_api_key="server-youversion-key"),
    )
    monkeypatch.setattr(bible.httpx, "AsyncClient", _FakeAsyncClient)

    client = TestClient(create_app())
    response = client.get(
        "/api/bible/youversion/passage.php",
        params={"bibleId": "206", "passageId": "JHN.3.16"},
    )

    body = response.json()
    assert body["success"] is True
    assert body["data"]["content"] == "For God so loved the world"
    assert body["data"]["reference"] == "John 3:16"
    assert "server-youversion-key" not in response.text
