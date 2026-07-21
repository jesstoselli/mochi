package com.mochi.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mochi.db.AppDatabase
import com.mochi.db.Flashcard
import com.mochi.util.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

/** Reactive read model for the Library grid. Re-emits whenever card progress changes. */
class LibraryStore(private val db: AppDatabase) {
    /** All units, recomputed on every card change (selectAll is ordered by frequency ASC). */
    fun units(): Flow<List<UnitSummary>> =
        db.flashcardQueries.selectAll().asFlow()
            .mapToList(Dispatchers.Default)
            .map { toUnitSummaries(it, nowMillis()) }
}
