# Sound Scheduler 2.0.1 — Production Readiness Assessment

**Prepared by:** Manus AI

**Assessment date:** 17 August 2026

**Scope:** Emergency startup-safety repair, data-preserving database upgrades, Android 17 safeguards, local execution history, and build validation.

## Executive assessment

Sound Scheduler **2.0.1** is an emergency maintenance update following a startup crash reported against the v2.0.0 debug build. It removes the unready Bluetooth background receiver path while preserving the existing Room database, routines, activity history, and automation state. The repair retains the established privacy-first, on-device-only architecture and truthful Android 17 automation dispatch model.

> **Release decision:** The source is ready for CI verification and staged physical-device acceptance. It is **not yet ready for public distribution** because final Android 13+ physical-device acceptance and owner-controlled release signing remain outstanding.

| Release dimension | Status | Assessment |
| --- | --- | --- |
| Local implementation | Hotfix complete | The unready Bluetooth receiver is removed from the installed manifest; current user-visible v1.4 capabilities remain intact. |
| Automated validation | Passed | Production unit tests, Android lint, and production debug APK packaging passed on 17 August 2026. |
| Database upgrade validation | Passed | Representative version-6 and version-8 databases containing two enabled daily routines upgraded to schema version 12 with all routines preserved and readable. |
| Android 17 integrity | Implemented | Background triggers use only an already-armed foreground automation service; unarmed triggers surface a recoverable re-arm state. |
| Privacy | Complete | No account, backend, analytics, advertising SDK, geocoding, location sharing, or location history is introduced. |
| Device acceptance | Pending | Android system access, timing, geofence, charging broadcast, and active-service behavior must be verified on a physical Android 13+ device. |
| Release signing | Pending owner | The sandbox cannot create a production-signed artifact without the owner-controlled keystore. |

## Product and safety controls

| Area | Completed behavior |
| --- | --- |
| Core routine action | Each routine persists an explicit `ring`, `vibrate`, or `silent` target. Legacy local `normal` and `custom` values remain readable and normalize to Ring. |
| Time routines | Supports one-time, daily, weekly, and monthly schedules, exact-alarm deferral reporting, and rescheduling for recurring routines. |
| Place routines | Supports user-captured, locally stored arrival and departure geofences with 100 m, 150 m, 250 m, or 500 m radii. |
| Charging routines | Supports local **Power connected** and **Power disconnected** triggers without an additional runtime permission. |
| Android sound access | `SoundModeController` checks Notification Policy access before every routine-triggered mode change and handles rejection safely. [1] [2] |
| Truthful Android 17 dispatch | Alarms, geofences, and power receivers dispatch only to an existing active foreground automation service. They never start foreground eligibility from the background. An unavailable service results in a local **automation re-arm required** record and user-facing recovery path rather than a false applied result. [6] |
| Automation card | The home screen reports **Active**, **Paused**, **Off**, or **Needs attention**, and offers Pause all, Resume automation, Re-arm, and Activity actions. |
| Global pause | Pause all cancels active registrations while recording exactly which routines were enabled. Resume restores only that saved set, protecting individually paused routines. |
| Local activity history | The local Room history records bounded outcome codes and trigger types, supports an attention filter and clearing, and retains at most 30 days or 100 records. It contains no coordinates, address, device identifier, or raw system messages. |
| Routine health | Routine cards display the latest local outcome, requested/observed sound mode where applicable, and last-run status. |
| Editing and conflicts | Routine editing preserves the row identity and history while refreshing time/place registration. A warning is shown before conflicting enabled time routines at the exact same time request different modes. |
| Permission design | Precise foreground location is requested only for an explicit place capture. Background geofence consent remains a separate Android settings action. [3] |
| Privacy | All state remains in the local Room database. The app does not transmit coordinates, retain a location trail, perform reverse geocoding, or expose data to a server. |
| Notifications | Persistent active-automation status is visible while armed. Other status notifications are quiet; notification denial does not itself prevent an authorized ringer-mode change. [5] |

