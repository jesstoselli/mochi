package com.mochi.util

import platform.Foundation.NSDate

actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
