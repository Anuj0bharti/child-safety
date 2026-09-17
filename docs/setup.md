# Developer Setup & Installation Guide

## 1. Prerequisites
Ensure the following tools are installed on your workstation:
* **Operating System**: Windows 11 / Linux / macOS
* **Python**: 3.11+
* **Node.js**: 20+ and npm 10+
* **Android Studio**: Ladybug / Iguana or later with bundled JDK 17+ (or JBR 25)
* **Android SDK**: Platforms 34 installed (`android-34`), build-tools `34.0.0` or `36.0.0`

---

## 2. Environment Configuration
Copy the configuration template:
```bash
cp .env.example .env
```
Key variables to configure in `.env`:
```ini
DATABASE_URL=sqlite+aiosqlite:///./child_safety.db
JWT_SECRET_KEY=your_secure_32_character_secret_here
AI_PROVIDER=gemini
AI_MODEL=gemini-1.5-flash
GEMINI_API_KEY=your_gemini_api_key
CORRIDOR_DEFAULT_WIDTH_METERS=100.0
DEVIATION_CONFIRMATION_SECONDS=15
```

---

## 3. Starting the Backend
1. Install Python dependencies:
   ```bash
   pip install -r backend/requirements.txt
   ```
2. Launch the FastAPI server:
   ```bash
   python -m uvicorn app.main:app --app-dir backend --host 0.0.0.0 --port 8000 --reload
   ```
3. Verify backend:
   * Swagger OpenAPI Docs: `http://localhost:8000/docs`
   * Health Check: `http://localhost:8000/health`

---

## 4. Starting the Emergency Authority Dashboard
1. Navigate to the dashboard directory:
   ```bash
   cd authority-dashboard
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Access the dashboard at `http://localhost:3000`.
   * Default Dispatcher Login: `police@authority.gov` / `policepassword`

---

## 5. Building the Android Applications

### Wear OS Watch App (`watch-app`):
```bash
cd watch-app
.\gradlew.bat test
.\gradlew.bat assembleDebug
```
* Generated APK: `watch-app/app/build/outputs/apk/debug/app-debug.apk`

### Parent Android App (`parent-app`):
```bash
cd parent-app
.\gradlew.bat test
.\gradlew.bat assembleDebug
```
* Generated APK: `parent-app/app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Running Automated Test Suites
Run all backend unit, geodesic math, risk engine, and end-to-end integration tests:
```bash
python -m pytest tests/ backend/tests/ -v
```

