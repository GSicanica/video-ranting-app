import AVFoundation
import AVKit

/// VideoPlayerBridge.swift
/// Swift wrapper for AVPlayer integration with Kotlin Multiplatform
/// Handles HLS streaming and YouTube video playback

class VideoPlayerBridge: NSObject {
    static let shared = VideoPlayerBridge()
    
    private var player: AVPlayer?
    private var playerViewController: AVPlayerViewController?
    private var statusObserver: NSKeyValueObservation?
    private var bufferObserver: NSKeyValueObservation?
    private var timeObserver: Any?
    
    override private init() {
        super.init()
    }
    
    // MARK: - Player Lifecycle
    
    func initializePlayer(with videoURL: String) -> Bool {
        guard let url = URL(string: videoURL) else {
            print("❌ Invalid video URL: \(videoURL)")
            return false
        }
        
        // Create asset with proper headers for HLS
        let asset = AVAsset(url: url)
        let playerItem = AVPlayerItem(asset: asset)
        
        // Configure streaming settings
        playerItem.preferredMaximumResolution = CGSize(width: 1920, height: 1080)
        
        player = AVPlayer(playerItem: playerItem)
        setupPlayerObservers()
        
        print("✅ Player initialized with URL: \(videoURL)")
        return true
    }
    
    func play() {
        guard let player = player else {
            print("❌ Player not initialized")
            return
        }
        player.play()
        print("▶️ Playback started")
    }
    
    func pause() {
        player?.pause()
        print("⏸️ Playback paused")
    }
    
    func stop() {
        player?.replaceCurrentItem(with: nil)
        player = nil
        removePlayerObservers()
        print("⏹️ Playback stopped")
    }
    
    func seek(to seconds: Double) {
        guard let player = player else { return }
        let time = CMTime(seconds: seconds, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player.seek(to: time, toleranceBefore: .zero, toleranceAfter: .zero) { _ in
            print("⏩ Seeked to \(seconds)s")
        }
    }
    
    // MARK: - Player Properties
    
    func getDuration() -> Double {
        guard let duration = player?.currentItem?.duration else {
            return 0
        }
        return duration.seconds
    }
    
    func getCurrentTime() -> Double {
        return player?.currentTime().seconds ?? 0
    }
    
    func setVolume(_ volume: Float) {
        player?.volume = max(0, min(1, volume))
    }
    
    func getVolume() -> Float {
        return player?.volume ?? 1.0
    }
    
    func setRate(_ rate: Float) {
        player?.rate = rate
    }
    
    func getRate() -> Float {
        return player?.rate ?? 1.0
    }
    
    // MARK: - Video Quality Management
    
    func getAvailableQualities() -> [String] {
        guard let asset = player?.currentItem?.asset else {
            return []
        }
        
        var qualities: [String] = []
        
        if asset.hasMediaCharacteristic(.visual) {
            for track in (try? asset.loadTracks(withMediaType: .video)) ?? [] {
                if let dimensions = try? track.load(.naturalSize) {
                    let height = Int(dimensions.height)
                    switch height {
                    case 2160:
                        qualities.append("4K")
                    case 1080:
                        qualities.append("1080p")
                    case 720:
                        qualities.append("720p")
                    case 480:
                        qualities.append("480p")
                    case 360:
                        qualities.append("360p")
                    default:
                        break
                    }
                }
            }
        }
        
        return qualities.removingDuplicates()
    }
    
    // MARK: - Subtitle/Caption Handling
    
    func getAvailableCaptions() -> [String] {
        guard let group = player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: .legible) else {
            return []
        }
        
        return group.options.compactMap { $0.displayName }
    }
    
    func selectCaption(_ caption: String) {
        guard let group = player?.currentItem?.asset.mediaSelectionGroup(forMediaCharacteristic: .legible) else {
            return
        }
        
        if let option = group.options.first(where: { $0.displayName == caption }) {
            player?.currentItem?.select(option, in: group)
            print("📝 Selected caption: \(caption)")
        }
    }
    
    // MARK: - Picture-in-Picture
    
    func enablePictureInPicture() -> Bool {
        guard let playerViewController = playerViewController else {
            print("❌ Player view controller not available")
            return false
        }
        
        if AVPictureInPictureController.isPictureInPictureSupported() {
            playerViewController.allowsPictureInPicturePlayback = true
            print("✅ Picture-in-Picture enabled")
            return true
        }
        
        return false
    }
    
    // MARK: - Playback Statistics
    
    func getPlaybackStatistics() -> PlaybackStats {
        guard let currentItem = player?.currentItem else {
            return PlaybackStats(duration: 0, currentTime: 0, bufferedDuration: 0, isPlaying: false)
        }
        
        let bufferedDuration = currentItem.loadedTimeRanges.reduce(0) { total, range in
            return total + range.timeRangeValue.duration.seconds
        }
        
        return PlaybackStats(
            duration: currentItem.duration.seconds,
            currentTime: player?.currentTime().seconds ?? 0,
            bufferedDuration: bufferedDuration,
            isPlaying: player?.timeControlStatus == .playing
        )
    }
    
    // MARK: - Network Quality Monitoring
    
    func getNetworkQuality() -> String {
        guard let currentItem = player?.currentItem else {
            return "unknown"
        }
        
        if let preferredPeakBitRate = currentItem.preferredPeakBitRate,
           preferredPeakBitRate > 0 {
            
            switch preferredPeakBitRate {
            case ..<500_000:
                return "poor"
            case 500_000..<1_000_000:
                return "fair"
            case 1_000_000..<5_000_000:
                return "good"
            default:
                return "excellent"
            }
        }
        
        return "unknown"
    }
    
