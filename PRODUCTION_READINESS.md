# Sound Scheduler 1.1.0 — Production Readiness Review

**Prepared by:** Manus AI

**Scope:** Source review, repair, dependency and release hardening, visual identity, build validation, and release gating.

**Assessment date:** 13 August 2026

## Executive assessment

Sound Scheduler has been upgraded from a non-buildable beta stub into a **buildable, locally persisted Android routine-alert application**. The stable scope is deliberately focused: named one-time, daily, weekly, and monthly alerts; local Room persistence; reboot and app-update recovery; contextual notification permission handling; exact-alarm fallback behavior; deletion; and a professional launcher icon.

> **Release decision:** The codebase is **ready for device acceptance testing and signing**, but it is **not yet publish-ready** because the available release APK is intentionally unsigned and no physical Android device was available for runtime permission, alarm-delivery, reboot, and launcher validation. The remaining gates require the owner’s signing key and a real device; they are not code defects.

## What was repaired

| Area | Previous state | Completed improvement |
| --- | --- | --- |
| Build system | Repository configuration prevented Gradle evaluation; KSP and Kotlin configuration were inconsistent; the release shrinker path was misspelled. | Centralized repository policy, aligned Kotlin 2.2.0 with KSP 2.2.0-2.0.2, corrected Gradle syntax, standardized Java 17 targets, and added a valid `proguard-rules.pro`. |
| Routine creation | The main screen created invalid time routines in memory only, causing model validation failure and data loss on restart. | Added a validated routine editor, Room-backed ViewModel/repository flow, and persisted creation/deletion. |
| Alert delivery | A process-bound hourly service did not schedule time routines or survive normal Android lifecycle constraints. | Replaced it with `AlarmManager` plus a dedicated receiver, one-time and recurring time calculation, and exact-alarm fallback. |
| Recovery | The boot receiver only started an unreliable service. | Active time routines are now reconstructed after boot, package replacement, activity resume, and exact-alarm permission grant. |
| Permissions | Notification and exact-alarm access were declared but not handled in the user flow. | Notification permission is requested in context when saving an alert; the UI explains and links to exact-alarm special access when necessary. |
| Security and privacy | The beta declared location, calendar, billing, and other incomplete paths despite no reliable implementation. | Removed incomplete exposed modules and permissions. The stable manifest has only boot recovery, exact alarms, and notifications; backups and cleartext traffic are disabled. |
| Visual identity | Launcher art was template-like and inconsistent across density buckets. | Created and applied a custom cobalt, white, and teal clock-and-sound-wave launcher icon with adaptive and legacy fallbacks. |
| Documentation | The README advertised unsupported premium functionality. | Rewrote the README for the implemented 1.1.0 stable scope and release process. |

## Validation evidence

| Check | Result | Evidence |
| --- | --- | --- |
| Kotlin compilation | Passed | `:app:compileProdDebugKotlin` completed successfully. |
| Unit tests | Passed | `:app:testProdDebugUnitTest` passed, including future, expired, daily recurrence, and model-validation cases. |
| Debug APK | Passed | `:app:assembleProdDebug` completed; output is signed with the Android debug key. |
| Minified release build | Passed | `:app:assembleProdRelease` completed, including R8 shrinking and release lint. |
| APK integrity | Passed | `unzip -t` reported no compressed-data errors in the release APK. |
| Manifest inspection | Passed | Package `com.soundscheduler.app`, `minSdk 24`, `targetSdk 35`, version `1.1.0` / code `2`; only required routine permissions remain. |
| Launcher resources | Passed | Adaptive wrappers and mdpi through xxxhdpi fallback entries are present in compiled release resources. |
| Source hygiene | Passed | `git diff --check` completed with no whitespace errors. |

## Generated artifacts

| Artifact | Status | Size | SHA-256 |
| --- | --- | ---:| --- |
| `app-prod-debug.apk` | Installable validation build, debug-signed | 6,543,328 bytes | `e0d09388fdc10e7e4bee8f65ce6f30e5c434ca284bcb6c55f11b95b5dfcf46e4` |
| `app-prod-release-unsigned.apk` | Optimized release candidate, **unsigned** | 1,828,748 bytes | `0f9eb026bfcf384cbadb09c3ff3103daba1db56379531c4a0a151d2af1db2e72` |
| `design/sound_scheduler_icon_transparent.png` | Master launcher artwork | 1920 × 1920 RGBA PNG | Included in source workspace |

## Required final release gates

The following owner-controlled steps remain before public distribution. Android requires special handling for exact alarms on recent platform versions, and notifications are a runtime permission on Android 13 and later. [1] [2]

| Gate | Why it remains | Completion criterion |
| --- | --- | --- |
| Release signing | No production keystore was present in the repository, and it would be inappropriate to invent or retain one on the owner’s behalf. | Provide an untracked `keystore.properties` file; run `:app:assembleProdRelease`; verify with `apksigner verify --verbose --print-certs`. |
| Physical-device acceptance | An emulator or physical device was not attached to the review environment. | Install the signed build on at least one Android 13+ device and complete the checklist below. |
| Play policy review | Exact-alarm use should be consistent with the app’s user-facing alert purpose and store policy. | Confirm the final Play listing and permission declaration align with the applicable policy. [1] |

## Physical-device acceptance checklist

1. Install the signed release over a clean Android 13+ device profile and verify the custom icon appears correctly in the launcher.
2. Create a one-time routine for two to five minutes in the future. Allow notifications and confirm a visible alert opens the app when tapped.
3. Disable exact-alarm special access, create another routine, and confirm the application explains the possible delivery delay without crashing.
4. Re-enable exact-alarm special access and confirm existing active routines remain scheduled.
5. Create daily, weekly, and monthly routines; inspect the next scheduled time and confirm the next recurrence remains active after delivery.
6. Delete a routine and confirm its pending alert does not fire.
7. Restart the device and confirm active routines are restored.
8. Upgrade an installed older build, if one exists, and confirm app-update recovery preserves active routines.
9. Deny notifications, create a routine, and confirm the app remains usable while explaining that Android will not display alerts.
10. Verify the final signed APK with `apksigner`, then upload the signed artifact or App Bundle through the chosen distribution channel.

## Ongoing operational monitoring

The stable 1.1.0 app is fully local and has no backend or always-on service to monitor. The appropriate post-release approach is therefore **release and device monitoring**, not server polling. A lightweight option is to review crash reports and user feedback after each rollout. A richer option is to add a privacy-reviewed crash-reporting service in a future release, only after obtaining the owner’s preferred service and privacy policy wording.

| Option | Tradeoffs | Cost | Setup complexity |
| --- | --- | --- | --- |
| Manual release watch | Review store crash/vitals data and user feedback after staged rollouts. No additional app SDK or data collection. | Typically included with the distribution channel. | Low. |
| Privacy-reviewed crash reporting | Faster alerting and stack traces, but introduces a third-party SDK, policy disclosure, and retention decisions. | Varies by provider and volume. | Medium. |

## References

[1] [Android Developers — Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

[2] [Android Developers — Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
