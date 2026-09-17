# Emergency Response & Authority Dispatch System

## 1. Core Philosophy: Manual SOS is Always Critical
A manual panic button pressed by a child is the ultimate safety trigger. The system adheres to this non-negotiable rule:
> **A confirmed manual SOS is ALWAYS CRITICAL (Score 100). AI or automated risk filters must NEVER block, delay, or downgrade the emergency event.**

---

## 2. Emergency Incident Lifecycle Flow

```
   CHILD WATCH SOS
          │
          ▼
   BACKEND INGESTION
   - Marks Child status: CRITICAL
   - Assigns Risk Score: 100
          │
          ├─────────────────────────────────────────────────┐
          │                                                 │
          ▼                                                 ▼
   PARENT NOTIFICATION                              EMERGENCY CASE CREATED
   - Instant WebSocket Push                         - Initial state: PENDING
   - Displays SOS overlay, GPS,                     - Real-time broadcast to Authority
     battery, and network transport                   Dashboard
          │                                                 │
          │                                                 ▼
          │                                         AUTHORITY ACKNOWLEDGES
          │                                         - State: ACKNOWLEDGED
          │                                         - Dispatcher name recorded
          │                                                 │
          │                                                 ▼
          │                                         DISPATCH UNITS
          │                                         - State: RESPONDING
          │                                                 │
          │                                                 ▼
          │                                         RESOLVE INCIDENT
          │                                         - State: RESOLVED
          │                                         - Resolution audit notes logged
          ▼                                                 │
   POST-ESCALATION AI CONTEXT                               │
   - Analyzes recent breadcrumbs, fall, & telemetry         │
   - Furnishes dispatcher & parent with                     │
     plain-language explanation and recommended action ◄────┘
```

---

## 3. Emergency Authority Dispatch Prototype
To maintain legal, ethical, and practical honesty as a college major project:
* The system provides a standalone **Emergency Authority Dashboard** web application.
* It simulates the emergency services / police dispatch desk.
* **Disclaimer**: Does not connect to real public 911/100/112 emergency networks.

