package com.mochi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mochi.ui.SessionStats
import com.mochi.ui.components.BouncyButton
import com.mochi.ui.components.SuccessAnimation

/** Shown when a review session finishes: a quick recap plus what to do next. */
@Composable
fun SessionCompleteScreen(
    stats: SessionStats,
    onContinue: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessAnimation(modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))

        Text("Session complete!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Reviewed ${stats.reviewed}  •  ${stats.correct} correct  •  ${stats.accuracyPercent}%",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        BouncyButton(onClick = onContinue) {
            Text("Continue")
        }
        Spacer(Modifier.height(12.dp))
        BouncyButton(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text("Done for now")
        }
    }
}
