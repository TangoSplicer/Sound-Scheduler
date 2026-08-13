# Sound Scheduler 1.1.0 Device Acceptance Plan

## Purpose

This checklist validates the supported stable scope: local time-based routine alerts with one-time, daily, weekly, and monthly recurrence. It intentionally does not test removed beta-only features such as billing, advertising, geofencing, calendar monitoring, widgets, backup, themes, or archive workflows.

## Test environment

| Item | Requirement |
| --- | --- |
| Build | Signed `prodRelease` APK or App Bundle. |
| Device | Physical Android device running Android 13 or later. |
| Initial state | Clean application install, notifications initially undecided, exact-alarm special access initially disabled where the device permits it. |
| Network | Not required after install. |

## Acceptance checks

| ID | Scenario | Expected result |
| --- | --- | --- |
| SS-01 | Fresh install and first launch | The home screen opens without a crash, the empty state is visible, and the custom launcher icon appears correctly. |
| SS-02 | Create one-time routine | A named routine scheduled a few minutes ahead appears in the list and remains after relaunch. |
| SS-03 | Allow notifications | Android grants notification access in context; the due routine produces a visible notification that opens the app. |
| SS-04 | Deny notifications | Routine creation remains available; the application explains that Android will not display alerts and does not crash. |
| SS-05 | Exact-alarm access granted | The status states that exact alerts are enabled; an alert fires at the selected time under normal device conditions. |
| SS-06 | Exact-alarm access denied | The application offers system settings access and saves the routine using a safe inexact fallback without a security exception. |
| SS-07 | Daily recurrence | A daily routine remains active after delivery and is scheduled for the next day. |
| SS-08 | Weekly recurrence | A weekly routine remains active after delivery and is scheduled for the next week. |
| SS-09 | Monthly recurrence | A monthly routine remains active after delivery and is scheduled for the next month. |
| SS-10 | Delete routine | The routine disappears and its pending alert does not fire. |
| SS-11 | Device restart | Active routines are reconstructed after boot. |
| SS-12 | Application update | Active routines are reconstructed after the package is updated. |
| SS-13 | Light and dark appearance | Text, routine rows, status card, and controls remain readable; no unexpected template launcher icon appears. |

## Exit criteria

The release is ready for distribution when every applicable acceptance check passes on at least one Android 13+ physical device, the production artifact has been signed with the owner’s release key, and `apksigner verify --verbose --print-certs` confirms the final signature.
