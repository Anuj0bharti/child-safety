"""
End-to-End System Scenario Test: Child Safety Band
College Major Project Master Verification
"""
from datetime import datetime, timedelta, timezone
import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app
from app.db.session import engine
from app.db.models import Base


@pytest.fixture(scope="module", autouse=True)
async def setup_test_db():
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)
    yield
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)


@pytest.mark.asyncio
async def test_full_college_project_demonstration_lifecycle():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # ==========================================
        # STEP 1: Smartwatch Device Boots & Registers
        # ==========================================
        dev_reg = await client.post("/api/v1/devices/register", json={
            "device_identifier": "WATCH-GALAXY-4",
            "device_model": "Samsung Galaxy Watch4 LTE 40mm"
        })
        assert dev_reg.status_code == 200
        device_data = dev_reg.json()
        device_id = device_data["id"]
        pairing_code = device_data["pairing_code"]
        device_token = device_data["device_token"]
        assert len(pairing_code) == 6
        assert device_token is not None
        dev_headers = {"X-Device-Token": device_token}

        # Watch sends initial heartbeat
        hb1 = await client.post(f"/api/v1/devices/{device_id}/heartbeat", json={
            "battery_percent": 92,
            "network_transport": "CELLULAR"
        }, headers=dev_headers)
        assert hb1.status_code == 200

        # ==========================================
        # STEP 2: Parent Signs Up & Creates Child Profile
        # ==========================================
        parent_reg = await client.post("/api/v1/auth/register", json={
            "email": "demo.parent@example.com",
            "password": "strongpassword123",
            "full_name": "Dr. Neha Verma",
            "role": "PARENT",
            "phone_number": "+919811002233"
        })
        assert parent_reg.status_code == 201

        login_res = await client.post("/api/v1/auth/login", json={
            "email": "demo.parent@example.com",
            "password": "strongpassword123"
        })
        assert login_res.status_code == 200
        parent_token = login_res.json()["access_token"]
        headers = {"Authorization": f"Bearer {parent_token}"}

        child_create = await client.post("/api/v1/children", json={
            "name": "Rohan Verma",
            "age": 10,
            "emergency_notes": "Asthmatic, carries inhaler in school bag"
        }, headers=headers)
        assert child_create.status_code == 201
        child_id = child_create.json()["id"]

        # ==========================================
        # STEP 3: Parent Pairs Watch with Child
        # ==========================================
        pair_res = await client.post("/api/v1/devices/pair", json={
            "child_id": child_id,
            "pairing_code": pairing_code
        }, headers=headers)
        assert pair_res.status_code == 200
        assert pair_res.json()["is_paired"] is True

        # ==========================================
        # STEP 4: Parent Configures Safe Route & Corridor
        # ==========================================
        route_res = await client.post(f"/api/v1/children/{child_id}/routes", json={
            "name": "Home to Delhi Public School",
            "start_point": {"lat": 28.6139, "lng": 77.2090, "name": "Home"},
            "destination_point": {"lat": 28.6250, "lng": 77.2090, "name": "School Campus"},
            "polyline_points": [
                {"lat": 28.6139, "lng": 77.2090},
                {"lat": 28.6180, "lng": 77.2090},
                {"lat": 28.6250, "lng": 77.2090}
            ],
            "corridor_width_meters": 100.0,
            "destination_radius_meters": 30.0,
            "is_active": True
        }, headers=headers)
        assert route_res.status_code == 201

        # ==========================================
        # STEP 5: Child Travels Normal Journey (ON_ROUTE, SAFE)
        # ==========================================
        t0 = datetime.now(timezone.utc)
        loc1 = await client.post("/api/v1/telemetry/location", json={
            "child_id": child_id,
            "device_id": device_id,
            "latitude": 28.6160,
            "longitude": 77.2090,
            "accuracy_meters": 4.5,
            "speed": 1.2,
            "timestamp": t0.isoformat()
        }, headers=dev_headers)
        assert loc1.status_code == 200
        assert loc1.json()["route_status"] == "ON_ROUTE"
        assert loc1.json()["risk_level"] == "SAFE"

        # ==========================================
        # STEP 6: Route Deviation Detection & Confirmation
        # (Child moves 160m outside corridor for >15s)
        # ==========================================
        dev_lon = 77.2090 + (160.0 / 97700.0) # ~160m East

        # First reading outside corridor (t = 0s) -> unconfirmed OUTSIDE_ROUTE
        loc2 = await client.post("/api/v1/telemetry/location", json={
            "child_id": child_id,
            "device_id": device_id,
            "latitude": 28.6180,
            "longitude": dev_lon,
            "accuracy_meters": 5.0,
            "timestamp": (t0 + timedelta(seconds=10)).isoformat()
        }, headers=dev_headers)
        assert loc2.json()["route_status"] == "OUTSIDE_ROUTE"

        # Second reading outside corridor (t = 26s, past 15s confirmation window) -> DEVIATION_CONFIRMED!
        loc3 = await client.post("/api/v1/telemetry/location", json={
            "child_id": child_id,
            "device_id": device_id,
            "latitude": 28.6180,
            "longitude": dev_lon,
            "accuracy_meters": 5.0,
            "timestamp": (t0 + timedelta(seconds=27)).isoformat()
        }, headers=dev_headers)
        assert loc3.json()["route_status"] == "DEVIATION_CONFIRMED"
        assert loc3.json()["risk_level"] == "WARNING"

        # ==========================================
        # STEP 7: Combined Incident (Route Deviation + Fall) -> Escalated to HIGH_RISK
        # ==========================================
        fall_res = await client.post("/api/v1/telemetry/sensors", json={
            "child_id": child_id,
            "device_id": device_id,
            "event_type": "possible_fall",
            "raw_data": {"magnitude": 27.5, "impact_timestamp": datetime.now(timezone.utc).isoformat()}
        }, headers=dev_headers)
        assert fall_res.status_code == 200
        assert fall_res.json()["risk_level"] in ["HIGH_RISK", "CRITICAL"]

        # ==========================================
        # STEP 8: Emergency SOS Activated on Watch
        # ==========================================
        sos_res = await client.post("/api/v1/telemetry/sos", json={
            "child_id": child_id,
            "device_id": device_id,
            "latitude": 28.6180,
            "longitude": dev_lon,
            "accuracy_meters": 4.0,
            "battery_percent": 88,
            "network_transport": "CELLULAR"
        }, headers=dev_headers)
        assert sos_res.status_code == 200
        sos_body = sos_res.json()
        assert sos_body["risk_level"] == "CRITICAL"
        assert sos_body["risk_score"] == 100
        case_id = sos_body["case_id"]

        # ==========================================
        # STEP 9: Authority Emergency Dashboard Response Lifecycle
        # ==========================================
        # Register Emergency Authority Account with Seed Token
        auth_reg = await client.post("/api/v1/auth/register-authority", json={
            "email": "police@authority.gov",
            "password": "policepassword",
            "full_name": "Dispatcher Officer Sharma",
            "authority_invite_token": "authority_secure_seed_token_2026"
        })
        assert auth_reg.status_code == 201

        # Authority Login
        auth_login = await client.post("/api/v1/auth/login", json={
            "email": "police@authority.gov",
            "password": "policepassword"
        })
        assert auth_login.status_code == 200
        auth_token = auth_login.json()["access_token"]
        auth_headers = {"Authorization": f"Bearer {auth_token}"}

        # Case Verification
        detail_res = await client.get(f"/api/v1/emergencies/{case_id}", headers=auth_headers)
        assert detail_res.status_code == 200
        case_details = detail_res.json()
        assert case_details["case"]["severity"] == "CRITICAL"
        assert case_details["child"]["name"] == "Rohan Verma"

        # Lifecycle: Acknowledge -> Respond -> Resolve
        ack = await client.post(f"/api/v1/emergencies/{case_id}/acknowledge", headers=auth_headers)
        assert ack.json()["case_status"] == "ACKNOWLEDGED"

        resp = await client.post(f"/api/v1/emergencies/{case_id}/respond", headers=auth_headers)
        assert resp.json()["case_status"] == "RESPONDING"

        res_final = await client.post(
            f"/api/v1/emergencies/{case_id}/resolve",
            params={"notes": "Patrol vehicle dispatched. Child safely located with family."},
            headers=auth_headers
        )
        assert res_final.json()["case_status"] == "RESOLVED"
