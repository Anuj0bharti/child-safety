from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parents[2] / ".env")


@dataclass(frozen=True)
class Settings:
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./child_safety.db")
    ai_provider: str = os.getenv("AI_PROVIDER", "disabled").lower()
    ai_model: str = os.getenv("AI_MODEL", "")
    gemini_api_key: str = os.getenv("GEMINI_API_KEY", "")
    nemotron_api_key: str = os.getenv("NEMOTRON_API_KEY", "")
    demo_api_key: str = os.getenv("DEMO_API_KEY", "local-demo-only-change-me")

    @property
    def database_path(self) -> Path:
        prefix = "sqlite:///"
        if not self.database_url.startswith(prefix):
            raise ValueError("This development build supports sqlite URLs only")
        return Path(self.database_url.removeprefix(prefix))


settings = Settings()
