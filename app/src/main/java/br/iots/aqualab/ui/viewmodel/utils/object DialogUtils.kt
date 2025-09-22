package com.seuprojeto.ui.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Sim",
        negativeButtonText: String = "Cancelar",
        onPositiveClicked: () -> Unit,
        onNegativeClicked: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                onNegativeClicked?.invoke()
                dialog.dismiss()
            }
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                onPositiveClicked.invoke()
                dialog.dismiss()
            }
            .show()
    }
}
        