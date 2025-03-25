package com.example.cardashboardtest.ui.theme

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.example.cardashboardtest.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ThemeSelectionDialog : DialogFragment() {
    private var themeSelectedListener: ((DashboardTheme) -> Unit)? = null
    private lateinit var modernThemeRadio: RadioButton
    private lateinit var corvetteThemeRadio: RadioButton
    private lateinit var radioGroup: RadioGroup

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_theme_selection, null)

        // Find views
        modernThemeRadio = view.findViewById(R.id.modernThemeRadio)
        corvetteThemeRadio = view.findViewById(R.id.corvetteThemeRadio)
        radioGroup = view.findViewById(R.id.themeRadioGroup)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Dashboard Theme")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val selectedTheme = when {
                    modernThemeRadio.isChecked -> DashboardTheme.MODERN
                    corvetteThemeRadio.isChecked -> DashboardTheme.CORVETTE_1985
                    else -> DashboardTheme.MODERN
                }
                Log.d("ThemeSelectionDialog", "Selected theme: $selectedTheme")
                themeSelectedListener?.invoke(selectedTheme)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d("ThemeSelectionDialog", "Dialog cancelled")
                dialog.dismiss()
            }
            .create()
    }

    fun setThemeSelectedListener(listener: (DashboardTheme) -> Unit) {
        themeSelectedListener = listener
    }

    companion object {
        fun newInstance(): ThemeSelectionDialog {
            return ThemeSelectionDialog()
        }
    }
} 