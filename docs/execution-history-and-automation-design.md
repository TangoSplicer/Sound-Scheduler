# Execution History, Last-Run Status, and Active Automation Design

**Prepared by:** Manus AI
**Date:** 14 August 2026
**Design baseline:** Sound Scheduler 1.2.4
**Purpose:** Specify the exact local schema, UI, service lifecycle, Android 17 correction, and subsequent feature choices for Sound Scheduler.

## 1. Product contract

Sound Scheduler must make one clear promise: **when automation is armed, the user can see that it is armed; when a routine runs, the user can see what happened; when Android prevents a change, the user can recover without guessing.** The application remains local-first: no sign-in, no server, no location history, no reverse geocoding, no tracking SDK, and no automatic data export.

A normal swipe away from Recents closes the activity but may leave a properly started foreground service active. A force-stop is a different user action that intentionally stops the application and its future work until the user opens it again. The app should never attempt to work around a force-stop. Android 17 may silently ignore `AudioManager.setRingerMode()` when the call is made from an invalid background audio lifecycle, so a visible foreground lifecycle must be established deliberately before a routine is due. [1]

> **Design rule:** The foreground-service notification is not a workaround to hide. It is the user’s proof that automation is armed and is the place from which they can pause it.

## 2. Audit finding and immediate Pixel Android 17 correction

### 2.1 What the current implementation gets right

Version 1.2.4 correctly starts an active automation service while the main activity is visible, uses a foreground-service notification, and attempts to retain the service while enabled routines exist. It also verifies the target ringer state before completing a one-time routine. These are necessary foundations.

### 2.2 Remaining failure mode

The receiver currently uses `ContextCompat.startForegroundService()` when a time or location event arrives. If the active service was reclaimed or is not known to be active, this recreates the service **from a background event**. Under Android 17 audio hardening, that service can lack the lifecycle capability required to make ringer-mode changes, even though it displays a foreground notification. The write can then be silently ignored. [1]

The correction is to make receiver behavior truthful and deterministic. A receiver must **not attempt to create the active automation lifecycle**. It may dispatch work only to an automation service already known to be foreground-active; otherwise it must record `AUTOMATION_REARM_REQUIRED`, leave the routine enabled, and prompt the user to reopen the app. This is safer than consuming a routine with a false success.

### 2.3 Required lifecycle architecture

| Component | Responsibility | Must never do |
| --- | --- | --- |
| `MainActivity` | Start/reconcile active automation only while visible; show explicit armed state | Pretend the foreground service is active without confirmation |
| `AutomationService` | Start foreground state immediately, maintain a single worker, execute queued routine requests, publish service health | Stop itself between enabled routines |
| `RoutineAlarmReceiver` | Validate action and enqueue only if active automation is available | Start a new foreground service from the alarm to manufacture eligibility |
| `LocationRoutineReceiver` | Validate the geofence and enqueue only if active automation is available | Bypass location/routine validation or create a background-started active service |
| `ExecutionRepository` | Atomically record an event and update routine summary | Store raw coordinates, addresses, or system log text |
| `AutomationActionReceiver` | Handle notification actions such as Pause all through the repository | Alter sound mode without a user-visible app state |

The app should maintain an in-process `AutomationService.isForegroundReady` state, updated only after `startForeground()` succeeds. The service must also publish a local `AutomationState` record, so the UI can show **Active**, **Re-arm needed**, **Paused**, or **Off** even after a process restart. A persisted state is a status record, not evidence that Android will permit a background write; the service’s live readiness remains the authority.

### 2.4 Exact execution rules

1. When the user opens Sound Scheduler and one or more routines are enabled, `MainActivity` asks `AutomationService` to start.
2. The service calls `startForeground()` immediately using the dedicated active-automation notification channel, then sets `isForegroundReady = true` only after that call succeeds.
3. A routine receiver receives an alarm or geofence event.
4. If `isForegroundReady` is true, the receiver submits the work to the existing service worker. The service applies the target mode, keeps foreground state active during final verification, creates a local execution record, and then completes/reschedules only on confirmed success.
5. If `isForegroundReady` is false, the receiver creates a local `AUTOMATION_REARM_REQUIRED` execution record. A one-time routine remains enabled and visible; a recurring routine remains scheduled. The user sees a clear prompt to open the app and re-arm automation.
6. When the final enabled routine is paused, deleted, or completed, the service stops foreground state, removes the persistent notification, and sets state to **Off**.
7. The notification’s **Pause automation** action sets a single local pause state, disables current execution, removes system registrations, and stops the service. It must not force-stop the package.

