from __future__ import annotations

from dataclasses import dataclass

from .schemas import EventType, RiskLevel, SafetyEventIn


@dataclass(frozen=True)
class RiskAssessment:
    score: int
    level: RiskLevel
    reason: str
    emergency: bool


SCORES: dict[EventType, int] = {
    EventType.SOS: 100,
    EventType.POSSIBLE_FALL: 50,
    EventType.GEOFENCE_VIOLATION: 30,
    EventType.ABNORMAL_MOVEMENT: 25,
    EventType.PROLONGED_INACTIVITY: 20,
    EventType.HEART_RATE_ANOMALY: 20,
}


def assess(event: SafetyEventIn) -> RiskAssessment:
    """Classify one event. Aggregate/cooldown logic is added by the event service."""
    score = SCORES[event.event_type]
    if event.event_type is EventType.SOS:
        return RiskAssessment(score=100, level=RiskLevel.CRITICAL, reason="Confirmed manual SOS", emergency=True)
    if score >= 80:
        level = RiskLevel.CRITICAL
    elif score >= 50:
        level = RiskLevel.HIGH_RISK
    elif score >= 30:
        level = RiskLevel.WARNING
    else:
        level = RiskLevel.SAFE
    return RiskAssessment(
        score=score,
        level=level,
        reason=event.event_type.value.replace("_", " ").title(),
        emergency=level is RiskLevel.CRITICAL,
    )
