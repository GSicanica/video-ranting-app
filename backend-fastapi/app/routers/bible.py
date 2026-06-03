import re

import httpx
from fastapi import APIRouter

from app.config import settings

router = APIRouter()

YOUVERSION_BASE_URL = "https://api.youversion.com/v1"
TAG_RE = re.compile(r"<[^>]*>")


def strip_html(value: str) -> str:
    return TAG_RE.sub("", value).strip()


@router.get("/api/bible/youversion/passage.php")
@router.get("/api/bible/youversion/passage")
async def youversion_passage(bibleId: str, passageId: str) -> dict:
    if not settings.youversion_api_key:
        return {
            "success": False,
            "message": "YOUVERSION_API_KEY is not configured",
            "data": None,
        }

    url = f"{YOUVERSION_BASE_URL}/bibles/{bibleId}/passages/{passageId}"
    params = {
        "include_notes": "false",
        "include_headings": "false",
        "include_chapter_numbers": "false",
        "include_verse_numbers": "false",
        "include_short_copyright": "false",
        "include_copyright": "false",
        "format": "text",
    }
    headers = {
        "X-YVP-App-Key": settings.youversion_api_key,
        "Accept": "application/json",
    }

    async with httpx.AsyncClient(timeout=8.0) as client:
        response = await client.get(url, params=params, headers=headers)

    if response.status_code != 200:
        return {
            "success": False,
            "message": f"YouVersion API error: {response.status_code}",
            "data": None,
        }

    payload = response.json()
    content = strip_html(str(payload.get("content", "")))
    return {
        "success": True,
        "message": "Passage fetched",
        "data": {
            "content": content,
            "reference": payload.get("reference"),
        },
    }
