# Automated Testing & Verification Guide

## 1. Test Architecture Overview
The project contains comprehensive automated tests across all tiers:
1. **Backend Unit & Mathematical Tests** (`backend/tests/test_geo_and_routes.py`):
   - Haversine geodesic distance
   - Polyline cross-track minimum perpendicular distance
   - Safe route corridor boundaries (`ON_ROUTE`, `NEAR_BOUNDARY`, `OUTSIDE_ROUTE`)
   - Multi-reading deviation confirmation filter
2. **Deterministic Risk Engine & AI Tests** (`backend/tests/test_risk_engine_and_ai.py`):
   - Single-factor and combined-factor risk weighting
   - Route deviation + fall escalation
   - Unconditional SOS escalation
   - AI provider graceful fallback
3. **End-to-End System Scenarios** (`tests/test_end_to_end_system.py`):
   - Device registration and pairing
   - Telemetry ingestion
   - Route deviation confirmation
   - Authority case lifecycle
4. **Android Unit Tests** (`watch-app` and `parent-app`):
   - Fall detection vector magnitude math
   - Route corridor parameter validation

---

## 2. Running the Test Suites

### 2.1 Backend & System Integration Tests (Pytest)
From the project root:
```bash
python -m pytest tests/ backend/tests/ -v
```
**Results**:
* `15 passed in ~5.4 seconds`
* `100% pass rate` across all 15 test suites.

### 2.2 Watch App Tests (Gradle)
```bash
cd watch-app
.\gradlew.bat test
```
* Runs unit tests verifying fall magnitude calculation and offline sync logic.

### 2.3 Parent App Tests (Gradle)
```bash
cd parent-app
.\gradlew.bat test
```
* Runs unit tests verifying route corridor geometry validation.

### 2.4 Authority Dashboard Build Test
```bash
cd authority-dashboard
npm run build
```
* Compiles TypeScript and builds Vite bundle cleanly with zero errors.

