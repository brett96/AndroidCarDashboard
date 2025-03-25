package com.example.cardashboardtest.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.cardashboardtest.R
import com.example.cardashboardtest.ui.theme.DashboardTheme
import com.example.cardashboardtest.utils.ThemePreferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ThemeSelectionDialog : DialogFragment() {
    private var themeSelectedListener: ((DashboardTheme) -> Unit)? = null
    private lateinit var themePreferences: ThemePreferences

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        themePreferences = ThemePreferences(requireContext())
        val currentTheme = themePreferences.getTheme()

        val view = layoutInflater.inflate(R.layout.dialog_theme_selection, null)
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Dashboard Theme")
            .setView(view)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    fun setThemeSelectedListener(listener: (DashboardTheme) -> Unit) {
        themeSelectedListener = listener
    }

    companion object {
        fun newInstance() = ThemeSelectionDialog()
    }
} 