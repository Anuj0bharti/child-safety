# AI Contextual Analysis System

## 1. Role of AI in Child Safety
Artificial Intelligence in the Child Safety Band system provides **contextual interpretation of anomalous safety events**. It does not replace the deterministic safety pipeline or delay manual emergency triggers. 

### Critical Safety Principles:
1. **Never a Single Point of Failure**: If an AI provider is unreachable, times out, or quota is exhausted, the deterministic risk engine independently drives alerts and dispatches emergencies.
2. **Post-Escalation Execution**: When manual SOS is pressed, the emergency case is provisioned immediately. AI contextual analysis runs concurrently or after escalation to furnish dispatchers and parents with structured insight.
3. **No Medical Diagnosis**: The AI interprets telemetry strictly from a child safety and situational perspective.

---

## 2. Supported Providers

### 2.1 Google Gemini (`GeminiProvider`)
* Utilizes the `gemini-1.5-flash` model.
* Enforces native JSON structured output schema:
  * `risk_level`: `"SAFE"` | `"WARNING"` | `"HIGH"` | `"CRITICAL"`
  * `risk_score`: Integer `0` to `100`
  * `reason`: Concise natural language explanation
  * `recommended_action`: Actionable recommendation for parent or authority
  * `confidence`: Float `0.0` to `1.0`

### 2.2 NVIDIA Nemotron (`NemotronProvider`)
* Utilizes NVIDIA NIM OpenAI-compatible API (`https://integrate.api.nvidia.com/v1/chat/completions`) with `nvidia/nemotron-4-340b-instruct`.
* Uses `response_format: {"type": "json_object"}`.

---

## 3. Configuration in `.env`
```ini
AI_PROVIDER=gemini       # Options: "gemini" or "nemotron"
AI_MODEL=gemini-1.5-flash
GEMINI_API_KEY=AIzaSy...
NEMOTRON_API_KEY=nvapi-...
```

---

## 4. Structured Context Payload Example
```json
{
  "childId": "CHILD-001",
  "sos": false,
  "recentEvents": [
    {"type": "route_deviation", "timestamp": "2026-09-10T21:40:00Z"},
    {"type": "possible_fall", "timestamp": "2026-09-10T21:40:12Z"}
  ],
  "location": {
    "available": true,
    "insideSafeZone": false,
    "insideSafeRoute": false,
    "distanceFromRouteMeters": 145.2
  },
  "batteryPercent": 84,
  "networkTransport": "CELLULAR"
}
```

## 5. Validated Structured Output Example
```json
{
  "risk_level": "HIGH",
  "risk_score": 85,
  "reason": "Child deviated 145m outside the safe route corridor followed immediately by an accelerometer impact spike indicating a possible fall.",
  "recommended_action": "Call the child directly or notify nearest school/security personnel along the route corridor.",
  "confidence": 0.94
}
```

