# Active Automation Roadmap: From Working Routine to Dependable Product

**Prepared by:** Manus AI
**Date:** 14 August 2026
**Baseline:** Sound Scheduler 1.2.4 debug build
**Scope:** The next work after the Android 17 active-automation lifecycle correction.

## Product position

Sound Scheduler should be a **quiet, local-first control plane for a phone’s Ring, Vibrate, and Silent states**. A person should be able to add a routine, leave the app interface, and trust the device to carry it out while still retaining a clear, immediate way to see what is armed, pause it, and understand the last result.

The current Android 17 design relies on a user-visible foreground automation service while at least one routine is enabled. That means the app interface may be swiped away normally, while the persistent **Sound Scheduler is active** notification confirms that Android is still allowing the app’s automation lifecycle to remain active. This is not the same as force-stopping the app: a force-stop deliberately stops the app and prevents its future work until the user opens it again. The product must explain this distinction simply, without making users learn Android internals. Android 17 can silently ignore ringer-mode writes that occur from an invalid background lifecycle, so active automation must remain visible and deliberate. [1]

> **The design goal is not an invisible background process. It is a low-noise, clearly armed automation capability that the user can verify and turn off at any moment.**

## Immediate release gate: prove 1.2.4 on the Pixel

No broad product expansion should begin until 1.2.4 passes the specific real-device case that motivated the redesign. This is the highest-value next step because it tests the real operating-system behavior that no local compilation or unit test can prove.

| Gate | Test | Pass condition | If it fails |
| --- | --- | --- | --- |
| **A1** | Open the app with an enabled routine | The persistent **Sound Scheduler is active** notification appears in the notification shade | Treat this as an active-automation lifecycle defect; do not test the scheduled switch yet |
| **A2** | Set one one-time Ring → Vibrate time routine two or three minutes ahead, then swipe the app away normally | The phone switches to Vibrate and the completed one-time routine disappears | Mark time automation viable on the Pixel; proceed with Ring and Silent variants |
| **A3** | Pause or delete the final enabled routine | The persistent automation notification disappears | Fix service-stop/reconciliation behavior before release |
| **A4** | Reopen the app after a normal swipe-away | Current mode, routine states, and the active automation indicator remain coherent | Fix recovery/status inconsistencies before release |
| **A5** | Force-stop the app from Android settings, then reopen it | The app explains that automation resumes after opening and rebuilding active routines | Document this Android limitation; do not attempt to circumvent force-stop behavior |

The time-triggered behavior must be checked for **Ring, Vibrate, and Silent**, and then repeated for one recurring routine. Exact alarms are intended for user-visible, precisely timed functionality, but permissions and device power management still require real-device confirmation. [2]

## Phase 1 — Make active automation obvious and controllable

Once the Pixel test passes, the first product update should turn the persistent service into an intentional feature rather than an implementation detail. The home screen should show whether automation is **On**, **Paused**, **Needs attention**, or **Off**. This state should be visible without scrolling and should match the persistent notification.

### 1.1 Add an Automation control card

Replace any vague status wording with a compact card at the top of the home screen.

| State | Home-screen copy | Persistent notification | Available action |
| --- | --- | --- | --- |
| **On** | “Automation active · 3 enabled routines” | “Sound Scheduler is active” | Pause all |
| **On, next time known** | “Next: Work quiet at 08:30” | Same | View routine |
| **On, place routines only** | “Automation active · 2 place routines” | Same | View routines |
| **Needs attention** | “Automation needs Modes access” or “Location access needed for 1 place routine” | Actionable warning | Fix access |
| **Paused** | “Automation paused” | No active foreground notification | Resume automation |
| **Off** | “No active routines” | No active foreground notification | Add routine |

The **Pause all** control should pause every enabled routine locally, cancel time alarms, remove geofences, and stop the foreground service. A matching **Resume automation** control should restore the user’s previously enabled routines after explicit confirmation. This is more reliable and understandable than asking users to kill the app.

### 1.2 Add notification actions

