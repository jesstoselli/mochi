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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mochi.ui.components.BouncyButton
import com.mochi.ui.components.MochiLogo
import com.mochi.ui.theme.LocalJapaneseFont

/** Landing screen: app identity + a clear call to start studying (or "caught up"). */
@Composable
fun HomeScreen(
    pending: Int,
    onStart: () -> Unit,
    onRefresh: () -> Unit,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MochiLogo(Modifier.size(112.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Mochi",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "もち",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = LocalJapaneseFont.current,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Japanese flashcards",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(48.dp))

        if (pending > 0) {
            val cards = if (pending == 1) "card" else "cards"
            Text(
                text = "$pending $cards ready",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(20.dp))
            BouncyButton(onClick = onStart) {
                Text("Start studying")
            }
        } else {
            Text("🎉", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("All caught up!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Come back later to keep your streak going.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            BouncyButton(onClick = onPractice) {
                Text("Practice anyway")
            }
            Spacer(Modifier.height(12.dp))
            BouncyButton(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text("Check again")
            }
        }
    }
}
