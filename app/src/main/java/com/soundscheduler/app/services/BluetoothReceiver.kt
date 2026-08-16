package com.soundscheduler.app.services

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soundscheduler.app.R
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.ExecutionHistoryRepository
import com.soundscheduler.app.data.Routine
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.utils.NotificationUtils
import java.util.concurrent.Executors

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val address = device?.address ?: return

        val transition = when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> Routine.CHARGING_TRANSITION_CONNECTED // reusing connected constant or define new
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> Routine.CHARGING_TRANSITION_DISCONNECTED
            else -> return
        }

        val pendingResult = goAsync()
        receiverExecutor.execute {
            try {
                val routines = AppDatabase.getDatabase(context).routineDao()
                    .getActiveRoutinesByType(Routine.TYPE_BLUETOOTH)
                    .filter { it.bluetoothDeviceAddress.equals(address, ignoreCase = true) }

                var needsRearmNotification = false
                routines.forEach { routine ->
                    val triggerType = if (transition == Routine.CHARGING_TRANSITION_CONNECTED) {
                        RoutineExecution.TRIGGER_BLUETOOTH_CONNECTED
                    } else {
                        RoutineExecution.TRIGGER_BLUETOOTH_DISCONNECTED
                    }

                    if (!SoundModeExecutionService.startForBluetoothRoutine(context, routine.id, transition)) {
                        ExecutionHistoryRepository.recordAutomationRearmRequired(
                            context = context,
                            routineId = routine.id,
                            triggerType = triggerType
                        )
                        needsRearmNotification = true
                    }
                }
                if (needsRearmNotification) {
                    NotificationUtils.sendNotification(
                        context,
                        context.getString(R.string.automation_rearm_notification_title),
                        context.getString(R.string.automation_rearm_notification_message)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverExecutor = Executors.newSingleThreadExecutor()
    }
}
