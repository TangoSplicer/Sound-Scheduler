# Beneficial Feature Slate for Sound Scheduler

**Prepared by:** Manus AI
**Date:** 14 August 2026
**Product baseline:** Time routines, local place routines, quick manual Ring/Vibrate/Silent controls, and the Android 17 active-automation lifecycle.

## Product standard

Sound Scheduler should not become a generic automation engine. Its strongest identity is much clearer: a **private, dependable way to decide when a phone should Ring, Vibrate, or be Silent**. Every proposed feature must therefore pass four tests.

| Test | Question | Required answer |
| --- | --- | --- |
| Daily value | Does it solve a recurring, real-world sound-management problem in fewer steps than the current app? | Yes, for a broad group of users or a clearly served power-user group |
| Reliability | Can it run predictably under current Android background, foreground-service, alarm, and permission rules? | Yes, with a visible recovery path when Android cannot guarantee delivery |
| Privacy | Can it remain entirely on-device without new tracking, account, cloud, or unnecessary sensitive data? | Yes, by default |
| Clarity | Can a user understand why the phone changed mode and undo or pause it quickly? | Yes, from the home screen and activity history |

> **Recommendation:** build fewer, deeper capabilities. A reliable routine editor, history, and global pause are more valuable than a long list of fragile trigger types.

## What must come first

Before adding a new trigger, prove the current Android 17 active-automation path on the physical Pixel. The persistent **Sound Scheduler is active** notification must be present before the app interface is swiped away, and a one-time Ring → Vibrate routine must then change the phone mode and remove itself only after verified success. Android 17 can silently ignore ringer-mode writes from an invalid background audio lifecycle, so reliable active automation is a foundation rather than an optional enhancement. [1]

The next development cycle should therefore contain both the Android 17 lifecycle correction and the product features that make that lifecycle transparent to the user: the Automation card, Pause all, and the local execution history.

## Recommended feature set

### 1. Automation control centre — **highest priority**

The new active automation service should be a visible product feature, not a technical side effect. Add a compact Automation card at the top of the home screen, synchronized with the persistent notification.

| Capability | User benefit | Why it belongs now |
| --- | --- | --- |
| Active / paused / needs-attention state | The user instantly knows whether automation is armed | Directly supports the Android 17 foreground-service model |
| Next change preview | Shows `Next: Vibrate at 08:30` or `2 place routines ready` | Prevents accidental surprises and gives confidence |
| Pause all | Stops all automated changes without deleting carefully created routines | Better than force-stopping the app |
| Resume automation | Re-arms preserved routines through an explicit, visible app action | Gives Android the user-initiated lifecycle it needs |
| View activity | Opens local evidence of what ran and why | Makes the product supportable |

The persistent notification should contain **Open** and a carefully confirmed **Pause automation** action. It should never disclose place labels, coordinates, or other private context on the lock screen. Android Quick Settings tiles are designed for recurring, rapid controls, so a future toggle tile is appropriate for **Pause/Resume automation** once it is tested on supported devices. It should not become a generic app launcher or a collection of multiple tiles. [2]

### 2. Local activity log and last-run status — **build with the control centre**

This is the most important trust feature. Each routine card should show its last confirmed result, while a dedicated Activity screen preserves a bounded, locally stored explanation of attempts.

| User situation | Card status | Activity entry |
| --- | --- | --- |
| Routine succeeded | `Changed to Vibrate · 2 min ago` | Requested Vibrate, observed Vibrate, time and trigger type |
| Android rejected the write | `Phone stayed on Ring · Needs attention` | `MODE_REJECTED`, observed Ring, recovery action |
| Service needs re-arming | `Open app to re-arm automation` | `AUTOMATION_REARM_REQUIRED` |
| Exact alarm unavailable | `May run later because exact alarms are off` | Scheduled and actual delivery times |
| Place routine lacks access | `Background location required` | `LOCATION_ACCESS_REQUIRED` without coordinates |

The activity history should retain at most 100 events or 30 days and contain no raw coordinates, addresses, routes, Bluetooth identifiers, or device analytics. It should be clearable at any time. This creates useful accountability without converting the app into a personal-behavior log.

### 3. Routine editor, templates, and next-run previews — **highest daily value after visibility**

Creating a routine should not be a one-way dialog. Users need to correct, reuse, and understand their rules.

