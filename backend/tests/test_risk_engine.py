from datetime import datetime, timezone

import pytest

from app.risk_engine import assess
from app.schemas import EventType, RiskLevel, SafetyEventIn


@pytest.mark.parametrize(
    ("event_type", "score", "level"),
    [
        (EventType.SOS, 100, RiskLevel.CRITICAL),
        (EventType.POSSIBLE_FALL, 50, RiskLevel.HIGH_RISK),
        (EventType.GEOFENCE_VIOLATION, 30, RiskLevel.WARNING),
        (EventType.HEART_RATE_ANOMALY, 20, RiskLevel.SAFE),
    ],
)
def test_deterministic_risk_classification(event_type, score, level) -> None:
    assessment = assess(SafetyEventIn(event_id="risk-test-001", event_type=event_type, occurred_at=datetime.now(timezone.utc)))
    assert assessment.score == score
    assert assessment.level is level


def test_manual_sos_is_emergency_without_ai() -> None:
    assessment = assess(SafetyEventIn(event_id="risk-test-002", event_type=EventType.SOS, occurred_at=datetime.now(timezone.utc)))
    assert assessment.emergency is True
