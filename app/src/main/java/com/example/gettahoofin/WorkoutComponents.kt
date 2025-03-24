package com.example.gettahoofin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.gettahoofin.ui.theme.AppGreen
import java.util.Locale

/**
 * Progress bar and timing information for the session
 */
@Composable
fun SessionProgressBar(
    sessionProgress: Float,
    totalRemainingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacing_medium)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
}

/**
 * Program banner displayed at the bottom of the screen
 */
@Composable
fun ProgramBanner(programName: String) {
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