The persistent notification should eventually contain two safe actions: **Open Sound Scheduler** and **Pause automation**. The pause action must be explicit and reversible, update the Room-backed state, cancel the relevant system registrations, and stop the service only after the state has been confirmed locally. Avoid a direct “change to Silent now” action in the notification until conflict rules and user confirmation behavior are designed.

### 1.3 Explain normal swipe-away versus force-stop

Add a short help row in Settings titled **Keeping routines ready**. It should say that swiping the app away is normal and the active notification confirms routines are armed. It should also explain that force-stopping the app deliberately turns automation off until the app is opened again. This preserves user control and avoids promising behavior Android does not permit.

## Phase 2 — Build local last-run status and activity history

This should be the next major feature after the active service is proven. The user’s recent tests show why: a routine can appear to have disappeared without explaining whether the mode truly changed, was rejected, lacked access, was delayed, or was intentionally paused. A local activity history turns the app from a black box into a supportable utility.

### 2.1 Data design

Add a new Room entity, such as `RoutineExecution`, rather than overloading the existing `Routine` table with unbounded history. The routine itself should store a small current-status summary for fast cards, while execution records store a bounded local history.

| Field | Purpose | Privacy rule |
| --- | --- | --- |
| Local execution ID | Primary key | Local only |
| Routine ID | Links an outcome to its routine | Delete history with its routine or mark it orphaned only during a short cleanup transaction |
| Trigger type | Time, arrival, or departure | No raw location trail |
| Requested sound mode | Ring, Vibrate, or Silent | Local only |
| Occurred-at timestamp | Shows when the attempt ran | Local only |
| Outcome code | Applied, missing access, rejected, deferred, paused, or invalid routine | Use stable codes rather than free-form system data |
| Safe detail | A concise user-facing explanation | Never store a street address, raw coordinate, SSID, or device identifier |
| Final observed sound mode | Verifies post-change result when available | Local only |

Add bounded summary fields to `Routine`, such as `lastAttemptAt`, `lastOutcome`, `lastObservedMode`, and `lastOutcomeDetail`. The detail should be selected from a controlled taxonomy rather than arbitrary logs. This keeps cards fast and removes the need to query a long event history just to show the latest state.

### 2.2 Outcome taxonomy

The system should use a small, stable set of outcome codes. Each code must have a clear recovery action or a clear explanation.

| Code | Meaning | Routine behavior | User presentation |
| --- | --- | --- | --- |
| `APPLIED` | Requested mode was confirmed | Complete one-time routine; reschedule recurring routine | “Changed to Vibrate” |
| `ACCESS_REQUIRED` | Modes/Do Not Disturb special access is absent | Keep routine active | “Allow Modes access” |
| `EXACT_ALARM_DEFERRED` | Android delivered an inexact time trigger later than planned | Keep recurring routine; record actual time | “Android delayed this routine” |
| `FOREGROUND_AUTOMATION_INACTIVE` | No active automation service was present when a background event arrived | Keep routine active | “Open the app to re-arm automation” |
| `MODE_REJECTED` | Android did not keep the requested ringer mode | Keep routine active | “Phone stayed on Ring” |
| `LOCATION_ACCESS_REQUIRED` | A place routine lacks background location access | Keep routine active but geofence unregistered | “Allow background location” |
| `LOCATION_UNAVAILABLE` | The selected place cannot currently be registered | Keep routine active and surface next step | “Location services unavailable” |
| `PAUSED` | User chose to pause automation or that routine | Do not execute | “Paused by you” |
| `INVALID_CONFIGURATION` | Stored data cannot be safely executed | Disable or flag after clear confirmation | “Needs review” |

The first release of the log should retain at most **100 execution records or 30 days**, whichever removes entries sooner. Offer a clear **Delete activity history** action. This is enough for support and user confidence without becoming a covert behavioral archive.

### 2.3 User interface

Add a **Last run** line to each routine card. A successful routine might say “Last run: changed to Vibrate, 2 min ago.” A failure might say “Last run: phone stayed on Ring · needs attention.” This status must not rely on color alone.