## Completed validation evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Release metadata | Passed | `versionCode 12`, `versionName 2.0.1` are defined for the production flavor. |
| Production unit tests | Passed | `:app:testProdDebugUnitTest` completed as part of the final 44-second validation suite. Coverage includes existing scheduling/location validation plus charging-routine and execution-history model contracts. |
| Production lint | Passed | `:app:lintProdDebug` completed; HTML report generated at `app/build/reports/lint-results-prodDebug.html`. |
| Production debug package | Passed | `:app:assembleProdDebug` completed and produced `app/build/outputs/apk/prod/debug/app-prod-debug.apk`. |
| Room schema | Validated | Database version 12 retains non-destructive migrations from versions 6 and 8, including the preservation check for two active daily time routines. |
| Privacy review | Passed | New activity data are bounded code values and timestamps; the schema excludes coordinates, addresses, device IDs, and raw system messages from execution history. |
| Automated repository workflow | Ready | The GitHub workflow runs unit tests, lint, and debug/release packaging for supported pushes and pull requests. |
| Physical-device acceptance | Pending owner/device test | Required for Android platform policy access, ringer-mode writes, power broadcasts, exact alarm behavior, and geofence delivery. |
| Owner release signing | Pending owner credential | A production release cannot be signed without the owner-controlled keystore and certificate verification. |

## Required physical-device release gates

| Gate | Completion criterion |
| --- | --- |
| Android sound-policy access | Verify the app opens Android’s policy-access screen and no routine reports a successful mode change without access. |
| Active automation lifecycle | With an enabled routine, verify the Automation card reports Active and the persistent active notification is present. Swipe the app away normally and verify the service remains eligible. |
| Android 17 time routine | On a Pixel Android 17 device, perform Ring → Vibrate and Ring → Silent time routines while active automation is armed; verify the device state and Activity result. [6] |
| Re-arm truthfulness | Reproduce an unarmed automation state, allow a time, place, or charging trigger, and verify the result is re-arm required rather than applied. Re-arm and repeat successfully. |
| Activity and last-run status | Verify successful and attention outcomes appear in Activity; verify clear history removes records only; verify a routine card exposes the latest outcome. |
| Global pause and resume | Verify Pause all cancels registrations without deletion and Resume restores only routines active when global pause began. |
| Routine editing and conflict warning | Verify edited time/place routines refresh their registrations and conflicting same-time, different-mode time routines show a deliberate warning. |
| Charging routines | Verify one connected and one disconnected power routine while Active, including logged trigger type and selected final ringer mode. |
| Place permissions and triggers | Confirm explicit foreground place capture, Android background-location consent, selected radius, and arrival/departure results on a real route. [3] [4] |
| Exact-alarm fallback | Disable exact-alarm access, confirm deferred behavior is reported safely, then restore access and verify future time routine recovery. [7] |
| Notification independence | Deny `POST_NOTIFICATIONS`, then confirm an authorized armed routine still changes the ringer mode; document expected notification limitations. [5] |
| Signing | Supply the owner-controlled release keystore, create the final signed package, and verify it with `apksigner verify --verbose --print-certs`. |

## Release hand-off

The production debug APK is appropriate for controlled installation and functional testing. It is debug-signed and therefore is **not** a store-distribution artifact. The release build remains unsigned in this environment until the owner provides an untracked `keystore.properties` file with the keystore path, store password, key alias, and key password.

The detailed physical-device procedure is maintained in [testplan.md](testplan.md). This report should be updated with the device model, Android version, access state, Automation-card state, trigger type, and observed outcome during owner sign-off.

## References

[1] [Android Developers — AudioManager API reference](https://developer.android.com/reference/android/media/AudioManager)

[2] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)

[3] [Android Developers — Request background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)

[4] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[5] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

[6] [Android Developers — Background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)

[7] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
