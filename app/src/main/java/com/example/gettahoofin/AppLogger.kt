package com.example.gettahoofin

import android.util.Log

/**
 * Logging utility with configurable logging level
 */
object AppLogger {
    // Set this to false in release builds
    private var isDebugMode = true

    /**
     * Configure debug mode
     * Call this during app initialization to set the logging mode
     */
    fun setDebugMode(debugMode: Boolean) {
        isDebugMode = debugMode
    }

    /**
     * Log a debug message
     */
    fun d(tag: String, message: String) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }

    /**
     * Log an info message
     */
    fun i(tag: String, message: String) {
        if (isDebugMode) {
            Log.i(tag, message)
        }
    }

    /**
     * Log a warning message
     */
    fun w(tag: String, message: String) {
        if (isDebugMode) {
            Log.w(tag, message)
        }
    }

    /**
     * Log an error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Always log errors, even in non-debug mode
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}