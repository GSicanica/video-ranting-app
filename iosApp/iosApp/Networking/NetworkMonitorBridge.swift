import Foundation
import Network

/**
 * Swift bridge for Kotlin Network Monitoring
 * Uses Network framework to monitor connectivity
 */

private let monitor = NWPathMonitor()
private let queue = DispatchQueue(label: "com.youtube.rating.network")
private var currentNetworkCallback: ((Bool) -> Void)? = nil

@_silgen_name("YouTubeRatingApp_initializeiOSNetworkMonitoring")
public func initializeiOSNetworkMonitoring(callback: @escaping (Bool) -> Void) {
    currentNetworkCallback = callback
    
    monitor.pathUpdateHandler = { path in
        let isOnline = path.status == .satisfied
        callback(isOnline)
    }
    
    monitor.start(queue: queue)
    
    // Initial status
    let initialStatus = monitor.currentPath.status == .satisfied
    callback(initialStatus)
}

@_silgen_name("YouTubeRatingApp_getiOSNetworkStatus")
public func getiOSNetworkStatus() -> Bool {
    return monitor.currentPath.status == .satisfied
}
