# Sound Scheduler Device Acceptance Plan

## Purpose

This plan validates the stable product purpose: local routines that change the device’s ringer mode to **Ring**, **Vibrate**, or **Silent**. It intentionally excludes removed beta concepts such as billing, advertising, geofencing, calendar monitoring, widgets, backup, themes, and archive workflows.

## Test environment

| Item | Requirement |
| --- | --- |
| Build | Signed `prodRelease` APK or App Bundle. |
| Device | Physical Android device running Android 13 or later. |
| Initial state | Clean install, Notification Policy access initially disabled, and notifications initially undecided. |
| Network | Not required after installation. |

## Acceptance checks

| ID | Scenario | Expected result |
| --- | --- | --- |
| SM-01 | Fresh install and first launch | The home screen opens without a crash, displays the current device mode, and shows the custom launcher icon correctly. |
| SM-02 | Sound-control access denied | The home status clearly reports that access is required and opens Android’s Notification Policy access screen from the button or routine-save prompt. |
| SM-03 | Sound-control access granted | The home status reports access enabled; existing future routines are scheduled again. |
| SM-04 | One-time Ring routine | A routine due within a few minutes sets the phone to Ring and then disappears from the active list. |
| SM-05 | One-time Vibrate routine | A routine due within a few minutes sets the phone to Vibrate and then disappears from the active list. |
| SM-06 | One-time Silent routine | A routine due within a few minutes sets the phone to Silent and then disappears from the active list. |
| SM-07 | Notification denial | Denying notifications does not stop an authorized sound-mode routine from changing the phone’s mode. |
| SM-08 | Exact-alarm access denied | The routine is saved safely and the application explains that Android may defer its timing. |
| SM-09 | Recurrence | Daily, weekly, and monthly routines set the chosen mode and remain active for their next occurrence. |
| SM-10 | Delete routine | Deleting a routine removes it and prevents its pending sound-mode change. |
| SM-11 | Device restart | Future active routines are reconstructed after boot when sound-control access is enabled. |
| SM-12 | App update | Future active routines are reconstructed after a package update when sound-control access is enabled. |
| SM-13 | Missing access at trigger | Android does not report a false success; the app offers a recoverable access path and retains one-time routines that could not be confirmed. |
| SM-14 | Visual and accessibility review | The sound-mode selector, status card, routine rows, and controls are readable in light and dark appearances. |

## Exit criteria

The release is ready for public distribution when all applicable acceptance checks pass on at least one Android 13+ physical device, the final artifact is signed with the owner’s release key, and `apksigner verify --verbose --print-certs` confirms the signature.