This flow supports the Android model where exact alarms can be delivered at the intended time, while audio changes still require a valid lifecycle. Exact alarms are a user-controlled special access and should be used only for the user-facing time routines that require them. [2]

## 3. Persistent active-notification specification

### 3.1 Separate channels

The app must split foreground and result notifications. A single low-priority channel makes it difficult to understand the active state and makes future result settings overly coupled.

| Channel | ID | Importance | Purpose | User controls |
| --- | --- | --- | --- | --- |
| Active automation | `sound_automation_active` | Low | Ongoing proof that automation is armed | User can keep it visible or adjust channel behavior in Android settings |
| Routine results | `sound_routine_results` | Low by default | Quiet confirmed-success and recoverable-failure results | User can opt into greater prominence without affecting the active service |
| Attention required | `sound_routine_attention` | Default | Rate-limited actionable recovery notice for missing Modes access, re-arm need, or a repeated failure | May be disabled, but the in-app status remains authoritative |

On Android 13+, notification denial should not prevent a foreground service from running; however Android may place its notice only in task-manager surfaces. The app must therefore show the same active/re-arm state in the home UI and never describe a denied notification as proof of failure. [3]

### 3.2 Notification layout

**Title:** `Sound Scheduler is active`
**Text:** `3 routines ready · Next: Quiet at 22:30`
**Small icon:** current Sound Scheduler notification icon
**Behavior:** ongoing, silent, low priority, `onlyAlertOnce`, no sound or vibration, immediate foreground-service display where supported.

| Action | Behavior | Confirmation |
| --- | --- | --- |
| Open | Opens the home screen and highlights the Automation card | None needed |
| Pause automation | Opens a confirmation activity/dialog or uses an explicit “Pause” confirmation notification flow | Must never pause accidentally from an unconfirmed tap |
| View activity | Opens the local Activity screen | None needed |

The notification should never expose a private location label, coordinate, or raw routine title when the device is locked. Its default content should show only counts and the next time routine. A user can choose richer lock-screen visibility through Android’s normal notification controls.

### 3.3 Automation state data

Create a singleton local table, `automation_state`, to support coherent UI and safe recovery.

| Column | Type | Meaning |
| --- | --- | --- |
| `id` | INTEGER primary key, fixed to `1` | Singleton record |
| `isPaused` | INTEGER/Boolean | User’s explicit global pause state |
| `lastArmedAtMillis` | INTEGER nullable | When the user last armed routines from the visible app |
| `lastActiveAtMillis` | INTEGER nullable | Last time foreground readiness was confirmed |
| `lastStateCode` | TEXT nullable | `ACTIVE`, `PAUSED`, `OFF`, `REARM_REQUIRED`, or `ACCESS_REQUIRED` |
| `lastStateDetailCode` | TEXT nullable | Controlled explanation code only |

Do not persist `isForegroundReady` as a permanent truth. It is an in-memory service property. The persisted table exists only to provide a last-known user-facing explanation after process recreation.

## 4. Exact local activity-log schema

### 4.1 `Routine` summary additions

Add a small set of nullable columns to the existing `routines` table. These fields make routine cards fast and avoid querying history merely to display the most recent result.

| Column | SQLite type | Nullability | Example | Purpose |
| --- | --- | --- | --- | --- |
| `lastAttemptAtMillis` | INTEGER | Nullable | `1786690260000` | When execution was last attempted |
| `lastOutcomeAtMillis` | INTEGER | Nullable | `1786690261200` | When final outcome was recorded |
| `lastOutcomeCode` | TEXT | Nullable | `APPLIED` | Stable result code |
| `lastObservedMode` | TEXT | Nullable | `vibrate` | Final verified mode when read safely |
| `lastOutcomeDetailCode` | TEXT | Nullable | `MODE_REJECTED_STAYED_RING` | Controlled recovery/message key |
| `lastExecutionId` | INTEGER | Nullable | `451` | Efficient link to current history record |

