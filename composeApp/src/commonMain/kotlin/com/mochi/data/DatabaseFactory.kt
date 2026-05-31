package com.mochi.data

import app.cash.sqldelight.db.SqlDriver
import com.mochi.db.AppDatabase

/**
 * Platform-specific database driver.
 * Android uses AndroidSqliteDriver; iOS uses NativeSqliteDriver.
 * See the `actual` implementations in androidMain / iosMain.
 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): AppDatabase =
    AppDatabase(driverFactory.createDriver())
