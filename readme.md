# Sound Scheduler

**Sound Scheduler** is a privacy-first Android app for scheduling local phone sound-mode changes. Create a routine that sets the device to **Ring**, **Vibrate**, or **Silent** at a chosen time, once or on a daily, weekly, or monthly schedule.

The application stores routines only on the device. It has no accounts, backend, advertising SDK, analytics SDK, calendar integration, or location tracking.

## Stable product scope

| Capability | Behavior |
| --- | --- |
| Sound modes | Sets the Android ringer mode to Ring, Vibrate, or Silent. |
| Quick controls | Applies Ring, Vibrate, or Silent immediately from the home screen once Android sound-control access is granted. |
| Timing | Supports one-time, daily, weekly, and monthly schedules. |
| Pause and resume | Temporarily pauses a routine without deleting it, then safely schedules its next occurrence again when re-enabled. |
| Recovery | Rebuilds future enabled schedules after device restart and app update. |
| Access recovery | Declares `ACCESS_NOTIFICATION_POLICY`, opens Android’s Notification Policy access page, and confirms the outcome when the user returns. |
| Notifications | Optional, quiet status confirmations; notification denial does not prevent an authorized mode change. |
| Privacy | All routine data stays in the local Room database. |

## Android access

Sound-mode changes require the manifest’s normal audio-settings capability and user-granted **Notification Policy access**. The app checks this access before each scheduled change and does not claim a change succeeded when Android rejects it. Android exposes this access through `NotificationManager.isNotificationPolicyAccessGranted()` and its system settings page. [1]

On Android 12 and later, exact-alarm access improves timing precision. Without it, Android may defer a routine to preserve battery life. [2] On Android 13 and later, notifications are optional and requested only to show routine-status confirmations. [3]

## Build

The project uses Android API 35, Android Gradle Plugin 8.11.1, Kotlin 2.2.0, and Java 17 bytecode targets.

1. Install a JDK capable of building Java 17 targets.
2. Install Android SDK Platform 35 and Build Tools 35.0.0.
3. Create a local `local.properties` at the repository root, for example `sdk.dir=/path/to/android-sdk`.
4. Run the verification build:

```bash
./gradlew :app:lintProdDebug :app:testProdDebugUnitTest :app:assembleProdDebug :app:assembleProdRelease
```

A repository workflow runs the same verification on supported pushes and pull requests. Release packages remain unsigned until the owner supplies an untracked `keystore.properties` file containing `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_ALIAS_PASSWORD`.

## Device acceptance

Before public distribution, install a signed release on an Android 13+ physical device and verify the custom launcher icon, visibility in the Do Not Disturb access list, successful Notification Policy return flow, quick controls, pause and resume behavior, exact-alarm access flow, one-time routine, each recurrence type, delete behavior, reboot recovery, and all three target modes.

## References

[1] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)

[2] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

[3] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

## License

See [Licence.md](Licence.md).
