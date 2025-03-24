package com.example.gettahoofin

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private val SUPPORTED_LOCALES = listOf(
        "en", // English (default)
        "zh", // Simplified Chinese
        "es", // Spanish
        "hi", // Hindi
        "ar", // Arabic
        "pt", // Portuguese
        "ru", // Russian
        "ja", // Japanese
        "de", // German
        "fr", // French
        "ko", // Korean
        "it", // Italian
        "sw"  // Swahili
    )

    // Get the current device locale
    fun getDeviceLocale(): Locale {
        // Since your app targets newer Android versions, we can use this directly
        return Locale.getDefault()
    }

    // Check if the device locale is supported, if not return the default locale (English)
    fun getSupportedLocale(): Locale {
        val deviceLocale = getDeviceLocale()
        val language = deviceLocale.language

        return if (SUPPORTED_LOCALES.contains(language)) {
            Locale(language)
        } else {
            Locale("en") // Default to English
        }
    }

    // Update the locale for the app
    fun updateLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    // Show fallback message if language is not supported
    fun isUsingFallbackLanguage(): Boolean {
        val deviceLocale = getDeviceLocale()
        return !SUPPORTED_LOCALES.contains(deviceLocale.language) && deviceLocale.language != "en"
    }
}