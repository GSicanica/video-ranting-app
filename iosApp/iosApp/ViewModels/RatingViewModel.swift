import SwiftUI
import shared

class RatingViewModel: ObservableObject {
    @Published var videoUrl: String = ""
    @Published var videoInfo: YouTubeVideoInfo?
    @Published var loveRating: Int32 = 0
    @Published var faithRating: Int32 = 0
    @Published var hopeRating: Int32 = 0
    @Published var comment: String = ""
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    @Published var submitSuccess = false
    
    private let apiClient: RatingApiClient
    private let logger = Logger.shared
    private let deviceId: String
    
    var canSubmit: Bool {
        return videoInfo != nil && 
               loveRating > 0 && 
               faithRating > 0 && 
               hopeRating > 0 &&
               !isSubmitting
    }
    
    init() {
        apiClient = RatingApiClient(
            baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
            enableDebugLogging: true,
            context: nil
        )
        
        // Get or create device ID
        if let savedDeviceId = UserDefaults.standard.string(forKey: "deviceId") {
            deviceId = savedDeviceId
        } else {
            deviceId = UUID().uuidString
            UserDefaults.standard.set(deviceId, forKey: "deviceId")
        }
    }
    
    func extractVideoId() -> String? {
        // Extract video ID from various YouTube URL formats
        let patterns = [
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([^&?/]+)",
            "youtube\\.com/embed/([^&?/]+)",
            "youtube\\.com/v/([^&?/]+)"
        ]
        
        for pattern in patterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) {
                let range = NSRange(videoUrl.startIndex..<videoUrl.endIndex, in: videoUrl)
                if let match = regex.firstMatch(in: videoUrl, range: range) {
                    if let videoIdRange = Range(match.range(at: 1), in: videoUrl) {
                        return String(videoUrl[videoIdRange])
                    }
                }
            }
        }
        return nil
    }
    
    func loadVideoInfo() {
        guard let videoId = extractVideoId() else {
            errorMessage = "Invalid YouTube URL"
            return
        }
        
        isLoading = true
        errorMessage = nil
        videoInfo = nil
        
        logger.info(tag: "RatingViewModel", message: "Loading video info for: \(videoId)", throwable: nil)
        
        apiClient.getYouTubeVideoInfoIos(videoId: videoId) { [weak self] info in
            guard let self = self else { return }
            self.isLoading = false
            self.videoInfo = info
            self.logger.info(tag: "RatingViewModel", message: "Loaded video: \(info.title)", throwable: nil)
        } onError: { [weak self] error in
            guard let self = self else { return }
            self.isLoading = false
            self.errorMessage = error
            self.logger.error(tag: "RatingViewModel", message: "Failed to load video: \(error)", throwable: nil)
        }
    }
    
    func clearVideo() {
        videoInfo = nil
        loveRating = 0
        faithRating = 0
        hopeRating = 0
        comment = ""
        errorMessage = nil
        submitSuccess = false
    }
    
    func submitRating() {
        guard let videoInfo = videoInfo else { return }
        guard canSubmit else { return }
        
        isSubmitting = true
        errorMessage = nil
        submitSuccess = false
        
        let rating = RatingRequest(
            videoId: videoInfo.videoId,
            videoTitle: videoInfo.title,
            videoThumbnail: videoInfo.thumbnail,
            videoChannel: videoInfo.channelName,
            platform: "youtube",
            videoUrl: videoUrl.isEmpty ? nil : videoUrl,
            love: Int32(loveRating),
            faith: Int32(faithRating),
            hope: Int32(hopeRating),
            category: nil,
            userToken: deviceId,
            language: videoInfo.language
        )
        
        logger.info(tag: "RatingViewModel", message: "Submitting rating for: \(videoInfo.videoId)", throwable: nil)
        
        apiClient.submitRatingIos(rating: rating) { [weak self] response in
            guard let self = self else { return }
            self.isSubmitting = false
            
            if response.success {
                self.submitSuccess = true
                self.logger.info(tag: "RatingViewModel", message: "Rating submitted successfully", throwable: nil)
                
                // Clear form after 2 seconds
                DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                    self.clearVideo()
                    self.videoUrl = ""
                }
            } else {
                self.errorMessage = response.message ?? "Failed to submit rating"
                self.logger.error(tag: "RatingViewModel", message: "Rating submission failed: \(response.message ?? "unknown")", throwable: nil)
            }
        } onError: { [weak self] error in
            guard let self = self else { return }
            self.isSubmitting = false
            self.errorMessage = error
            self.logger.error(tag: "RatingViewModel", message: "Rating submission error: \(error)", throwable: nil)
        }
    }

    func prefillVideoId(_ videoId: String) {
        if videoInfo?.videoId == videoId { return }
        videoUrl = "https://www.youtube.com/watch?v=\(videoId)"
        loadVideoInfo()
    }
}
