package com.example.gettahoofin

import android.util.Log

/**
 * Singleton class to manage state between screens
 * This is a direct approach that doesn't rely on navigation flags
 */
object StateManager {
    // State for workout
    private var savedIntervalIndex: Int = -1
    private var savedRemainingTime: Int = 0
    private var wasRunning: Boolean = false
    private var hasStoredState: Boolean = false
    private var programName: String? = null

    /**
     * Save the state before navigation
     */
    fun saveWorkoutState(intervalIndex: Int, remainingTime: Int, isRunning: Boolean, program: String) {
        Log.d("StateManager", "Saving state: interval=$intervalIndex, time=$remainingTime, running=$isRunning, program=$program")
        savedIntervalIndex = intervalIndex
        savedRemainingTime = remainingTime
        wasRunning = isRunning
        programName = program
        hasStoredState = true
    }

    /**
     * Check if there's state to restore
     */
    fun hasWorkoutState(): Boolean {
        return hasStoredState && savedIntervalIndex >= 0
    }

    /**
     * Get the saved interval index
     */
    fun getSavedIntervalIndex(): Int {
        return savedIntervalIndex
    }

    /**
     * Get the saved remaining time
     */
    fun getSavedRemainingTime(): Int {
        return savedRemainingTime
    }

    /**
     * Check if the workout was running
     */
    fun wasWorkoutRunning(): Boolean {
        return wasRunning
    }

    /**
     * Get the saved program name
     */
    fun getSavedProgramName(): String? {
        return programName
    }

    /**
     * Clear all stored state
     */
    fun clearWorkoutState() {
        Log.d("StateManager", "Clearing saved state")
        savedIntervalIndex = -1
        savedRemainingTime = 0
        wasRunning = false
        programName = null
        hasStoredState = false
    }
}