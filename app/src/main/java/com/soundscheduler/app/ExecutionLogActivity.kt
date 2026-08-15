package com.soundscheduler.app

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.soundscheduler.app.data.AppDatabase
import com.soundscheduler.app.data.ExecutionHistoryRepository
import com.soundscheduler.app.data.RoutineExecution
import com.soundscheduler.app.data.RoutineExecutionDetail
import com.soundscheduler.app.ui.ExecutionAdapter

class ExecutionLogActivity : AppCompatActivity() {
    private lateinit var executionAdapter: ExecutionAdapter
    private lateinit var executionListView: ListView
    private lateinit var emptyStateTextView: TextView
    private var showingAttentionOnly = false
    private var allExecutions: List<RoutineExecutionDetail> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_execution_log)

        executionListView = findViewById(R.id.executionListView)
        emptyStateTextView = findViewById(R.id.executionEmptyStateTextView)
        executionAdapter = ExecutionAdapter(this)
        executionListView.adapter = executionAdapter

        findViewById<MaterialButton>(R.id.showAllExecutionsButton).setOnClickListener {
            showingAttentionOnly = false
            render()
        }
        findViewById<MaterialButton>(R.id.showAttentionExecutionsButton).setOnClickListener {
            showingAttentionOnly = true
            render()
        }
        findViewById<MaterialButton>(R.id.clearExecutionHistoryButton).setOnClickListener {
            confirmClearHistory()
        }

        AppDatabase.getDatabase(this).routineExecutionDao().getAllExecutionDetails()
            .observe(this, Observer { executions ->
                allExecutions = executions.orEmpty()
                render()
            })
    }

    private fun render() {
        val visibleExecutions = if (showingAttentionOnly) {
            allExecutions.filter { it.execution.outcomeCode != RoutineExecution.OUTCOME_APPLIED }
        } else {
            allExecutions
        }
        executionAdapter.submitList(visibleExecutions)
        val hasItems = visibleExecutions.isNotEmpty()
        executionListView.visibility = if (hasItems) View.VISIBLE else View.GONE
        emptyStateTextView.visibility = if (hasItems) View.GONE else View.VISIBLE
        emptyStateTextView.setText(
            if (showingAttentionOnly) R.string.activity_log_empty_attention else R.string.activity_log_empty
        )
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_activity_history_title)
            .setMessage(R.string.clear_activity_history_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_activity_history) { _, _ ->
                ExecutionHistoryRepository.clearAll(this)
            }
            .show()
    }
}
