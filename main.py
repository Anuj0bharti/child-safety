from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware

from .ai import provider_for
from .config import settings
from .database import Database
from .realtime import manager
from .schemas import DemoEventIn, DeviceRegistrationIn, DeviceRegistrationOut, EmergencyActionIn, EmergencyCaseOut, EventReceipt, SafetyEventIn
from .services import case_out, complete_ai_analysis, ingest_event


database = Database(settings.database_path)
ai_provider = provider_for(settings)


@asynccontextmanager
async def lifespan(app: FastAPI):
    database.initialize()
    yield


app = FastAPI(title="Child Safety Band API", version="0.1.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


def current_device(x_device_token: Annotated[str | None, Header()] = None):
    if not x_device_token:
        raise HTTPException(status_code=401, detail="Missing X-Device-Token")
    device = database.device_by_token(x_device_token)
    if not device:
        raise HTTPException(status_code=401, detail="Invalid device token")
    return device


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "online", "ai_provider": settings.ai_provider}


@app.post("/api/v1/devices/register", response_model=DeviceRegistrationOut)
def register_device(payload: DeviceRegistrationIn) -> DeviceRegistrationOut:
    token = database.register_device(payload.device_id, payload.child_id, payload.display_name)
    return DeviceRegistrationOut(device_id=payload.device_id, device_token=token)


async def _ingest(device, payload: SafetyEventIn) -> EventReceipt:
    receipt, case = await ingest_event(database, device, payload)
    if case and not receipt.duplicate:
        await manager.broadcast({"type": "EMERGENCY_CREATED", "case": case.model_dump(mode="json")})
    if not receipt.duplicate:
        async def analyze() -> None:
            analysis = await complete_ai_analysis(database, ai_provider, payload, receipt.emergency_case_id)
            await manager.broadcast({"type": "AI_ANALYSIS_UPDATED", "event_id": receipt.event_id, "case_id": receipt.emergency_case_id, "analysis": analysis.model_dump(mode="json")})
        asyncio.create_task(analyze())
    return receipt


@app.post("/api/v1/events", response_model=EventReceipt)
async def receive_event(payload: SafetyEventIn, device=Depends(current_device)) -> EventReceipt:
    return await _ingest(device, payload)


@app.post("/api/v1/demo/events", response_model=EventReceipt)
async def inject_demo_event(payload: DemoEventIn, x_demo_key: Annotated[str | None, Header()] = None) -> EventReceipt:
    if x_demo_key != settings.demo_api_key:
        raise HTTPException(status_code=401, detail="Invalid demo key")
    if payload.mode != "DEMO":
        raise HTTPException(status_code=422, detail="Demo endpoint requires mode=DEMO")
    device = database.device_by_id(payload.device_id)
    if not device:
        raise HTTPException(status_code=404, detail="Register the demo device first")
    event = SafetyEventIn(**payload.model_dump(exclude={"device_id"}))
    return await _ingest(device, event)


@app.get("/api/v1/emergencies", response_model=list[EmergencyCaseOut])
def emergencies() -> list[EmergencyCaseOut]:
    return [case_out(row, database) for row in database.cases()]


@app.post("/api/v1/emergencies/{case_id}/acknowledge", response_model=EmergencyCaseOut)
async def acknowledge(case_id: str, payload: EmergencyActionIn) -> EmergencyCaseOut:
    row = database.update_case(case_id, "ACKNOWLEDGED", payload.authority_id, payload.note)
    if not row:
        raise HTTPException(status_code=404, detail="Emergency case not found")
    case = case_out(row, database)
    await manager.broadcast({"type": "EMERGENCY_ACKNOWLEDGED", "case": case.model_dump(mode="json")})
    return case


@app.post("/api/v1/emergencies/{case_id}/resolve", response_model=EmergencyCaseOut)
async def resolve(case_id: str, payload: EmergencyActionIn) -> EmergencyCaseOut:
    row = database.update_case(case_id, "RESOLVED", payload.authority_id, payload.note)
    if not row:
        raise HTTPException(status_code=404, detail="Emergency case not found")
    case = case_out(row, database)
    await manager.broadcast({"type": "EMERGENCY_RESOLVED", "case": case.model_dump(mode="json")})
    return case


@app.websocket("/ws/emergencies")
async def emergency_socket(websocket: WebSocket) -> None:
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)
