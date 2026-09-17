# Child Safety Band — Development Environment Report

Generated on: 2026-09-10  
Target Device: Samsung Galaxy Watch4 LTE 40mm (Wear OS) / Android Parent Device  

---

## 1. AVAILABLE

The following components and tools are discovered, installed, and ready for development:

| Component | Version / Location | Notes |
|---|---|---|
| **Operating System** | Windows 11 Pro (`Microsoft Windows NT 10.0.26200.0`) | Host platform |
| **Git** | `git version 2.55.0.windows.3` | Monorepo version control ready |
| **Python** | `Python 3.11.9` (`C:\Users\anujb\AppData\Local\Programs\Python\Python311\python.exe`) | Backend runtime for FastAPI, SQLAlchemy, Pytest |
| **Node.js & npm** | Node `v24.19.0`, npm `11.17.0` | Authority Dashboard web build (Vite + React) |
| **Android Studio** | Installed at `C:\Program Files\Android\Android Studio` | IDE & toolchain provider |
| **Bundled JDK** | OpenJDK 25 (`C:\Program Files\Android\Android Studio\jbr\bin\java.exe`) | Used for Gradle builds and Kotlin compilation |
| **Android SDK** | `C:\Users\anujb\AppData\Local\Android\Sdk` | Android SDK root directory |
| **Android Platforms** | `android-34` (Android 14 / Wear OS 4/5), `android-36`, `android-37.0` | Target platforms for Watch App and Parent App |
| **Android Build Tools** | `36.0.0` | AAPT2, D8, DX, zipalign |
| **Android Debug Bridge (ADB)** | Available at `C:\Users\anujb\AppData\Local\Android\Sdk\platform-tools\adb.exe` | Device and emulator deployment tool |
| **Android Virtual Device (AVD)** | `Pixel_7.avd` configured under `~/.android/avd` | Phone emulator available |
| **Package Manager** | `winget v1.29.290` | Windows package manager available if needed |

---

## 2. MISSING

The following software is not globally on `PATH` or not installed locally:

| Component | Status | Impact & Mitigation |
|---|---|---|
| **PostgreSQL (`psql`)** | Not in system `PATH` | The backend uses an asynchronous SQLAlchemy engine supporting standard PostgreSQL via `DATABASE_URL` for production, and automatically supports SQLite for automated test suites and standalone local execution. |
| **Global `java` / `javac` on `PATH`** | Not configured in system environment variables | Android Studio bundles `jbr` (OpenJDK 25) at `C:\Program Files\Android\Android Studio\jbr`. We can set `JAVA_HOME` and include it in build scripts or Gradle invocations. |
| **Wear OS AVD** | Only phone AVD (`Pixel_7`) is currently provisioned | Wear OS emulator AVD can be created via Android Studio AVD Manager or command line `avdmanager` for emulator-based Wear OS testing. |
| **Docker** | Not installed / not in `PATH` | Optional for local single-process dev; backend can run directly via Python Uvicorn. |

---

## 3. REQUIRED (For Complete Implementation & Build)

1. **Gradle Wrapper (`gradlew`) in Android Projects**:
   - Both `watch-app` and `parent-app` must be equipped with Gradle wrapper scripts pointing to `JAVA_HOME` (`Android Studio/jbr`) to allow reproducible command-line builds.
2. **Python Dependencies**:
   - `fastapi`, `uvicorn`, `sqlalchemy`, `asyncpg`, `aiosqlite`, `pydantic`, `pydantic-settings`, `websockets`, `google-genai`, `httpx`, `pytest`, `pytest-asyncio`, `geopy`, `shapely`.
3. **Node Dependencies**:
   - Vite, React, Lucide-react, Tailwind CSS, Leaflet/MapLibre for interactive Authority Dashboard.
4. **Environment Variables Template (`.env.example`)**:
   - For database URLs, JWT secrets, AI provider choice (`gemini` or `nemotron`), API keys, and escalation thresholds.

---

## 4. OPTIONAL

- **PostgreSQL local server installation**: Can be installed via `winget install PostgreSQL.PostgreSQL` or connected to a remote PostgreSQL instance (e.g. Supabase, Neon, or local Docker).
- **Firebase Cloud Messaging (FCM) Service Account**: Optional mock/local push notification logging until real FCM credentials are provided in `.env`.
- **NVIDIA Nemotron API Key**: Optional alternative provider to Google Gemini.

---

## 5. PHYSICAL HARDWARE NOT AVAILABLE

- **Samsung Galaxy Watch4 LTE 40mm (Wear OS)**:
  - *Current Status*: Not physically available yet.
  - *Strategy*: Strict adherence to Hardware Honesty rules. All real Wear OS APIs (`LocationServices`, `SensorManager`, `Health Services`, `ConnectivityManager`, `TelephonyManager`, `Room` persistence) are implemented with complete production architecture.
  - *Classification*:
    - Actual software: **IMPLEMENTED**
    - Logic & math: **TESTED WITH AUTOMATED TESTS**
    - Real watch hardware: **PHYSICAL HARDWARE TEST REQUIRED**

