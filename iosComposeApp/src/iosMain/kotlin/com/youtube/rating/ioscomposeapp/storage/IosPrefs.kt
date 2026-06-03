package com.youtube.rating.ioscomposeapp.storage

import platform.Foundation.NSUserDefaults

internal object IosPrefs {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    fun getString(key: String, defaultValue: String? = null): String? {
        return (defaults.stringForKey(key) ?: defaultValue)
    }

    fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
        defaults.synchronize()
    }
}

