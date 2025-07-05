package com.soundscheduler.app.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.soundscheduler.app.utils.CalendarRoutineManager

class SchedulerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val calendarCheckRunnable = object : Runnable {
        override fun run() {
            CalendarRoutineManager.checkAndTriggerCalendarRoutines(this@SchedulerService)
            handler.postDelayed(this, 60 * 60 * 1000) // every hour
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(calendarCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(calendarCheckRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}