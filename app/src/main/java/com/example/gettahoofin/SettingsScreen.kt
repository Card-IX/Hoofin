// SettingsScreen.kt
package com.example.gettahoofin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource

@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(appSettings.isSoundEnabled) }
    var keepScreenOn by remember { mutableStateOf(appSettings.isKeepScreenOn) }
    var soundVolume by remember { mutableFloatStateOf(appSettings.soundVolume) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_medium)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_medium))
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_elevation))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sound setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_medium)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Play sound when intervals change")
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            appSettings.isSoundEnabled = it
                        }
                    )
                }

                // Only show volume slider if sound is enabled
                if (soundEnabled) {
                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.spacing_medium))
                    ) {
                        Text(
                            text = "Sound Volume",
                            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
                        )

                        Slider(
                            value = soundVolume,
                            onValueChange = {
                                soundVolume = it
                                appSettings.soundVolume = it
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                // Keep screen on setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.spacing_medium)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Keep screen on during workout")
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = {
                            keepScreenOn = it
                            appSettings.isKeepScreenOn = it
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBack,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_medium))
        ) {
            Text("Back")
        }
    }
}