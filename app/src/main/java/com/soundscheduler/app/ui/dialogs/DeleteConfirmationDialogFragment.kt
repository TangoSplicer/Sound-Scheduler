package com.soundscheduler.app.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.soundscheduler.app.R

class DeleteConfirmationDialogFragment : DialogFragment() {

    interface DeleteConfirmationListener {
        fun onConfirmDelete()
    }

    var listener: DeleteConfirmationListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.delete_confirmation_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    listener?.onConfirmDelete()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
