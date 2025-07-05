package com.soundscheduler.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import com.soundscheduler.app.utils.PremiumManager

class SoundProfileService : Service() {

    companion object {
        const val ACTION_SET_PROFILE = "com.soundscheduler.app.SET_PROFILE"
        const val EXTRA_PROFILE_TYPE = "profile_type"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SET_PROFILE) {
            val profileType = intent.getStringExtra(EXTRA_PROFILE_TYPE) ?: "normal"
            setSoundProfile(profileType)
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun setSoundProfile(profileType: String) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (profileType) {
            "silent" -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            "vibrate" -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            "normal" -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}