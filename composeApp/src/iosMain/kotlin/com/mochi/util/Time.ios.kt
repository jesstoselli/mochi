package com.mochi.util

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun todayEpochDay(): Long {
    val epochSeconds = NSDate().timeIntervalSince1970
    val offsetSeconds = NSTimeZone.localTimeZone.secondsFromGMT.toDouble()
    return ((epochSeconds + offsetSeconds) / 86_400.0).toLong()
}
