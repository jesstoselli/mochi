package com.mochi.util

/** Current wall-clock time in epoch milliseconds. Platform-specific. */
expect fun nowMillis(): Long

/** Today's local date as an epoch-day (days since 1970), for streak/daily counters. */
expect fun todayEpochDay(): Long
