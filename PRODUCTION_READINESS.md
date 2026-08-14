# Sound Scheduler Production Readiness

**Prepared by:** Manus AI

**Scope:** Sound-mode automation, private location-routine implementation, Android permission review, build hardening, automated verification, release gating, and device acceptance planning.

**Assessment date:** 14 August 2026

## Executive assessment

Sound Scheduler 1.2.1 is a **local Android sound-mode automation app**. Its stable scope includes named time routines and recurring place routines that set the phone to **Ring**, **Vibrate**, or **Silent**. Place routines operate on a user-captured local coordinate, a user-provided private label, a selected radius, and an arrival/departure transition. There are no accounts, backend services, geocoding, location sharing, analytics, or advertising SDKs.

> **Release decision:** The source is ready for automated verification and physical-device acceptance testing. It is **not yet public-distribution ready** until the owner signs the rebuilt release artifact and verifies sound-mode, permission, and real-world location behavior on an Android 13+ physical device.

## Product and safety changes

| Area | Completed behavior |
| --- | --- |
| Core routine action | Each routine persists an explicit `ring`, `vibrate`, or `silent` target. Older `normal` and `custom` local values safely normalize to Ring. |
| Platform control | `SoundModeController` maps user choices to Android ringer modes, checks Notification Policy access before every change, and handles policy rejection safely. [1] [2] |
| Android 17 execution | Time and place receivers now use a short-lived `mediaPlayback` foreground service to perform the mode change, preventing Android 17 background audio hardening from silently ignoring `setRingerMode()`. [6] |
| Time execution | The alarm receiver reports success only after Android applies the requested mode. Recurring time routines queue their next occurrence; unconfirmed one-time routines remain visible. |
| Place execution | The geofence receiver validates the triggering request ID, routine state, stored coordinate/radius/transition, and matching arrival/departure event before it applies the selected mode. |
| Permission design | Precise foreground location is requested only on an explicit capture action. The app directs users to Android app settings for the separate background “Allow all the time” consent required for place routines while closed. [3] |
| Privacy | Coordinates and private labels remain in Room. The app does not perform reverse geocoding, transmit coordinates, retain a location history, or expose location to a server. |
| Geofence lifecycle | Only enabled, valid location routines are registered. Pause, deletion, boot, package replacement, and resumption rebuild or remove local geofences appropriately. [4] |
| User recovery | The home screen shows current phone mode and access health, opens the relevant system settings page when needed, and reports the access result after return. |
| Notifications | Notifications are optional, quiet status confirmations. Denying them does not prevent an authorized mode change. [5] |
| App identity | The custom adaptive and legacy launcher icon remains applied across density buckets. |

## Completed validation evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Production debug compilation | Passed | `:app:assembleProdDebug` completed after the Android 17 foreground-execution patch. |
| Production unit tests | Passed | `:app:testProdDebugUnitTest` completed with existing scheduling tests plus valid/invalid location data and geofence request-ID coverage. |
| Production lint | Passed | `:app:lintProdDebug` completed after the foreground-service manifest, service, receiver, resource, and documentation changes. |
| Debug package | Passed | `:app:assembleProdDebug` produced an installable debug-signed APK. |
| Room migration | Implemented | Non-destructive 2→3 migration adds nullable latitude, longitude, radius, and transition fields, preserving existing routine records. |
| Automated repository verification | Ready | The repository workflow compiles, tests, lints, and packages debug and release variants on supported pushes and pull requests. |
| Physical-device acceptance | Pending owner/device test | Required for Android system access, mode execution, and real-world geofence timing. |
| Owner signing | Pending owner credential | Production release signing cannot be completed without the owner-controlled release keystore. |

## Required device acceptance

| Gate | Completion criterion |
| --- | --- |
| Notification Policy access | Confirm the app opens Android’s policy-access screen and changes no mode until access is enabled. |
| Ring, Vibrate, and Silent | Verify one routine for each target produces the corresponding Android ringer mode. |
| Notification independence | Deny `POST_NOTIFICATIONS`, then confirm an authorized routine still changes the phone mode. |
| Exact-alarm fallback | Disable exact-alarm access, confirm safe deferred scheduling, then re-enable and confirm future time routines are reconstructed. |
| Android 17 background routine | On a Pixel Android 17 device with Modes access allowed, background the app and verify a time routine shows the brief foreground execution notification and changes Ring → Vibrate. |
| Foreground location | Confirm the explicit place-capture action requests precise location and has a recoverable failure path. |
| Background location | Confirm Android settings allow precise “Allow all the time” location access and active place routines are registered after return. |
| Arrival and departure | Validate a selected radius and both transition types on a physical test route, recording the device model, OS version, access state, radius, and observed result. |
| Location lifecycle | Confirm pause, resume, delete, boot, and app-update behavior correctly removes or restores the expected local geofences. |
| Signing | Supply the owner-controlled release keystore and verify the final signed artifact with `apksigner verify --verbose --print-certs`. |

## Release notes for the owner

The source declares `MODIFY_AUDIO_SETTINGS` and `ACCESS_NOTIFICATION_POLICY` for ringer-mode control. Android still subjects interruption and ringer behavior to user-managed Notification Policy access; this is not a normal runtime permission. The app uses `NotificationManager.isNotificationPolicyAccessGranted()` before every scheduled or location-triggered change and directs users to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` when access is absent. [1] [2]

Place routines are deliberately local-only. Android geofencing depends on Google Play services and can be affected by system location state and background power management. The selected 100–500 m radii and the physical-device test plan are intended to make that constraint explicit rather than claim precision the operating system cannot guarantee. [4]

Exact-alarm and notification behavior remain separate: exact-alarm access improves time-routine precision, while notification permission affects only optional status messages. [5] [7]

## References

[1] [Android Developers — AudioManager API reference](https://developer.android.com/reference/android/media/AudioManager)

[2] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)

[3] [Android Developers — Request background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)

[4] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[5] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

[6] [Android Developers — Background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)

[7] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
