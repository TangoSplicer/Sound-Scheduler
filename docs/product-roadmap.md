# Sound Scheduler Product, Quality, and Release Roadmap

**Prepared by:** Manus AI  
**Date:** 14 August 2026  
**Planning baseline:** Version 1.2.2 debug build, including time routines, local place routines, Android 17 foreground execution, and navigation-safe home-screen layout.

## Executive direction

Sound Scheduler already has a strong and differentiated foundation: it is a **local-first Android automation utility** that changes a phone between Ring, Vibrate, and Silent from either a time schedule or a user-captured place. Its current privacy promise is unusually clear: no account, no backend, no analytics, no reverse geocoding, and no location sharing. The next objective should not be feature volume. It should be to prove that core automation is dependable on real devices, make routine behavior easy to understand and recover, and only then add features that improve everyday usefulness without weakening the privacy model.

> **Recommended product principle:** a scheduled change should always be predictable, explainable, reversible, and locally controlled.

The immediate priority is a short stabilization and release-readiness cycle. The Android 17 and edge-to-edge fixes are source-validated, but the affected Pixel device still needs to confirm the real background time trigger, the corrected bottom safe area, and location transitions. Android can defer or suppress background execution in some conditions, and geofence behavior remains dependent on system location and power management; device evidence therefore carries more weight than another purely source-level feature in this stage. [1] [2] [3]

| Planning horizon | Primary outcome | Release posture |
| --- | --- | --- |
| **Now through the next validation cycle** | Demonstrate reliable core behavior on physical Android 13–17 devices and close release gates | No new major features until critical checks pass |
| **Stability release** | Improve resilience, diagnostics, accessibility, and test coverage | Suitable for signed limited release after owner approval |
| **Usability release** | Make routine creation, conflict handling, editing, and recovery much clearer | Expand only features with clear daily value |
| **Automation expansion** | Add carefully consented optional triggers and advanced controls | Preserve local-only defaults and explicit user intent |
| **Maturity** | Establish a repeatable release, compatibility, and privacy-maintenance program | Publish only after ongoing policy and device validation |

## Current-state assessment

The application currently supports named one-time and recurring time routines, Ring/Vibrate/Silent targets, quick manual controls, pause/resume/delete actions, private arrival and departure geofences, Room migrations, boot/update reconstruction, quiet status notifications, and build automation. The current debug package also addresses two recent device-facing regressions: Android 17 background audio hardening and navigation-bar overlap on an edge-to-edge Pixel layout.

The largest remaining risks are not conceptual gaps in the feature list. They are **physical-device confirmation**, **owner-controlled signing**, and **platform variability**. Location routines need real arrival/departure tests with background location granted. Time routines need confirmation while the app is backgrounded, especially on Android 17. A signed artifact must be created only with the owner’s release key and verified before distribution.

| Area | Status | Roadmap implication |
| --- | --- | --- |
| Time-based sound modes | Implemented and source-validated | Confirm Ring, Vibrate, Silent, recurrence, reboot, and Android 17 background execution on device |
| Place routines | Implemented and source-validated | Validate permission journey, arrival, departure, radii, lifecycle, and power-management behavior on a controlled route |
| Privacy model | Strong local-only default | Protect this as a non-negotiable product constraint during every expansion |
| UI quality | Professionally upgraded; latest safe-area correction pending device confirmation | Perform a full edge-to-edge, font-scale, dark-mode, and accessibility pass |
| Automated verification | Local tests/lint/package and GitHub Actions pass | Modernize workflow actions and add device-facing regression automation where practical |
| Public release | Blocked by physical testing and owner signing key | Use a staged internal/closed release before broader distribution |

## Phase 0 — Finish the current device validation

This phase should be completed before any substantial feature work. The purpose is to verify the exact conditions users will experience, rather than infer behavior from unit tests alone.

### Required acceptance sequence

