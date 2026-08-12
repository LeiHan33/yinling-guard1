package com.yinling.guard.ui.family

import android.view.LayoutInflater
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.yinling.guard.R
import com.yinling.guard.core.model.KeywordCategory
import com.yinling.guard.data.ServiceLocator
import com.yinling.guard.service.GuardAccessibilityService

object AddKeywordDialog {
    fun show(fragment: Fragment, onAdded: () -> Unit) {
        val context = fragment.requireContext()
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_keyword, null, false)
        val input = view.findViewById<TextInputEditText>(R.id.et_keyword)
        val categoryGroup = view.findViewById<RadioGroup>(R.id.rg_category)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .create()

        view.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btn_confirm).setOnClickListener {
            val word = input.text?.toString().orEmpty()
            val category = when (categoryGroup.checkedRadioButtonId) {
                R.id.rb_health_scam -> KeywordCategory.HEALTH_SCAM.value
                R.id.rb_incitement -> KeywordCategory.INCITEMENT.value
                R.id.rb_clickbait -> KeywordCategory.CLICKBAIT.value
                else -> KeywordCategory.RUMOR.value
            }
            ServiceLocator.familyManager.addKeyword(word, category)
                .onSuccess {
                    GuardAccessibilityService.reloadRules()
                    dialog.dismiss()
                    onAdded()
                }
                .onFailure {
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }
}