| Feature | Presentable user experience | Implementation guardrail |
| --- | --- | --- |
| Edit any routine | Tap a routine to edit title, sound mode, time/place, recurrence, radius, and transition | Cancel/re-register only the affected alarm or geofence after save |
| Duplicate routine | `Duplicate` opens a reviewed copy rather than silently creating a second rule | New copy must be disabled until saved and checked for conflict |
| Local templates | Offer starting points such as **Weeknight quiet**, **Work arrival vibrate**, and **Weekend ring** | Templates contain no location, account, or cloud data |
| Next three occurrences | The editor and card show upcoming local dates/times | Use device timezone and correctly recalculate after timezone changes |
| Selected weekdays | A clear Mon–Sun selector replaces an overly broad weekly option | Show the next three run dates before saving |
| Date exclusions | A user can skip holidays or annual leave days | Store only local dates; no calendar permission needed |

This feature set removes friction from the most common workflows without adding any new sensitive permission.

### 4. Manual override and scheduled return — **very beneficial, with explicit control**

A user frequently wants a short interruption to automation: “Keep my phone on Vibrate for the next hour,” “Ring until tomorrow morning,” or “Do not let the work routine change the mode tonight.” The right solution is a visible, temporary override rather than hidden conflict behavior.

| Override | Example | Required presentation |
| --- | --- | --- |
| Hold current/selected mode | `Vibrate for 1 hour` | Home card: `Override active · ends at 18:30` |
| Pause automation until time | `Pause routines until 07:00 tomorrow` | Persistent notification remains with `Paused until 07:00` if it must keep the active lifecycle for scheduled restoration |
| Pause routine until next run | Skip just tonight’s quiet rule | Routine card states `Skipped once` and shows next normal occurrence |
| Cancel override | Return to normal rules immediately | One clear action from home and notification |

Time-bounded pauses must be designed alongside the foreground-service lifecycle. If a scheduled resume relies on background execution, the service must remain visibly armed with a truthful “Paused until …” state. An indefinite Pause all should stop the active service and remove the persistent notification.

### 5. Deterministic conflict protection — **essential before more triggers**

As soon as a user has several routines, silent conflicts are more damaging than missing features. Build a conflict engine before adding Bluetooth, Wi-Fi, calendar, or NFC triggers.

| Situation | Rule | User-facing behavior |
| --- | --- | --- |
| Two time routines at the same minute request different modes | Warn before save and require a choice | `Conflicts with Weeknight quiet at 22:30` |
| Multiple triggers arrive near the same time | The most recently confirmed routine wins unless a temporary override is active | Activity log explains the final decision |
| Repeated geofence delivery | Apply a short per-routine cooldown | One result rather than repeated mode changes |
| Manual quick control after automation runs | Offer `Keep this mode for…` rather than silently fighting the next routine | User retains intent and sees expiry |

A recommended precedence order is: **temporary override → explicit manual hold → most recently confirmed routine → ordinary scheduled/placed routine**. The app should display the next rule capable of changing the mode.

### 6. Charging-state routines — **the best first optional trigger**

A charging connection is a familiar, low-sensitivity local context: phones often charge overnight, at a desk, or in a car. A user may want Silent when charging at night, Ring on disconnect in the morning, or Vibrate when placed on an office dock.

| Trigger | Example routine | Privacy / permission impact | Recommendation |
| --- | --- | --- | --- |
| Power connected | `At bedtime, when charging: Silent` | No location, account, or Nearby Devices permission | Add after conflict rules |
| Power disconnected | `When unplugged in the morning: Ring` | No additional sensitive permission | Add after the connected case is stable |
| Battery threshold | `At 15%: Vibrate` | Technically possible but can create noisy, surprising changes | Defer unless user demand is clear |

Android provides charging-state broadcasts and a sticky battery-status intent. The active automation service can register only for the charging events while automation is active, keeping the feature local and bounded. Broadcast delivery must still be treated as system-controlled rather than perfectly instantaneous. [3] [4]

### 7. A single Quick Settings automation tile — **useful, but only after Pause all is mature**

One toggleable tile labeled **Sound automation** would let frequent users pause or resume their routines without opening the app. It is valuable because it expresses a real two-state control, not a shortcut disguised as a feature.

