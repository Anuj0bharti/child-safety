# Map System Architecture

## 1. Overview
The Parent Android application and the Emergency Authority Dashboard both feature real-time interactive maps to visualize:
1. Child's real-time location with an accuracy circle.
2. Historical breadcrumb trail showing recent trajectory.
3. Circular Safe Zones (geofences).
4. Safe Route polyline path.
5. Configurable safety corridor buffer (e.g. 100 meters on either side of the path).
6. Deviation incident markers and SOS emergency locations.

---

## 2. Parent Mobile Application Implementation
* **Library**: `org.osmdroid:osmdroid-android:6.1.18` (OpenStreetMap Android library).
* **Rationale**:
  * Free, open-source, and eliminates mandatory proprietary billing accounts or API keys for student demonstrations.
  * Native hardware-accelerated vector and tile rendering.
  * Direct support for `Polyline`, `Polygon`, and custom `Marker` overlays.
* **Corridor Visualization**:
  * Safe route polyline drawn in `#0284C7` (Sky Blue).
  * Child location marker updated in real time via live StateFlow updates.
  * Trail breadcrumbs drawn in `#64748B` (Slate Grey).

---

## 3. Emergency Authority Dashboard Implementation
* **Library**: Leaflet (`leaflet@1.9.4`) with React.
* **Tile Provider**: OpenStreetMap Mapnik tiles (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`).
* **Interactive Overlays**:
  * High-risk incident marker (Pulse red `#ef4444`).
  * Accuracy circle reflecting GPS uncertainty estimate.
  * Auto-pan on incident selection.