Add an **Activity** screen in the main navigation or top app bar. It should group local records by day, filter by All / Applied / Needs attention, and let the user clear the history. Do not show coordinates, background routes, or a map. For a place routine, show only the user’s own private label and “On arrival” or “On departure.”

## Phase 3 — Improve routine authoring and protect against conflicts

The current create flow should become a full routine editor after execution history is available. This prevents accidental schedules and makes the app manageable when users have more than a few routines.

### 3.1 Editing, duplication, and clear previews

| Improvement | User value | Acceptance criterion |
| --- | --- | --- |
| Edit routine | Correct a time, target mode, label, radius, or transition without deleting/recreating | Editing safely cancels and replaces only the affected alarm/geofence |
| Duplicate routine | Create similar schedules quickly | Duplicate opens in edit mode and requires explicit save |
| Next-run preview | Makes time rules understandable | The card displays the next scheduled occurrence in device-local time |
| Place readiness preview | Makes place rules understandable | The card states whether the geofence is registered and what access is missing |
| Selected weekdays | Supports work and school schedules precisely | A user can choose individual days and view the next three occurrences |
| Temporary override | Supports “Vibrate for one hour” without permanent routine changes | User can cancel the override and see when normal automation resumes |

### 3.2 Define conflict behavior before adding more triggers

Multiple active routines can otherwise cause rapid and surprising mode changes. Add a documented precedence model before implementing Wi-Fi, Bluetooth, calendar, or NFC triggers. A sensible baseline is: **temporary override > manual action with a selected pause duration > most recently confirmed enabled routine > scheduled fallback**.

Warn users at save time when two time routines request different modes at the same minute. Add a short cooldown for duplicate location transition events. Display the next routine capable of changing the current mode in the Automation card.

## Phase 4 — Finish location routines as a reliable, privacy-first feature

Location routines are promising, but they must not be treated as complete until they pass controlled real-world tests. Android geofence delivery depends on system location availability, Google Play services, and background power management; the product should make those constraints visible rather than imply street-level precision. [3]

| Work item | Improvement | Success criterion |
| --- | --- | --- |
| Place readiness | Show “Registered,” “Needs location access,” or “Location services off” on each place routine | User knows whether the trigger is armed without guessing |
| Capture quality | Show capture accuracy and require a sensible radius relative to it | A poor capture is flagged before saving, not after a missed arrival |
| Arrival/departure test flow | Add a small tester/help sheet for crossing the selected radius once | Support testing without location history or map sharing |
| Geofence result records | Record arrival/departure, mode request, and final mode only | No raw coordinate trail is stored |
| Device recovery | Re-register active geofences after boot, update, access changes, and explicit manual refresh | The Activity log records success or reason for failure |
| Battery guidance | Explain that 150–500 m radii are more reliable than tiny radii | Support copy is realistic and does not add continuous GPS tracking |

Do not add continuous location tracking, automatic address lookup, cloud mapping, or location history. Those features would reduce the product’s privacy advantage while providing little value for its stated purpose.

## Phase 5 — Make the app polished, accessible, and low-noise

The product should now receive an accessibility and visual-quality pass rather than more trigger types.

| Area | Recommended work | Definition of done |
| --- | --- | --- |
| Edge-to-edge layouts | Verify gesture and three-button navigation, display cutouts, landscape, and keyboard overlap | No primary action, list item, or dialog control is obscured |
| Font scale | Test 100%, 130%, and 200% system font scale | Routine cards wrap gracefully; no critical label truncates |
| TalkBack | Add concise labels for mode controls, pause/resume, delete, routine state, and persistent automation state | A non-visual user can create, inspect, pause, and resume routines |
| Contrast and state | Do not rely solely on green/red or icon shape to show status | Every state is available as visible text and content description |
| Notification hygiene | Persistent active notification is low priority; success notifications are quiet; failures are actionable and rate-limited | The user is informed without being spammed |
| Empty and error states | Offer templates and recovery actions, not unexplained blank screens | Every missing permission or unarmed trigger gives one next action |

## Phase 6 — Production engineering and controlled release