These fields are state summaries, not a second history table. They are always overwritten by the next execution attempt and are cleared when the routine is deleted.

### 4.2 `routine_executions` entity

```kotlin
@Entity(
    tableName = "routine_executions",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId", "occurredAtMillis"]),
        Index(value = ["occurredAtMillis"]),
        Index(value = ["outcomeCode", "occurredAtMillis"])
    ]
)
data class RoutineExecution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Int,
    val triggerType: String,
    val requestedMode: String,
    val occurredAtMillis: Long,
    val scheduledForAtMillis: Long? = null,
    val outcomeCode: String,
    val observedMode: String? = null,
    val detailCode: String? = null
)
```

### 4.3 Field constraints and privacy guarantees

| Field | Allowed values | Prohibited content |
| --- | --- | --- |
| `triggerType` | `time`, `location_enter`, `location_exit` | Raw geofence request IDs, coordinates, addresses |
| `requestedMode` | `ring`, `vibrate`, `silent` | Legacy/free-form values |
| `outcomeCode` | Controlled outcome taxonomy below | Exception stack traces or device logs |
| `observedMode` | `ring`, `vibrate`, `silent`, null | Volume levels, contact audio state, other app data |
| `detailCode` | Controlled recovery code | Human-entered location label, address, SSID, or system message |
| `scheduledForAtMillis` | Nullable time-routine target | Location timestamps unrelated to a fired transition |

Routine deletion cascades to its execution rows. This is intentional: the Activity screen is a routine-support tool, not a permanent behavioral archive. The user also receives a **Clear activity history** action that deletes all rows without changing active routines.

### 4.4 Outcome taxonomy

| Outcome code | Meaning | Complete/remove one-time routine? | User recovery |
| --- | --- | --- | --- |
| `APPLIED` | Target mode remained verified after confirmation window | Yes | None |
| `MODE_REJECTED` | Device did not retain target ringer mode | No | Open app; verify automation state and Modes access |
| `ACCESS_REQUIRED` | Modes/Do Not Disturb special access absent | No | Grant Modes access |
| `AUTOMATION_REARM_REQUIRED` | Foreground-ready service unavailable when the event arrived | No | Open app to re-arm automation |
| `EXACT_ALARM_DEFERRED` | Time delivery occurred later than target because exact alarms are unavailable | Recurring remains; one-time completes only after confirmed change | Grant exact-alarm access if precise timing matters |
| `LOCATION_ACCESS_REQUIRED` | Place routine cannot arm because background location is absent | No | Grant background location |
| `LOCATION_UNAVAILABLE` | Location services/Play services cannot register or deliver safely | No | Turn on location or review device support |
| `PAUSED` | User paused that routine or all automation | No execution | Resume when ready |
| `INVALID_CONFIGURATION` | Stored routine cannot be executed safely | No; flag disabled only after UI confirmation | Review routine |

### 4.5 Transactional repository API

Every attempt must create exactly one execution record and one matching routine summary update in the same Room transaction.

```kotlin
@Transaction
suspend fun recordExecutionAndUpdateRoutine(
    execution: RoutineExecution,
    summary: RoutineExecutionSummary
)
```

For non-successes, write the event before sending an optional notification. Notifications are not a durable audit record and may be denied, delayed, or cleared by the user. During a successful time routine, write `APPLIED`, complete/reschedule the routine, and then reconcile active automation state in a second short transaction. This ordering prevents a one-time routine from disappearing before its outcome is persisted.

### 4.6 Retention and maintenance

Apply retention immediately after each insert, on a background executor.

