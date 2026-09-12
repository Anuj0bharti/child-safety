from __future__ import annotations

from datetime import datetime, timezone

from fastapi.testclient import TestClient
from app.main import app, database


def setup_function() -> None:
    if database.path.exists():
        database.path.unlink()


def test_sos_is_critical_creates_case_and_is_idempotent() -> None:
    with TestClient(app) as client:
        registration = client.post("/api/v1/devices/register", json={"device_id": "WATCH-001", "child_id": "CHILD-001", "display_name": "Demo Watch"})
        assert registration.status_code == 200
        token = registration.json()["device_token"]
        event = {
            "event_id": "demo-sos-0001",
            "event_type": "SOS",
            "occurred_at": datetime.now(timezone.utc).isoformat(),
            "location": {"latitude": 28.6139, "longitude": 77.209, "accuracy_meters": 8.5},
            "battery_percent": 82,
            "network_transport": "WIFI",
            "mode": "DEMO",
        }
        first = client.post("/api/v1/events", headers={"X-Device-Token": token}, json=event)
        assert first.status_code == 200
        receipt = first.json()
        assert receipt["risk_level"] == "CRITICAL"
        assert receipt["risk_score"] == 100
        assert receipt["emergency_case_id"]
        assert receipt["ai_analysis"]["status"] == "PENDING"

        duplicate = client.post("/api/v1/events", headers={"X-Device-Token": token}, json=event)
        assert duplicate.status_code == 200
        assert duplicate.json()["duplicate"] is True

        cases = client.get("/api/v1/emergencies")
        assert len(cases.json()) == 1
        case_id = receipt["emergency_case_id"]
        acknowledged = client.post(f"/api/v1/emergencies/{case_id}/acknowledge", json={"authority_id": "AUTH-001", "note": "Contacting parent"})
        assert acknowledged.json()["status"] == "ACKNOWLEDGED"
        resolved = client.post(f"/api/v1/emergencies/{case_id}/resolve", json={"authority_id": "AUTH-001", "note": "Demo complete"})
        assert resolved.json()["status"] == "RESOLVED"


def test_demo_endpoint_rejects_real_mode() -> None:
    with TestClient(app) as client:
        client.post("/api/v1/devices/register", json={"device_id": "WATCH-002", "child_id": "CHILD-001", "display_name": "Demo Watch"})
        response = client.post("/api/v1/demo/events", headers={"X-Demo-Key": "test-demo-key"}, json={
            "device_id": "WATCH-002", "event_id": "demo-real-0001", "event_type": "SOS", "occurred_at": datetime.now(timezone.utc).isoformat(), "mode": "REAL"
        })
        assert response.status_code == 422
