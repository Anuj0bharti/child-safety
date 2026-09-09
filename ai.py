from __future__ import annotations

import json
from abc import ABC, abstractmethod
from typing import Any

import httpx

from .config import Settings
from .schemas import AiAnalysis, RiskLevel


SYSTEM_INSTRUCTION = """You are a child-safety event analysis assistant. Analyze only supplied structured event data. Never invent sensor values or location. Never diagnose a medical condition. Do not claim certainty when evidence is insufficient. A confirmed manual SOS is always CRITICAL. Return JSON only with risk_level (SAFE, WARNING, HIGH_RISK, or CRITICAL), risk_score (0-100), reason, recommended_action, and uncertainty."""


class AIProvider(ABC):
    @abstractmethod
    async def analyze(self, context: dict[str, Any]) -> AiAnalysis: ...


class DisabledProvider(AIProvider):
    async def analyze(self, context: dict[str, Any]) -> AiAnalysis:
        return AiAnalysis(provider="disabled", status="UNAVAILABLE", reason="AI provider is not configured")


class GeminiProvider(AIProvider):
    def __init__(self, key: str, model: str):
        self.key = key
        self.model = model

    async def analyze(self, context: dict[str, Any]) -> AiAnalysis:
        if not self.key or not self.model:
            return AiAnalysis(provider="gemini", status="UNAVAILABLE", reason="Gemini key or model is not configured")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent?key={self.key}"
        body = {
            "systemInstruction": {"parts": [{"text": SYSTEM_INSTRUCTION}]},
            "contents": [{"role": "user", "parts": [{"text": json.dumps(context)}]}],
            "generationConfig": {"responseMimeType": "application/json", "temperature": 0.1},
        }
        try:
            async with httpx.AsyncClient(timeout=12) as client:
                response = await client.post(url, json=body)
                response.raise_for_status()
            text = response.json()["candidates"][0]["content"]["parts"][0]["text"]
            parsed = json.loads(text)
            return AiAnalysis(provider="gemini", status="SUCCESS", **parsed)
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as error:
            return AiAnalysis(provider="gemini", status="FAILED", reason=f"AI response unavailable: {type(error).__name__}")


class NemotronProvider(AIProvider):
    """OpenAI-compatible NVIDIA endpoint; the exact model remains environment-configured."""

    def __init__(self, key: str, model: str):
        self.key = key
        self.model = model

    async def analyze(self, context: dict[str, Any]) -> AiAnalysis:
        if not self.key or not self.model:
            return AiAnalysis(provider="nemotron", status="UNAVAILABLE", reason="Nemotron key or model is not configured")
        body = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": SYSTEM_INSTRUCTION},
                {"role": "user", "content": json.dumps(context)},
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1,
        }
        try:
            async with httpx.AsyncClient(timeout=12) as client:
                response = await client.post(
                    "https://integrate.api.nvidia.com/v1/chat/completions",
                    headers={"Authorization": f"Bearer {self.key}"},
                    json=body,
                )
                response.raise_for_status()
            parsed = json.loads(response.json()["choices"][0]["message"]["content"])
            return AiAnalysis(provider="nemotron", status="SUCCESS", **parsed)
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as error:
            return AiAnalysis(provider="nemotron", status="FAILED", reason=f"AI response unavailable: {type(error).__name__}")


def provider_for(settings: Settings) -> AIProvider:
    if settings.ai_provider == "gemini":
        return GeminiProvider(settings.gemini_api_key, settings.ai_model)
    if settings.ai_provider == "nemotron":
        return NemotronProvider(settings.nemotron_api_key, settings.ai_model)
    return DisabledProvider()