| Priority | Check | Completion evidence | Owner |
| --- | --- | --- | --- |
| **P0** | Install 1.2.2 over the current debug build | The home screen opens; **Add sound routine** is entirely above the Pixel navigation controls | Device tester |
| **P0** | Android 17 background time routine | App is backgrounded—not force-stopped—and a Ring → Vibrate time routine shows the brief execution notification and changes the device mode | Device tester |
| **P0** | Ring, Vibrate, and Silent checks | One separate one-time routine succeeds for each target mode with Modes access enabled | Device tester |
| **P0** | Notification-denial independence | An authorized routine still changes mode after ordinary notification permission is denied | Device tester |
| **P0** | Exact-alarm fallback | The app communicates timing deferral correctly when exact alarms are unavailable and restores future routines afterward | Device tester |
| **P0** | Location consent journey | Capture, background-location settings return, and geofence registration are understandable and recoverable | Device tester |
| **P0** | Arrival and departure route test | Each selected transition changes mode once; selected radius and context are recorded | Device tester |
| **P1** | Boot and package-update recovery | Enabled time and location routines rebuild correctly after restart/update | Device tester |
| **P1** | Accessibility and visual review | 100%, 130%, and 200% font scale; dark appearance; TalkBack labels; gesture and three-button navigation | Device tester |

Record each result in the acceptance plan with device model, Android version, build version, permission state, routine configuration, expected result, actual result, and screenshots or screen recordings for failures. A structured test log will prevent ambiguous reports such as “rejected” from becoming costly rediscovery work.

### Immediate defect policy

Any P0 failure should create a small, reproducible issue with one owner, acceptance criterion, affected Android version, and a linked artifact. No new trigger type should enter development until P0 failures are resolved or explicitly accepted as a documented platform limitation. For a P1 failure, decide whether it blocks a limited release based on the number of affected devices and whether a safe recovery path exists.

## Phase 1 — Release stabilization and observability

After the current validation passes, the first engineering release should focus on explaining and recovering from platform behavior. The user should never need to guess whether a routine did not run, ran but was blocked, or ran later than expected.

### 1.1 Improve routine outcome visibility

Introduce a local **Activity log** screen. Each log record should store only operational metadata already needed for troubleshooting: local routine ID, trigger type, intended sound mode, timestamp, outcome, and a short reason code. It should not store a continuous location trail, a resolved street address, contact data, or any remote identifier.

| Capability | User value | Privacy boundary | Suggested acceptance criterion |
| --- | --- | --- | --- |
| Last-run status on each routine card | Shows whether the routine succeeded, was deferred, was paused, or needs access | Store only routine-local execution metadata | A user can identify the last outcome from the card without opening settings |
| Local event log | Gives a supportable explanation after a missed trigger | Retain a bounded local history, such as 30–90 days or a user-selected maximum | User can clear all history from Settings |
| Human-readable outcome reasons | Separates missing Modes access, missing location access, exact-alarm deferral, and OS rejection | No device fingerprinting or server reporting | Every non-success outcome maps to a recovery action or an explicit platform limitation |
| Export diagnostic bundle | Makes optional support possible without telemetry | User manually shares a redacted text file; coordinates excluded by default | Export requires explicit tap and preview |

The Android 17 foreground service should remain short-lived and visible only while applying the requested change. It must not turn into a persistent background process. Android explicitly constrains background audio and foreground-service behavior, so long-running execution would create both policy and battery risk. [1] [3]

### 1.2 Harden platform recovery

Strengthen platform-specific recovery paths without expanding permissions. The app should re-check key access when it returns to the foreground, clearly show the difference between a permission and a special access, and display the next scheduled time or the next registered place trigger.

Recommended work includes an exact-alarm capability indicator, a location-services-off indicator, a Google Play services availability check for location routines, a manual **Refresh routines** action, and a clear explanation after boot or app update. Add an explicit routine-validation pass that safely disables or flags corrupted or obsolete records rather than failing silently.

### 1.3 Establish a proper Android test pyramid

Unit tests are valuable but cannot emulate the operating system behaviors that matter most here. Expand testing in layers.

| Test layer | Focus | Initial backlog |
| --- | --- | --- |
| Unit tests | Data validation, recurrence math, conflict rules, request ID parsing, routine state transitions | Add tests for routine precedence, restoration, event-log retention, and every migration |
| Instrumentation tests | Activity UI, permission-state rendering, Room migrations, accessible labels, system-inset layouts | Add automated tests for create/edit/pause/resume/delete and large-font layout |
| Device smoke tests | Exact alarms, DND/Modes access, Android 17 foreground execution, geofence registration | Maintain a scripted manual matrix for Pixel and one non-Pixel OEM device |
| Compatibility tests | Android 13 through current Android release, gesture and three-button navigation | Run the minimum smoke suite for each supported API level before release |
| Release checks | Signed package integrity, manifest review, privacy declaration review | Automate `apksigner` verification once the owner supplies a release key |

