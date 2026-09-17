# College Major Project Final Demonstration Script

## 1. Demonstration Setup & Pre-flight Checklist
1. **Start Backend**:
   ```bash
   python -m uvicorn app.main:app --app-dir backend --host 0.0.0.0 --port 8000 --reload
   ```
2. **Start Authority Web Dashboard**:
   ```bash
   cd authority-dashboard && npm run dev
   ```
   Open `http://localhost:3000` in browser.
3. **Launch Parent App**:
   Install and run `parent-app` on phone or emulator.
4. **Launch Wear OS Watch App**:
   Install and run `watch-app` on Wear OS device or emulator.

---

## 2. Step-by-Step Teacher Demonstration Flow

### Scene 1: System Boot & Architecture Explanation
* **Explain to Teacher**: The system connects a child Wear OS smartwatch, an Android parent smartphone, a Python FastAPI backend with a deterministic risk engine, and an emergency authority dispatch dashboard.
* **Demonstrate**: Open Authority Dashboard at `http://localhost:3000`. Show the active WebSocket connection indicator (`LIVE FEED ACTIVE`).

### Scene 2: Parent Registration & Smartwatch Pairing
* **Demonstrate in Parent App**:
  1. Log into Parent App.
  2. View the child profile ("Aarav").
  3. Click **Pair Watch**, enter the 6-character code shown on the watch, and tap **Pair Watch With Child**.
  4. Show the paired device status showing `ONLINE`, Battery `92%`, Network `LTE/CELLULAR`.

### Scene 3: Safe Route Corridor Creation
* **Demonstrate in Parent App**:
  1. Click **Create Route**.
  2. Define name: "Home to School".
  3. Configure the **Safe Corridor Buffer Slider** to **100 meters**.
  4. Tap **Save & Activate Safe Route**.
  5. Open **View Live Map & Route** to show the interactive OpenStreetMap rendering the route polyline and corridor.

### Scene 4: Normal Journey (SAFE)
* **Demonstrate**:
  1. Smartwatch sends GPS fix along the designated path.
  2. Parent app displays:
     * Status: `SAFE`
     * Route Status: `ON ROUTE`
     * Distance to Route: `12m` (well inside the 100m corridor).

### Scene 5: Safe Route Deviation Confirmation
* **Demonstrate**:
  1. The child moves 150m away from the path (outside the 100m corridor).
  2. Point out that the system **does not panic on a single reading** (preventing GPS jump false alarms).
  3. After the 15-second confirmation window elapses with consecutive readings outside:
     * Smartwatch state updates to `ROUTE DEVIATION`.
     * Parent app immediately receives an alert: `ROUTE DEVIATION CONFIRMED` (Distance: 150m, Allowed: 100m).
     * Safety tier updates to `WARNING`.

### Scene 6: Combined Incident Escalation (Fall + Deviation)
* **Demonstrate**:
  1. While outside the route, the watch's accelerometer detects an impact spike.
  2. Deterministic Risk Engine combines factors:
     $$\text{Score} = 30 (\text{Deviation}) + 50 (\text{Fall}) = 80 \rightarrow \textbf{HIGH RISK / CRITICAL}$$
  3. AI Contextual Analysis interprets the compound context:
     *"Child deviated 150m outside safe route corridor followed by high-g impact spike."*
  4. Emergency Authority Dashboard receives a high-priority dispatch case automatically!

### Scene 7: Manual Emergency SOS Activation
* **Demonstrate**:
  1. Tap the large **SOS** button on the smartwatch.
  2. Haptic feedback pulses on the watch.
  3. State immediately transitions to **`CRITICAL`** (Score 100).
  4. Parent phone sounds critical alarm with live GPS coordinates.
  5. Emergency Authority Dashboard flashes a new critical incident card with child medical notes.

### Scene 8: Authority Dispatch & Incident Resolution
* **Demonstrate in Authority Dashboard**:
  1. Dispatcher clicks **Acknowledge Case** (state changes from `PENDING` to `ACKNOWLEDGED`).
  2. Dispatcher clicks **Dispatch Units / Respond** (state changes to `RESPONDING`).
  3. Dispatcher adds resolution notes (*"Child located safely by school security officer"*) and clicks **Confirm Resolution** (state changes to `RESOLVED`).
  4. All connected parents and dispatchers see the updated resolution in real time.

