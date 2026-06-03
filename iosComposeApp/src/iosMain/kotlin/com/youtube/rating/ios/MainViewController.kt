package com.youtube.rating.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.youtube.rating.ioscomposeapp.AppRoot

/**
 * Main UIViewController for iOS that hosts the Compose UI
 * This is called from Swift's SceneDelegate
 */
fun MainViewController() = ComposeUIViewController {
    AppRoot()
}
