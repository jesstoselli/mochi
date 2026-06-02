package com.mochi.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Current wall-clock time in epoch milliseconds (multiplatform stdlib clock). */
@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Today as an epoch-day index, used for streak/daily counters. UTC-based for simplicity
 * (a streak may roll over at UTC midnight rather than local — fine for this app).
 */
fun todayEpochDay(): Long = nowMillis() / 86_400_000L
