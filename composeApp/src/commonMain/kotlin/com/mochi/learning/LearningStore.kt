package com.mochi.learning

import com.mochi.db.AppDatabase
import com.mochi.db.StillLearning

/** Read-only access to the words whose most recent answer was "Still learning". */
class LearningStore(private val db: AppDatabase) {
    fun stillLearning(): List<StillLearning> =
        db.reviewLogQueries.stillLearning().executeAsList()
}
