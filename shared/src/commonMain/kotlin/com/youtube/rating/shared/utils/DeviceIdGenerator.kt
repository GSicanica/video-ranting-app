package com.youtube.rating.shared.utils

import kotlin.random.Random

object DeviceIdGenerator {
    fun generateRandomDeviceId(): String {
        // Generate a random device ID for testing
        return "device_${Random.nextLong()}"
    }
}
