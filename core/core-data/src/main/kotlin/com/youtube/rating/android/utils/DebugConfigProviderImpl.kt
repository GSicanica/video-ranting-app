package com.youtube.rating.android.utils

import com.youtube.rating.android.core.LegacyBuildConfig as BuildConfig

class DebugConfigProviderImpl : DebugConfigProvider {
    override val debugUnlockPassword: String
        get() = BuildConfig.DEBUG_UNLOCK_PASSWORD
}