## Phase 2 — Make routine automation more useful every day

Once core reliability is demonstrated, focus on features that reduce the number of taps required to express common intent. These should be introduced through a clear precedence model rather than as disconnected checkboxes.

### 2.1 Routine editing, duplication, and templates

Users should be able to edit an existing time or place routine, duplicate it, and start from local templates such as **Weeknight Silent**, **Work arrival Vibrate**, and **Weekend Ring**. Templates should contain only default times, recurrence choices, and labels; they must not add accounts, cloud synchronization, or preset places.

A creation flow should eventually become a full routine editor with a preview: trigger, next occurrence or selected place transition, target mode, recurrence, state, and last result. This will reduce accidental schedules and make later conflict controls intelligible.

### 2.2 Better recurrence and temporary behavior

The existing daily, weekly, and monthly choices cover the basic need. The next step is **selected weekdays** and **date exclusions**. Avoid implementing arbitrary cron syntax in a consumer Android app; it is difficult to explain and creates a high-support surface.

Add two high-value controls after the simpler editor work is stable:

| Feature | Why it matters | Safeguard |
| --- | --- | --- |
| Selected weekdays | Matches work/school patterns better than a single weekly rule | Show the upcoming three occurrences before save |
| Date exclusions | Lets a user suspend a normal routine during holidays or leave | Keep exclusions local and visibly listed |
| Temporary mode with end time | Supports “Silent for one hour” without creating a permanent routine | Always show restore time and permit one-tap cancellation |
| Manual override pause | Prevents a routine from immediately undoing a deliberate manual change | Offer a clear “pause automation until…” choice rather than silently changing rules |
| Routine duplication | Simplifies similar schedules and place setups | Duplicate disabled by default until reviewed and saved |

### 2.3 Conflict resolution and routine precedence

As routine count grows, conflicting rules become the dominant source of distrust. Introduce a deterministic, documented policy before expanding trigger types. A recommended baseline is: **explicit temporary override > manual quick control > most recently triggered enabled routine > scheduled fallback**. The app should display why the current mode changed and which upcoming routine can change it next.

Do not let two routines fight in rapid succession. Add a short de-duplication window for identical repeated geofence events, a per-routine cooldown visible in the log, and conflict warnings at save time when two time routines are scheduled for the same moment with different target modes.

## Phase 3 — Expand triggers carefully, without breaking privacy

Only implement triggers with a concrete user story and a clear permission disclosure. The app should remain fully useful with time and place routines alone.

### Recommended optional triggers

| Candidate | User story | Permission / platform impact | Recommendation |
| --- | --- | --- | --- |
| Wi-Fi network presence | Set Vibrate at a saved work Wi-Fi and Ring after leaving | Nearby/network state; SSID privacy considerations | Consider after conflict engine and disclosure design are complete |
| Bluetooth device connection | Set Ring when a car kit disconnects or Vibrate when office headset connects | Bluetooth runtime permissions on modern Android | High practical value; implement as an optional local trigger later |
| Charging state | Switch to Ring while charging overnight | No sensitive location data; power broadcasts vary by version | Good low-privacy-cost candidate |
| Calendar-aware quiet time | Silence during on-device meetings | Calendar permission and sensitive event metadata | Defer unless an entirely on-device, narrowly scoped approach is clearly valuable |
| NFC manual profile switch | Tap a user-owned tag to apply a mode | NFC hardware dependency; no background location | Consider as a simple opt-in power-user feature |
| Continuous movement or activity recognition | Change mode while driving or exercising | Sensitive data and ongoing sensor behavior | Do not prioritize; it conflicts with the app’s simple privacy posture |

Each new trigger should use the same `Routine` abstraction, lifecycle rules, log entries, conflict engine, and permission-status surface as time and place routines. Avoid a separate architecture for each trigger; that would make support and testing grow non-linearly.

## Phase 4 — User experience, accessibility, and design maturity

