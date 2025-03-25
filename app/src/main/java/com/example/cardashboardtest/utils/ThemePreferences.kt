package com.example.cardashboardtest.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.cardashboardtest.ui.theme.DashboardTheme

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(): DashboardTheme {
        val themeName = prefs.getString(KEY_THEME, DashboardTheme.MODERN.name)
        return DashboardTheme.fromString(themeName ?: DashboardTheme.MODERN.name)
    }

    fun setTheme(theme: DashboardTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "dashboard_preferences"
        private const val KEY_THEME = "selected_theme"
    }
} 