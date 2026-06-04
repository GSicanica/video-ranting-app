import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    environment: str = os.getenv("APP_ENV", "development")
    youversion_api_key: str = os.getenv("YOUVERSION_API_KEY", "")
    legacy_base_url: str = os.getenv(
        "LEGACY_BASE_URL",
        "https://tmbv-hms.com/aYOUTUBEocjenivanje5",
    )
    csrf_token: str = os.getenv("CSRF_TOKEN", "local-dev-csrf-token")


settings = Settings()
