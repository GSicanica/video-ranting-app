import SwiftUI
import shared

class CommentsViewModel: ObservableObject {
    @Published var comments: [VideoComment] = []
    @Published var newCommentText: String = ""
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var errorMessage: String?
    
    private let apiClient: RatingApiClient
    private let logger = Logger.shared
    private var userToken: String
    private var isRegistering = false
    private var pendingAfterRegister: [() -> Void] = []
    let videoId: String
    let videoTitle: String
    
    init(videoId: String, videoTitle: String) {
        self.videoId = videoId
        self.videoTitle = videoTitle
        
        apiClient = RatingApiClient(
            baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
            enableDebugLogging: true,
            context: nil
        )
        
        if let storedToken = UserDefaults.standard.string(forKey: "userToken"), !storedToken.isEmpty {
            userToken = storedToken
        } else {
            userToken = ""
        }
    }

    private func ensureUserToken(onReady: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            if !self.userToken.isEmpty {
                onReady?()
                return
            }
            if let onReady {
                self.pendingAfterRegister.append(onReady)
            }
            if self.isRegistering { return }
            self.isRegistering = true

            self.apiClient.anonymousRegister { [weak self] response, error in
                guard let self else { return }
                DispatchQueue.main.async {
                    self.isRegistering = false
                    if let token = response?.userToken, !token.isEmpty {
                        self.userToken = token
                        UserDefaults.standard.set(token, forKey: "userToken")
                        let callbacks = self.pendingAfterRegister
                        self.pendingAfterRegister.removeAll()
                        callbacks.forEach { $0() }
                    } else {
                        let message = error?.localizedDescription ?? "Anonymous register failed"
                        self.errorMessage = message
                        self.pendingAfterRegister.removeAll()
                    }
                }
            }
        }
    }
    
    func loadComments() {
        ensureUserToken { [weak self] in
            self?._loadComments()
        }
    }

    private func _loadComments() {
        isLoading = true
        errorMessage = nil
        
        logger.info(tag: "CommentsViewModel", message: "Loading comments for video: \(videoId)", throwable: nil)
        
        apiClient.getVideoCommentsIos(videoId: videoId) { [weak self] commentsList in
            guard let self = self else { return }
            self.isLoading = false
            self.comments = commentsList
            self.logger.info(tag: "CommentsViewModel", message: "Loaded \(commentsList.count) comments", throwable: nil)
        } onError: { [weak self] error in
            guard let self = self else { return }
            self.isLoading = false
            self.errorMessage = error
            self.logger.error(tag: "CommentsViewModel", message: "Failed to load comments: \(error)", throwable: nil)
        }
    }
    
    func submitComment() {
        guard !newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        
        ensureUserToken { [weak self] in
            self?._submitComment()
        }
    }

    private func _submitComment() {
        isSubmitting = true
        errorMessage = nil
        
        let commentText = newCommentText.trimmingCharacters(in: .whitespacesAndNewlines)
        
        logger.info(tag: "CommentsViewModel", message: "Submitting comment for video: \(videoId)", throwable: nil)
        
        apiClient.addCommentIos(videoId: videoId, deviceId: userToken, commentText: commentText) { [weak self] response in
            guard let self = self else { return }
            self.isSubmitting = false
            
            if response.success {
                self.newCommentText = ""
                self.logger.info(tag: "CommentsViewModel", message: "Comment submitted successfully", throwable: nil)
                // Reload comments
                self.loadComments()
            } else {
                self.errorMessage = response.message ?? "Failed to submit comment"
                self.logger.error(tag: "CommentsViewModel", message: "Comment submission failed: \(response.message ?? "unknown")", throwable: nil)
            }
        } onError: { [weak self] error in
            guard let self = self else { return }
            self.isSubmitting = false
            self.errorMessage = error
            self.logger.error(tag: "CommentsViewModel", message: "Comment submission error: \(error)", throwable: nil)
        }
    }
    
    func refresh() {
        loadComments()
    }
}
