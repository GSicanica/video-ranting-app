package com.youtube.rating.shared.utils

/**
 * Global logging configuration for the entire app.
 * 
 * Configured via local.properties: enable.logging=true/false
 * This value is injected at build time via BuildConfig.
 */
object LogConfig {
    /**
     * Master switch for all logging in the app.
     * This is set at compile time from local.properties.
     * 
     * To change: Edit androidApp/local.properties
     * enable.logging=true   // Enable all logs
     * enable.logging=false  // Disable all logs
     */
    var ENABLE_LOGS = true  // Default value, will be overridden by BuildConfig
    
    /**
     * Log a message if logging is enabled.
     */
    fun log(message: String) {
        if (ENABLE_LOGS) {
            println(message)
        }
    }
    
    /**
     * Log an exception if logging is enabled.
     */
    fun logError(message: String, throwable: Throwable? = null) {
        if (ENABLE_LOGS) {
            println("❌ $message")
            throwable?.printStackTrace()
        }
    }
}