After the above device gates and reliability work pass, prepare for a signed release. The main risk at that point is release process discipline, not feature availability.

| Release activity | Required action |
| --- | --- |
| Signing | Use only the owner-controlled release keystore; never commit or upload it to the repository |
| Release verification | Run `apksigner verify --verbose --print-certs` against the final artifact and test upgrade from the current debug/internal build path where signatures permit |
| CI maintenance | Upgrade deprecated GitHub Actions versions, pin trusted actions, retain wrapper verification, and archive test/lint reports |
| Compatibility matrix | Test at least one Android 13 device, the affected Android 17 Pixel, and one non-Pixel device before closed release |
| Closed testing | Use a small invited group before broad distribution and collect feedback manually without third-party analytics |
| Store compliance | Prepare a concise privacy policy, accurate Data safety disclosure, foreground-service disclosure, and clear rationale for exact alarms/location before publication |

The app should not be considered publicly production-ready until the active automation design, Ring/Vibrate/Silent background tests, location route tests, boot/update recovery, and final signed package all pass. [1] [2] [3]

## Suggested delivery order

The following order makes the app better without sacrificing the narrow, privacy-first promise.

| Priority | Delivery slice | Why now | Release gate |
| --- | --- | --- | --- |
| **P0** | Prove 1.2.4 active automation on the Pixel | Establish that the core promise works | A1–A5 pass |
| **P0** | Add clear active state, Pause all, and notification pause action | Gives users control over the new persistent lifecycle | Service stops and resumes correctly |
| **P0** | Add local last-run status and bounded activity log | Explains success, delay, or failure honestly | No unbounded history; every outcome has recovery copy |
| **P1** | Editing, duplication, next-run previews, selected weekdays | Makes routine management practical | Time and geofence replacements are atomic |
| **P1** | Conflict policy, cooldown, and temporary override | Prevents competing automation | Conflicts are visible before save |
| **P1** | Complete controlled location test matrix | Establishes practical geofence reliability | Arrival, departure, recovery, and access cases pass |
| **P2** | Accessibility, large-font, dark-theme, and edge-to-edge audit | Makes the product usable under real device preferences | Core workflow passes accessibility review |
| **P2** | Manual encrypted local export/import | Adds portability without accounts | Export is user-initiated, reviewable, and deleteable |
| **P3** | One carefully consented optional trigger, beginning with charging state | Adds value with low privacy cost | Uses shared conflict, log, and lifecycle architecture |

## Features to defer deliberately

Do not add accounts, cloud synchronization, advertising or analytics SDKs, passive location history, continuous GPS tracking, reverse geocoding, unrestricted services, or stealthy attempts to defeat Android’s force-stop behavior. Those additions would increase privacy, battery, and policy risk without strengthening the primary promise: dependable local sound-mode routines.

## Definition of “fully functional”

Sound Scheduler is functionally complete enough for a controlled production release when it meets all of the following conditions:

| Capability | Required standard |
| --- | --- |
| Time routines | Ring, Vibrate, and Silent changes work from a normally swiped-away app on supported Android 13–17 devices, with a visible armed state and truthful final outcome |
| Active automation | Persistent notification appears only while enabled routines require it; Pause all and last-routine removal stop it reliably |
| One-time and recurring state | A one-time routine disappears only after confirmed success; a recurring routine reschedules correctly; failures remain visible and explainable |
| Place routines | Capture, permission, arrival, departure, radius, pause, delete, reboot, and update cases have recorded physical-device results |
| User recovery | Missing access, delayed timing, inactive automation, and rejected changes each provide a direct next action |
| Privacy | All routine data and activity history remain local; no account, server, geocoding, tracking, or third-party analytics is introduced |
| Quality | Unit tests, lint, CI, physical-device checks, accessibility audit, and signed-package verification pass |

## References

[1]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers — Background audio hardening"

[2]: https://developer.android.com/develop/background-work/services/alarms "Android Developers — Schedule alarms"

[3]: https://developer.android.com/develop/sensors-and-location/location/geofencing "Android Developers — Create and monitor geofences"
