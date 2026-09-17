# Safe Route Corridor & Deviation Confirmation Engine

## 1. Project Novelty & Problem Definition
A standard geofence is a simple circular or polygonal boundary. However, in a child's daily routine (such as traveling from **Home** to **School**), safety is defined not just by staying within static circles, but by **adhering to an intended geographic route**.

Treating a route as an infinitely thin line fails in practice due to real-world GPS inaccuracy, pavement widths, and road crossing variations. Therefore, this project implements a **geographic corridor buffer** around the polyline path with **intelligent deviation confirmation**.

---

## 2. Geodesic Mathematics & Cross-Track Calculation

### 2.1 Great-Circle Distance (Haversine Formula)
To measure true spherical distance across the Earth's surface:
$$\Delta\sigma = 2 \arcsin \sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}$$
$$d = R \cdot \Delta\sigma \quad (R = 6371000\text{ m})$$

### 2.2 Local Metric Projection & Segment Perpendicular Distance
For each line segment $A \rightarrow B$ of the polyline and child GPS position $P$:
1. Coordinates are projected into metric Cartesian offsets relative to segment origin $A$:
   $$\Delta x = (\lambda_P - \lambda_A) \cdot 111412.84 \cdot \cos\left(\frac{\phi_P + \phi_A + \phi_B}{3}\right)$$
   $$\Delta y = (\phi_P - \phi_A) \cdot 111132.954$$
2. The normalized projection factor $t$ along segment $AB$ is calculated and clamped to $[0, 1]$:
   $$t = \text{clamp}\left(\frac{(P - A) \cdot (B - A)}{|B - A|^2}, 0, 1\right)$$
3. The closest point $P_{\text{closest}} = A + t(B - A)$.
4. The minimum cross-track distance to the route is:
   $$d_{\text{min}} = \min_{i} |P - P_{\text{closest}, i}|$$

---

## 3. Corridor States
* $d_{\text{min}} \le 0.8 \times \text{corridor\_width}$: **`ON_ROUTE`**
* $0.8 \times \text{corridor\_width} < d_{\text{min}} \le \text{corridor\_width}$: **`NEAR_BOUNDARY`**
* $d_{\text{min}} > \text{corridor\_width}$: **`OUTSIDE_ROUTE`**
* Distance to destination $\le 30\text{m}$: **`ROUTE_COMPLETED`**

---

## 4. Multi-Reading Deviation Confirmation Filter

To prevent false alarms caused by temporary multipath GPS jumps (e.g. walking near tall buildings):
1. **GPS Accuracy Guard**: If reported GPS uncertainty exceeds 50 meters, or if the uncertainty interval encompasses the corridor boundary, confirmation is withheld.
2. **Time Confirmation Window**: The child must remain outside the allowed corridor for consecutive readings spanning at least **15 seconds** (configurable via `DEVIATION_CONFIRMATION_SECONDS`).
3. **Automatic Reset**: If a subsequent reading places the child back within the corridor, the confirmation timer instantly resets to zero.

---

## 5. Verification Test Cases

| Case | Scenario | Corridor | Offset | Accuracy | Expected Result | Verified Status |
|---|---|---|---|---|---|---|
| **Case 1** | Child directly on polyline | 100m | 0m | 5m | `ON_ROUTE` | **PASSED** |
| **Case 2** | Child 50m from center line | 100m | 50m | 5m | `ON_ROUTE` | **PASSED** |
| **Case 3** | Child 150m from center line | 100m | 150m | 5m | `OUTSIDE_ROUTE` | **PASSED** |
| **Case 4** | One bad GPS reading outside, then back inside | 100m | 140m $\rightarrow$ 20m | 5m | `NO CONFIRMED DEVIATION` | **PASSED** |
| **Case 5** | Child remains outside for $>15$s | 100m | 145m | 5m | `DEVIATION_CONFIRMED` | **PASSED** |
| **Case 6** | Deviation + possible fall | 100m | 150m + Fall | 5m | `HIGH_RISK` / `CRITICAL` | **PASSED** |
| **Case 7** | SOS while outside route | 100m | 150m + SOS | 5m | `CRITICAL` | **PASSED** |

