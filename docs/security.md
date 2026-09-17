# Security, Authentication & Data Privacy

## 1. Security Principles
The Child Safety Band system handles highly sensitive child location and safety telemetry. The architecture implements defense-in-depth:
1. **No Hardcoded Secrets**: Secrets, AI keys, and JWT keys are loaded strictly from `.env` environment variables.
2. **Password Security**: Passwords are never stored in plaintext. They are salted and hashed using **bcrypt**.
3. **Role-Based Access Control (RBAC)**: Enforces distinct scopes (`PARENT`, `AUTHORITY`, `ADMIN`). Parents can only access their assigned children. Authorities only inspect active emergency incidents.
4. **Device Pairing Security**: Smartwatches generate randomized 6-character uppercase alphanumeric pairing tokens that expire upon successful pairing, eliminating universal shared secrets.
5. **Data Minimization & Privacy**: Child identities are pseudonymous (`CHILD-001`). Location history is retrievable only with valid Bearer JWT credentials.

---

## 2. Authentication Flow

```
Client (Parent / Authority)                   Backend
        │                                        │
        ├─────── POST /auth/login ──────────────►│ (Validates bcrypt hash)
        │                                        │
        │◄────── Returns JWT Bearer Token ───────┤ (HS256 signed with 24h expiry)
        │                                        │
        ├─────── GET /children (Bearer Token) ──►│ (Decodes sub & role)
        │                                        │
```

---

## 3. Audit Logging
Key actions (device pairing, emergency creation, case acknowledgments, case resolutions, dispatcher notes) are written to the database `audit_logs` table with actor IDs and UTC timestamps to ensure complete accountability.

