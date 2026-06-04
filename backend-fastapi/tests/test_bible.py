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


class _FakeFallbackResponse:
    status_code = 200

    def json(self) -> dict:
        return {
            "reference": "John 3:16",
            "text": "\nFor God so loved the world\n",
        }


class _FakeFallbackAsyncClient:
    requested_url = None
    requested_params = None

    def __init__(self, *args, **kwargs):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, exc_type, exc, traceback):
        return None

    async def get(self, url, params, headers):
        self.__class__.requested_url = url
        self.__class__.requested_params = params
        return _FakeFallbackResponse()


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


def test_youversion_proxy_falls_back_when_backend_key_is_missing(monkeypatch):
    monkeypatch.setattr(bible, "settings", Settings(youversion_api_key=""))
    monkeypatch.setattr(bible.httpx, "AsyncClient", _FakeFallbackAsyncClient)

    client = TestClient(create_app())
    response = client.get(
        "/api/bible/youversion/passage.php",
        params={"bibleId": "206", "passageId": "JHN.3.16"},
    )

    body = response.json()
    assert body["success"] is True
    assert body["data"]["content"] == "For God so loved the world"
    assert body["data"]["reference"] == "John 3:16"
    assert _FakeFallbackAsyncClient.requested_url == "https://bible-api.com/John 3:16"
    assert _FakeFallbackAsyncClient.requested_params == {"translation": "web"}
