package com.mochi.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochi.stats.StatsUiState
import com.mochi.ui.motion.AnimatedCounter

/** Essential stats: streak, today's reviews, words learned, and a 7-day chart. */
@Composable
fun StatsScreen(stats: StatsUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Statistics", style = MaterialTheme.typography.headlineSmall)

        val days = if (stats.streak == 1) "day" else "days"
        AnimatedStatCard(label = "Current streak", value = stats.streak, prefix = "🔥 ", suffix = " $days")
        AnimatedStatCard(label = "Reviews today", value = stats.reviewsToday.toInt())
        AnimatedStatCard(label = "Words learned", value = stats.totalLearned.toInt())

        if (stats.last7Days.isNotEmpty()) {
            SevenDayChart(stats.last7Days)
        }
    }
}

@Composable
private fun AnimatedStatCard(label: String, value: Int, prefix: String = "", suffix: String = "") {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            AnimatedCounter(
                value = value,
                prefix = prefix,
                suffix = suffix,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun SevenDayChart(values: List<Long>) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                val count = values.size
                val gap = size.width * 0.03f
                val barWidth = (size.width - gap * (count - 1)) / count
                val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
                values.forEachIndexed { i, value ->
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = radius,
                    )
                    if (value > 0L) {
                        val barHeight = (size.height * (value.toFloat() / max)).coerceAtLeast(barWidth)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
    }
}
