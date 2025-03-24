package com.example.gettahoofin

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class to hold workout position information
 */
data class WorkoutPosition(
    val programName: String?,
    val weekIndex: Int,
    val sessionIndex: Int,
    val completed: Boolean
)

/**
 * Enhanced app preferences storage with throttling for write operations
 */
class AppPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_LAST_PROGRAM = "last_program"
        private const val KEY_LAST_WEEK_INDEX = "last_week_index"
        private const val KEY_LAST_SESSION_INDEX = "last_session_index"
        private const val KEY_SESSION_COMPLETED = "session_completed"

        // Throttle writes to happen at most every 2 seconds
        private const val WRITE_THROTTLE_MS = 2000L
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Throttling mechanism for saving workout position
    private var saveJob: Job? = null
    private val saveScope = CoroutineScope(Dispatchers.IO)
    private val pendingSave = AtomicBoolean(false)

    // Current values for throttled saving
    @Volatile private var currentProgram: String? = null
    @Volatile private var currentWeekIndex: Int = 0
    @Volatile private var currentSessionIndex: Int = 0
    @Volatile private var currentCompleted: Boolean = false

    /**
     * Checks if the disclaimer has been accepted
     */
    fun isDisclaimerAccepted(): Boolean {
        return sharedPreferences.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }

    /**
     * Sets the disclaimer as accepted
     */
    fun setDisclaimerAccepted() {
        sharedPreferences.edit {
            putBoolean(KEY_DISCLAIMER_ACCEPTED, true)
        }
    }

    /**
     * Saves the current workout position with throttling
     * This method stores the values to be saved but throttles the actual disk write
     */
    fun saveWorkoutPosition(programName: String, weekIndex: Int, sessionIndex: Int, completed: Boolean = false) {
        AppLogger.d("AppPreferences", "Throttled save requested: program=$programName, week=$weekIndex, " +
                "session=$sessionIndex, completed=$completed")

        // Store the latest values
        currentProgram = programName
        currentWeekIndex = weekIndex
        currentSessionIndex = sessionIndex
        currentCompleted = completed

        // Set pending flag
        pendingSave.set(true)

        // If no save job is active, start one
        if (saveJob == null || saveJob?.isActive == false) {
            saveJob = saveScope.launch {
                while (pendingSave.getAndSet(false)) {
                    // Do the actual save
                    performSave()

                    // Wait before checking for more pending saves
                    delay(WRITE_THROTTLE_MS)
                }
            }
        }
    }

    /**
     * Actually perform the save operation
     */
    private fun performSave() {
        val program = currentProgram
        val weekIndex = currentWeekIndex
        val sessionIndex = currentSessionIndex
        val completed = currentCompleted

        AppLogger.d("AppPreferences", "Performing actual save: program=$program, week=$weekIndex, " +
                "session=$sessionIndex, completed=$completed")

        sharedPreferences.edit {
            if (program != null) {
                putString(KEY_LAST_PROGRAM, program)
                putInt(KEY_LAST_WEEK_INDEX, weekIndex)
                putInt(KEY_LAST_SESSION_INDEX, sessionIndex)
                putBoolean(KEY_SESSION_COMPLETED, completed)
            } else {
                // Clear values if program is null
                remove(KEY_LAST_PROGRAM)
                remove(KEY_LAST_WEEK_INDEX)
                remove(KEY_LAST_SESSION_INDEX)
                remove(KEY_SESSION_COMPLETED)
            }
        }
    }

    /**
     * Gets the last saved workout position
     * @return WorkoutPosition containing program name, week index, session index, and completed flag
     */
    fun getWorkoutPosition(): WorkoutPosition {
        val program = sharedPreferences.getString(KEY_LAST_PROGRAM, null)
        val weekIndex = sharedPreferences.getInt(KEY_LAST_WEEK_INDEX, 0)
        val sessionIndex = sharedPreferences.getInt(KEY_LAST_SESSION_INDEX, 0)
        val completed = sharedPreferences.getBoolean(KEY_SESSION_COMPLETED, false)

        return WorkoutPosition(program, weekIndex, sessionIndex, completed)
    }

    /**
     * Clears the saved workout position
     */
    fun clearWorkoutPosition() {
        // Use the throttled save mechanism with null program
        currentProgram = null
        pendingSave.set(true)

        if (saveJob == null || saveJob?.isActive == false) {
            saveJob = saveScope.launch {
                while (pendingSave.getAndSet(false)) {
                    performSave()
                    delay(WRITE_THROTTLE_MS)
                }
            }
        }
    }

    /**
     * Force an immediate save regardless of throttling
     * Use this when app is being closed or backgrounded
     */
    fun forceSave() {
        if (pendingSave.getAndSet(false)) {
            performSave()
        }
    }
}