package com.example.gettahoofin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

/**
 * Screen for accepting the disclaimer before starting a workout
 */
@Composable
fun DisclaimerScreen(onDisclaimerAccepted: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_medium)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Add this at the top
        FallbackLanguageMessage()

        Text(
            stringResource(R.string.disclaimer_text),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_medium))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
            Text(
                stringResource(R.string.disclaimer_checkbox),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_small))
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
        Button(onClick = { if (isChecked) onDisclaimerAccepted() }, enabled = isChecked) {
            Text(stringResource(R.string.continue_button))
        }
    }
}