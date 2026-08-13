package com.soundscheduler.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.utils.RoutineRescheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                RoutineRescheduler.rescheduleActiveRoutines(context) {
                    pendingResult.finish()
                }
            }
        }
    }
}
