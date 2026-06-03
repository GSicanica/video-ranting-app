import Foundation
import UIKit
import UserNotifications

/**
 * Swift bridge for Kotlin Notifications
 * Handles UNUserNotificationCenter operations
 */

// MARK: - Notification Functions

@_silgen_name("YouTubeRatingApp_notifyiOSNotification")
public func notifyiOSNotification(title: String, message: String, tag: String?) {
    let content = UNMutableNotificationContent()
    content.title = title
    content.body = message
    content.sound = .default
    content.badge = NSNumber(value: UIApplication.shared.applicationIconBadgeNumber + 1)
    
    // Add custom data
    if let tag = tag {
        content.userInfo = ["notificationTag": tag]
    }
    
    // Create trigger for immediate notification
    let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
    let request = UNNotificationRequest(identifier: tag ?? UUID().uuidString, content: content, trigger: trigger)
    
    UNUserNotificationCenter.current().add(request) { error in
        if let error = error {
            print("Error showing notification: \(error.localizedDescription)")
        }
    }
}

@_silgen_name("YouTubeRatingApp_requestiOSNotificationPermissions")
public func requestiOSNotificationPermissions() {
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
}

@_silgen_name("YouTubeRatingApp_cancelihOSNotification")
public func cancelihOSNotification(tag: String) {
    UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [tag])
    UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [tag])
}

@_silgen_name("YouTubeRatingApp_cancelAlliOSNotifications")
public func cancelAlliOSNotifications() {
    UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    UNUserNotificationCenter.current().removeAllDeliveredNotifications()
}
