import SwiftUI
import shared

class FavoritesViewModel: ObservableObject {
    @Published var favoriteVideos: [VideoStats] = []
    @Published var favorites: Set<String> = [] // Video IDs
    @Published var isLoading = false
    @Published var isSyncing = false
    @Published var errorMessage: String?
    
    private let apiClient = RatingApiClient(
        baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
        enableDebugLogging: true,
        context: nil
    )
    private let logger = Logger()
    private let deviceId: String
    private let favoritesKey = "favoriteVideos"
    
    init() {
        if let storedDeviceId = UserDefaults.standard.string(forKey: "deviceId") {
            self.deviceId = storedDeviceId
        } else {
            let newDeviceId = UUID().uuidString
            UserDefaults.standard.set(newDeviceId, forKey: "deviceId")
            self.deviceId = newDeviceId
        }
        
        loadLocalFavorites()
    }
    
    func loadLocalFavorites() {
        if let data = UserDefaults.standard.data(forKey: favoritesKey),
           let savedFavorites = try? JSONDecoder().decode(Set<String>.self, from: data) {
            self.favorites = savedFavorites
            logger.info(
                tag: "FavoritesViewModel",
                message: "Loaded \(savedFavorites.count) favorites from local storage",
                throwable: nil
            )
        }
    }
    
    func saveLocalFavorites() {
        if let data = try? JSONEncoder().encode(favorites) {
            UserDefaults.standard.set(data, forKey: favoritesKey)
            logger.info(
                tag: "FavoritesViewModel",
                message: "Saved \(favorites.count) favorites to local storage",
                throwable: nil
            )
        }
    }
    
    func toggleFavorite(videoId: String) {
        if favorites.contains(videoId) {
            favorites.remove(videoId)
        } else {
            favorites.insert(videoId)
        }
        saveLocalFavorites()
        syncFavorites()
    }
    
    func isFavorite(videoId: String) -> Bool {
        return favorites.contains(videoId)
    }
    
    func loadFavoriteVideos() {
        guard !favorites.isEmpty else {
            self.favoriteVideos = []
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        // Load all videos and filter favorites
        apiClient.getAllVideosIos(
            onSuccess: { (videos: [VideoStats]) in
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.favoriteVideos = videos.filter { video in
                        self.favorites.contains(video.videoId)
                    }
                    
                    self.logger.info(
                        tag: "FavoritesViewModel",
                        message: "Loaded \(self.favoriteVideos.count) favorite videos",
                        throwable: nil
                    )
                }
            },
            onError: { (error: String) in
                DispatchQueue.main.async {
                    self.isLoading = false
                    self.errorMessage = error
                    self.logger.error(
                        tag: "FavoritesViewModel",
                        message: "Error loading videos: \(error)",
                        throwable: nil
                    )
                }
            }
        )
    }
    
    func syncFavorites() {
        isSyncing = true
        
        let favoriteDtos = favorites.map { videoId in
            FavoriteVideoDto(
                videoId: videoId,
                title: "",
                thumbnail: nil,
                channelName: nil,
                avgLove: 0.0,
                avgFaith: 0.0,
                avgHope: 0.0,
                totalRatings: 0,
                category: nil,
                timestamp: Int64(Date().timeIntervalSince1970 * 1000)
            )
        }
        
        apiClient.syncFavoritesIos(
            deviceId: deviceId,
            favorites: favoriteDtos,
            onSuccess: { (response: SyncFavoritesResponse) in
                DispatchQueue.main.async {
                    self.isSyncing = false
                    
                    if response.success {
                        // Update local favorites with server data
                        let serverFavorites = Set(response.favorites.map { $0.videoId })
                        self.favorites = serverFavorites
                        self.saveLocalFavorites()
                        
                        self.logger.info(
                            tag: "FavoritesViewModel",
                            message: "Synced favorites: \(response.message), count: \(response.count)",
                            throwable: nil
                        )
                        
                        // Reload favorite videos
                        self.loadFavoriteVideos()
                    } else {
                        self.errorMessage = response.message
                        self.logger.error(
                            tag: "FavoritesViewModel",
                            message: "Sync failed: \(response.message)",
                            throwable: nil
                        )
                    }
                }
            },
            onError: { (error: String) in
                DispatchQueue.main.async {
                    self.isSyncing = false
                    self.errorMessage = error
                    self.logger.error(
                        tag: "FavoritesViewModel",
                        message: "Error syncing favorites: \(error)",
                        throwable: nil
                    )
                }
            }
        )
    }
    
    func refresh() {
        syncFavorites()
    }
}
