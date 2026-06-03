package com.youtube.rating.android.utils

import android.os.StrictMode
import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig
import com.youtube.rating.shared.utils.Logger

/**
 * Manages StrictMode for detecting performance issues
 */
object StrictModeManager {
    
    @Volatile
    private var isEnabled = false
    
    fun enable() {
        if (isEnabled) return
        if (BuildConfig.DEBUG) {
            Logger.debug("StrictMode", "Enabling StrictMode")
        }
        
        // Thread policy - detects operations on main thread
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyFlashScreen() // Flash red screen on violation
                .build()
        )
        
        // VM policy - detects memory leaks
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectActivityLeaks()
                .detectCleartextNetwork()
                .penaltyLog()
                .build()
        )
        
        isEnabled = true
        if (BuildConfig.DEBUG) {
            Logger.info("StrictMode", "StrictMode enabled")
        }
    }
    
    fun disable() {
        if (!isEnabled) return
        if (BuildConfig.DEBUG) {
            Logger.debug("StrictMode", "Disabling StrictMode")
        }
        
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)
        
        isEnabled = false
        if (BuildConfig.DEBUG) {
            Logger.info("StrictMode", "StrictMode disabled")
        }
    }
    
    fun isEnabled(): Boolean = isEnabled
}
