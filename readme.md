# Sound Scheduler

**Sound Scheduler** is a privacy-first Android app for changing the device ringer mode to **Ring**, **Vibrate**, or **Silent** at a chosen time, when the device arrives at or departs from a user-saved place, or when power is connected or disconnected.

The application stores routine data only on the device. It has no accounts, backend, advertising SDK, analytics SDK, calendar integration, geocoding, or location sharing. Place labels are private labels chosen by the user; the app never turns coordinates into an address or transmits them.

## Stable product scope

| Capability | Behavior |
| --- | --- |
| Sound modes | Sets the Android ringer mode to Ring, Vibrate, or Silent. |
| Quick controls | Applies Ring, Vibrate, or Silent immediately from the home screen once Android sound-control access is granted. |
| Time routines | Supports one-time, daily, weekly, and monthly schedules. |
| Place routines | Supports recurring **On arrival** and **On departure** routines using a local circular geofence around a captured device location. |
| Charging routines | Supports **Power connected** and **Power disconnected** routines without requiring an additional runtime permission. |
| Place radius | Offers 100 m, 150 m, 250 m, and 500 m radii; 150 m is the default. |
| Automation control | Shows an explicit Active, Paused, Off, or Needs attention state. **Pause until...** allows temporary overrides (1h, 4h, until morning). Pause all safely suspends all registrations. |
| Precision scheduling | Supports specific weekday selection (Mon–Sun) and displays next-run previews on routine cards. |
| Quick Settings tile | Provides a toggleable **Sound automation** tile in the notification shade for rapid control. |
| Activity and last run | Records the outcome of local attempts and displays a per-routine last-run summary. Retention is capped at 30 days or 100 events, whichever is smaller; the history contains no coordinates, addresses, device identifiers, or raw system messages. |
| Editing and conflicts | Allows existing routines to be edited or duplicated. Warns before two enabled time routines at the same time request different ringer modes, while allowing a deliberate Save anyway choice. |
| Pause and resume | Temporarily pauses an individual routine without deleting it. A paused place routine is removed from Android geofencing; resuming restores eligible local geofences. |
| Recovery | Rebuilds future time routines and eligible place geofences after device restart and app update. |
| Access recovery | Opens Android’s sound-control and app settings pages when user-granted access is required, then reports the outcome on return. |
| Notifications | Optional, quiet status confirmations; notification denial does not prevent an authorized mode change. |
| Privacy | All routine data stays in the local Room database. No location data leaves the device. |

## Android access

Sound-mode changes require the manifest’s normal audio-settings capability and user-granted **Notification Policy access**. The app checks this access before each scheduled or location-triggered change and does not claim a change succeeded when Android rejects it. Android exposes this access through `NotificationManager.isNotificationPolicyAccessGranted()` and its system settings page. [1]

Place routines request **precise foreground location** only when the user elects to capture a place. To receive a geofence transition while the app is not open, users must then enable **Allow all the time** location access in Android app settings. Android treats background location as a separate consent step on Android 10 and later. [2] The app uses the captured coordinates solely to create local Google Play services geofences; it does not continuously record a location history. [3]

> Android may delay or miss a place transition when device location is disabled, Google Play services is unavailable, or the operating system applies background power constraints. Choose an appropriate radius—typically 150–500 m—and validate important routines on the target device. [3]

On Android 12 and later, exact-alarm access improves the timing precision of time routines. Without it, Android may defer a routine to preserve battery life. [4] On Android 13 and later, notifications are optional and requested only to show routine-status confirmations. [5]

On Android 17, Android can silently ignore background ringer-mode APIs. When eligible routines are enabled, Sound Scheduler is armed from an open app screen as an **active foreground automation service** and shows a persistent low-priority “Sound Scheduler is active” notification. Time, place, and charging events dispatch only to that already-armed service. If it is no longer armed, the app does not report a false success: it records **re-arm required**, shows a re-arm notification when notifications are available, and exposes the condition in the Automation card. This behavior is required even when Modes access has already been granted. [6]

## Automation, activity, and editing

The **Automation** card is the authoritative control surface for background routines. **Active** means the user-visible service is armed and eligible enabled routines may request a mode change. **Paused** means global pause has safely cancelled routine registrations. **Off** means no automation service is armed. **Needs attention** means the service must be re-armed by opening Sound Scheduler and using **Re-arm**; this condition is also recorded in the local Activity screen rather than presented as a successful sound-mode change.

The **Activity** screen shows local outcomes for all routine attempts. Use **Needs attention** to focus on actionable failures. Clearing history removes only execution records; it never removes routines. Each routine card also displays its latest local status, so routine health can be checked without reviewing the entire log.

Use the edit icon on a routine card to change its trigger, target sound mode, or enabled state. Updating a time or place routine refreshes its Android registration. When an enabled time routine shares its exact scheduled time with another enabled routine that requests a different target mode, Sound Scheduler presents a conflict warning before saving.

## Using charging routines

Create a routine, choose **When charging**, select **Power connected** or **Power disconnected**, and select Ring, Vibrate, or Silent. Charging routines use the Android power broadcast only as a trigger; they change the ringer mode only while the persistent active-automation service is already armed. They require no new location or runtime permission.

## Using place routines

Create a routine, choose **At a place**, set the target sound mode, and tap **Capture this place** while standing at the desired point. Add a private label such as “Home,” select a radius, and choose either **On arrival** or **On departure**. The app will direct the user to Android settings if background location access is still needed.

| Trigger | Expected behavior |
| --- | --- |
| On arrival | Changes the phone mode after the device enters the selected circular area. |
| On departure | Changes the phone mode after the device exits the selected circular area. |
| Pause | Stops the routine and removes its registered geofence. |
| Resume | Re-registers all eligible active place routines when sound and location access are available. |
| Delete | Removes the routine from local storage and removes its registered geofence. |

## Build

The project uses Android API 35, Android Gradle Plugin 8.11.1, Kotlin 2.2.0, Java 17 bytecode targets, and Google Play services Location 21.3.0 for geofencing.

1. Install a JDK capable of building Java 17 targets.
2. Install Android SDK Platform 35 and Build Tools 35.0.0.
3. Create a local `local.properties` at the repository root, for example `sdk.dir=/path/to/android-sdk`.
4. Run the verification build:

```bash
./gradlew :app:lintProdDebug :app:testProdDebugUnitTest :app:assembleProdDebug :app:assembleProdRelease
```

A repository workflow runs the same verification on supported pushes and pull requests. Release packages remain unsigned until the owner supplies an untracked `keystore.properties` file containing `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_ALIAS_PASSWORD`.

## Device acceptance

Before public distribution, install a signed release on an Android 13+ physical device and verify the custom launcher icon, Notification Policy access return flow, quick controls, active automation notification and re-arm flow, individual and global pause/resume behavior, activity history, last-run status, routine editing, time-conflict warning, exact-alarm access flow, each target mode, time recurrence, charging connect/disconnect routines, arrival and departure location routines, app-update recovery, and restart recovery. The full procedure is in [testplan.md](testplan.md).

## References

[1] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)

[2] [Android Developers — Request background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)

[3] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[4] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

[5] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

[6] [Android Developers — Background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)

## License

See [Licence.md](Licence.md).
