package com.mochi.util

/** Current wall-clock time in epoch milliseconds. Platform-specific. */
expect fun nowMillis(): Long

/**
 * Today as an epoch-day index, used for streak/daily counters. UTC-based for simplicity
 * (a streak may roll over at UTC midnight rather than local — fine for this app).
 */
fun todayEpochDay(): Long = nowMillis() / 86_400_000L