| Rule | Value | Rationale |
| --- | --- | --- |
| Age limit | Delete records older than 30 days | Sufficient support window without a long behavioral archive |
| Count limit | Retain at most 100 total local execution rows | Bounds storage even for frequent routines |
| Cleanup trigger | After each event insert and on app startup | Keeps implementation simple and predictable |
| Manual deletion | `Clear activity history` in Activity settings | Gives the user direct local-data control |
| Export | Not in first release | Avoids exposing operational data before a privacy-reviewed manual export design exists |

## 5. Exact UI design

### 5.1 Home screen: Automation card

Place this card below the current sound-mode header and above quick mode controls.

| Element | Active state | Needs-attention state | Paused/off state |
| --- | --- | --- | --- |
| Leading icon | Shield/check with subtle active accent | Warning outline | Paused indicator |
| Heading | `Automation active` | `Automation needs attention` | `Automation paused` / `No active routines` |
| Supporting text | `3 routines ready · Next: Quiet at 22:30` | `Open the app to re-arm background automation` | `Resume saved routines` / `Add a routine` |
| Primary action | `Pause all` | `Re-arm automation` or `Fix access` | `Resume automation` / `Add routine` |
| Secondary action | `View activity` | `View activity` | Optional, not required |

Use a Material card with semantic text; do not rely only on color or animation. The active card must say **Active** even if the notification is not shown in the shade due to user notification settings.

### 5.2 Routine card: last-run block

Add the following structured section beneath the routine detail, separated by a subtle divider.

```text
Last run
✓ Changed to Vibrate · 2 min ago
```

For an exception:

```text
Needs attention
! Phone stayed on Ring · Open app to re-arm
```

| Status | Icon treatment | Text | Action |
| --- | --- | --- | --- |
| Applied | Check mark with accessible text | `Changed to Vibrate · 2 min ago` | Opens Activity filtered to that routine |
| Delayed | Clock icon | `Ran 12 min late because exact alarms are off` | Opens exact-alarm guidance |
| Re-arm needed | Warning icon | `Open app to re-arm background automation` | Re-arms if activity visible |
| Access missing | Key/permission icon | `Modes access required` | Opens correct Android settings page |
| No history | Neutral clock icon | `Not run yet` | None |
| Paused | Pause icon | `Paused by you` | Resume routine |

If a title is long, the status must wrap without pushing pause/delete controls below a safe touch target. The feature requires a RecyclerView/Multi-view-type migration if the existing ListView cannot accommodate dynamic status and action elements accessibly.

### 5.3 Activity screen

**Top app bar:** `Activity`
**Subheading:** `Stored only on this device`
**Filters:** `All`, `Applied`, `Needs attention`
**Toolbar action:** overflow → `Clear activity history`

Each item uses this layout:

```text
Today
  09:00  Work arrival
          Requested Vibrate · Changed to Vibrate
          On arrival

Yesterday
  22:30  Weeknight quiet
          Requested Silent · Phone stayed on Ring
          Open app to re-arm
```

The screen must use the routine’s private label only when it remains associated with an existing routine. It must never show coordinates, a map, route history, or an address. Clicking an item opens the routine editor, not a map.

### 5.4 Empty state and deletion confirmation

No history state:

> **No activity yet**
> Completed and attempted sound routines will appear here. Activity is stored only on this device.

Clear-history confirmation:

> **Clear activity history?**
> This permanently removes local run records. Your routines and automation settings will not change.

## 6. Room migration and test plan

The current database is version 3. The execution-history release should be **database version 4**.

| Migration step | Required SQL/Room behavior |
| --- | --- |
| `MIGRATION_3_4` | Add six nullable routine summary columns with safe defaults/nulls |
| `MIGRATION_3_4` | Create `automation_state` singleton table |
| `MIGRATION_3_4` | Create `routine_executions` with foreign key cascade and required indexes |
| Initial state | Insert singleton `automation_state` row with `id = 1`, `isPaused = 0`, `lastStateCode = OFF` |
| Upgrade validation | Existing time and location routines remain unchanged and show `Not run yet` until their first execution |

