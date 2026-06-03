package com.youtube.rating.shared.platform.notifications

open class PlatformNotificationService {
    suspend fun showNotification(title: String, message: String, tag: String? = null) = Unit

    fun requestPermissions() = Unit

    fun cancelNotification(tag: String) = Unit

    fun cancelAllNotifications() = Unit
}

class PlatformNotificationServiceImpl : PlatformNotificationService()
