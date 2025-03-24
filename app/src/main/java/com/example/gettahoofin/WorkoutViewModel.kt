package com.example.gettahoofin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val soundPlayer: SoundPlayer,
    private val appSettings: AppSettings,
    private val appPreferences: AppPreferences
) : ViewModel() {
    // Workout state - changed from private to internal for direct access
    internal val _currentIntervalIndex = MutableStateFlow(0)
    val currentIntervalIndex: StateFlow<Int> = _currentIntervalIndex.asStateFlow()

    internal val _remainingTime = MutableStateFlow(0)
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    internal val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    internal val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    internal val _sessionProgress = MutableStateFlow(0f)
    val sessionProgress: StateFlow<Float> = _sessionProgress.asStateFlow()

    // Session completed state flow
    internal val _sessionCompleted = MutableStateFlow(false)
    val sessionCompleted: StateFlow<Boolean> = _sessionCompleted.asStateFlow()

    // Timer job
    private var timerJob: Job? = null

    // Store paused time for resuming after settings
    private var pausedRemainingTime: Int? = null

    // Session data
    internal var currentSession: Session? = null
    private var totalSessionSeconds = 0
    private var _currentProgramName: String? = null
    private var _currentWeekIndex: Int = 0
    private var _currentSessionIndex: Int = 0

    // Flag to track user interaction
    private var hasInteracted = false

    // Public variables to store/restore state when navigating to settings
    var savedIntervalIndex: Int = 0
    var savedRemainingTime: Int = 0
    var wasRunningBeforeSettings: Boolean = false

    // Store state data for settings navigation
    private var storedIntervalIndex: Int = -1
    private var storedRemainingTime: Int = 0
    private var storedWasRunning: Boolean = false
    private var hasStoredState: Boolean = false

    // Public getters for program, week and session indices
    val currentProgramName: String?
        get() = _currentProgramName

    val currentWeekIndex: Int
        get() = _currentWeekIndex

    val currentSessionIndex: Int
        get() = _currentSessionIndex

    /**
     * Direct method to apply stored state values, bypassing other state handling
     */
    fun applyStoredState(intervalIndex: Int, remainingTime: Int, wasRunning: Boolean) {
        Log.d("WorkoutViewModel", "===== DIRECT STATE OVERRIDE =====")
        Log.d("WorkoutViewModel", "Applying values: interval=$intervalIndex, time=$remainingTime, wasRunning=$wasRunning")

        // Cancel any existing timer job
        timerJob?.cancel()

        // Directly force the MutableStateFlow values
        _currentIntervalIndex.value = intervalIndex
        _remainingTime.value = remainingTime

        // Set to paused initially
        _isRunning.value = false
        _isPaused.value = true

        // Mark as interacted
        hasInteracted = true

        // Update progress
        updateSessionProgress()

        Log.d("WorkoutViewModel", "State values forced: interval=${_currentIntervalIndex.value}, time=${_remainingTime.value}")

        // Use a delayed coroutine to restart if needed
        if (wasRunning) {
            viewModelScope.launch {
                delay(1500) // Longer delay for reliability
                Log.d("WorkoutViewModel", "Auto-restarting timer after direct override...")
                _isRunning.value = true
                _isPaused.value = false
                startTimer()
            }
        }
    }

    /**
     * Store the current state before navigating to settings
     */
    fun storeStateForSettings(intervalIndex: Int, remainingTime: Int, wasRunning: Boolean) {
        Log.d("WorkoutViewModel", "Storing state for settings: interval=$intervalIndex, time=$remainingTime, running=$wasRunning")
        storedIntervalIndex = intervalIndex
        storedRemainingTime = remainingTime
        storedWasRunning = wasRunning
        hasStoredState = true

        // Also store in the public properties for compatibility
        savedIntervalIndex = intervalIndex
        savedRemainingTime = remainingTime
        wasRunningBeforeSettings = wasRunning
    }

    /**
     * Check if there is stored state to restore
     */
    fun hasStateToRestore(): Boolean {
        return hasStoredState && storedIntervalIndex >= 0
    }

    /**
     * Initialize or restore the workout based on whether we have stored state
     */
    fun initializeOrRestoreWorkout(
        session: Session,
        programName: String = "",
        weekIndex: Int = 0,
        sessionIndex: Int = 0,
        startIntervalIndex: Int = 0,
        isReturningFromSettings: Boolean = false
    ) {
        // If returning from settings and we have stored state, restore it
        if (isReturningFromSettings && hasStateToRestore()) {
            Log.d("WorkoutViewModel", "==== RESTORING from stored state ====")
            Log.d("WorkoutViewModel", "Stored state: interval=$storedIntervalIndex, time=$storedRemainingTime, wasRunning=$storedWasRunning")

            // Setup the session and program info first
            currentSession = session
            _currentProgramName = programName.trim()
            _currentWeekIndex = weekIndex
            _currentSessionIndex = sessionIndex

            // Cancel any existing timer
            timerJob?.cancel()

            // Calculate total duration (needed for progress bar)
            totalSessionSeconds = session.intervals.sumOf { (it.duration * 60).toInt() }

            // Restore the exact state
            _currentIntervalIndex.value = storedIntervalIndex
            _remainingTime.value = storedRemainingTime
            pausedRemainingTime = null

            // Set to paused initially
            _isRunning.value = false
            _isPaused.value = true

            // Update progress immediately
            updateSessionProgress()

            // Auto-restart if it was running before
            if (storedWasRunning) {
                viewModelScope.launch {
                    delay(800)
                    Log.d("WorkoutViewModel", "Auto-restarting timer...")
                    toggleStartPause()
                }
            }

            // Mark as interacted so state saving happens
            hasInteracted = true

            // Clear the stored state flag (but keep the values for debugging)
            hasStoredState = false

            Log.d("WorkoutViewModel", "State restored successfully")
        } else {
            // Normal initialization
            Log.d("WorkoutViewModel", "Regular initialization with program: '$programName'")

            // Call the original init method
            initializeWorkout(session, programName, weekIndex, sessionIndex, startIntervalIndex)

            // Reset stored state
            clearStoredState()
        }
    }

    /**
     * Clear any stored state
     */
    private fun clearStoredState() {
        storedIntervalIndex = -1
        storedRemainingTime = 0
        storedWasRunning = false
        hasStoredState = false
    }

    /**
     * Force update the state directly
     */
    fun forceUpdateState(intervalIndex: Int, remainingSeconds: Int, autoRestart: Boolean) {
        Log.d("WorkoutViewModel", "===== FORCE UPDATE STATE CALLED =====")
        Log.d("WorkoutViewModel", "Setting values: interval=$intervalIndex, time=$remainingSeconds, autoRestart=$autoRestart")

        try {
            // Cancel any existing timer to ensure clean state
            timerJob?.cancel()

            // Update state values directly
            _currentIntervalIndex.value = intervalIndex
            _remainingTime.value = remainingSeconds
            pausedRemainingTime = null

            // Ensure we're paused
            _isRunning.value = false
            _isPaused.value = true

            // Mark as interacted for state saving
            hasInteracted = true

            // Update session progress bar
            updateSessionProgress()

            Log.d("WorkoutViewModel", "State values set: interval=${_currentIntervalIndex.value}, time=${_remainingTime.value}")

            // Auto-restart with a delay if needed
            if (autoRestart) {
                viewModelScope.launch {
                    delay(1000) // Increased delay for better reliability
                    Log.d("WorkoutViewModel", "Force auto-restarting timer...")
                    toggleStartPause()
                }
            }

            Log.d("WorkoutViewModel", "Force update completed successfully")
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error during force update", e)
        }
    }

    /**
     * Directly restore the timer state using saved values
     */
    fun restoreDirectTimerState() {
        // Add extensive logging to diagnose issues
        Log.d("WorkoutViewModel", "========== RESTORE STATE CALLED ==========")
        Log.d("WorkoutViewModel", "Saved values: interval=$savedIntervalIndex, time=$savedRemainingTime, wasRunning=$wasRunningBeforeSettings")
        Log.d("WorkoutViewModel", "Current session is null? ${currentSession == null}")

        if (currentSession == null) {
            Log.e("WorkoutViewModel", "Cannot restore state - no session available")
            return
        }

        // Validate index is in range
        if (savedIntervalIndex < 0 || savedIntervalIndex >= currentSession!!.intervals.size) {
            Log.e("WorkoutViewModel", "Invalid interval index for restoration: $savedIntervalIndex, max allowed: ${currentSession!!.intervals.size - 1}")
            return
        }

        try {
            // Stop any existing timer
            timerJob?.cancel()

            // Force update the state directly using the MutableStateFlow values
            Log.d("WorkoutViewModel", "Setting interval index to $savedIntervalIndex")
            _currentIntervalIndex.value = savedIntervalIndex

            Log.d("WorkoutViewModel", "Setting remaining time to $savedRemainingTime")
            _remainingTime.value = savedRemainingTime

            // Reset the paused time as we've directly set the remaining time
            pausedRemainingTime = null

            // Update session progress
            updateSessionProgress()

            // Ensure we're in a paused state initially
            _isRunning.value = false
            _isPaused.value = true

            // Mark that the user has interacted with the workout
            hasInteracted = true

            Log.d("WorkoutViewModel", "State successfully restored, now checking if we should restart timer")

            // If the workout was running before going to settings, restart it after a short delay
            if (wasRunningBeforeSettings) {
                Log.d("WorkoutViewModel", "Workout was running before settings, scheduling auto-restart")

                viewModelScope.launch {
                    delay(800) // Longer delay to ensure UI is fully rendered
                    Log.d("WorkoutViewModel", "Auto-restarting timer now...")

                    // Make absolutely sure we're in paused state before toggling
                    _isRunning.value = false
                    _isPaused.value = true

                    // Now toggle to start
                    toggleStartPause()

                    Log.d("WorkoutViewModel", "Timer restarted: running=${_isRunning.value}, paused=${_isPaused.value}")
                }
            } else {
                Log.d("WorkoutViewModel", "Workout was not running before settings, keeping paused")
            }

            Log.d("WorkoutViewModel", "========== RESTORE STATE COMPLETED ==========")
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error during state restoration", e)
        }
    }

    /**
     * Initialize the workout with a session
     */
    fun initializeWorkout(
        session: Session,
        programName: String = "",
        weekIndex: Int = 0,
        sessionIndex: Int = 0,
        startIntervalIndex: Int = 0
    ) {
        // Log the program name to debug
        Log.d("WorkoutViewModel", "Initializing workout with program: '$programName'")

        // Cancel any existing timer job
        timerJob?.cancel()

        // Set workout state
        currentSession = session
        _currentProgramName = programName.trim()  // Add trim() to handle empty spaces
        _currentWeekIndex = weekIndex
        _currentSessionIndex = sessionIndex
        _currentIntervalIndex.value = startIntervalIndex
        _isRunning.value = false
        _isPaused.value = false
        _sessionProgress.value = 0f
        _sessionCompleted.value = false
        pausedRemainingTime = null  // Reset paused time

        // Reset interaction flag
        hasInteracted = false

        // Reset settings state tracking variables - important!
        wasRunningBeforeSettings = false

        // Also reset stored state
        clearStoredState()

        // Calculate total duration of all intervals
        totalSessionSeconds = session.intervals.sumOf { (it.duration * 60).toInt() }

        // Set initial remaining time
        if (session.intervals.isNotEmpty() && startIntervalIndex < session.intervals.size) {
            val interval = session.intervals[startIntervalIndex]
            _remainingTime.value = kotlin.math.ceil(interval.duration * 60).toInt()
        } else if (session.intervals.isNotEmpty()) {
            // Fallback to first interval if index is out of bounds
            val firstInterval = session.intervals[0]
            _remainingTime.value = kotlin.math.ceil(firstInterval.duration * 60).toInt()
        }

        // Update initial progress
        updateSessionProgress()
    }

    /**
     * Start or pause the workout
     */
    fun toggleStartPause() {
        hasInteracted = true  // Mark as interacted when user starts/pauses
        if (_isRunning.value) {
            // Pause
            _isPaused.value = true
            _isRunning.value = false

            // Store the current remaining time
            pausedRemainingTime = _remainingTime.value

            timerJob?.cancel()

            // Save state when pausing
            saveCurrentState()
        } else {
            // Start
            // Restore paused time if available
            if (pausedRemainingTime != null) {
                _remainingTime.value = pausedRemainingTime!!
                pausedRemainingTime = null
            }

            _isPaused.value = false
            _isRunning.value = true
            startTimer()
        }
    }

    /**
     * Restart the workout
     */
    fun restart() {
        hasInteracted = true  // Mark as interacted when user restarts
        if (_isRunning.value || _isPaused.value) {
            timerJob?.cancel()
            _isRunning.value = false
            _isPaused.value = false
            _currentIntervalIndex.value = 0
            pausedRemainingTime = null  // Reset paused time

            // Reset timer to first interval
            val firstIntervalDuration = currentSession?.intervals?.get(0)?.duration ?: 0.0
            // Use ceiling here too for consistency
            _remainingTime.value = kotlin.math.ceil(firstIntervalDuration * 60).toInt()

            updateSessionProgress()
            saveCurrentState()
        }
    }

    /**
     * Skip to the next interval
     */
    fun skipToNextInterval() {
        hasInteracted = true  // Mark as interacted when user skips
        if (currentSession != null) {
            if (_currentIntervalIndex.value < currentSession!!.intervals.size - 1) {
                // Not the last interval yet, move to next
                _currentIntervalIndex.value += 1
                _remainingTime.value = kotlin.math.ceil(currentSession!!.intervals[_currentIntervalIndex.value].duration * 60).toInt()
                pausedRemainingTime = null  // Reset paused time

                // Pause the workout
                _isRunning.value = false
                _isPaused.value = true
                timerJob?.cancel()

                updateSessionProgress()

                // Save state
                saveCurrentState()

                // Play sound when interval changes if enabled
                if (appSettings.isSoundEnabled) {
                    soundPlayer.playIntervalChangeSound()
                }
            } else {
                // This is the last interval, mark workout as complete
                Log.d("WorkoutViewModel", "Last interval reached, marking session as completed")
                _isRunning.value = false
                _isPaused.value = false
                _sessionProgress.value = 1.0f  // Set progress to 100%
                _remainingTime.value = 0
                pausedRemainingTime = null  // Reset paused time

                // Mark session as completed
                markSessionCompleted()
            }
        }
    }

    /**
     * Skip to the next session in the current week
     * @param programData The program data containing all weeks and sessions
     * @return True if successfully skipped to next session, false if at end of week
     */
    fun skipToNextSession(programData: Program): Boolean {
        hasInteracted = true

        // Check if we can move to the next session in the current week
        if (programData.weeks.size > _currentWeekIndex) {
            val currentWeek = programData.weeks[_currentWeekIndex]
            if (currentWeek.sessions.size > _currentSessionIndex + 1) {
                // Move to next session in current week
                _currentSessionIndex++
                _currentIntervalIndex.value = 0 // Reset to first interval
                pausedRemainingTime = null  // Reset paused time

                // Initialize with the new session
                if (currentWeek.sessions.size > _currentSessionIndex) {
                    val newSession = currentWeek.sessions[_currentSessionIndex]

                    // Reset session state
                    _isRunning.value = false
                    _isPaused.value = false
                    _sessionProgress.value = 0f
                    _sessionCompleted.value = false

                    // Set up the new session
                    currentSession = newSession
                    totalSessionSeconds = newSession.intervals.sumOf { (it.duration * 60).toInt() }

                    // Set timer to first interval
                    if (newSession.intervals.isNotEmpty()) {
                        _remainingTime.value = kotlin.math.ceil(newSession.intervals[0].duration * 60).toInt()
                    }

                    // Save the new position
                    saveCurrentState()

                    return true
                }
            }
        }

        return false
    }

    /**
     * Skip to the first session of the next week
     * @param programData The program data containing all weeks and sessions
     * @return True if successfully skipped to next week, false if at end of program
     */
    fun skipToNextWeek(programData: Program): Boolean {
        hasInteracted = true

        // Check if we can move to the next week
        if (programData.weeks.size > _currentWeekIndex + 1) {
            // Move to first session of next week
            _currentWeekIndex++
            _currentSessionIndex = 0 // Reset to first session
            _currentIntervalIndex.value = 0 // Reset to first interval
            pausedRemainingTime = null  // Reset paused time

            // Initialize with the new session
            if (programData.weeks.size > _currentWeekIndex &&
                programData.weeks[_currentWeekIndex].sessions.isNotEmpty()) {
                val newSession = programData.weeks[_currentWeekIndex].sessions[0]

                // Reset session state
                _isRunning.value = false
                _isPaused.value = false
                _sessionProgress.value = 0f
                _sessionCompleted.value = false

                // Set up the new session
                currentSession = newSession
                totalSessionSeconds = newSession.intervals.sumOf { (it.duration * 60).toInt() }

                // Set timer to first interval
                if (newSession.intervals.isNotEmpty()) {
                    _remainingTime.value = kotlin.math.ceil(newSession.intervals[0].duration * 60).toInt()
                }

                // Save the new position
                saveCurrentState()

                return true
            }
        }

        return false
    }

    /**
     * Mark the current session as completed
     */
    fun markSessionCompleted() {
        Log.d("WorkoutViewModel", "markSessionCompleted called")
        _sessionCompleted.value = true

        // Force saving regardless of hasInteracted flag
        if (_currentProgramName != null && !_currentProgramName!!.isEmpty()) {
            Log.d("WorkoutViewModel", "Saving workout position with program: $_currentProgramName, completed=true")
            appPreferences.saveWorkoutPosition(
                _currentProgramName!!,
                _currentWeekIndex,
                _currentSessionIndex,
                true // Mark as completed
            )
        } else {
            Log.d("WorkoutViewModel", "Not saving - program name is null or empty")
        }
    }

    /**
     * Save the current workout state to preferences
     */
    fun saveCurrentState() {
        if (hasInteracted && _currentProgramName != null && !_currentProgramName!!.isEmpty()) {
            Log.d("WorkoutViewModel", "Saving current state: program=$_currentProgramName, week=$_currentWeekIndex, session=$_currentSessionIndex")
            appPreferences.saveWorkoutPosition(
                _currentProgramName!!,
                _currentWeekIndex,
                _currentSessionIndex,
                _sessionCompleted.value
            )
        } else {
            Log.d("WorkoutViewModel", "Not saving state - hasInteracted=$hasInteracted, programName=$_currentProgramName")
        }
    }

    /**
     * Start the timer for the current interval
     */
    internal fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (_isRunning.value) {
                delay(1000)
                if (_remainingTime.value > 0) {
                    _remainingTime.value -= 1
                    updateSessionProgress()
                } else {
                    // Move to next interval
                    val session = currentSession
                    if (session != null && _currentIntervalIndex.value < session.intervals.size - 1) {
                        _currentIntervalIndex.value += 1
                        _remainingTime.value = kotlin.math.ceil(session.intervals[_currentIntervalIndex.value].duration * 60).toInt()
                        pausedRemainingTime = null  // Reset paused time

                        // Save progress
                        saveCurrentState()

                        // Play sound when interval changes if enabled
                        if (appSettings.isSoundEnabled) {
                            soundPlayer.playIntervalChangeSound()
                        }
                    } else {
                        // All intervals finished
                        _isRunning.value = false
                        _sessionProgress.value = 1.0f
                        pausedRemainingTime = null  // Reset paused time

                        // Mark session as completed
                        markSessionCompleted()
                    }
                }
            }
        }
    }

    /**
     * Update the session progress
     */
    private fun updateSessionProgress() {
        if (totalSessionSeconds <= 0 || currentSession == null) return

        // Calculate elapsed seconds
        val completedIntervalSeconds = if (_currentIntervalIndex.value > 0) {
            currentSession!!.intervals.subList(0, _currentIntervalIndex.value)
                .sumOf { (it.duration * 60).toInt() }
        } else 0

        // Add time elapsed in current interval
        val currentIntervalTotalSeconds = currentSession!!.intervals[_currentIntervalIndex.value].let {
            (it.duration * 60).toInt()
        }
        val currentIntervalElapsedSeconds = currentIntervalTotalSeconds - _remainingTime.value

        val totalElapsedSeconds = completedIntervalSeconds + currentIntervalElapsedSeconds

        // Update progress
        _sessionProgress.value = totalElapsedSeconds.toFloat() / totalSessionSeconds.toFloat()
    }

    /**
     * Get the current interval
     */
    fun getCurrentInterval(session: Session? = null): Interval? {
        // If session is provided directly, use it
        val sessionToUse = session ?: currentSession
        val index = _currentIntervalIndex.value

        if (sessionToUse == null) {
            return null
        }

        if (index < 0 || index >= sessionToUse.intervals.size) {
            return null
        }

        return sessionToUse.intervals[index]
    }

    /**
     * Get the total remaining time in the session (in seconds)
     */
    fun getTotalRemainingSeconds(): Int {
        if (currentSession == null) return 0

        val completedIntervalSeconds = if (_currentIntervalIndex.value > 0) {
            currentSession!!.intervals.subList(0, _currentIntervalIndex.value)
                .sumOf { (it.duration * 60).toInt() }
        } else 0

        val currentIntervalElapsedSeconds =
            (currentSession!!.intervals[_currentIntervalIndex.value].duration * 60).toInt() - _remainingTime.value

        val totalElapsedSeconds = completedIntervalSeconds + currentIntervalElapsedSeconds

        return totalSessionSeconds - totalElapsedSeconds
    }

    /**
     * Reset the completion state after showing the dialog
     */
    fun resetCompletionState() {
        _sessionCompleted.value = false
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}