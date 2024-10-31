package com.tommasoberlose.anotherwidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import com.tommasoberlose.anotherwidget.global.Preferences

class AWApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Preferences
        Kotpref.init(this)

        // Dark theme
        AppCompatDelegate.setDefaultNightMode(Preferences.darkThemePreference)
    }
}
