package com.example.gettahoofin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.example.gettahoofin.ui.theme.AppGreen

/**
 * Control buttons for the workout (start/pause, restart)
 */
@Composable
fun WorkoutControls(
    isRunning: Boolean,
    isPaused: Boolean,
    onStartPauseClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Start/Pause toggle button
        Button(
            onClick = ClickUtils.debounceClick { onStartPauseClick() },
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
            onClick = ClickUtils.debounceClick { onRestartClick() },
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
}

/**
 * Home button for returning to program selection
 */
@Composable
fun HomeButton(onClick: () -> Unit) {
    Button(
        onClick = ClickUtils.debounceClick { onClick() }, // Fixed: Pass a lambda to debounceClick
        modifier = Modifier
            .padding(dimensionResource(R.dimen.spacing_small))
            .size(dimensionResource(R.dimen.workout_control_button_size)),
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
}