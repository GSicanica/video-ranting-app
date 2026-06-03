import UIKit
import UserNotifications

@main
final class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    // MARK: - UIApplicationDelegate Methods
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Setup User Notifications Delegate
        UNUserNotificationCenter.current().delegate = self
        
        // Initialize platform services
        initializeKotlinPlatformServices()
        
        // Handle notification launch option
        if let notification = launchOptions?[.remoteNotification] as? [AnyHashable: Any] {
            handleRemoteNotification(notification)
        }
        
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
    
    // MARK: - Remote Notifications (Push)
    
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        handleRemoteNotification(userInfo)
        completionHandler(.newData)
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ Failed to register for remote notifications: \(error.localizedDescription)")
    }
    
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("✅ Device Token: \(token)")
        
        // Save device token for push notifications
        UserDefaults.standard.set(token, forKey: "devicePushToken")
        
        // Send to backend via Kotlin layer if needed
        sendDeviceTokenToBackend(token: token)
    }
    
    // MARK: - UNUserNotificationCenterDelegate Methods
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        print("📬 Received notification while in foreground")
        
        // Show notification even if app is in foreground
        var options: UNNotificationPresentationOptions = [.banner, .sound]
        if #available(iOS 14.0, *) {
            options = [.banner, .sound, .badge]
        }
        
        completionHandler(options)
    }
    
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("🔔 User tapped notification")
        
        handleReceivedNotification(userInfo)
        completionHandler()
    }
    
    // MARK: - Private Helper Methods
    
    private func handleRemoteNotification(_ userInfo: [AnyHashable: Any]) {
        print("📨 Handling remote notification")
    }
    
    private func handleReceivedNotification(_ userInfo: [AnyHashable: Any]) {
        // Forward notification to Kotlin layer for processing
        if let tag = userInfo["tag"] as? String {
            print("Notification tag: \(tag)")
        }
    }
    
    private func sendDeviceTokenToBackend(token: String) {
        print("📤 Device token registered: \(token)")
    }
    
    private func initializeKotlinPlatformServices() {
        print("🚀 Initializing Kotlin platform services...")
        // Platform bootstrap hooks are handled from SceneDelegate via shared RuntimeConfig.
    }
    
    // MARK: - Badge Management
    
    func resetNotificationBadge() {
        UIApplication.shared.applicationIconBadgeNumber = 0
    }
    
    func setNotificationBadge(_ count: Int) {
        UIApplication.shared.applicationIconBadgeNumber = count
    }
}

