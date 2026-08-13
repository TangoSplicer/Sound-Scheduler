# Sound Scheduler

**Sound Scheduler** is a privacy-first Android app for creating named, on-device scheduled alerts. Version **1.1.0** delivers a focused, reliable routine-reminder experience without accounts, advertising SDKs, remote services, or data collection.

## What the stable release does

The app stores routines locally with Room and uses Android alarms to deliver the next scheduled alert even when the app is not open. Users can create one-time, daily, weekly, or monthly routines, review active routines, and delete a routine together with its scheduled alarm. Routine delivery is restored after device restart, app update, and exact-alarm permission changes.

On Android 13 and later, the app asks for notification access when the user saves a routine. On Android 12 and later, it uses exact alarms when the user grants the relevant special access; otherwise, it schedules an inexact fallback and clearly tells the user that Android may defer the alert to preserve battery life. See the [Android exact-alarm guidance](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms) and [notification permission guidance](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

| Area | Stable behavior in 1.1.0 |
| --- | --- |
| Storage | Local Room database; no backend or account required. |
| Scheduling | One-time, daily, weekly, and monthly alerts with reboot recovery. |
| Permissions | Contextual notification request and exact-alarm settings recovery. |
| Visual identity | A custom clock-and-sound-wave launcher icon at all Android density buckets. |
| Privacy | No ads, billing SDK, analytics SDK, calendar read access, location access, or cleartext network traffic. |

## Intentionally excluded from this release

The prior beta advertised features that were incomplete or unverified: subscriptions, advertising, geofencing, calendar monitoring, ringer-profile changes, widgets, backup/restore, archive management, templates, and theme switching. Those paths were removed from the stable scope rather than shipped as nonfunctional production claims. They should be reintroduced only with dedicated design, permissions, implementation, and device testing.

## Build requirements

The project uses Android Gradle Plugin 8.11.1, Kotlin 2.2.0, Java 17 bytecode targets, and Android API 35.

1. Install a Java Development Kit capable of building Java 17 targets.
2. Install Android SDK Platform 35 and Build Tools 35.0.0.
3. Create a local `local.properties` file at the repository root with the SDK location, for example `sdk.dir=/path/to/android-sdk`.
4. Run the following command:

```bash
./gradlew :app:assembleProdDebug
```

For a signed production package, create an untracked `keystore.properties` file in the repository root containing `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_ALIAS_PASSWORD`, then run:

```bash
./gradlew :app:assembleProdRelease
```

## Before public distribution

Run the generated APK on at least one physical device running Android 13 or later. Confirm the notification permission flow, exact-alarm special-access flow, a one-time alert, each recurrence type, deletion, restart recovery, and launcher-icon appearance. The project includes unit coverage for routine timing calculations; device tests remain essential for platform permission and delivery behavior.

## License

See [Licence.md](Licence.md).
