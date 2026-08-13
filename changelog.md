# Changelog

## [1.1.1] — 13 August 2026

This maintenance and product-quality update fixes the special-access blocker found in the debug APK and adds professional routine-management controls.

| Area | Change |
| --- | --- |
| Notification Policy access | Declares `ACCESS_NOTIFICATION_POLICY`, allowing Sound Scheduler to appear in Android’s Do Not Disturb access list. |
| Access recovery | Confirms the grant result after users return from Android settings and reconstructs future routines after approval. |
| Direct controls | Adds home-screen Ring, Vibrate, and Silent quick controls. |
| Routine management | Adds persisted pause and resume controls that cancel or restore alarm delivery without deleting a routine. |
| Data safety | Adds a non-destructive Room migration to retain existing routines while introducing the enabled-state field. |
| Quality | Extends tests and device acceptance coverage; updates product and troubleshooting documentation. |

## [1.1.0] — 13 August 2026

Sound Scheduler pivoted from alert reminders to local routines that set the phone to Ring, Vibrate, or Silent on one-time, daily, weekly, and monthly schedules. This release introduced durable alarms, local Room persistence, reboot recovery, exact-alarm fallback, quiet optional status notifications, and the custom launcher identity.