The product should feel calm rather than over-instrumented. Sound automation is inherently sensitive because users notice a mistake at exactly the wrong moment. The interface should prioritize clear state and recovery.

### UX backlog

| Area | Recommended improvement | Definition of done |
| --- | --- | --- |
| Home screen | Show active routine count, next time trigger, and a concise location-routine readiness state | Information is understandable without scrolling or opening a card |
| Routine cards | Add edit action, last-run result, next occurrence, and explicit enabled/paused state | No interaction relies on color alone |
| Creation flow | Use step grouping or a concise progressive-disclosure form | Location details do not distract users creating a time routine |
| Empty state | Offer two template entry points: time routine and place routine | No permission prompt occurs until the user chooses the relevant action |
| Settings | Centralize Modes access, exact alarms, notifications, location readiness, data controls, and help | Each row states current status and opens the correct system screen or local view |
| Accessibility | Test TalkBack action labels, order, touch targets, contrast, dynamic type, and landscape | Core routine creation and pause/resume can be completed without relying on vision |
| Visual design | Apply dynamic color only if it preserves contrast and makes state meaningful | Stable action placement remains consistent across Android navigation modes |

### Notification design

Keep notifications low-noise. Routine success can remain quiet by default, while a failed or blocked routine should be actionable and rate-limited. The Android 17 execution notification is an operational requirement; it should communicate that the app is briefly applying the user’s own scheduled setting, then disappear when the action is complete. [1]

## Phase 5 — Privacy, data control, and trust

Privacy is a product feature, not merely a compliance statement. The present no-account, no-backend posture should remain the default architecture. Any later data feature should begin with a local-only implementation and a deletion path.

### Privacy commitments to preserve

| Commitment | Implementation direction |
| --- | --- |
| No account required | Do not gate scheduling, backups, or support behind sign-in |
| No cloud location history | Keep geofence coordinates in the local database; never add passive history collection |
| No reverse geocoding by default | Continue using a user-chosen private label rather than sending coordinates to a service |
| No advertising SDK | Avoid SDKs that introduce tracking or opaque data sharing |
| Explicit permission timing | Request a permission only after the feature that requires it is selected |
| User-controlled deletion | Provide clear deletion for routines, local activity log, and any future export files |

A future manual backup and restore feature can be valuable, but it should use the Android document picker, explain exactly what is exported, exclude diagnostic history and coordinates unless explicitly chosen, and offer optional local-file encryption. Do not silently enable device-to-cloud backup for location routines; preserve the existing conservative backup posture until users make an informed choice.

If the application is distributed through Google Play, maintain a concise privacy policy and accurate Data safety declaration. Foreground-service and sensitive-permission disclosures should be reviewed before every material trigger expansion. [4] [5]

## Phase 6 — Distribution, release engineering, and maintenance

The app is not ready for a wide production release until the owner signs the artifact and physical-device acceptance is complete. After that, use staged distribution rather than a single broad launch.

### Release sequence

| Stage | Scope | Exit criterion |
| --- | --- | --- |
| Internal debug validation | Owner and a small number of trusted devices | P0 device matrix passes and defects are triaged |
| Signed internal release | Owner-signed package or internal Play testing | Signature verification passes; installation/upgrade path tested |
| Closed testing | Small diverse Android device group | Core time and place workflows show no unresolved severe issues |
| Limited production rollout | Controlled percentage of eligible users | No critical automation failures and support feedback remains manageable |
| General availability | Broader distribution | Privacy declaration, store listing, help copy, and release notes are complete |

The repository’s GitHub Actions workflow should be modernized to remove its current deprecated Node.js action warnings. Pin action versions, enable dependency update automation, retain Gradle wrapper verification, archive lint/test reports, and fail builds on newly introduced lint errors. Add a release workflow that can build a signed artifact only when the owner supplies signing material through an approved secret-management process; do not place signing keys in the repository or debug environment.

## Phase 7 — Metrics without surveillance

The product should not add traditional third-party analytics merely to create a roadmap. A privacy-consistent alternative is an optional, local-only **Reliability dashboard** showing counts of successful, deferred, and blocked routines over a user-selected retention period. It remains on the device, can be cleared at any time, and may be manually exported only after an explicit user action.

