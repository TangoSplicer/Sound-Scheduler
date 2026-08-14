package com.soundscheduler.app.utils

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
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

    /**
     * Applies a routine mode from a background execution service and confirms that Android keeps
     * the requested state for a short bounded window before the service is allowed to finish.
     * Call this only off the main thread while the service is in the foreground.
     */
    fun applyRoutineModeAndConfirm(context: Context, routine: Routine): ApplyResult {
        return applyModeAndConfirm(context, routine.targetSoundMode())
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

    private fun applyModeAndConfirm(context: Context, mode: String): ApplyResult {
        if (!hasNotificationPolicyAccess(context)) return ApplyResult.POLICY_ACCESS_REQUIRED

        val targetRingerMode = ringerValueFor(mode) ?: return ApplyResult.REJECTED_BY_SYSTEM
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            audioManager.ringerMode = targetRingerMode
            confirmStableMode(audioManager, targetRingerMode)
        } catch (_: SecurityException) {
            ApplyResult.POLICY_ACCESS_REQUIRED
        }
    }

    private fun confirmStableMode(audioManager: AudioManager, targetRingerMode: Int): ApplyResult {
        val deadline = SystemClock.elapsedRealtime() + CONFIRMATION_WINDOW_MILLIS
        var stableSince: Long? = null
        do {
            val now = SystemClock.elapsedRealtime()
            if (audioManager.ringerMode == targetRingerMode) {
                if (stableSince == null) stableSince = now
                if (now - stableSince >= REQUIRED_STABLE_MILLIS) return ApplyResult.APPLIED
            } else {
                stableSince = null
            }
            SystemClock.sleep(CONFIRMATION_POLL_MILLIS)
        } while (SystemClock.elapsedRealtime() < deadline)

        return if (
            audioManager.ringerMode == targetRingerMode &&
            stableSince != null &&
            SystemClock.elapsedRealtime() - stableSince >= REQUIRED_STABLE_MILLIS
        ) {
            ApplyResult.APPLIED
        } else {
            ApplyResult.REJECTED_BY_SYSTEM
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

    private const val CONFIRMATION_WINDOW_MILLIS = 2_000L
    private const val REQUIRED_STABLE_MILLIS = 1_200L
    private const val CONFIRMATION_POLL_MILLIS = 150L
}
