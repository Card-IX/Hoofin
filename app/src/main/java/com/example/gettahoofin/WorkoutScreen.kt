package com.example.gettahoofin

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.gettahoofin.ui.theme.AppGreen
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import android.util.Log
import android.os.Handler
import android.os.Looper

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    session: Session,
    programName: String,
    program: Program? = null,
    onExit: () -> Unit,
    onSettingsClick: () -> Unit,
    appSettings: AppSettings,
    appPreferences: AppPreferences,
    isReturningFromSettings: Boolean = false
) {
    // Load pace definitions
    val context = LocalContext.current
    val paceDefinitions = remember {
        val jsonString = getJsonDataFromAsset(context, "pace_definitions.json")
        if (jsonString != null) {
            val gson = Gson()
            gson.fromJson(jsonString, PaceDefinitions::class.java).paceDefinitions
        } else {
            emptyList()
        }
    }

    // Keep screen on based on settings
    val view = LocalView.current
    LaunchedEffect(appSettings.isKeepScreenOn) {
        view.keepScreenOn = appSettings.isKeepScreenOn
    }

    // Initialize the workout (if needed)
    LaunchedEffect(session) {
        delay(50) // Add a small delay to ensure complete initialization
    }

    // Direct state restoration if returning from settings
    if (isReturningFromSettings) {
        // Use the Android Handler for guaranteed execution with delay
        val didApplyRestoration = remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            if (StateManager.hasWorkoutState() && !didApplyRestoration.value) {
                val handler = Handler(Looper.getMainLooper())

                // First, log that we're attempting restoration
                Log.d("WorkoutScreen", "Setting up delayed state restoration handler...")

                // Set up a delayed action to restore state
                handler.postDelayed({
                    if (StateManager.hasWorkoutState()) {
                        val intervalIndex = StateManager.getSavedIntervalIndex()
                        val remainingTime = StateManager.getSavedRemainingTime()
                        val wasRunning = StateManager.wasWorkoutRunning()

                        Log.d("WorkoutScreen", "DIRECTLY applying stored state: interval=$intervalIndex, time=$remainingTime, running=$wasRunning")

                        // Apply stored state directly to the ViewModel
                        viewModel._currentIntervalIndex.value = intervalIndex
                        viewModel._remainingTime.value = remainingTime

                        didApplyRestoration.value = true

                        // Set up another delayed action to restart timer if needed
                        if (wasRunning) {
                            handler.postDelayed({
                                Log.d("WorkoutScreen", "Starting timer after state restoration")
                                viewModel._isRunning.value = true
                                viewModel._isPaused.value = false
                                viewModel.startTimer()
                            }, 1000) // 1 second delay
                        }
                    }
                }, 1500) // 1.5 second delay for initial restoration
            }

            // Cleanup
            onDispose {
                // No cleanup needed for Handler
                Log.d("WorkoutScreen", "DisposableEffect cleanup - restoration applied: ${didApplyRestoration.value}")
            }
        }
    }

    // Collect state from ViewModel
    val currentIntervalIndex by viewModel.currentIntervalIndex.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val sessionProgress by viewModel.sessionProgress.collectAsState()
    val sessionCompleted by viewModel.sessionCompleted.collectAsState()

    // Use session directly for the first render
    // Either get interval from ViewModel OR directly from session as backup
    val currentInterval = viewModel.getCurrentInterval() ?:
    if (session.intervals.isNotEmpty() && currentIntervalIndex >= 0 && currentIntervalIndex < session.intervals.size)
        session.intervals[currentIntervalIndex]
    else null

    // Actions
    val startPauseAction = { viewModel.toggleStartPause() }
    val restartAction = { viewModel.restart() }

    // Skip actions
    val skipIntervalAction = { viewModel.skipToNextInterval() }
    val skipSessionAction = { program?.let { viewModel.skipToNextSession(it) } }
    val skipWeekAction = { program?.let { viewModel.skipToNextWeek(it) } }

    // Settings action with state saving and pause
    val handleSettingsClick = {
        // Save state to the StateManager
        StateManager.saveWorkoutState(
            currentIntervalIndex,
            remainingTime,
            isRunning,
            programName
        )

        // Log the saved state
        Log.d("WorkoutScreen", "Saving state before settings: interval=$currentIntervalIndex, time=$remainingTime, running=$isRunning")

        // Pause if running
        if (isRunning) {
            viewModel.toggleStartPause()
        }

        // Navigate to settings
        onSettingsClick()
    }

    // Completion dialog
    if (sessionCompleted) {
        AlertDialog(
            onDismissRequest = { viewModel.resetCompletionState() },
            title = { Text("Workout Complete!") },
            text = { Text("Congratulations on completing your workout session!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCompletionState()
                        onExit() // Return to program selection
                    }
                ) {
                    Text("Done")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_medium)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small), Alignment.Top)
    ) {
        // Add top spacer to avoid status bar overlap
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.workout_top_spacer)))

        // Workout header with interval info and skip buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Week with skip button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Week ${viewModel.currentWeekIndex + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = ClickUtils.debounceClick { skipWeekAction() },
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip to next week",
                        modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
                    )
                }
            }

            // Session with skip button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Session ${viewModel.currentSessionIndex + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = ClickUtils.debounceClick { skipSessionAction() },
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip to next session",
                        modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
                    )
                }
            }

            // Interval with skip button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Interval ${currentIntervalIndex + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = ClickUtils.debounceClick { skipIntervalAction() },
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip to next interval",
                        modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
                    )
                }
            }

            // Settings button
            IconButton(
                onClick = ClickUtils.debounceClick { handleSettingsClick() },
                modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
                )
            }
        }

        // Add more space between header and timer display
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxlarge)))

        // Large timer display with tooltip
        if (currentInterval != null) {
            // Use the renamed function
            IntervalTypeInfo(
                intervalType = currentInterval.type,
                paceDefinitions = paceDefinitions
            )

            val minutes = remainingTime / 60
            val seconds = remainingTime % 60

            Text(
                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(R.dimen.spacing_medium))
            )
        } else {
            // Fallback if no interval data available
            Text(
                text = "No interval data available",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Red
            )
        }

        // Add space after timer display
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

        // Session progress bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.spacing_medium)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display total time information
            val totalRemainingSeconds = viewModel.getTotalRemainingSeconds()
            val totalRemainingMinutes = totalRemainingSeconds / 60
            val totalRemainingSecondsInMinute = totalRemainingSeconds % 60

            // Time remaining in session
            Text(
                text = stringResource(
                    R.string.session_time_remaining,
                    String.format(Locale.US, "%02d:%02d", totalRemainingMinutes, totalRemainingSecondsInMinute)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_tiny)))

            LinearProgressIndicator(
                progress = { sessionProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.workout_progress_bar_height)),
                color = AppGreen,
                trackColor = Color(0xFFE0E0E0),  // Light gray
            )
        }

        // Add more space after progress bar
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxlarge)))

        // Control buttons for Start/Pause and Restart in one row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Start/Pause toggle button
            Button(
                onClick = ClickUtils.debounceClick { startPauseAction() },
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.spacing_small))
                    .size(dimensionResource(R.dimen.workout_control_button_size)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppGreen
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) stringResource(R.string.pause) else stringResource(R.string.start),
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_control_icon_size))
                )
            }

            // Restart button
            Button(
                onClick = ClickUtils.debounceClick { restartAction() },
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.spacing_small))
                    .size(dimensionResource(R.dimen.workout_control_button_size)),
                enabled = isRunning || isPaused,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppGreen,
                    disabledContainerColor = Color(0xFFBDBDBD)  // Light gray when disabled
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = stringResource(R.string.restart),
                    modifier = Modifier.size(dimensionResource(R.dimen.workout_control_icon_size))
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

        // Home button on its own line
        Button(
            onClick = ClickUtils.debounceClick {
                // Use collected state variable
                appPreferences.saveWorkoutPosition(
                    programName,
                    viewModel.currentWeekIndex,
                    viewModel.currentSessionIndex,
                    sessionCompleted  // Use the collected state variable
                )
                onExit()
            },
            modifier = Modifier
                .padding(dimensionResource(R.dimen.spacing_small))
                .size(dimensionResource(R.dimen.workout_control_button_size))
                .align(Alignment.CenterHorizontally),  // Center the home button
            colors = ButtonDefaults.buttonColors(
                containerColor = AppGreen
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = stringResource(R.string.home),
                modifier = Modifier.size(dimensionResource(R.dimen.workout_control_icon_size))
            )
        }

        // Spacer that pushes the banner to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Program banner at the bottom
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppGreen,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensionResource(R.dimen.card_elevation)
            )
        ) {
            Text(
                text = programName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_medium)),
                color = Color.Black  // Black text on green background
            )
        }
    }
}

@Composable
fun IntervalTypeInfo(
    intervalType: String,
    paceDefinitions: List<PaceDefinition>
) {
    var showTooltip by remember { mutableStateOf(false) }

    // Find matching pace definition
    val definition = paceDefinitions.find { it.type == intervalType }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Main interval name - centered in the box
        Text(
            text = intervalType,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        )

        // Info button positioned at the right edge
        IconButton(
            onClick = { showTooltip = !showTooltip },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Interval info",
                tint = AppGreen
            )
        }

        // Tooltip
        if (showTooltip && definition != null) {
            Popup(
                alignment = Alignment.BottomCenter,
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = definition.type,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = definition.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Effort: ${definition.perceivedEffort}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Signs: ${definition.physicalSigns}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// Helper function to load JSON data from assets
fun getJsonDataFromAsset(context: Context, fileName: String): String? {
    val jsonString: String
    try {
        val inputStream: InputStream = context.assets.open(fileName)
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        jsonString = String(buffer, Charsets.UTF_8)
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return null
    }
    return jsonString
}