import UIKit
import shared
import SwiftUI
import UserNotifications

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        // Configure shared logging
        shared.Logger.shared.isEnabled = true
        shared.Logger.shared.minLevel = shared.LogLevel.debug

        // Get configuration from Info.plist
        let baseUrl = Bundle.main.object(forInfoDictionaryKey: "YT_BASE_URL") as? String
        let dataBaseUrl = Bundle.main.object(forInfoDictionaryKey: "YT_DATA_BASE_URL") as? String
        
        // Set runtime configuration
        shared.RuntimeConfig.shared.setBaseUrls(
            baseUrl: baseUrl ?? "https://api.youtuberating.app",
            dataBaseUrl: dataBaseUrl ?? "https://data.youtuberating.app"
        )

        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
            if let error = error {
                print("Notification permission error: \(error.localizedDescription)")
            }
        }

        // Use the app's SwiftUI shell (currently the most complete iOS UI path).
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = UIHostingController(rootView: ContentView())
        window.makeKeyAndVisible()
        self.window = window
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        // Called when the scene is being released by the system.
        // Cleanup resources
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        // Called when the scene has moved from an inactive state to an active state.
        // Resume any tasks that were paused
    }

    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from an active state to an inactive state.
        // Pause ongoing tasks, disable timers
    }

    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from the background to the foreground.
        // Undo the changes made on entering the background
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from the foreground to the background.
        // Save data, release resources
    }
}
