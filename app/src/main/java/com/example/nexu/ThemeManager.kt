package com.example.nexu

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView

object ThemeManager {

    private const val PREF = "theme_pref"
    private const val KEY = "is_dark"

    fun isDark(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY, false)
    }

    fun setDark(context: Context, dark: Boolean) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY, dark).apply()
    }

    /**
     * Aplica el fondo gradiente a cualquier layout raíz
     */
    fun applyThemeBackground(context: Context, root: View) {
        val isDark = isDark(context)

        val startColor = if (isDark) {
            Color.parseColor("#063749")
        } else {
            Color.parseColor("#E6EAF6")
        }

        val endColor = if (isDark) {
            Color.parseColor("#042430")
        } else {
            Color.parseColor("#F8E7EE")
        }

        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColor, endColor)
        )

        root.background = gradient
    }

    /**
     * Cambia color del texto según el tema
     */
    fun applyTextColor(context: Context, textView: TextView) {
        val isDark = isDark(context)

        val color = if (isDark) {
            Color.parseColor("#FFFFFF")
        } else {
            Color.parseColor("#132E63")
        }

        textView.setTextColor(color)
    }
}
