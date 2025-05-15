package com.example.cardashboardtest.ui.theme

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import com.example.cardashboardtest.R
import com.example.cardashboardtest.utils.ThemePreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ThemeSelectionDialog : DialogFragment() {
    private var themeSelectedListener: ((DashboardTheme) -> Unit)? = null
    private lateinit var modernThemeRadio: RadioButton
    private lateinit var corvetteThemeRadio: RadioButton
    private lateinit var nissanThemeRadio: RadioButton
    private lateinit var radioGroup: RadioGroup
    private lateinit var themePreferences: ThemePreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        themePreferences = ThemePreferences(requireContext())
        val currentTheme = themePreferences.getTheme()

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_theme_selection, null)

        modernThemeRadio = view.findViewById(R.id.modernThemeRadio)
        corvetteThemeRadio = view.findViewById(R.id.corvetteThemeRadio)
        nissanThemeRadio = view.findViewById(R.id.nissanThemeRadio)
        radioGroup = view.findViewById(R.id.themeRadioGroup)

        when (currentTheme) {
            DashboardTheme.MODERN -> modernThemeRadio.isChecked = true
            DashboardTheme.CORVETTE_1985 -> corvetteThemeRadio.isChecked = true
            DashboardTheme.NISSAN_280ZX -> nissanThemeRadio.isChecked = true
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Dashboard Theme")
            .setView(view)
            .setPositiveButton("Apply") { _, _ ->
                val selectedTheme = when (radioGroup.checkedRadioButtonId) {
                    R.id.modernThemeRadio -> DashboardTheme.MODERN
                    R.id.corvetteThemeRadio -> DashboardTheme.CORVETTE_1985
                    R.id.nissanThemeRadio -> DashboardTheme.NISSAN_280ZX
                    else -> themePreferences.getTheme() // Fallback to current theme if somehow nothing is selected
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