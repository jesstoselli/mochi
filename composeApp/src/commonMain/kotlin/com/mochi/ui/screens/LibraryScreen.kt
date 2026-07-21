package com.mochi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochi.library.UnitSummary
import com.mochi.ui.theme.LocalJapaneseFont

/**
 * The Library: a grid of study units. Each unit shows its number, a sample kanji, a progress
 * label (learned/total) and a "due" badge. Tapping a unit opens its study session.
 */
@Composable
fun LibraryScreen(
    units: List<UnitSummary>,
    onOpenUnit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "${units.size} units • tap to study",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(units, key = { it.unitId }) { unit ->
                UnitCard(unit = unit, onClick = { onOpenUnit(unit.unitId) })
            }
        }
    }
}

@Composable
private fun UnitCard(unit: UnitSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = unitColor(unit.unitId),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Unit ${unit.unitId + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart),
            )
            if (unit.dueCount > 0) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = "${unit.dueCount} due",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = unit.sampleFront,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = LocalJapaneseFont.current,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                text = "${unit.learnedCount}/${unit.totalCount} learned",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/** A distinct pastel per unit (cycled), tinted onto the surface — keeps the Mochi palette. */
@Composable
private fun unitColor(unitId: Int): Color {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.surfaceVariant,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.primaryContainer,
    )
    return palette[unitId % palette.size]
}
