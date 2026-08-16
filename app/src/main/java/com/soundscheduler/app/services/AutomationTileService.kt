package com.soundscheduler.app.services

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.soundscheduler.app.MainActivity
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.AutomationControlRepository
import com.soundscheduler.app.data.AutomationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AutomationTileService : TileService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val stateDao = AppDatabase.getDatabase(this).automationStateDao()
        serviceScope.launch {
            val state = stateDao.getState()
            if (state?.isPaused == true) {
                // Resume
                AutomationControlRepository.resumeAll(this@AutomationTileService) {
                    updateTile()
                }
            } else {
                // Pause (indefinitely from tile for simplicity)
                AutomationControlRepository.pauseAll(this@AutomationTileService) {
                    updateTile()
                }
            }
        }
    }

    private fun updateTile() {
        val stateDao = AppDatabase.getDatabase(this).automationStateDao()
        serviceScope.launch {
            val state = stateDao.getState()
            val tile = qsTile ?: return@launch
            
            if (state?.isPaused == true) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Automation Paused"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to resume"
                }
            } else {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Automation Active"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to pause"
                }
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
