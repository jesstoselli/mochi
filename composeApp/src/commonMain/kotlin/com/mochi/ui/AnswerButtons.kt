package com.mochi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The two self-rating buttons shown under a card.
 * `onAnswer(true)` = the user knew it; `onAnswer(false)` = still learning.
 */
@Composable
fun AnswerButtons(
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BouncyButton(
            onClick = { onAnswer(false) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text("Still learning")
        }
        BouncyButton(onClick = { onAnswer(true) }) {
            Text("I knew it")
        }
    }
}
