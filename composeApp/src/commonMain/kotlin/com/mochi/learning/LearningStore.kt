package com.mochi.learning

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mochi.db.AppDatabase
import com.mochi.db.StillLearning
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Reactive access to the words whose most recent answer was "Still learning".
 * Emits a fresh list whenever the underlying tables change (e.g. after an answer).
 */
class LearningStore(
    private val db: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    fun stillLearning(): Flow<List<StillLearning>> =
        db.reviewLogQueries.stillLearning().asFlow().mapToList(dispatcher)
}