| Measure | Why it matters | Privacy-safe collection approach |
| --- | --- | --- |
| Routine success rate | Detects whether core automation is dependable | Local aggregate only |
| Blocked routine reasons | Identifies access and platform friction | Local reason counts only; no device identifier |
| Time-to-recovery | Measures clarity of recovery UI | Local timestamp differences only |
| Place-trigger confirmation rate | Detects unreliable device/environment combinations | Local aggregate; no route history |
| Upgrade and migration outcome | Verifies database compatibility | Local migration status, cleared after a bounded retention period |

Use these measures only after the activity log exists, and make the dashboard optional. Do not transmit it automatically.

## Prioritized delivery backlog

The following order is recommended. It deliberately front-loads proof of reliability and avoids scope creep.

| Rank | Initiative | Value | Effort | Dependency |
| --- | --- | --- | --- | --- |
| 1 | Complete P0 Pixel acceptance, including 1.2.2 safe area and Android 17 time routine | Critical | Small | Current device test |
| 2 | Complete physical arrival/departure, reboot, and update location matrix | Critical | Medium | Background location and test route |
| 3 | Produce owner-signed internal release and verify signature | Critical | Small | Owner release key |
| 4 | Add local last-run status and bounded activity log | High | Medium | Stable routine outcomes |
| 5 | Add robust routine editing and duplication | High | Medium | UI/editor refactor |
| 6 | Add selected weekdays, date exclusions, and future-occurrence preview | High | Medium | Recurrence model extension |
| 7 | Add conflict warnings, deterministic precedence, cooldown, and manual override pause | High | Medium | Routine state model and activity log |
| 8 | Expand unit, instrumentation, and device smoke coverage | High | Ongoing | Test environment decisions |
| 9 | Conduct full accessibility, dark-mode, font-scale, and edge-to-edge audit | High | Medium | Stable layouts |
| 10 | Add Settings hub and clearer recovery surfaces | Medium | Medium | Outcome/reason taxonomy |
| 11 | Add optional encrypted manual export/import | Medium | Medium | Data schema/versioning plan |
| 12 | Add one low-risk optional trigger, beginning with charging state | Medium | Medium | Conflict engine and consent copy |
| 13 | Evaluate Bluetooth or Wi-Fi trigger only after privacy review | Medium | Large | New permissions and trigger framework |
| 14 | Build optional local reliability dashboard | Medium | Medium | Activity log |
| 15 | Modernize CI action versions and automate dependency hygiene | Medium | Small | Workflow maintenance |

## Explicit non-goals for the next releases

The roadmap should deliberately reject several tempting additions until there is a strong, privacy-preserving use case. Do not add an account system, remote routine synchronization, cloud location storage, advertising, continuous GPS tracking, silent permission prompts, unrestricted long-running foreground services, or attempts to bypass Android’s Modes/Do Not Disturb controls. These choices would weaken the product’s trust model and increase platform-policy risk without improving the core promise.

## Decisions requested from the product owner

The immediate test result will decide whether the Android 17 implementation needs another corrective iteration. Once that result is known, the following decisions should set the next release scope.

| Decision | Recommended default | Why it matters |
| --- | --- | --- |
| First signed-release channel | Small internal or closed test cohort | Limits risk while real devices validate automation |
| Initial supported Android range | Maintain Android 13+ as tested priority while retaining current minimum compatibility | Aligns support promise to real device validation |
| Next feature after stabilization | Local activity log and last-run status | Improves trust and supportability without adding permissions |
| Data portability | Manual, explicit export/import only | Preserves local-first design |
| Optional trigger strategy | Charging state first; Bluetooth/Wi-Fi only after privacy review | Adds value with less sensitive data exposure |
| Telemetry policy | No third-party analytics; optional local-only reliability dashboard | Protects the existing privacy differentiator |

## References

[1]: https://developer.android.com/about/versions/17/changes/bg-audio "Android Developers — Background audio hardening"

[2]: https://developer.android.com/develop/sensors-and-location/location/geofencing "Android Developers — Create and monitor geofences"

[3]: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start "Android Developers — Restrictions on starting a foreground service from the background"

[4]: https://support.google.com/googleplay/android-developer/answer/10144311 "Google Play Console Help — Data safety section"

[5]: https://support.google.com/googleplay/android-developer/answer/13392821 "Google Play Console Help — Foreground services and full-screen intent requirements"
