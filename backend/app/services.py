from __future__ import annotations

import json
import uuid
from typing import Any

from .ai import AIProvider
from .database import Database
from .risk_engine import assess
from .schemas import AiAnalysis, EmergencyCaseOut, EventReceipt, LocationPayload, SafetyEventIn


def _analysis_context(event: SafetyEventIn) -> dict[str, Any]:
    return {
        "eventType": event.event_type.value,
        "sos": event.event_type.value == "SOS",
        "timestamp": event.occurred_at.isoformat(),
        "location": event.location.model_dump() if event.location else {"available": False},
        "batteryPercent": event.battery_percent,
        "network": event.network_transport,
        "sensorSnapshot": event.sensor_snapshot.model_dump(exclude_none=True) if event.sensor_snapshot else {},
        "mode": event.mode,
    }


async def ingest_event(database: Database, device: Any, event: SafetyEventIn) -> tuple[EventReceipt, EmergencyCaseOut | None]:
    existing = database.event(event.event_id)
    if existing:
        return EventReceipt(
            event_id=event.event_id,
            risk_level=existing["risk_level"],
            risk_score=existing["risk_score"],
            emergency_case_id=existing["case_id"],
            ai_analysis=AiAnalysis.model_validate_json(existing["ai_json"]),
            duplicate=True,
        ), None

    risk = assess(event)
    # Persist the safety event and an emergency case before initiating optional AI work.
    initial_ai = AiAnalysis(provider="pending", status="PENDING")
    case_id = str(uuid.uuid4()) if risk.emergency else None
    database.store_event(
        event_id=event.event_id,
        device_id=device["device_id"],
        event_json=event.model_dump(mode="json"),
        score=risk.score,
        level=risk.level.value,
        case_id=None,
        ai_json=initial_ai.model_dump(mode="json"),
    )
    if case_id:
        database.create_case(
            case_id=case_id,
            device_id=device["device_id"],
            child_id=device["child_id"],
            event_id=event.event_id,
            score=risk.score,
            level=risk.level.value,
            reason=risk.reason,
            ai_json=initial_ai.model_dump(mode="json"),
        )
        database.link_event_to_case(event.event_id, case_id)
    case = case_out(database.case(case_id), database) if case_id else None
    return EventReceipt(
        event_id=event.event_id,
        risk_level=risk.level,
        risk_score=risk.score,
        emergency_case_id=case_id,
        ai_analysis=initial_ai,
    ), case


async def complete_ai_analysis(database: Database, ai: AIProvider, event: SafetyEventIn, case_id: str | None) -> AiAnalysis:
    analysis = await ai.analyze(_analysis_context(event))
    database.update_ai_analysis(event.event_id, case_id, analysis.model_dump(mode="json"))
    return analysis


def case_out(row: Any, database: Database | None = None) -> EmergencyCaseOut:
    event: dict[str, Any] = {}
    if database:
        event_row = database.event(row["event_id"])
        event = json.loads(event_row["event_json"]) if event_row else {}
    location = event.get("location")
    return EmergencyCaseOut(
        case_id=row["case_id"],
        child_id=row["child_id"],
        device_id=row["device_id"],
        status=row["status"],
        risk_level=row["risk_level"],
        risk_score=row["risk_score"],
        reason=row["reason"],
        location=LocationPayload.model_validate(location) if location else None,
        ai_analysis=AiAnalysis.model_validate_json(row["ai_json"]),
        created_at=row["created_at"],
        updated_at=row["updated_at"],
    )
