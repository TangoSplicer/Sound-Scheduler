# Sound-Mode Routine Design

## Product behavior

A routine is a locally stored time rule with one target device sound mode. At the selected time, the application changes the ringer mode to **Ring**, **Vibrate**, or **Silent**. One-time routines retire after a successful change; recurring routines compute and schedule their next occurrence.

| User-facing mode | Stored value | Android target |
| --- | --- | --- |
| Ring | `ring` | `AudioManager.RINGER_MODE_NORMAL` |
| Vibrate | `vibrate` | `AudioManager.RINGER_MODE_VIBRATE` |
| Silent | `silent` | `AudioManager.RINGER_MODE_SILENT` |

## Routine lifecycle

1. The user names a routine, chooses a time, target sound mode, and optional recurrence.
2. The app persists the routine and schedules an `AlarmManager` broadcast.
3. At delivery, the receiver checks notification-policy access and attempts the requested ringer-mode change.
4. On success, it emits a low-priority confirmation notification when notification access is available. One-time routines are marked complete; recurring routines are scheduled again.
5. If notification-policy access is unavailable or Android rejects the change, the routine is retained and the app exposes a clear action to open system access settings. The routine never silently reports success.
6. Active routines are reconstructed after boot, package update, resume, exact-alarm permission change, and notification-policy access change.

## Access and failure behavior

`MODIFY_AUDIO_SETTINGS` is declared for sound-mode control. Notification-policy access is user-granted special access, not a runtime permission dialog; the app checks `NotificationManager.isNotificationPolicyAccessGranted()` before every scheduled change. Missing access produces a durable user-visible state, a recoverable notification when possible, and a direct route to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`.

Notifications are secondary confirmation only. A notification denial must not prevent an authorized sound-mode routine from changing the phone mode.

## Data compatibility

The existing `soundProfile` database field is retained to avoid destructive migration. `normal` is treated as the legacy representation of `ring`; `custom` is no longer selectable. New routines store only `ring`, `vibrate`, or `silent`.

## Interface

The home screen reports the current device mode and whether policy access is enabled. A dedicated settings button opens the access page when needed. The routine editor adds a target-mode selector. Each routine row identifies its target mode before its scheduled time and recurrence.
