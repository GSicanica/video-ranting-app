import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    environment: str = os.getenv("APP_ENV", "development")
    youversion_api_key: str = os.getenv("YOUVERSION_API_KEY", "")
    livekit_api_key: str = os.getenv("LIVEKIT_API_KEY", "")
    livekit_api_secret: str = os.getenv("LIVEKIT_API_SECRET", "")
    livekit_url: str = os.getenv("LIVEKIT_URL", "")
    csrf_token: str = os.getenv("CSRF_TOKEN", "local-dev-csrf-token")


settings = Settings()
