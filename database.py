from __future__ import annotations

import json
import secrets
import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterator


SCHEMA = """
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS devices (
    device_id TEXT PRIMARY KEY,
    child_id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS safety_events (
    event_id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL REFERENCES devices(device_id),
    event_json TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level TEXT NOT NULL,
    case_id TEXT,
    ai_json TEXT NOT NULL,
    created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS emergency_cases (
    case_id TEXT PRIMARY KEY,
    device_id TEXT NOT NULL REFERENCES devices(device_id),
    child_id TEXT NOT NULL,
    event_id TEXT NOT NULL REFERENCES safety_events(event_id),
    status TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level TEXT NOT NULL,
    reason TEXT NOT NULL,
    ai_json TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    authority_id TEXT,
    resolution_note TEXT
);
"""


class Database:
    def __init__(self, database_path: Path):
        self.path = database_path

    def initialize(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.connection() as conn:
            conn.executescript(SCHEMA)

    @contextmanager
    def connection(self) -> Iterator[sqlite3.Connection]:
        conn = sqlite3.connect(self.path)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA foreign_keys = ON")
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    def register_device(self, device_id: str, child_id: str, display_name: str) -> str:
        token = secrets.token_urlsafe(32)
        now = datetime.now(timezone.utc).isoformat()
        with self.connection() as conn:
            conn.execute(
                "INSERT INTO devices(device_id, child_id, display_name, token, created_at) VALUES (?, ?, ?, ?, ?) "
                "ON CONFLICT(device_id) DO UPDATE SET child_id=excluded.child_id, display_name=excluded.display_name, token=excluded.token",
                (device_id, child_id, display_name, token, now),
            )
        return token

    def device_by_token(self, token: str) -> sqlite3.Row | None:
        with self.connection() as conn:
            return conn.execute("SELECT * FROM devices WHERE token = ?", (token,)).fetchone()

    def device_by_id(self, device_id: str) -> sqlite3.Row | None:
        with self.connection() as conn:
            return conn.execute("SELECT * FROM devices WHERE device_id = ?", (device_id,)).fetchone()

    def event(self, event_id: str) -> sqlite3.Row | None:
        with self.connection() as conn:
            return conn.execute("SELECT * FROM safety_events WHERE event_id = ?", (event_id,)).fetchone()

    def store_event(self, *, event_id: str, device_id: str, event_json: dict, score: int, level: str, case_id: str | None, ai_json: dict) -> None:
        with self.connection() as conn:
            conn.execute(
                "INSERT INTO safety_events(event_id, device_id, event_json, risk_score, risk_level, case_id, ai_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (event_id, device_id, json.dumps(event_json), score, level, case_id, json.dumps(ai_json), datetime.now(timezone.utc).isoformat()),
            )

    def create_case(self, *, case_id: str, device_id: str, child_id: str, event_id: str, score: int, level: str, reason: str, ai_json: dict) -> None:
        now = datetime.now(timezone.utc).isoformat()
        with self.connection() as conn:
            conn.execute(
                "INSERT INTO emergency_cases(case_id, device_id, child_id, event_id, status, risk_score, risk_level, reason, ai_json, created_at, updated_at) VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, ?)",
                (case_id, device_id, child_id, event_id, score, level, reason, json.dumps(ai_json), now, now),
            )

    def link_event_to_case(self, event_id: str, case_id: str) -> None:
        with self.connection() as conn:
            conn.execute("UPDATE safety_events SET case_id = ? WHERE event_id = ?", (case_id, event_id))

    def update_ai_analysis(self, event_id: str, case_id: str | None, ai_json: dict) -> None:
        with self.connection() as conn:
            conn.execute("UPDATE safety_events SET ai_json = ? WHERE event_id = ?", (json.dumps(ai_json), event_id))
            if case_id:
                conn.execute("UPDATE emergency_cases SET ai_json = ?, updated_at = ? WHERE case_id = ?", (json.dumps(ai_json), datetime.now(timezone.utc).isoformat(), case_id))

    def cases(self) -> list[sqlite3.Row]:
        with self.connection() as conn:
            return conn.execute("SELECT * FROM emergency_cases ORDER BY created_at DESC").fetchall()

    def case(self, case_id: str) -> sqlite3.Row | None:
        with self.connection() as conn:
            return conn.execute("SELECT * FROM emergency_cases WHERE case_id = ?", (case_id,)).fetchone()

    def update_case(self, case_id: str, status: str, authority_id: str, note: str) -> sqlite3.Row | None:
        now = datetime.now(timezone.utc).isoformat()
        with self.connection() as conn:
            conn.execute(
                "UPDATE emergency_cases SET status=?, authority_id=?, resolution_note=?, updated_at=? WHERE case_id=?",
                (status, authority_id, note, now, case_id),
            )
            return conn.execute("SELECT * FROM emergency_cases WHERE case_id = ?", (case_id,)).fetchone()
