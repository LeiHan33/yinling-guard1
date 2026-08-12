package com.yinling.guard.ui.common

import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.yinling.guard.R

object PasswordDialog {
    fun show(
        fragment: Fragment,
        title: String,
        description: String? = null,
        onConfirm: (String, (String) -> Unit) -> Boolean,
        onCancel: (() -> Unit)? = null
    ) {
        val context = fragment.requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_password, null, false)
        val titleView = view.findViewById<TextView>(R.id.tv_title)
        val descView = view.findViewById<TextView>(R.id.tv_desc)
        val input = view.findViewById<TextInputEditText>(R.id.et_password)
        val errorView = view.findViewById<TextView>(R.id.tv_error)

        titleView.text = title
        if (description.isNullOrBlank()) {
            descView.visibility = android.view.View.GONE
        } else {
            descView.visibility = android.view.View.VISIBLE
            descView.text = description
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        view.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }
        view.findViewById<MaterialButton>(R.id.btn_confirm).setOnClickListener {
            val password = input.text?.toString().orEmpty()
            errorView.visibility = android.view.View.GONE
            val showError: (String) -> Unit = { message ->
                errorView.visibility = android.view.View.VISIBLE
                errorView.text = message
            }
            if (onConfirm(password, showError)) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