    // MARK: - Private Helpers
    
    private func setupPlayerObservers() {
        guard let player = player else { return }
        
        // Observe player status
        statusObserver = player.currentItem?.observe(\.status) { [weak self] _, _ in
            self?.playerStatusDidChange()
        }
        
        // Observe buffer status
        bufferObserver = player.currentItem?.observe(\.loadedTimeRanges) { [weak self] _, _ in
            self?.bufferStatusDidChange()
        }
        
        // Observe periodic time updates (every 0.5 seconds)
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC)),
            queue: .main
        ) { [weak self] _ in
            self?.playerTimeDidUpdate()
        }
    }
    
    private func removePlayerObservers() {
        statusObserver?.invalidate()
        bufferObserver?.invalidate()
        
        if let timeObserver = timeObserver {
            player?.removeTimeObserver(timeObserver)
        }
    }
    
    private func playerStatusDidChange() {
        if let status = player?.currentItem?.status {
            switch status {
            case .readyToPlay:
                print("✅ Player ready to play")
                playerStatusCallback(status: "ready", error: nil)
            case .failed:
                let error = player?.currentItem?.error?.localizedDescription ?? "Unknown error"
                print("❌ Player failed: \(error)")
                playerStatusCallback(status: "error", error: error)
            case .unknown:
                print("⏳ Player loading...")
                playerStatusCallback(status: "loading", error: nil)
            @unknown default:
                break
            }
        }
    }
    
    private func bufferStatusDidChange() {
        let bufferedDuration = player?.currentItem?.loadedTimeRanges.reduce(0) { total, range in
            return total + range.timeRangeValue.duration.seconds
        } ?? 0
        
        bufferStatusCallback(bufferedDuration: bufferedDuration)
    }
    
    private func playerTimeDidUpdate() {
        let currentTime = player?.currentTime().seconds ?? 0
        let duration = player?.currentItem?.duration.seconds ?? 0
        
        timeUpdateCallback(currentTime: currentTime, duration: duration)
    }
}

// MARK: - Data Structures

struct PlaybackStats {
    let duration: Double
    let currentTime: Double
    let bufferedDuration: Double
    let isPlaying: Bool
}

// MARK: - Kotlin Interop Functions

@_silgen_name("iOSVideoPlayer_initializePlayer")
public func iOSVideoPlayer_initializePlayer(videoURL: String) -> Bool {
    return VideoPlayerBridge.shared.initializePlayer(with: videoURL)
}

@_silgen_name("iOSVideoPlayer_play")
public func iOSVideoPlayer_play() {
    VideoPlayerBridge.shared.play()
}

@_silgen_name("iOSVideoPlayer_pause")
public func iOSVideoPlayer_pause() {
    VideoPlayerBridge.shared.pause()
}

@_silgen_name("iOSVideoPlayer_stop")
public func iOSVideoPlayer_stop() {
    VideoPlayerBridge.shared.stop()
}

@_silgen_name("iOSVideoPlayer_seek")
public func iOSVideoPlayer_seek(seconds: Double) {
    VideoPlayerBridge.shared.seek(to: seconds)
}

@_silgen_name("iOSVideoPlayer_getDuration")
public func iOSVideoPlayer_getDuration() -> Double {
    return VideoPlayerBridge.shared.getDuration()
}

@_silgen_name("iOSVideoPlayer_getCurrentTime")
public func iOSVideoPlayer_getCurrentTime() -> Double {
    return VideoPlayerBridge.shared.getCurrentTime()
}

@_silgen_name("iOSVideoPlayer_setVolume")
public func iOSVideoPlayer_setVolume(volume: Float) {
    VideoPlayerBridge.shared.setVolume(volume)
}

@_silgen_name("iOSVideoPlayer_getVolume")
public func iOSVideoPlayer_getVolume() -> Float {
    return VideoPlayerBridge.shared.getVolume()
}

@_silgen_name("iOSVideoPlayer_setRate")
public func iOSVideoPlayer_setRate(rate: Float) {
    VideoPlayerBridge.shared.setRate(rate)
}

@_silgen_name("iOSVideoPlayer_getRate")
public func iOSVideoPlayer_getRate() -> Float {
    return VideoPlayerBridge.shared.getRate()
}

@_silgen_name("iOSVideoPlayer_getNetworkQuality")
public func iOSVideoPlayer_getNetworkQuality() -> String {
    return VideoPlayerBridge.shared.getNetworkQuality()
}

// MARK: - Callback Functions (Called from Kotlin)

private var playerStatusCallback: (String, String?) -> Void = { _, _ in }
private var bufferStatusCallback: (Double) -> Void = { _ in }
private var timeUpdateCallback: (Double, Double) -> Void = { _, _ in }

@_silgen_name("iOSVideoPlayer_setPlayerStatusCallback")
public func iOSVideoPlayer_setPlayerStatusCallback(
    callback: @escaping @convention(c) (String?, String?) -> Void
) {
    playerStatusCallback = { status, error in
        callback(status, error)
    }
}

@_silgen_name("iOSVideoPlayer_setBufferStatusCallback")
public func iOSVideoPlayer_setBufferStatusCallback(
    callback: @escaping @convention(c) (Double) -> Void
) {
    bufferStatusCallback = { duration in
        callback(duration)
    }
}

@_silgen_name("iOSVideoPlayer_setTimeUpdateCallback")
public func iOSVideoPlayer_setTimeUpdateCallback(
    callback: @escaping @convention(c) (Double, Double) -> Void
) {
    timeUpdateCallback = { current, duration in
        callback(current, duration)
    }
}

// MARK: - Extensions

extension Array where Element: Equatable {
    mutating func removingDuplicates() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
