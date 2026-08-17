#!/usr/bin/env python3
import json
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app" / "schemas" / "com.soundscheduler.app.data.AppDatabase"
DB_PATH = ROOT / "build" / "migration-v8-to-v12-test.db"


def schema(path: Path):
    with path.open() as f:
        return json.load(f)["database"]


def create_database_from_schema(conn, db_schema):
    for entity in db_schema["entities"]:
        sql = entity["createSql"].replace("${TABLE_NAME}", entity["tableName"])
        conn.execute(sql)
        for index in entity.get("indices", []):
            conn.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))


def table_columns(conn, table):
    return {
        row[1]: {"type": row[2], "notnull": bool(row[3]), "default": row[4]}
        for row in conn.execute(f"PRAGMA table_info({table})")
    }


def verify_upgrade(start_version: int):
    start_schema = schema(SCHEMAS / f"{start_version}.json")
    v12 = schema(SCHEMAS / "12.json")
    DB_PATH.unlink(missing_ok=True)
    conn = sqlite3.connect(DB_PATH)
    try:
        create_database_from_schema(conn, start_schema)
        routine_columns = table_columns(conn, "routines")
        insert_columns = [
            "title", "type", "time", "isCompleted", "soundProfile", "isEnabled",
            "wasEnabledBeforeGlobalPause", "recurrence"
        ]
        if "daysOfWeek" in routine_columns:
            insert_columns.append("daysOfWeek")
        placeholders = ", ".join("?" for _ in insert_columns)
        insert_sql = f"INSERT INTO routines ({', '.join(insert_columns)}) VALUES ({placeholders})"
        for title, time, mode in [
            ("Morning ring", 1760000000000, "ring"),
            ("Evening vibrate", 1760040000000, "vibrate"),
        ]:
            values = [title, "time", time, 0, mode, 1, 0, "daily"]
            if "daysOfWeek" in routine_columns:
                values.append(None)
            conn.execute(insert_sql, values)

        migration_sql = []
        if start_version < 7:
            migration_sql.append("ALTER TABLE routines ADD COLUMN daysOfWeek TEXT")
        if start_version < 8:
            migration_sql.append("ALTER TABLE automation_state ADD COLUMN pauseUntilMillis INTEGER")
        if start_version < 9:
            migration_sql.extend([
                "ALTER TABLE routines ADD COLUMN bluetoothDeviceAddress TEXT",
                "ALTER TABLE routines ADD COLUMN wifiSsid TEXT",
            ])
        if start_version < 10:
            migration_sql.extend([
                "ALTER TABLE routines ADD COLUMN calendarKeyword TEXT",
                "ALTER TABLE routines ADD COLUMN calendarBufferMinutes INTEGER NOT NULL DEFAULT 5",
            ])
        if start_version < 11:
            migration_sql.extend([
                "ALTER TABLE routines ADD COLUMN batteryThreshold INTEGER",
                "ALTER TABLE routines ADD COLUMN batteryTriggerDirection TEXT",
            ])
        if start_version < 12:
            migration_sql.append("ALTER TABLE routines ADD COLUMN webhookUrl TEXT")
        for statement in migration_sql:
            conn.execute(statement)
        conn.execute("PRAGMA user_version = 12")
        conn.commit()

        expected = {
            field["columnName"]: {"type": field["affinity"], "notnull": field["notNull"]}
            for entity in v12["entities"] if entity["tableName"] == "routines"
            for field in entity["fields"]
        }
        actual = table_columns(conn, "routines")
        if set(expected) != set(actual):
            raise AssertionError(f"Column mismatch. Expected {set(expected)}, got {set(actual)}")
        for column, expected_definition in expected.items():
            actual_definition = actual[column]
            if actual_definition["type"] != expected_definition["type"] or actual_definition["notnull"] != expected_definition["notnull"]:
                raise AssertionError(
                    f"Schema mismatch for {column}: expected {expected_definition}, got {actual_definition}"
                )

        routines = conn.execute(
            "SELECT title, recurrence, soundProfile, calendarBufferMinutes FROM routines ORDER BY id"
        ).fetchall()
        expected_routines = [
            ("Morning ring", "daily", "ring", 5),
            ("Evening vibrate", "daily", "vibrate", 5),
        ]
        if routines != expected_routines:
            raise AssertionError(f"Routine preservation mismatch: {routines}")
        print(f"PASS: v{start_version} schema upgraded to v2 schema; two daily routines preserved and readable.")
    finally:
        conn.close()


def main():
    for start_version in (6, 8):
        verify_upgrade(start_version)


if __name__ == "__main__":
    main()
