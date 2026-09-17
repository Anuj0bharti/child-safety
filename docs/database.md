# Database Schema & Entity Relationship Model

## 1. Database Architecture
The backend employs an asynchronous SQLAlchemy object-relational mapping architecture supporting:
* **Production**: PostgreSQL via `asyncpg` (`postgresql+asyncpg://user:pass@host:5432/child_safety`)
* **Local / Testing**: SQLite via `aiosqlite` (`sqlite+aiosqlite:///./child_safety.db`)

---

## 2. Entity Relationship Diagram

```
       ┌──────────────┐
       │    USERS     │
       │--------------│
       │ id (PK)      │
       │ email        │
       │ role         │
       └──────┬───────┘
              │ 1:N
              ▼
       ┌──────────────┐          1:1          ┌──────────────┐
       │   CHILDREN   │◄─────────────────────┤   DEVICES    │
       │--------------│                       │--------------│
       │ id (PK)      │                       │ id (PK)      │
       │ parent_id    │                       │ pairing_code │
       │ status       │                       │ battery      │
       └──────┬───────┘                       │ transport    │
              │                               └──────────────┘
              ├──────────────┬──────────────┬──────────────┬──────────────┐
              │ 1:N          │ 1:N          │ 1:N          │ 1:N          │ 1:N
              ▼              ▼              ▼              ▼              ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │  LOCATIONS  │ │ SAFE_ROUTES │ │ SAFE_ZONES  │ │ RISK_EVENTS │ │ EMERGENCIES │
       │-------------│ │-------------│ │-------------│ │-------------│ │-------------│
       │ latitude    │ │ polyline    │ │ center_lat  │ │ risk_score  │ │ case_status │
       │ longitude   │ │ corridor_m  │ │ center_lng  │ │ risk_level  │ │ severity    │
       │ accuracy    │ │ is_active   │ │ radius_m    │ │ factors     │ │ notes       │
       └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 3. Detailed Table Specifications

### 3.1 `users`
* `id` (VARCHAR 36, PK): UUID.
* `email` (VARCHAR 255, Unique, Indexed): User login email.
* `password_hash` (VARCHAR 255): Salted bcrypt password hash.
* `full_name` (VARCHAR 255): Display name.
* `role` (VARCHAR 50): `PARENT`, `AUTHORITY`, or `ADMIN`.
* `phone_number` (VARCHAR 50, Nullable): Contact phone.
* `created_at` (TIMESTAMP): Registration time.

### 3.2 `children`
* `id` (VARCHAR 36, PK): UUID.
* `parent_id` (VARCHAR 36, FK $\rightarrow$ `users.id`, Indexed): Owning parent.
* `name` (VARCHAR 255): Child's name.
* `age` (INTEGER, Nullable): Child age.
* `emergency_notes` (TEXT, Nullable): Medical allergies / contact instructions.
* `current_status` (VARCHAR 50): Current safety tier (`SAFE`, `WARNING`, `HIGH_RISK`, `CRITICAL`).
* `current_risk_score` (INTEGER): Deterministic risk score (0–100).
* `created_at` (TIMESTAMP).

### 3.3 `devices`
* `id` (VARCHAR 36, PK): UUID.
* `device_identifier` (VARCHAR 100, Unique, Indexed): Hardware identity (e.g. `WATCH-001`).
* `child_id` (VARCHAR 36, FK $\rightarrow$ `children.id`, Nullable, Indexed): Paired child.
* `pairing_code` (VARCHAR 20, Nullable): One-time alphanumeric token for pairing.
* `is_paired` (BOOLEAN): Pairing state.
* `last_heartbeat` (TIMESTAMP, Nullable): Telemetry timestamp.
* `battery_percent` (INTEGER, Nullable): Remaining battery level.
* `network_transport` (VARCHAR 50): `CELLULAR`, `WIFI`, `BLUETOOTH`, or `NO_INTERNET`.
* `device_model` (VARCHAR 100): E.g., `Samsung Galaxy Watch4 LTE 40mm`.

### 3.4 `locations`
* `id` (VARCHAR 36, PK): UUID.
* `child_id` (VARCHAR 36, FK $\rightarrow$ `children.id`, Indexed).
* `device_id` (VARCHAR 36, FK $\rightarrow$ `devices.id`).
* `latitude` (FLOAT): Geographic latitude.
* `longitude` (FLOAT): Geographic longitude.
* `accuracy_meters` (FLOAT, Nullable): GPS accuracy estimate.
* `speed` (FLOAT, Nullable): Speed over ground in m/s.
* `timestamp` (TIMESTAMP): Device fix timestamp.

### 3.5 `safe_routes`
* `id` (VARCHAR 36, PK): UUID.
* `child_id` (VARCHAR 36, FK $\rightarrow$ `children.id`, Indexed).
* `name` (VARCHAR 100): E.g. "Home to School".
* `start_point` (JSON): `{"lat": float, "lng": float, "name": str}`.
* `destination_point` (JSON): `{"lat": float, "lng": float, "name": str}`.
* `polyline_points` (JSON): Ordered coordinate list `[{"lat": float, "lng": float}, ...]`.
* `corridor_width_meters` (FLOAT): Allowed corridor width (e.g., 100m).
* `destination_radius_meters` (FLOAT): Destination proximity threshold (default 30m).
* `is_active` (BOOLEAN): Whether this route is currently being enforced.

### 3.6 `route_deviations`
* `id` (VARCHAR 36, PK): UUID.
* `child_id` (VARCHAR 36, FK $\rightarrow$ `children.id`, Indexed).
* `route_id` (VARCHAR 36, FK $\rightarrow$ `safe_routes.id`, Indexed).
* `timestamp` (TIMESTAMP): Confirmation timestamp.
* `latitude`, `longitude` (FLOAT): Deviation coordinates.
* `gps_accuracy` (FLOAT, Nullable): Fix uncertainty.
* `distance_from_route_meters` (FLOAT): Measured perpendicular offset.
* `corridor_width_meters` (FLOAT): Configured threshold.
* `is_confirmed` (BOOLEAN): Whether multi-reading confirmation window elapsed.

### 3.7 `emergency_cases`
* `id` (VARCHAR 36, PK): UUID.
* `child_id` (VARCHAR 36, FK $\rightarrow$ `children.id`, Indexed).
* `case_status` (VARCHAR 50): `PENDING`, `ACKNOWLEDGED`, `RESPONDING`, `RESOLVED`.
* `severity` (VARCHAR 50): `HIGH_RISK`, `CRITICAL`.
* `initial_event_type` (VARCHAR 100): `MANUAL_SOS`, `ROUTE_DEVIATION_FALL`, etc.
* `latitude`, `longitude` (FLOAT, Nullable): Incident coordinates.
* `dispatcher_notes` (TEXT, Nullable): Resolution / dispatch notes.
* `acknowledged_by`, `acknowledged_at` (Nullable).
* `resolved_by`, `resolved_at` (Nullable).

