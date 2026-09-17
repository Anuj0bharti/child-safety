# Troubleshooting & Common Issues

## 1. Backend & Database
* **Issue**: `ModuleNotFoundError: No module named 'app'`
  * **Fix**: Ensure `PYTHONPATH` includes `backend` or run pytest from the root where `pytest.ini` is configured: `python -m pytest backend/tests/`.
* **Issue**: `DATABASE_URL` connection error with PostgreSQL
  * **Fix**: If PostgreSQL service is not actively running locally, the backend automatically supports SQLite: set `DATABASE_URL=sqlite+aiosqlite:///./child_safety.db` in `.env`.

---

## 2. Android Build & Gradle
* **Issue**: `JAVA_HOME is not set`
  * **Fix**: Android Studio bundles OpenJDK inside `C:\Program Files\Android\Android Studio\jbr`. The provided `gradlew.bat` automatically configures this directory as `JAVA_HOME`.
* **Issue**: `Compose Compiler Gradle plugin is required`
  * **Fix**: Kotlin 2.0+ requires `org.jetbrains.kotlin.plugin.compose` applied in build scripts. This is fully configured in both `watch-app` and `parent-app`.

---

## 3. Wear OS Smartwatch
* **Issue**: Location says `PERMISSION REQUIRED`
  * **Fix**: Open Android Settings on the watch $\rightarrow$ Apps $\rightarrow$ Child Safety $\rightarrow$ Permissions $\rightarrow$ Location $\rightarrow$ Allow all the time.
* **Issue**: Network displays `CELLULAR UNAVAILABLE`
  * **Fix**: If the watch does not currently have an active cellular eSIM plan, connect the watch to Wi-Fi. The network monitor will seamlessly switch to `WI-FI` transport.
* **Issue**: Telemetry events queued offline
  * **Fix**: As designed! If the watch temporarily loses internet, events are preserved in the local Room SQLite DB (`pending_events`) and will synchronize automatically when connection returns.

---

## 4. Authority Dashboard
* **Issue**: WebSocket shows `CONNECTING...`
  * **Fix**: Ensure the FastAPI backend is running on `http://localhost:8000`. Vite proxies `/ws` directly to `ws://localhost:8000/api/v1/ws/authority`.

