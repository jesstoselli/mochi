package com.mochi.library

import com.mochi.db.Flashcard
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStoreTest {

    // now = 100; a card is "due" if next_review == null (new) OR next_review <= now.
    private val now = 100L

    private fun card(rank: Long, nextReview: Long?) = Flashcard(
        id = rank,
        front = "front$rank",
        back = "back$rank",
        reading = "reading$rank",
        category = "Kaishi 1.5k",
        sentence = null,
        sentence_meaning = null,
        audio = null,
        frequency = rank,
        next_review = nextReview,
        interval_days = 0L,
        ease = 2.5,
    )

    @Test
    fun chunksCardsIntoUnitsOfFifty() {
        // 120 cards -> 3 units (50, 50, 20).
        val cards = List(120) { card(it.toLong(), nextReview = null) }
        val units = toUnitSummaries(cards, now)
        assertEquals(3, units.size)
        assertEquals(50, units[0].totalCount)
        assertEquals(50, units[1].totalCount)
        assertEquals(20, units[2].totalCount)
        assertEquals(0, units[0].unitId)
        assertEquals(2, units[2].unitId)
    }

    @Test
    fun countsLearnedAndDuePerUnit() {
        // Unit 0: 50 cards. 10 learned & due (next_review=50<=100),
        // 5 learned & not due (next_review=200>100), rest new (null, counted new not due).
        val cards = List(50) { i ->
            val nr = when {
                i < 10 -> 50L      // learned, due now
                i < 15 -> 200L     // learned, not due
                else -> null       // new
            }
            card(i.toLong(), nr)
        }
        val unit = toUnitSummaries(cards, now).single()
        assertEquals(15, unit.learnedCount) // next_review != null
        assertEquals(10, unit.dueCount)     // next_review <= now
    }

    @Test
    fun sampleFrontIsTheFirstCardOfTheUnit() {
        val cards = List(50) { card(it.toLong(), nextReview = null) }
        assertEquals("front0", toUnitSummaries(cards, now).single().sampleFront)
    }
}
