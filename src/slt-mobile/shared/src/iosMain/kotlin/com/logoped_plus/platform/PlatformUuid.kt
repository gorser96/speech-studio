package com.logoped_plus.platform

import platform.Foundation.NSUUID

actual fun randomUuidString(): String = NSUUID().UUIDString.lowercase()
