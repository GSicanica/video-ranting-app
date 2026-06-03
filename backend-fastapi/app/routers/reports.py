import time
from hashlib import sha256

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter()


class ReportVideoRequest(BaseModel):
    video_id: str | None = Field(default=None, alias="videoId")
    user_token: str | None = Field(default=None, alias="userToken")
    device_id: str | None = Field(default=None, alias="deviceId")
    reason: str = "manual_user_report"


@router.post("/api/reports/report-video.php")
@router.post("/api/reports/report-video")
async def report_video(request: ReportVideoRequest) -> dict:
    if not request.video_id:
        return {"success": False, "message": "Missing videoId"}
    if not request.user_token:
        return {"success": False, "message": "Missing userToken"}

    report_hash = sha256(
        f"{request.video_id}:{request.user_token}:{time.time_ns()}".encode("utf-8")
    ).hexdigest()[:16]
    return {
        "success": True,
        "message": "Report accepted",
        "reportId": report_hash,
    }
