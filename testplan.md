# Sound Scheduler Device Acceptance Plan

## Purpose

This plan validates **Sound Scheduler 1.2.0**: private, on-device routines that change a device’s ringer mode to **Ring**, **Vibrate**, or **Silent** at a scheduled time or when the user arrives at or departs from a saved place. The application does not create accounts, send location to a backend, geocode place names, or share location.

> Location detection remains subject to Android system location availability, power management, and Google Play services. These checks therefore require a physical device and controlled location movement or a reliable test route. [1]

## Test environment

| Item | Requirement |
| --- | --- |
| Build | Signed `prodRelease` APK or App Bundle; the `prodDebug` APK is acceptable for pre-release functional checks. |
| Device | Physical Android device running Android 13 or later with current Google Play services. |
| Initial state | Clean install; Notification Policy access disabled; notification permission undecided; precise location and background location not granted. |
| Network | Not needed by Sound Scheduler after installation, though Google Play services must be operational for geofence delivery. |
| Location test route | A controlled journey that begins outside the selected radius and crosses it once on arrival and once on departure. Use a radius of 150–500 m to account for real-world location accuracy. |

## Time-routine acceptance checks

| ID | Scenario | Expected result |
| --- | --- | --- |
| SM-01 | Fresh install and first launch | The home screen opens without a crash, displays the current device mode, and shows the custom launcher icon correctly. |
| SM-02 | Sound-control access denied | The home status clearly reports that access is required and opens Android’s Notification Policy access screen from the button or routine-save prompt. Sound Scheduler is visible in Android’s Do Not Disturb access list. |
| SM-03 | Sound-control access granted | Returning after granting access shows a clear success result, hides the access button, and rebuilds future time routines. |
| SM-04 | One-time Ring routine | A routine due within a few minutes sets the phone to Ring and then disappears from the active list. |
| SM-05 | One-time Vibrate routine | A routine due within a few minutes sets the phone to Vibrate and then disappears from the active list. |
| SM-06 | One-time Silent routine | A routine due within a few minutes sets the phone to Silent and then disappears from the active list. |
| SM-07 | Notification denial | Denying notifications does not stop an authorized sound-mode routine from changing the phone’s mode. |
| SM-08 | Exact-alarm access denied | The routine is saved safely and the application explains that Android may defer its timing. |
| SM-09 | Recurrence | Daily, weekly, and monthly routines set the chosen mode and remain active for their next occurrence. |
| SM-10 | Delete routine | Deleting a time routine removes it and prevents its pending sound-mode change. |
| SM-11 | Device restart | Future active time routines are reconstructed after boot when sound-control access is enabled. |
| SM-12 | App update | Future active time routines are reconstructed after a package update when sound-control access is enabled. |
| SM-13 | Missing sound access at trigger | Android does not report a false success; the app offers a recoverable access path and retains one-time routines that could not be confirmed. |
| SM-14 | Quick controls | With sound-control access granted, the Ring, Vibrate, and Silent quick controls immediately set the selected device mode and refresh the visible current-mode status. |
| SM-15 | Pause routine | Pausing a future routine keeps it visible, marks it paused, and prevents its scheduled change from firing. |
| SM-16 | Resume routine | Resuming a paused future routine schedules its next occurrence again and restores its enabled state. |
| SM-17 | Visual and accessibility review | The trigger selector, sound-mode selector, status card, quick controls, routine rows, switches, and delete controls are readable in light and dark appearances with usable touch targets. |
| SM-18 | Android 17 background time routine | On Android 17 with Modes access allowed, close or background the app, schedule a Ring → Vibrate change within a few minutes, and confirm that the brief foreground-status notification appears, the phone changes to Vibrate, and the routine reports success. This verifies the foreground execution service required by Android audio hardening. [2] |
| SM-19 | Durable result confirmation | For a one-time Android 17 time routine, remove it from the active list only after the selected device mode remains applied through the confirmation window. If Android rejects or later reverses the change, retain the routine, keep it enabled, and show a recoverable failure result rather than a false success. |

## Location-routine acceptance checks

| ID | Scenario | Expected result |
| --- | --- | --- |
| LR-01 | Create a place routine without location access | Selecting **At a place** and capturing a point first requests foreground location. If background access is still missing, the saved routine remains visible and the app gives a clear path to app settings for precise “Allow all the time” access. |
| LR-02 | Privacy copy and local label | The place editor says the captured point stays on device. A user can supply a private label, and no address lookup, map sharing, account prompt, or networking screen appears. |
| LR-03 | Capture current place | With precise foreground location enabled, capture succeeds, restores the button state, and permits saving only after a non-blank private label is provided. A capture failure leaves the editor open with a recoverable message. |
| LR-04 | Arrival routine | With Notification Policy access and precise “Allow all the time” location enabled, begin outside the selected radius and enter it. The phone changes to the selected mode once, and the optional low-priority confirmation accurately names the routine and target mode. |
| LR-05 | Departure routine | Begin inside the selected radius, then cross outside it. The phone changes to the selected mode once. Entering the area alone must not trigger this departure routine. |
| LR-06 | Radius selection | Save routines using 100 m, 150 m, 250 m, and 500 m. The card reports the selected radius and retains it after app restart. |
| LR-07 | Pause, resume, and delete | Pausing a place routine removes its active geofence and prevents a transition from changing the sound mode. Resuming re-registers it when access is present. Deleting removes it permanently and prevents later transitions. |
| LR-08 | Restart and update recovery | With location access granted, restart the phone and then install an app update. The active place routine remains shown and triggers on the next appropriate transition after Android restores app execution. |

## Exit criteria

The release is ready for public distribution when all applicable checks pass on at least one Android 13+ physical device, the final artifact is signed with the owner’s release key, and `apksigner verify --verbose --print-certs` confirms the signature. Location checks must be recorded with device model, Android version, selected radius, permission state, and observed transition result.

## References

[1]: https://developer.android.com/develop/sensors-and-location/location/geofencing "Android Developers: Create and monitor geofences"

[2]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers: Background audio hardening"
