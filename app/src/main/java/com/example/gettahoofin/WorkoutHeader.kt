// File: WorkoutHeader.kt
package com.example.gettahoofin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource

/**
 * Header for the workout screen with navigation and skip controls
 */
@Composable
fun WorkoutHeader(
    weekIndex: Int,
    sessionIndex: Int,
    intervalIndex: Int,
    skipWeekAction: () -> Unit,
    skipSessionAction: () -> Unit,
    skipIntervalAction: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Week with skip button
        SkipButtonColumn(
            title = "Week ${weekIndex + 1}",
            onSkipClick = skipWeekAction,
            contentDescription = "Skip to next week"
        )

        // Session with skip button
        SkipButtonColumn(
            title = "Session ${sessionIndex + 1}",
            onSkipClick = skipSessionAction,
            contentDescription = "Skip to next session"
        )

        // Interval with skip button
        SkipButtonColumn(
            title = "Interval ${intervalIndex + 1}",
            onSkipClick = skipIntervalAction,
            contentDescription = "Skip to next interval"
        )

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
            )
        }
    }
}

/**
 * Column with title and skip button used in the header
 */
@Composable
fun SkipButtonColumn(
    title: String,
    onSkipClick: () -> Unit,
    contentDescription: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(
            onClick = ClickUtils.debounceClick { onSkipClick() },
            modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_button_size))
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = contentDescription,
                modifier = Modifier.size(dimensionResource(R.dimen.workout_skip_icon_size))
            )
        }
    }
}