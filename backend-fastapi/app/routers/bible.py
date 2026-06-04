import re

import httpx
from fastapi import APIRouter

from app.config import settings

router = APIRouter()

YOUVERSION_BASE_URL = "https://api.youversion.com/v1"
BIBLE_API_BASE_URL = "https://bible-api.com"
TAG_RE = re.compile(r"<[^>]*>")

USFM_TO_BIBLE_API_BOOK = {
    "GEN": "Genesis",
    "EXO": "Exodus",
    "LEV": "Leviticus",
    "NUM": "Numbers",
    "DEU": "Deuteronomy",
    "JOS": "Joshua",
    "JDG": "Judges",
    "RUT": "Ruth",
    "1SA": "1 Samuel",
    "2SA": "2 Samuel",
    "1KI": "1 Kings",
    "2KI": "2 Kings",
    "1CH": "1 Chronicles",
    "2CH": "2 Chronicles",
    "EZR": "Ezra",
    "NEH": "Nehemiah",
    "EST": "Esther",
    "JOB": "Job",
    "PSA": "Psalms",
    "PRO": "Proverbs",
    "ECC": "Ecclesiastes",
    "SNG": "Song of Solomon",
    "ISA": "Isaiah",
    "JER": "Jeremiah",
    "LAM": "Lamentations",
    "EZK": "Ezekiel",
    "DAN": "Daniel",
    "HOS": "Hosea",
    "JOL": "Joel",
    "AMO": "Amos",
    "OBA": "Obadiah",
    "JON": "Jonah",
    "MIC": "Micah",
    "NAM": "Nahum",
    "HAB": "Habakkuk",
    "ZEP": "Zephaniah",
    "HAG": "Haggai",
    "ZEC": "Zechariah",
    "MAL": "Malachi",
    "MAT": "Matthew",
    "MRK": "Mark",
    "LUK": "Luke",
    "JHN": "John",
    "ACT": "Acts",
    "ROM": "Romans",
    "1CO": "1 Corinthians",
    "2CO": "2 Corinthians",
    "GAL": "Galatians",
    "EPH": "Ephesians",
    "PHP": "Philippians",
    "COL": "Colossians",
    "1TH": "1 Thessalonians",
    "2TH": "2 Thessalonians",
    "1TI": "1 Timothy",
    "2TI": "2 Timothy",
    "TIT": "Titus",
    "PHM": "Philemon",
    "HEB": "Hebrews",
    "JAS": "James",
    "1PE": "1 Peter",
    "2PE": "2 Peter",
    "1JN": "1 John",
    "2JN": "2 John",
    "3JN": "3 John",
    "JUD": "Jude",
    "REV": "Revelation",
}


def strip_html(value: str) -> str:
    return TAG_RE.sub("", value).strip()


def passage_id_to_reference(passage_id: str) -> str | None:
    parts = [part.strip() for part in passage_id.split(".") if part.strip()]
    if len(parts) < 2:
        return None

    book = USFM_TO_BIBLE_API_BOOK.get(parts[0].upper())
    if book is None:
        return None

    chapter = parts[1]
    verse = parts[2] if len(parts) >= 3 else None
    return f"{book} {chapter}:{verse}" if verse else f"{book} {chapter}"


async def fetch_bible_api_fallback(passage_id: str) -> dict:
    reference = passage_id_to_reference(passage_id)
    if reference is None:
        return {
            "success": False,
            "message": f"Unsupported passage id: {passage_id}",
            "data": None,
        }

    async with httpx.AsyncClient(timeout=8.0) as client:
        response = await client.get(
            f"{BIBLE_API_BASE_URL}/{reference}",
            params={"translation": "web"},
            headers={"Accept": "application/json"},
        )

    if response.status_code != 200:
        return {
            "success": False,
            "message": f"Bible fallback API error: {response.status_code}",
            "data": None,
        }

    payload = response.json()
    content = str(payload.get("text", "")).strip()
    if not content:
        return {
            "success": False,
            "message": "Bible fallback returned empty content",
            "data": None,
        }

    return {
        "success": True,
        "message": "Passage fetched",
        "data": {
            "content": content,
            "reference": payload.get("reference") or reference,
        },
    }


@router.get("/api/bible/youversion/passage.php")
@router.get("/api/bible/youversion/passage")
async def youversion_passage(bibleId: str, passageId: str) -> dict:
    if not settings.youversion_api_key:
        return await fetch_bible_api_fallback(passage_id=passageId)

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
        return await fetch_bible_api_fallback(passage_id=passageId)

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
