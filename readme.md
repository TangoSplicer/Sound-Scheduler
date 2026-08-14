# Sound Scheduler

**Sound Scheduler** is a privacy-first Android app for changing the device ringer mode to **Ring**, **Vibrate**, or **Silent** at a chosen time or when the device arrives at or departs from a user-saved place.

The application stores routine data only on the device. It has no accounts, backend, advertising SDK, analytics SDK, calendar integration, geocoding, or location sharing. Place labels are private labels chosen by the user; the app never turns coordinates into an address or transmits them.

## Stable product scope

| Capability | Behavior |
| --- | --- |
| Sound modes | Sets the Android ringer mode to Ring, Vibrate, or Silent. |
| Quick controls | Applies Ring, Vibrate, or Silent immediately from the home screen once Android sound-control access is granted. |
| Time routines | Supports one-time, daily, weekly, and monthly schedules. |
| Place routines | Supports recurring **On arrival** and **On departure** routines using a local circular geofence around a captured device location. |
| Place radius | Offers 100 m, 150 m, 250 m, and 500 m radii; 150 m is the default. |
| Pause and resume | Temporarily pauses either routine type without deleting it. A paused place routine is removed from Android geofencing; resuming restores eligible local geofences. |
| Recovery | Rebuilds future time routines and eligible place geofences after device restart and app update. |
| Access recovery | Opens Android’s sound-control and app settings pages when user-granted access is required, then reports the outcome on return. |
| Notifications | Optional, quiet status confirmations; notification denial does not prevent an authorized mode change. |
| Privacy | All routine data stays in the local Room database. No location data leaves the device. |

## Android access

Sound-mode changes require the manifest’s normal audio-settings capability and user-granted **Notification Policy access**. The app checks this access before each scheduled or location-triggered change and does not claim a change succeeded when Android rejects it. Android exposes this access through `NotificationManager.isNotificationPolicyAccessGranted()` and its system settings page. [1]

Place routines request **precise foreground location** only when the user elects to capture a place. To receive a geofence transition while the app is not open, users must then enable **Allow all the time** location access in Android app settings. Android treats background location as a separate consent step on Android 10 and later. [2] The app uses the captured coordinates solely to create local Google Play services geofences; it does not continuously record a location history. [3]

> Android may delay or miss a place transition when device location is disabled, Google Play services is unavailable, or the operating system applies background power constraints. Choose an appropriate radius—typically 150–500 m—and validate important routines on the target device. [3]

On Android 12 and later, exact-alarm access improves the timing precision of time routines. Without it, Android may defer a routine to preserve battery life. [4] On Android 13 and later, notifications are optional and requested only to show routine-status confirmations. [5]

On Android 17, Android can silently ignore background ringer-mode APIs. Sound Scheduler therefore starts a **brief foreground execution service** for a time or place trigger, displays a low-priority “Applying sound routine” status notification, changes the requested mode, and stops immediately. This is required even when Modes access has already been granted. [6]

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

Before public distribution, install a signed release on an Android 13+ physical device and verify the custom launcher icon, Notification Policy access return flow, quick controls, pause/resume behavior, exact-alarm access flow, each target mode, time recurrence, arrival and departure location routines, app-update recovery, and restart recovery. The full procedure is in [testplan.md](testplan.md).

## References

[1] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)

[2] [Android Developers — Request background location](https://developer.android.com/develop/sensors-and-location/location/permissions/background)

[3] [Android Developers — Create and monitor geofences](https://developer.android.com/develop/sensors-and-location/location/geofencing)

[4] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

[5] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

[6] [Android Developers — Background audio hardening](https://developer.android.com/about/versions/17/changes/bg-audio)

## License

See [Licence.md](Licence.md).
