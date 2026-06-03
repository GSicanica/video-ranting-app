import Foundation
import shared

class VideoDetailsViewModel: ObservableObject {
    @Published var videoStats: VideoStats?
    @Published var comments: [VideoComment] = []
    @Published var isLoading = false
    @Published var isLoadingComments = false
    @Published var errorMessage: String?
    @Published var showRatingView = false
    
    private let apiClient: RatingApiClient
    private let logger: Logger
    
    init() {
        self.apiClient = RatingApiClient(
            baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
            enableDebugLogging: true,
            context: nil
        )
        self.logger = Logger()
    }
    
    func loadVideoDetails(videoId: String) {
        isLoading = true
        errorMessage = nil
        
        logger.info(
            tag: "VideoDetailsViewModel",
            message: "Loading details for video: \(videoId)",
            throwable: nil
        )
        
        apiClient.getAllVideosIos(
            onSuccess: { (videos: [VideoStats]) in
                DispatchQueue.main.async {
                    self.isLoading = false
                    if let video = videos.first(where: { $0.videoId == videoId }) {
                        self.videoStats = video
                        self.loadComments(videoId: videoId)
                    } else {
                        self.errorMessage = "Video not found"
                        self.logger.error(
                            tag: "VideoDetailsViewModel",
                            message: "Video not found: \(videoId)",
                            throwable: nil
                        )
                    }
                }
            },
            onError: { error in
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = error
                    self.logger.error(
                        tag: "VideoDetailsViewModel",
                        message: "Failed to load video details: \(error)",
                        throwable: nil
                    )
                }
            }
        )
    }
    
    func loadComments(videoId: String) {
        isLoadingComments = true
        
        apiClient.getVideoCommentsIos(
            videoId: videoId,
            onSuccess: { (comments: [VideoComment]) in
                DispatchQueue.main.async {
                    self.isLoadingComments = false
                    self.comments = comments
                    self.logger.info(
                        tag: "VideoDetailsViewModel",
                        message: "Loaded \(comments.count) comments",
                        throwable: nil
                    )
                }
            },
            onError: { error in
                DispatchQueue.main.async {
                    self.isLoadingComments = false
                    self.logger.error(
                        tag: "VideoDetailsViewModel",
                        message: "Failed to load comments: \(error)",
                        throwable: nil
                    )
                }
            }
        )
    }
    
    func refresh(videoId: String) {
        loadVideoDetails(videoId: videoId)
    }
    
    func getCategoryDisplayName(_ category: String?) -> String {
        guard let category = category else { return "Unknown" }
        switch category {
        case "Thanks": return "Thanks"
        case "teaching": return "Teaching"
        case "testimony": return "Testimony"
        case "bible": return l10n("bible_title")
        case "markofbeast": return "Mark of Beast"
        default: return category.capitalized
        }
    }
}
