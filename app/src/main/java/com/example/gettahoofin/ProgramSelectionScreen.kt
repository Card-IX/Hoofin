package com.example.gettahoofin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.gettahoofin.ui.theme.AppGreen

/**
 * Screen for selecting a workout program using a grid layout
 */
@Composable
fun ProgramSelectionScreen(
    onProgramSelected: (String) -> Unit,
    onSettingsClicked: () -> Unit,
    programDataProvider: (String) -> Program?
) {
    val programNames = listOf(
        "Start Hoofin': Couch to Walker",
        "Progress Hoofin': Walker to Jogger",
        "Lively Hoofin': Jogger to Runner",
        "Advanced Hoofin': Runner to Racer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Add a spacer at the top to avoid camera notch
        Spacer(modifier = Modifier.height(40.dp))

        // Title row with centered title and settings icon on right
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Centered title
            Text(
                text = "Select a Program",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )

            // Settings icon remains on the right
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        // Optional message about current language
        FallbackLanguageMessage()

        Spacer(modifier = Modifier.height(24.dp))

        // Grid of programs with fixed cell size
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(programNames) { programName ->
                val program = programDataProvider(programName)
                ProgramCard(
                    programName = programName,
                    description = program?.description ?: "",
                    onClick = { onProgramSelected(programName) }
                )
            }
        }

        // Add spacer to push content up if needed
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Card representing a single program in the grid
 */
@Composable
fun ProgramCard(
    programName: String,
    description: String,
    onClick: () -> Unit
) {
    // Use a standard size for all cards - fixed height and width
    Card(
        modifier = Modifier
            .size(width = 170.dp, height = 280.dp) // Standardized size for all cards
            .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = AppGreen
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Program title
            Text(
                text = programName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Description with single line spacing
            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 1.0.em, // Single-spaced text
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )

            // Start button at bottom with icon, styled like session button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppGreen, // Match the card background color
                    contentColor = Color.Black  // Black icon and text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Play icon
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Start")
            }
        }
    }
}