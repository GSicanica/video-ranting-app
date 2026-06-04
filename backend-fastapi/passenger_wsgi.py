import asyncio
import gzip
import json
import zlib
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from urllib.parse import parse_qs

from app.config import settings
from app.routers.bible import youversion_passage
from app.routers.csrf import csrf_token
from app.routers.health import health
from app.routers.reports import ReportVideoRequest, report_video


def _json_response(start_response, status: str, payload: dict) -> list[bytes]:
    body = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    start_response(
        status,
        [
            ("Content-Type", "application/json; charset=utf-8"),
            ("Content-Length", str(len(body))),
        ],
    )
    return [body]


def _read_json(environ) -> dict:
    try:
        length = int(environ.get("CONTENT_LENGTH") or 0)
    except ValueError:
        length = 0
    if length <= 0:
        return {}
    body = environ["wsgi.input"].read(length)
    return json.loads(body.decode("utf-8") or "{}")


def _decode_legacy_payload(payload: bytes, encoding: str) -> bytes:
    normalized = encoding.lower().strip()
    if normalized == "gzip":
        return gzip.decompress(payload)
    if normalized == "deflate":
        return zlib.decompress(payload)
    return payload


def _normalize_legacy_response(content_type: str, payload: bytes) -> tuple[str, bytes]:
    if content_type.lower().startswith("application/json"):
        try:
            # Normalize JSON to valid UTF-8 for Ktor clients. Some legacy endpoints
            # negotiate compressed or incorrectly encoded bodies through forwarded headers.
            payload.decode("utf-8")
        except UnicodeDecodeError:
            payload = payload.decode("cp1250", errors="replace").encode("utf-8")
        content_type = "application/json; charset=utf-8"

    return content_type, payload


def _proxy_legacy(environ, start_response, path: str) -> list[bytes]:
    method = environ.get("REQUEST_METHOD", "GET").upper()
    query = environ.get("QUERY_STRING", "")
    target = settings.legacy_base_url.rstrip("/") + path
    if query:
        target = f"{target}?{query}"

    body = b""
    try:
        length = int(environ.get("CONTENT_LENGTH") or 0)
    except ValueError:
        length = 0
    if length > 0:
        body = environ["wsgi.input"].read(length)

    headers = {}
    for key, value in environ.items():
        if not key.startswith("HTTP_"):
            continue
        name = key[5:].replace("_", "-").title()
        if name in {"Host", "Connection", "Accept-Encoding"}:
            continue
        headers[name] = value
    if environ.get("CONTENT_TYPE"):
        headers["Content-Type"] = environ["CONTENT_TYPE"]

    request = Request(target, data=body if method not in {"GET", "HEAD"} else None, headers=headers, method=method)
    try:
        with urlopen(request, timeout=20) as response:
            payload = response.read()
            encoding = response.headers.get("Content-Encoding", "")
            payload = _decode_legacy_payload(payload=payload, encoding=encoding)
            status = f"{response.status} {response.reason}"
            content_type = response.headers.get("Content-Type", "application/octet-stream")
    except HTTPError as exc:
        payload = exc.read()
        encoding = exc.headers.get("Content-Encoding", "")
        payload = _decode_legacy_payload(payload=payload, encoding=encoding)
        status = f"{exc.code} {exc.reason}"
        content_type = exc.headers.get("Content-Type", "application/octet-stream")
    except URLError as exc:
        return _json_response(
            start_response,
            "502 Bad Gateway",
            {"success": False, "message": f"Legacy backend unavailable: {exc.reason}"},
        )

    content_type, payload = _normalize_legacy_response(content_type=content_type, payload=payload)
    start_response(
        status,
        [
            ("Content-Type", content_type),
            ("Content-Length", str(len(payload))),
        ],
    )
    return [payload]


def application(environ, start_response):
    method = environ.get("REQUEST_METHOD", "GET").upper()
    path = environ.get("PATH_INFO") or "/"
    if path.startswith("/backend-fastapi/"):
        path = path.removeprefix("/backend-fastapi")

    try:
        if method == "GET" and path in {"/api/health.php", "/api/health"}:
            payload = asyncio.run(health())
            return _json_response(start_response, "200 OK", payload)

        if method == "GET" and path in {"/api/csrf-token.php", "/api/csrf-token"}:
            payload = asyncio.run(csrf_token())
            return _json_response(start_response, "200 OK", payload)

        if method == "GET" and path in {
            "/api/bible/youversion/passage.php",
            "/api/bible/youversion/passage",
        }:
            query = parse_qs(environ.get("QUERY_STRING", ""))
            payload = asyncio.run(
                youversion_passage(
                    bibleId=query.get("bibleId", [""])[0],
                    passageId=query.get("passageId", [""])[0],
                )
            )
            return _json_response(start_response, "200 OK", payload)

        if method == "POST" and path in {
            "/api/reports/report-video.php",
            "/api/reports/report-video",
        }:
            payload = asyncio.run(report_video(ReportVideoRequest(**_read_json(environ))))
            return _json_response(start_response, "200 OK", payload)

        if path.startswith("/api/livekit/"):
            return _json_response(
                start_response,
                "404 Not Found",
                {"success": False, "message": "Endpoint not found"},
            )

        return _proxy_legacy(environ=environ, start_response=start_response, path=path)

    except Exception as exc:
        return _json_response(
            start_response,
            "500 Internal Server Error",
            {"success": False, "message": str(exc)},
        )
