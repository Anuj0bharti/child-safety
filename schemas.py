from __future__ import annotations

from datetime import datetime, timezone
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field, field_validator


class EventType(str, Enum):
    SOS = "SOS"
    POSSIBLE_FALL = "POSSIBLE_FALL"
    GEOFENCE_VIOLATION = "GEOFENCE_VIOLATION"
    ABNORMAL_MOVEMENT = "ABNORMAL_MOVEMENT"
    PROLONGED_INACTIVITY = "PROLONGED_INACTIVITY"
    HEART_RATE_ANOMALY = "HEART_RATE_ANOMALY"


class RiskLevel(str, Enum):
    SAFE = "SAFE"
    WARNING = "WARNING"
    HIGH_RISK = "HIGH_RISK"
    CRITICAL = "CRITICAL"


class LocationPayload(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    accuracy_meters: float = Field(ge=0)
    provider: str | None = None


class SensorSnapshot(BaseModel):
    accelerometer: dict[str, float] | None = None
    gyroscope: dict[str, float] | None = None
    heart_rate_bpm: float | None = Field(default=None, ge=0, le=300)


class SafetyEventIn(BaseModel):
    event_id: str = Field(min_length=8, max_length=128)
    event_type: EventType
    occurred_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    location: LocationPayload | None = None
    battery_percent: int | None = Field(default=None, ge=0, le=100)
    network_transport: str = Field(default="UNKNOWN", max_length=40)
    sensor_snapshot: SensorSnapshot | None = None
    mode: str = Field(default="REAL", pattern="^(REAL|DEMO)$")

    @field_validator("occurred_at")
    @classmethod
    def timestamp_must_be_timezone_aware(cls, value: datetime) -> datetime:
        if value.tzinfo is None:
            raise ValueError("occurred_at must include a timezone")
        return value.astimezone(timezone.utc)


class DeviceRegistrationIn(BaseModel):
    device_id: str = Field(pattern=r"^[A-Za-z0-9_-]{3,64}$")
    child_id: str = Field(pattern=r"^[A-Za-z0-9_-]{3,64}$")
    display_name: str = Field(min_length=1, max_length=100)


class DeviceRegistrationOut(BaseModel):
    device_id: str
    device_token: str


class AiAnalysis(BaseModel):
    provider: str
    status: str
    risk_level: RiskLevel | None = None
    risk_score: int | None = Field(default=None, ge=0, le=100)
    reason: str | None = None
    recommended_action: str | None = None
    uncertainty: str | None = None


class EventReceipt(BaseModel):
    event_id: str
    risk_level: RiskLevel
    risk_score: int = Field(ge=0, le=100)
    emergency_case_id: str | None = None
    ai_analysis: AiAnalysis
    duplicate: bool = False


class EmergencyActionIn(BaseModel):
    authority_id: str = Field(min_length=3, max_length=64)
    note: str = Field(default="", max_length=500)


class EmergencyCaseOut(BaseModel):
    case_id: str
    child_id: str
    device_id: str
    status: str
    risk_level: RiskLevel
    risk_score: int
    reason: str
    location: LocationPayload | None = None
    ai_analysis: AiAnalysis
    created_at: datetime
    updated_at: datetime


class DemoEventIn(SafetyEventIn):
    device_id: str = Field(pattern=r"^[A-Za-z0-9_-]{3,64}$")
