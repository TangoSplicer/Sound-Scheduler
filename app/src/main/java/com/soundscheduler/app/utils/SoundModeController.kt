package com.soundscheduler.app.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.soundscheduler.app.data.Routine

object SoundModeController {
    enum class ApplyResult {
        APPLIED,
        POLICY_ACCESS_REQUIRED,
        REJECTED_BY_SYSTEM
    }

    fun hasNotificationPolicyAccess(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun currentMode(context: Context): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return modeForRingerValue(audioManager.ringerMode)
    }

    fun applyRoutineMode(context: Context, routine: Routine): ApplyResult {
        return applyMode(context, routine.targetSoundMode())
    }

    fun applyMode(context: Context, mode: String): ApplyResult {
        if (!hasNotificationPolicyAccess(context)) return ApplyResult.POLICY_ACCESS_REQUIRED

        val targetRingerMode = ringerValueFor(mode) ?: return ApplyResult.REJECTED_BY_SYSTEM
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            audioManager.ringerMode = targetRingerMode
            if (audioManager.ringerMode == targetRingerMode) {
                ApplyResult.APPLIED
            } else {
                ApplyResult.REJECTED_BY_SYSTEM
            }
        } catch (_: SecurityException) {
            ApplyResult.POLICY_ACCESS_REQUIRED
        }
    }

    fun modeForRingerValue(ringerMode: Int): String = when (ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> Routine.PROFILE_SILENT
        AudioManager.RINGER_MODE_VIBRATE -> Routine.PROFILE_VIBRATE
        else -> Routine.PROFILE_RING
    }

    fun ringerValueFor(mode: String): Int? = when (mode) {
        Routine.PROFILE_RING, Routine.PROFILE_NORMAL, Routine.PROFILE_CUSTOM -> AudioManager.RINGER_MODE_NORMAL
        Routine.PROFILE_VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
        Routine.PROFILE_SILENT -> AudioManager.RINGER_MODE_SILENT
        else -> null
    }
}