Required tests include Room migration tests for v3→v4, transaction atomicity tests, retention boundary tests, summary mapping tests, time and location outcome tests, re-arm-required receiver tests, notification action tests, and accessibility/layout tests at 100%, 130%, and 200% font scale.

The device matrix must include the affected Pixel Android 17 device, at least one Android 13 device, and one non-Pixel Android device. The test suite should explicitly test a normal swipe-away, an in-app Pause all, a final-routine deletion, and a force-stop/reopen recovery. Android alarms and foreground-service behavior cannot be fully proven by unit testing alone. [1] [2]

## 7. Implementation order

| Slice | Deliverable | Dependencies | Acceptance gate |
| --- | --- | --- | --- |
| **1** | Correct receiver dispatch: do not manufacture foreground state from a background event; record re-arm requirement | Active service state API | A dead service never consumes a one-time routine |
| **2** | Dedicated active-automation notification channel and Automation card | Service state repository | User can see active, paused, off, and re-arm-needed states |
| **3** | `automation_state` table, Pause all state, notification actions | Room v4 and action receiver | Last routine pause/delete stops service; resume re-arms visibly |
| **4** | `routine_executions` table, transactional repository, local retention | Room v4 | Every attempted execution produces one durable, privacy-safe local record |
| **5** | Routine-card last run status and Activity screen | Slice 4 | User can explain the latest result without Android settings or logs |
| **6** | Full physical-device regression and signed internal build | All prior slices | Pixel Android 17 and Android 13+ matrix passes |

## 8. Next feature decisions

The next features should build on execution history and conflict handling rather than add uncontrolled trigger types.

| Rank | Feature | Decision | Why |
| --- | --- | --- | --- |
| 1 | Activity log and last-run status | **Build next** | Directly solves trust, support, and false-success visibility |
| 2 | Automation card, Pause all, and notification actions | **Build with the log** | Makes the persistent service transparent and user-controlled |
| 3 | Routine editing, duplication, and next-run preview | **Build after the log** | High daily value and low privacy cost |
| 4 | Selected weekdays, date exclusions, temporary overrides | **Build after editing** | Improves real schedules without new permissions |
| 5 | Conflict rules and routine cooldowns | **Build before new triggers** | Prevents automation from fighting itself |
| 6 | Manual encrypted local export/import | **Evaluate later** | Useful portability without accounts; needs careful privacy review |
| 7 | Charging-state trigger | **First optional trigger** | Useful and low-sensitivity; fits existing local architecture |
| 8 | Bluetooth-device trigger | **Consider later** | Useful but adds runtime permissions and support complexity |
| 9 | Wi-Fi trigger | **Defer until privacy review** | SSID/network state can reveal sensitive location context |
| 10 | Calendar trigger | **Defer** | Adds sensitive event-data access and high user-expectation risk |
| 11 | Accounts, cloud sync, ads, analytics, continuous GPS | **Do not add** | Conflicts with the privacy-first product identity |

## 9. Definition of the next production-quality milestone

The next release is ready for a signed internal test only when all items below pass.

| Area | Required proof |
| --- | --- |
| Android 17 time switching | Persistent active notification is present; Ring, Vibrate, and Silent routines work after normal swipe-away on the affected Pixel |
| Truthful lifecycle | If active automation is unavailable, routines remain visible and record `AUTOMATION_REARM_REQUIRED` rather than being consumed |
| User control | Pause all, resume, last-routine deletion, and notification action reconcile local state, system registrations, and foreground notification correctly |
| Execution history | Every result has one local record, an accurate card summary, bounded retention, and clear deletion control |
| Privacy | No new network, account, analytics, address, raw coordinate history, or hidden data collection is introduced |
| Device quality | Android 13+, Android 17 Pixel, and one non-Pixel smoke test pass; automated tests, lint, and CI remain green |
| Release handling | Owner signs the final artifact, verifies it, and uses controlled internal testing before a broader release |

## References

[1]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers — Background audio hardening"

[2]: https://developer.android.com/develop/background-work/services/alarms "Android Developers — Schedule alarms"

[3]: https://developer.android.com/develop/ui/compose/notifications/notification-permission "Android Developers — Notification runtime permission"
