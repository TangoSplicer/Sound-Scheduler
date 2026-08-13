# Android Sound-Mode Routine Platform Findings

**Reviewed:** 13 August 2026

Sound Scheduler will use `AudioManager` ringer modes as its stable product abstraction:

| Product mode | Android constant | Meaning |
| --- | --- | --- |
| Ring | `AudioManager.RINGER_MODE_NORMAL` | The device may ring and may vibrate. |
| Vibrate | `AudioManager.RINGER_MODE_VIBRATE` | The device is silent and may vibrate. |
| Silent | `AudioManager.RINGER_MODE_SILENT` | The device is silent and does not vibrate. |

`AudioManager` supplies ringer-mode control and exposes the three required mode constants. Android also provides `NotificationManager.isNotificationPolicyAccessGranted()` to check whether the application is allowed to modify notification policy. Because ringer changes can be restricted by the user’s Do Not Disturb policy, execution must check that access at the moment of the scheduled change, handle `SecurityException`, and guide the user to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` when access is missing.

Scheduled execution should keep the existing durable `AlarmManager` path, which survives normal process death and supports reboot reconstruction. A routine should not pretend it completed when policy access was missing: it should retain its schedule semantics, show a notification when possible, and surface a clear user-visible “access required” state on the next app open.

## Sources

1. [Android Developers — AudioManager API reference](https://developer.android.com/reference/android/media/AudioManager)
2. [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)