The tile should show **Active**, **Paused**, or **Needs app open**. Tapping **Paused** should open a short, clear re-arm interaction if Android needs the user to launch the main activity to establish foreground readiness. Tapping while locked must only perform safe, reversible actions. Android recommends Quick Settings tiles for frequently accessed, rapid controls and supports stateful, toggleable tiles; it also warns against making too many tiles per app. [2]

### 8. Manual encrypted local backup and restore — **good later feature**

Routines are worth preserving, especially after users configure several places and schedules. A manual export/import feature can be valuable if it is intentionally user-controlled.

| Requirement | Design choice |
| --- | --- |
| No account | Use Android’s document picker; never create a cloud account or automatic sync |
| User visibility | Show exactly what will be exported: routines, private labels, optional coordinates, and no activity history by default |
| Security | Offer opt-in password encryption using modern Android cryptography; do not invent an insecure custom format |
| Compatibility | Store schema version and validate before import |
| Safety | Preview import conflicts and require explicit merge/replace choice |

This feature should follow the activity log and routine editing work, because the data model must be stable before portability is safe.

## Features worth deferring

The following ideas can be attractive but are not justified until the core system is proven across devices.

| Feature | Why defer or reject |
| --- | --- |
| Bluetooth connection routines | Useful for a car or headset, but needs Nearby Devices permission, device-selection design, disconnection edge cases, and more background-event testing. Android 12+ treats Bluetooth access as a runtime Nearby Devices permission. [5] |
| Wi-Fi network routines | A work/home SSID can be valuable, but network identifiers can imply location and Android restrictions make the privacy and support burden higher |
| Calendar-aware silence | Requires sensitive calendar-event access and users expect strong correctness around meeting boundaries and exceptions |
| NFC tags | Can be a pleasant power-user control, but it is less broadly useful than editing, pause, history, and charging routines |
| Home-screen widget | Helpful but overlaps with Quick Settings and notification actions; build only after the home control model is proven |
| Contact/caller exceptions | Risks scope creep into full Do Not Disturb policy management and requires nuanced user expectations |
| Continuous movement/activity triggers | Add sensor sensitivity, battery cost, and privacy risk without a strong enough advantage |
| Account, cloud synchronization, advertising, or telemetry SDKs | Conflicts with the core local-first trust proposition |

## Release sequence

The recommended feature order intentionally favors reliability and trust over trigger count.

| Release slice | Scope | Why this order |
| --- | --- | --- |
| **Reliability 1** | Finish Pixel Android 17 lifecycle correction; truthful re-arm result; active-notification state | Proves the core promise and prevents false successes |
| **Reliability 2** | Automation control card, Pause all, notification actions, local activity history, last-run status | Makes the persistent service understandable and user-controlled |
| **Routine quality** | Edit, duplicate, templates, selected weekdays, exclusions, next-run preview | Improves the workflows most users already need |
| **Conflict and override** | Manual hold, pause-until, conflict warnings, cooldowns | Prevents the richer routine set from behaving unpredictably |
| **Context expansion** | Charging connected/disconnected trigger with same history and conflict engine | Adds the safest meaningful new automation context |
| **Power-user convenience** | Single Quick Settings tile, then manual encrypted backup/import | Adds rapid control and portability without cloud lock-in |
| **Conditional future work** | Bluetooth or NFC only after user demand and Android-device matrix evidence | Avoids adding permission-heavy, fragile triggers just to broaden the list |

## What “better” should mean

A more polished Sound Scheduler is not the one with the most triggers. It is the one where a user can create a rule in under a minute, see whether it is armed, understand the next change, pause it instantly, and explain the last result without granting more data access than the feature actually needs.

The practical feature recommendation is therefore:

1. **Make active automation understandable and controllable.**
2. **Make every execution visible and truthful through local history.**
3. **Make existing time and place routines easier to edit and safer when they overlap.**
4. **Add charging-state routines as the first new trigger.**
5. **Add a single Quick Settings pause/resume tile only after it can accurately reflect the active state.**

## References

[1]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers — Background audio hardening"

[2]: https://developer.android.com/develop/ui/views/quicksettings-tiles "Android Developers — Create custom Quick Settings tiles"

[3]: https://developer.android.com/training/monitoring-device-state/battery-monitoring "Android Developers — Monitor the battery level and charging state"

[4]: https://developer.android.com/develop/background-work/background-tasks/broadcasts "Android Developers — Broadcasts overview"

[5]: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions "Android Developers — Bluetooth permissions"
