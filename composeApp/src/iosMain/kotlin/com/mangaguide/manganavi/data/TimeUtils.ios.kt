package com.mangaguide.manganavi.data

import platform.Foundation.NSDate

actual fun currentTimeMillis(): Long =
    (NSDate.date().timeIntervalSince1970 * 1000).toLong()
