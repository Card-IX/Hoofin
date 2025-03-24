// AppSettings.kt
package com.example.gettahoofin

import android.content.Context
import androidx.core.content.edit

class AppSettings(context: Context) {
    companion object {
        private const val SETTINGS_NAME = "app_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SOUND_VOLUME = "sound_volume"
    }

    private val sharedPreferences = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

    var isSoundEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true) // Default to true
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_SOUND_ENABLED, value)
            }
        }

    var isKeepScreenOn: Boolean
        get() = sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, true) // Default to true
        set(value) {
            sharedPreferences.edit {
                putBoolean(KEY_KEEP_SCREEN_ON, value)
            }
        }

    var soundVolume: Float
        get() = sharedPreferences.getFloat(KEY_SOUND_VOLUME, 0.7f) // Default to 70%
        set(value) {
            sharedPreferences.edit {
                putFloat(KEY_SOUND_VOLUME, value)
            }
        }
}