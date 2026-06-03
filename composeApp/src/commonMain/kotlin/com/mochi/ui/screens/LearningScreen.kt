package com.mochi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mochi.learning.LearningWord
import com.mochi.ui.theme.LocalJapaneseFont

/**
 * The "Still learning" list: words whose most recent answer was "Still learning", so the
 * user can revisit them anytime. Each item shows the Japanese word with its meaning below;
 * tapping plays the pronunciation.
 */
@Composable
fun LearningScreen(
    words: List<LearningWord>,
    onPlay: (LearningWord) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (words.isEmpty()) {
        EmptyState(modifier)
        return
    }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Still learning", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = if (words.size == 1) "1 word to revisit" else "${words.size} words to revisit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(words, key = { it.id }) { word ->
                WordRow(word = word, onPlay = onPlay)
            }
        }
    }
}

@Composable
private fun WordRow(word: LearningWord, onPlay: (LearningWord) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay(word) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = LocalJapaneseFont.current,
                )
                if (word.reading.isNotBlank() && word.reading != word.word) {
                    Text(
                        text = word.reading,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = LocalJapaneseFont.current,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!word.audio.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Play pronunciation",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Still learning", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Words you mark \"Still learning\" during review collect here, " +
                "so you can come back and study them whenever you like.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
