# Notification Policy Access — Debug APK Correction

## Root cause and correction

The original debug APK declared `MODIFY_AUDIO_SETTINGS` but omitted `ACCESS_NOTIFICATION_POLICY`. Android exposes an application in the **Do Not Disturb access** settings only when it declares the Notification Policy permission; without that declaration, the user cannot grant the special access needed for Silent and Vibrate routine changes.

The corrected debug APK now declares both permissions:

| Permission | Role |
| --- | --- |
| `android.permission.MODIFY_AUDIO_SETTINGS` | Allows the app to work with Android audio settings. |
| `android.permission.ACCESS_NOTIFICATION_POLICY` | Makes the app eligible for the user-managed Notification Policy / Do Not Disturb special access required for ringer-policy changes. |

The application also records when it opens the special-access screen, checks `isNotificationPolicyAccessGranted()` when the user returns, rebuilds future routines upon approval, and shows a clear success or still-disabled message.

## Test the corrected debug build

1. Install the newly rebuilt debug APK over the prior debug build. If Android will not accept the update, uninstall the prior debug build first and install the new APK.
2. Open **Sound Scheduler** and tap **Grant sound access**.
3. In Android’s **Do Not Disturb access** list, select **Sound Scheduler** and enable the switch.
4. Return to the app. It should report that sound-control access is granted and hide the access button.
5. Create a routine two to five minutes in the future and verify it changes the phone to the selected mode.

If Sound Scheduler still does not appear in the special-access list after installing the corrected APK, restart the device once and return to the app’s **Grant sound access** action. Device-management policies, work profiles, and some OEM enterprise restrictions can prevent users from granting Do Not Disturb access.

## References

[1] [Android Developers — Manifest.permission.ACCESS_NOTIFICATION_POLICY](https://developer.android.com/reference/android/Manifest.permission#ACCESS_NOTIFICATION_POLICY)

[2] [Android Developers — AudioManager API reference](https://developer.android.com/reference/android/media/AudioManager)

[3] [Android Developers — NotificationManager API reference](https://developer.android.com/reference/android/app/NotificationManager)
