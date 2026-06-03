import Foundation
import shared

class RatedVideosViewModel: ObservableObject {
    @Published var ratedVideos: [RatedVideo] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    @Published var selectedCategory = "all"
    @Published var sortBy = "latest"
    @Published var currentPage = 1
    @Published var hasMore = true
    
    private let apiClient = RatingApiClient(
        baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
        enableDebugLogging: true,
        context: nil
    )
    private let logger = Logger()
    private var userToken: String
    private var isRegistering = false
    private var pendingAfterRegister: [() -> Void] = []
    
    init() {
        if let storedToken = UserDefaults.standard.string(forKey: "userToken"), !storedToken.isEmpty {
            self.userToken = storedToken
        } else {
            self.userToken = ""
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
                        self.logger.info(tag: "RatedVideosViewModel", message: "Anonymous register ok", throwable: nil)
                        let callbacks = self.pendingAfterRegister
                        self.pendingAfterRegister.removeAll()
                        callbacks.forEach { $0() }
                    } else {
                        let message = error?.localizedDescription ?? "Anonymous register failed"
                        self.logger.error(tag: "RatedVideosViewModel", message: message, throwable: nil)
                        self.errorMessage = message
                        self.pendingAfterRegister.removeAll()
                    }
                }
            }
        }
    }
    
    func loadRatedVideos(loadMore: Bool = false) {
        ensureUserToken { [weak self] in
            self?._loadRatedVideos(loadMore: loadMore)
        }
    }

    private func _loadRatedVideos(loadMore: Bool = false) {
        if loadMore {
            if !hasMore || isLoading { return }
            currentPage += 1
        } else {
            currentPage = 1
            ratedVideos = []
        }
        
        isLoading = true
        errorMessage = nil
        
        let page = Int32(currentPage)
        let perPage: Int32 = 20
        let category = selectedCategory == "all" ? nil : selectedCategory
        
        apiClient.getRatedVideosIos(
            deviceId: userToken,
            page: page,
            perPage: perPage,
            category: category,
            sortBy: sortBy,
            onSuccess: { (response: RatedVideosResponse) in
                DispatchQueue.main.async {
                    self.isLoading = false
                    
                    if response.success {
                        if loadMore {
                            self.ratedVideos.append(contentsOf: response.data)
                        } else {
                            self.ratedVideos = response.data
                        }
                        
                        if let pagination = response.pagination {
                            self.hasMore = Int(pagination.page) < Int(pagination.totalPages)
                        } else {
                            self.hasMore = false
                        }
                        
                        self.logger.info(
                            tag: "RatedVideosViewModel",
                            message: "Loaded \(response.data.count) rated videos, page \(self.currentPage), hasMore: \(self.hasMore)",
                            throwable: nil
                        )
                    } else {
                        self.errorMessage = "Failed to load rated videos"
                        self.logger.error(
                            tag: "RatedVideosViewModel",
                            message: "Load rated videos failed",
                            throwable: nil
                        )
                    }
                }
            },
            onError: { (error: String) in
                DispatchQueue.main.async {
                    self.isLoading = false
                    if error.lowercased().contains("invalid or expired user token") {
                        UserDefaults.standard.removeObject(forKey: "userToken")
                        self.userToken = ""
                        self.ensureUserToken { [weak self] in
                            self?._loadRatedVideos(loadMore: loadMore)
                        }
                        return
                    }
                    self.errorMessage = error
                    self.logger.error(
                        tag: "RatedVideosViewModel",
                        message: "Error loading rated videos: \(error)",
                        throwable: nil
                    )
                }
            }
        )
    }
    
    func refresh() {
        currentPage = 1
        hasMore = true
        loadRatedVideos()
    }
    
    func changeCategoryAndSort(category: String, sort: String) {
        selectedCategory = category
        sortBy = sort
        refresh()
    }
}
