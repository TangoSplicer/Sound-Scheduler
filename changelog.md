# Changelog

## [1.2.2] — 14 August 2026

This layout patch fixes the home-screen primary action on edge-to-edge Android devices.

| Area | Change |
| --- | --- |
| Navigation-safe layout | Applies live system-bar and display-cutout insets to the home-screen content container. |
| Primary action | Keeps **Add sound routine** fully above gesture and three-button navigation areas while retaining the established 20 dp visual padding. |
| Quality | Re-runs production unit tests, Android lint, and debug packaging after the safe-area update. |

## [1.2.1] — 14 August 2026

This compatibility patch fixes scheduled sound-mode changes on Android 17 devices, including the reported Pixel 8 case where **Modes access** was allowed but a background time routine remained on Ring.

| Area | Change |
| --- | --- |
| Android 17 compatibility | Routes time and location routine execution through a short-lived foreground service so Android background audio hardening does not silently ignore `AudioManager.setRingerMode()`. [1] |
| Foreground execution | Declares the required foreground-service permissions and `mediaPlayback` type, publishes a brief low-priority status notification, applies the requested mode, then stops immediately. |
| Trigger handling | Keeps exact-alarm and geofence receivers lightweight; they now hand off only the routine identifier and validated transition to the execution service. |
| Diagnostics | Adds a documented Pixel Android 17 root-cause record and a physical-device regression check for background time routines. |

## [1.2.0] — 13 August 2026

This feature release adds private, local **place routines** alongside established time routines. Users can now set the device to Ring, Vibrate, or Silent upon arrival at or departure from a captured place without creating an account or sending location data to a service.

| Area | Change |
| --- | --- |
| Place routines | Adds arrival and departure triggers with selectable 100 m, 150 m, 250 m, and 500 m local radii. |
| Privacy | Captures a device location only on user request, stores coordinates and private labels locally, and performs no geocoding, location sharing, analytics, or backend synchronization. |
| Permission flow | Requests precise foreground location for point capture and directs users to Android app settings for the separate background “Allow all the time” consent needed while the app is closed. |
| Geofence lifecycle | Registers only enabled, valid local routines; removes paused and deleted routines; restores eligible geofences after device restart and app update. |
| Sound-mode execution | Validates geofence transitions against the stored arrival/departure setting, applies the selected device mode through the existing sound-control safeguards, and sends optional quiet confirmations. |
| Data safety | Introduces a non-destructive Room 2→3 migration for coordinates, radius, and transition data while retaining existing routines. |
| Quality | Adds deterministic unit coverage for location data validation and geofence request IDs, expands the physical-device acceptance plan, and completes production debug build, unit-test, and lint validation. |

## References

[1]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers: Background audio hardening"

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
