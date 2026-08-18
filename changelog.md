# Changelog

## [2.0.2] — 18 August 2026

This maintenance release addresses the update-only crash pattern: a clean install opened, while an app update with enabled existing routines could fail before the first user launch.

| Area | Change |
| --- | --- |
| Update safety | Removes `MY_PACKAGE_REPLACED` from the background reschedule receiver. An APK update no longer opens the routine database or reschedules enabled routines before the user deliberately opens Sound Scheduler. |
| Existing routines | Existing routines, activity history, and automation state stay in the on-device database. On the first post-update launch, the existing visible activity resumes scheduling through the established user-visible lifecycle. |
| Migration validation | Adds Android-level database tests that open real version-1 and version-8 Room databases through the production builder, verify migrated daily routines, and launch the home screen after an upgrade. |
| Regression protection | Adds a package-replacement receiver test that verifies an update cannot access or migrate the routine database in the background. |

## [2.0.1] — 17 August 2026

This emergency maintenance release addresses a startup regression reported after the v2.0.0 update. It is designed as an **in-place update**: it does not clear or recreate the existing Sound Scheduler database.

| Area | Change |
| --- | --- |
| Startup safety | Removes the unready Bluetooth background receiver path from the installed manifest. This path was not yet exposed through the routine editor and could run without the required Android Bluetooth permission flow. |
| Data preservation | Retains the version-12 Room schema and all existing routines, execution history, and automation state. |
| Migration evidence | Adds regression checks that upgrade representative v1.3 and v1.4 databases with two enabled daily routines to the v2 schema and verify the routines remain readable and scheduled. |
| Quality | Passes the full production unit-test, lint, and debug APK packaging suite. |

## [1.4.0] — 16 August 2026

This release adds precision scheduling with selected weekdays, manual automation overrides, and power-user convenience features including routine duplication and a Quick Settings tile.

| Area | Change |
| --- | --- |
| Precision scheduling | Adds individual weekday selection (Mon–Sun) for weekly routines. |
| Next-run previews | Displays the next scheduled occurrence time directly on each routine card. |
| Manual overrides | Adds **Pause until...** with 1-hour, 4-hour, and until-morning options. Skips sound-mode changes during the pause window and records them as **Paused** in activity history. |
| Quick Settings tile | Adds a toggleable **Sound automation** tile to the Android notification shade for rapid pause and resume. |
| Routine duplication | Adds a **Duplicate** action to each routine card to quickly create similar schedules. |
| Data migration | Updates the Room database to version 8 with non-destructive migrations for specific weekdays and temporary pause state. |
| Quality | Adds regression tests for weekday-based alarm calculations and passes full production validation. |

## [1.3.0] — 15 August 2026

This feature release makes automation state visible and truthful on Android 17, adds private local activity history, supports safe routine editing, and introduces power-connection routines. It does not add accounts, analytics, network services, or location sharing.

| Area | Change |
| --- | --- |
| Truthful Android 17 automation | Background alarms, geofences, and power broadcasts no longer create foreground eligibility themselves. When the active automation service is not armed, the app records a clear **re-arm required** result and notifies the user instead of implying that a ringer-mode write succeeded. |
| Automation control card | Adds an always-visible home-screen state card with **Active**, **Paused**, **Off**, and **Needs attention** states, plus Pause all, Resume automation, Re-arm, and Activity actions. |
| Global pause and recovery | Pause all cancels registrations without deleting routines and remembers which routines were enabled. Resume restores only those routines, avoiding unintended activation of previously paused routines. |
| Activity and last run | Adds a local-only activity screen, attention filter, clear-history confirmation, and per-routine last-run summary. History retains at most 30 days or 100 events, stores no coordinates, addresses, device identifiers, or raw system messages, and remains solely on the device. |
| Routine editing | Adds editing for time, place, and charging routines while preserving the routine identity and its local history. Existing alarm and geofence registrations are refreshed after an edit. |
| Conflict safeguard | Warns before enabled time routines with the exact same trigger time request different ringer modes, while retaining an explicit Save anyway choice for deliberate configurations. |
| Charging routines | Adds local routines for power connected and power disconnected events. These events dispatch only to an already-armed automation service and require no new runtime permission. |
| Data migration | Updates the Room database to version 6 with non-destructive migrations for local execution history, automation state, global-pause restoration, and charging transition fields. |
| Quality | Adds contract tests for charging routines and execution-history taxonomy, then passes production unit tests, Android lint, and production debug APK packaging. |

## [1.2.4] — 14 August 2026

This Android 17 lifecycle correction replaces the ineffective alarm-started service approach with a user-visible automation service that begins while the app is open.

| Area | Change |
| --- | --- |
| Active automation | Starts a visible foreground service while the user is in Sound Scheduler whenever one or more routines are enabled. |
| Android 17 capability | Keeps the service active while automation is enabled, so scheduled and place-triggered ringer-mode writes use a foreground lifecycle established by explicit user interaction. |
| Notification clarity | Shows a persistent, low-priority **Sound Scheduler is active** notification while enabled background automation is ready. |
| Battery and control | Stops the service when the final enabled routine is completed, paused, or deleted. |

## [1.2.3] — 14 August 2026

This corrective build addresses the observed Pixel false-success case in which a background time routine was consumed even though the phone remained in Ring mode.

| Area | Change |
| --- | --- |
| Durable confirmation | Holds the brief foreground execution lifecycle through a bounded confirmation window and verifies that the requested ringer mode remains stable before reporting success. |
| One-time safety | A routine is marked complete only after stable confirmation. If Android does not keep the requested mode, the routine remains visible and enabled for investigation or retry. |
| Trigger consistency | Applies the same durable confirmation to both time and place routine execution. |

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
