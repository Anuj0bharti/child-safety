# System Architecture — Child Safety Band

## 1. Executive Summary
The **Child Safety Band** is an integrated IoT, Mobile, and Cloud safety ecosystem designed to provide continuous, autonomous child protection. Unlike simple GPS trackers, the system continuously cross-checks actual sensor telemetry against geographic safety constraints, evaluates risk through a deterministic scoring engine, provides contextual AI interpretations, and alerts both parents and emergency authorities.

---

## 2. High-Level Topology

```
                  ┌──────────────────────────────────────────────────┐
                  │          Samsung Galaxy Watch4 LTE (Wear OS)     │
                  │  - Android Location APIs (GPS, Lat/Lng/Accuracy) │
                  │  - Accelerometer/Gyroscope (Fall Detection)      │
                  │  - Heart Rate / Health Services                  │
                  │  - Standalone LTE / Wi-Fi / Bluetooth Detection  │
                  │  - Room Local DB (Guaranteed Offline Queue)      │
                  │  - Large Tactile SOS Trigger                     │
                  └────────────────────────┬─────────────────────────┘
                                           │ HTTPS / WebSockets
                                           ▼
                  ┌──────────────────────────────────────────────────┐
                  │                 FastAPI Backend                  │
                  │  - Device Authentication & Pairing Engine        │
                  │  - Geodesic Safe Route Corridor Checker          │
                  │  - Circular Geofence Safe Zone Engine            │
                  │  - Deterministic Risk Engine (0-100 Score)       │
                  │  - AI Provider Service (Gemini / Nemotron)       │
                  │  - Realtime WebSocket Telemetry & Dispatch Feed  │
                  └───────────────┬──────────────────┬───────────────┘
                                  │                  │
               WebSocket / HTTPS  │                  │  WebSocket / HTTPS
                                  ▼                  ▼
┌───────────────────────────────────────┐  ┌───────────────────────────────────────┐
│        Parent Android Application     │  │      Emergency Authority Dashboard    │
│  - Jetpack Compose UI                 │  │  - Vite + React + Tailwind + Leaflet  │
│  - Real Interactive OSM Map           │  │  - Realtime Incident Dispatch Feed    │
│  - Safe Route Corridor Builder        │  │  - Case States:                       │
│  - Instant Deviation & Fall Alerts    │  │    PENDING → ACKNOWLEDGED →           │
│  - Battery & Connectivity Status      │  │    RESPONDING → RESOLVED              │
│  - AI Contextual Safety Insights      │  │  - AI Incident Context Analysis       │
└───────────────────────────────────────┘  └───────────────────────────────────────┘
```

---

## 3. Component Breakdown

### 3.1 Wear OS Watch App (`watch-app/`)
* **Framework**: Kotlin, Jetpack Compose for Wear OS, Android SDK 34 (Wear OS 4/5), Min SDK 30.
* **Location Tracking**: `FusedLocationProviderClient` operating at 5–10s intervals with high-accuracy GPS fix.
* **Fall Detection Algorithm**: Continuous 3D acceleration magnitude sampling ($\sqrt{x^2+y^2+z^2}$) detecting weightlessness drop ($< 3.0\text{ m/s}^2$) followed by high-g impact spike ($> 25.0\text{ m/s}^2$) and post-impact inactivity.
* **Network Independence**: Standalone connectivity manager checking `TRANSPORT_CELLULAR`, `TRANSPORT_WIFI`, or `TRANSPORT_BLUETOOTH`. Never depends on parent smartphone presence if watch has active LTE/Wi-Fi.
* **Offline Resilience**: Local SQLite database via Android Room (`pending_events`) queues all location breadcrumbs, falls, and manual SOS triggers if internet connectivity drops. Automatically synchronizes with idempotency keys upon reconnection.
* **Diagnostics Screen**: Displays real-time device capability matrix, distinguishing verified platform components from `PHYSICAL HARDWARE TEST REQUIRED` features.

### 3.2 Backend Service (`backend/`)
* **Framework**: Python 3.11, FastAPI, Asynchronous SQLAlchemy, Uvicorn, WebSockets.
* **Database**: PostgreSQL (production) with asynchronous SQLite test fallback for zero-configuration development.
* **Geodesic Safe Route Engine**: Computes exact great-circle perpendicular cross-track distances to polyline route segments. Manages configurable corridor buffers (50m–200m).
* **Consecutive Deviation Filter**: Eliminates single-point GPS jumps by verifying multi-reading deviation over a configurable confirmation period (15 seconds) accounting for reported GPS accuracy.
* **Deterministic Risk Engine**: Weighted scoring matrix (SOS = 100, Fall = +50, Route Deviation = +30, Geofence = +30, Abnormal Movement = +25).
* **AI Provider Abstraction**: Pluggable interface supporting Google Gemini and NVIDIA Nemotron with graceful deterministic fallback.

### 3.3 Parent Android App (`parent-app/`)
* **Framework**: Kotlin, Jetpack Compose, Material 3, Android SDK 34.
* **Interactive Map**: OpenStreetMap integration via `osmdroid` for responsive vector map rendering, live breadcrumb trails, route polylines, and corridor buffer overlays without proprietary API key locks.
* **Route Creator**: Interactive UI allowing parents to define origin, destination, and intermediate waypoints, with adjustable corridor width sliders.
* **Device Pairing**: Secure 6-character alphanumeric pairing token workflow linking smartwatches to specific children.

### 3.4 Emergency Authority Dashboard (`authority-dashboard/`)
* **Framework**: React 18, TypeScript, Tailwind CSS, Leaflet, Vite.
* **Realtime Dispatch**: WebSocket connection receives immediate alerts for high-risk escalations and manual SOS triggers.
* **Case Lifecycle**: Formal state transitions: `PENDING` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `RESPONDING` $\rightarrow$ `RESOLVED`.
* **Map & AI Insight**: Live GPS coordinate fix, accuracy radius, route corridor context, and AI recommendations.

