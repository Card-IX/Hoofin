package com.example.gettahoofin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Enum representing different screens in the app
 */
enum class AppScreen {
    PROGRAM_SELECTION,
    DISCLAIMER,
    WORKOUT,
    SETTINGS
}

/**
 * Navigation component to manage screen transitions
 */
@Composable
fun AppNavigation(
    viewModel: WorkoutViewModel,
    programDataProvider: (String) -> Program?,
    appPreferences: AppPreferences,
    appSettings: AppSettings
) {
    // Navigation state
    var currentScreen by rememberSaveable {
        // Start at program selection if disclaimer already accepted, otherwise show disclaimer
        if (appPreferences.isDisclaimerAccepted()) {
            mutableStateOf(AppScreen.PROGRAM_SELECTION)
        } else {
            mutableStateOf(AppScreen.DISCLAIMER)
        }
    }

    // Track if we came from workout screen for returning after settings
    var cameFromWorkout by rememberSaveable { mutableStateOf(false) }

    // Track if we're just temporarily visiting settings (mid-workout)
    var visitingSettings by rememberSaveable { mutableStateOf(false) }

    // Specific flag for resume dialog control
    var shouldShowResumeDialog by rememberSaveable { mutableStateOf(true) }

    // Program selection state
    var selectedProgram by rememberSaveable { mutableStateOf<String?>(null) }

    // Get program data based on selection
    val programData = remember(selectedProgram) {
        selectedProgram?.let { programDataProvider(it) }
    }

    when (currentScreen) {
        AppScreen.PROGRAM_SELECTION -> {
            // Reset dialog flag when returning to program selection
            shouldShowResumeDialog = true

            ProgramSelectionScreen(
                onProgramSelected = { program ->
                    selectedProgram = program
                    currentScreen = AppScreen.WORKOUT
                },
                onSettingsClicked = {
                    cameFromWorkout = false // Coming from program selection
                    visitingSettings = false // Not mid-workout
                    currentScreen = AppScreen.SETTINGS
                },
                programDataProvider = programDataProvider
            )
        }

        AppScreen.DISCLAIMER -> {
            DisclaimerScreen(
                onDisclaimerAccepted = {
                    // Save that the disclaimer has been accepted
                    appPreferences.setDisclaimerAccepted()
                    currentScreen = AppScreen.PROGRAM_SELECTION
                }
            )
        }

        // In your AppNavigation.kt, update the WORKOUT case:

        // Update the WORKOUT case in AppNavigation.kt

        AppScreen.WORKOUT -> {
            val program = programData
            if (program != null && program.weeks.isNotEmpty()) {
                // Get saved workout position
                val workoutPosition = appPreferences.getWorkoutPosition()

                // Default values for week and session
                var weekIndex = 0
                var sessionIndex = 0
                var startFromBeginning = true

                // Add this before the if statement that checks workoutPosition
                Log.d("AppNavigation", "WorkoutPosition: program=${workoutPosition.programName}, " +
                        "week=${workoutPosition.weekIndex}, session=${workoutPosition.sessionIndex}, " +
                        "completed=${workoutPosition.completed}, visitingSettings=$visitingSettings")

                // Check if there's a saved position for this program - ONLY IF NOT RETURNING FROM SETTINGS
                if (!visitingSettings && workoutPosition.programName == program.name) {
                    // ... existing code for handling workout position ...
                }

                // Safety checks for valid indices
                weekIndex = if (weekIndex >= program.weeks.size) 0 else weekIndex

                if (sessionIndex >= program.weeks[weekIndex].sessions.size) {
                    sessionIndex = 0
                }

                // Initialize workout with proper values - ONLY IF NOT RETURNING FROM SETTINGS
                if (startFromBeginning && !visitingSettings) {
                    weekIndex = 0
                    sessionIndex = 0
                    appPreferences.clearWorkoutPosition()
                }

                // If returning from settings, use our special restoration method
                if (visitingSettings) {
                    viewModel.initializeOrRestoreWorkout(
                        program.weeks[weekIndex].sessions[sessionIndex],
                        program.name,
                        weekIndex,
                        sessionIndex,
                        0,  // Always start at interval 1 (index 0)
                        true  // Explicitly mark as returning from settings
                    )
                } else {
                    // Normal initialization for non-settings return
                    viewModel.initializeOrRestoreWorkout(
                        program.weeks[weekIndex].sessions[sessionIndex],
                        program.name,
                        weekIndex,
                        sessionIndex,
                        0,  // Always start at interval 1 (index 0)
                        false  // Not returning from settings
                    )
                }

                // Get the current visiting settings flag before resetting it
                val isReturningFromSettings = visitingSettings

                // Reset the flag after using it for initialization
                visitingSettings = false

                // Show the workout screen
                WorkoutScreen(
                    viewModel = viewModel,
                    session = program.weeks[weekIndex].sessions[sessionIndex],
                    programName = program.name,
                    program = program,
                    onExit = {
                        currentScreen = AppScreen.PROGRAM_SELECTION
                    },
                    onSettingsClick = {
                        // We don't need to save state here anymore as it's handled in WorkoutScreen
                        visitingSettings = true
                        cameFromWorkout = true
                        shouldShowResumeDialog = false  // Explicitly disable resume dialog
                        currentScreen = AppScreen.SETTINGS
                    },
                    appSettings = appSettings,
                    appPreferences = appPreferences,
                    isReturningFromSettings = isReturningFromSettings
                )
            } else {
                // Fall back to program selection if data is missing
                currentScreen = AppScreen.PROGRAM_SELECTION
            }
        }

        AppScreen.SETTINGS -> {
            SettingsScreen(
                appSettings = appSettings,
                onBack = {
                    // Return to the appropriate screen based on where we came from
                    if (cameFromWorkout) {
                        cameFromWorkout = false
                        // IMPORTANT: Keep visitingSettings flag true when returning
                        // This will be reset after restoration in the WORKOUT case
                        currentScreen = AppScreen.WORKOUT
                    } else {
                        currentScreen = AppScreen.PROGRAM_SELECTION
                    }
                }
            )
        }
    }
}