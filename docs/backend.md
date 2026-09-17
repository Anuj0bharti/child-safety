# FastAPI Backend Specification & API Reference

## 1. Architecture Overview
The backend is built with Python 3.11 and **FastAPI**, backed by **SQLAlchemy 2.0 (Async)**. It functions as the central orchestration engine for child safety telemetry ingestion, geofence and route corridor verification, deterministic risk scoring, resilient AI contextual analysis, and real-time bi-directional WebSocket notifications.

---

## 2. API Endpoints

### 2.1 Authentication & Authorization
* `POST /api/v1/auth/register`
  * Body: `{"email": str, "password": str, "full_name": str, "role": "PARENT" | "AUTHORITY" | "ADMIN", "phone_number": str}`
  * Returns: User object.
* `POST /api/v1/auth/login`
  * Body: `{"email": str, "password": str}`
  * Returns: JWT Bearer access token, user ID, role, and full name.
* `GET /api/v1/auth/me`
  * Protected endpoint returning current user profile.

### 2.2 Devices & Pairing
* `POST /api/v1/devices/register`
  * Body: `{"device_identifier": str, "device_model": str}`
  * Returns: Device object including a generated 6-character uppercase alphanumeric `pairing_code`.
* `POST /api/v1/devices/pair` (Requires `PARENT` role)
  * Body: `{"child_id": str, "pairing_code": str}`
  * Binds watch device to specified child profile and marks device as paired.
* `POST /api/v1/devices/{id}/heartbeat`
  * Body: `{"battery_percent": int, "network_transport": str}`
  * Updates watch last heartbeat timestamp, battery level, and connection transport.

### 2.3 Children Management
* `POST /api/v1/children`
  * Creates child profile associated with logged-in parent.
* `GET /api/v1/children`
  * Lists children registered under the authenticated parent.
* `GET /api/v1/children/{id}/status`
  * Aggregates real-time child status: latest GPS fix, risk score, risk tier, device connectivity, and active route status.
* `GET /api/v1/children/{id}/history`
  * Returns historical location breadcrumb trail and confirmed route deviation records.

### 2.4 Telemetry Ingestion
* `POST /api/v1/telemetry/location`
  * Ingests real GPS latitude, longitude, accuracy, and speed.
  * Checks circular geofence safe zones.
  * Evaluates active safe route cross-track distance and updates consecutive deviation confirmation filter.
  * Updates deterministic risk engine.
  * Broadcasts live update to parent via WebSocket.
* `POST /api/v1/telemetry/sensors`
  * Ingests sensor events (`possible_fall`, `abnormal_movement`, `heart_rate`).
  * Evaluates multi-signal risk and triggers alerts if abnormal.
* `POST /api/v1/telemetry/sos`
  * Confirmed manual panic trigger from watch.
  * **Critical Rule**: Immediately escalates status to `CRITICAL` (Score 100).
  * Automatically provisions an `EmergencyCase` in state `PENDING`.
  * Instantly broadcasts alerts to Parent WebSocket and Emergency Authority WebSocket.
  * Triggers post-escalation AI contextual summary without blocking the response.

### 2.5 Safe Routes & Geofences
* `POST /api/v1/children/{child_id}/routes`
  * Creates a route with ordered polyline coordinates and configurable corridor width.
* `GET /api/v1/children/{child_id}/routes`
  * Lists saved routes for the child.
* `POST /api/v1/routes/{id}/activate`
  * Sets the active route and deactivates other routes for that child.
* `POST /api/v1/routes/{id}/deactivate`
  * Deactivates the route.

### 2.6 Emergency Authority Dispatch
* `GET /api/v1/emergencies` (Requires `AUTHORITY` role)
  * Lists active and resolved emergency incidents. Filterable by status (`PENDING`, `ACKNOWLEDGED`, `RESPONDING`, `RESOLVED`).
* `GET /api/v1/emergencies/{id}`
  * Full incident inspection: child profile, telemetry, map coordinates, and AI reasoning.
* `POST /api/v1/emergencies/{id}/acknowledge`
  * Moves case from `PENDING` to `ACKNOWLEDGED`.
* `POST /api/v1/emergencies/{id}/respond`
  * Moves case from `ACKNOWLEDGED` to `RESPONDING`.
* `POST /api/v1/emergencies/{id}/resolve`
  * Resolves case and records resolution audit notes.

### 2.7 WebSockets
* `/api/v1/ws/parent/{parent_id}`: Live telemetry stream, deviation warnings, fall alerts.
* `/api/v1/ws/authority`: Instant emergency case feed for dispatchers.

