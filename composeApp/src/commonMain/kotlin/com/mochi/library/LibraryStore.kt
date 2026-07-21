package com.mochi.library

import com.mochi.db.Flashcard

/** How many words make up one study unit. */
const val UNIT_SIZE = 50

/**
 * A single row in the Library grid. Units are derived from frequency rank (no schema change):
 * unit N holds the cards ranked [N*UNIT_SIZE, N*UNIT_SIZE + UNIT_SIZE).
 */
data class UnitSummary(
    val unitId: Int,
    val learnedCount: Int,
    val totalCount: Int,
    val dueCount: Int,
    val sampleFront: String,
)

/**
 * Groups frequency-ordered [cards] into units of [UNIT_SIZE].
 * - learnedCount = cards with next_review != null (matches the "words learned" stat).
 * - dueCount = cards due for review now (next_review <= now); brand-new cards are not counted.
 *
 * Pure and DB-free so it can be unit-tested. [cards] MUST already be ordered by frequency ASC.
 */
fun toUnitSummaries(cards: List<Flashcard>, now: Long): List<UnitSummary> =
    cards.chunked(UNIT_SIZE).mapIndexed { index, chunk ->
        UnitSummary(
            unitId = index,
            learnedCount = chunk.count { it.next_review != null },
            totalCount = chunk.size,
            dueCount = chunk.count { it.next_review != null && it.next_review <= now },
            sampleFront = chunk.first().front,
        )
    }
