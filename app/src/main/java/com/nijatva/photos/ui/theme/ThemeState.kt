package com.nijatva.photos.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nijatva.photos.data.Prefs

/**
 * Appearance settings held in observable state so a change in the Appearance screen
 * re-themes the whole app immediately (and is persisted through [Prefs]).
 */
object ThemeState {
    /** "system" | "light" | "dark" | "black" */
    var mode by mutableStateOf("system")
        private set

    /** "green" | "saffron" | "indigo" */
    var accent by mutableStateOf("green")
        private set

    var dynamic by mutableStateOf(false)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        mode = Prefs.themeMode(context)
        accent = Prefs.accent(context)
        dynamic = Prefs.dynamicColor(context)
        loaded = true
    }

    fun setMode(context: Context, value: String) {
        mode = value
        Prefs.setThemeMode(context, value)
    }

    fun setAccent(context: Context, value: String) {
        accent = value
        Prefs.setAccent(context, value)
    }

    fun setDynamic(context: Context, value: Boolean) {
        dynamic = value
        Prefs.setDynamicColor(context, value)
    }

    val themeOptions = listOf("system" to "System", "dark" to "Dark", "light" to "Light", "black" to "Black")
    val accentOptions = listOf("green" to "Green", "saffron" to "Saffron", "indigo" to "Indigo")
}
