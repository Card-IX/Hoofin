// File: IntervalDisplay.kt
package com.example.gettahoofin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.gettahoofin.ui.theme.AppGreen

/**
 * Displays the current interval type with an info tooltip
 * (renamed to avoid conflicts with existing function)
 */
@Composable
fun IntervalInfoDisplay(
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
                imageVector = Icons.Default.Info,
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

/**
 * Displays the time remaining for the current interval
 */
@Composable
fun IntervalTimerDisplay(remainingTime: Int) {
    val minutes = remainingTime / 60
    val seconds = remainingTime % 60

    Text(
        text = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp) // Use dp value directly instead of dimensionResource
    )
}