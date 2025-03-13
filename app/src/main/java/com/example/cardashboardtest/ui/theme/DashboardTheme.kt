package com.example.cardashboardtest.ui.theme

enum class DashboardTheme {
    MODERN,  // Put first to make it the default
    CORVETTE_1985;

    companion object {
        fun fromString(value: String): DashboardTheme {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                MODERN
            }
        }
    }
} 