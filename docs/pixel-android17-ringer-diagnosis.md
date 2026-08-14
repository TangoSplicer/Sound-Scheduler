# Pixel Android 17 Ringer-Rejection Diagnosis

## Confirmed device state

The reported device is a Google Pixel 8 running Android 17. The failed routine was time-based and requested **Ring → Vibrate**. The user supplied a screenshot of **Modes access** showing **Sound Scheduler — Allowed**. This rules out the ordinary missing Notification Policy / Do Not Disturb special-access condition.

## Root cause

Android 17 introduces **background audio hardening**. When an app is not visible and is not running a qualifying foreground service, the system silently ignores `AudioManager` volume and ringer-mode APIs, including `AudioManager.setRingerMode()`. No exception or platform error is returned. The existing time-routine receiver performed the mode change directly from a background broadcast receiver, then correctly observed that the phone remained in Ring mode and issued its generic rejection notification.

This behavior applies to the reported Pixel Android 17 device even though Modes access is granted. The framework source also confirms that the external ringer-mode setter can return without a change when hardening enforcement blocks the audio method.

## Corrective implementation

Scheduled and geofence-triggered sound-mode changes now hand off to a short-lived foreground execution service with the `mediaPlayback` foreground-service type. The manifest declares the required foreground-service permissions and service type. Android permits a foreground-service start in response to a user-requested exact alarm or a geofencing transition; the service immediately publishes the required low-priority status notification, applies the mode, reports the established success or access result, and stops.

## Physical verification

After installing the corrective build on the Pixel 8, verify the home-screen Ring → Vibrate quick control and a time routine while the app is not visible. The quick control should already work because the activity is visible; the time routine is the Android 17 regression check. If the device is connected to development tools, `adb dumpsys audio` or Logcat can confirm an `AudioHardening` event if a device-level restriction remains.

## Sources

- Android Developers — Background audio hardening: https://developer.android.com/about/versions/17/changes/bg-audio
- Android Developers — Restrictions on starting a foreground service from the background: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android framework `AudioService.java`, main branch: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/audio/AudioService.java
- Android Developers `AudioManager` API reference: https://developer.android.com/reference/android/media/AudioManager
