import SwiftUI
import shared

class SearchViewModel: ObservableObject {
    @Published var searchQuery: String = ""
    @Published var videos: [VideoSearchResult] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var selectedCategory: String?
    @Published var sortBy: String = "latest"
    @Published var currentPage: Int = 1
    @Published var hasMore: Bool = true
    
    private let apiClient: RatingApiClient
    private let logger = Logger.shared
    
    let categories = [
        (l10n("all"), nil),
        (l10n("thanks"), "Thanks"),
        (l10n("teaching"), "Teaching"),
        (l10n("testimony"), "Testimony"),
        (l10n("bible"), "Bible"),
        (l10n("mark_of_beast"), "MarkOfBeast")
    ]
    
    let sortOptions = [
        (l10n("latest"), "latest"),
        (l10n("sort_best_rated"), "popular"),
        (l10n("sort_highest_love"), "love"),
        (l10n("sort_highest_faith"), "faith"),
        (l10n("sort_highest_hope"), "hope")
    ]
    
    init() {
        apiClient = RatingApiClient(
            baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
            enableDebugLogging: true,
            context: nil
        )
    }
    
    func search(loadMore: Bool = false) {
        if isLoading { return }
        
        if !loadMore {
            currentPage = 1
            videos = []
        }
        
        isLoading = true
        errorMessage = nil
        
        let query = searchQuery.isEmpty ? nil : searchQuery
        
        logger.info(tag: "SearchViewModel", message: "Searching: query=\(query ?? "all"), category=\(selectedCategory ?? "all"), sort=\(sortBy), page=\(currentPage)", throwable: nil)
        
        apiClient.searchVideosIos(
            searchQuery: query,
            page: Int32(currentPage),
            perPage: 20,
            sortBy: sortBy,
            category: selectedCategory,
            languages: []
        ) { [weak self] response in
            guard let self = self else { return }
            self.isLoading = false
            
            if loadMore {
                self.videos.append(contentsOf: response.videos)
            } else {
                self.videos = response.videos
            }
            
            self.hasMore = response.hasMore
            self.logger.info(tag: "SearchViewModel", message: "Found \(response.videos.count) videos, hasMore: \(response.hasMore)", throwable: nil)
        } onError: { [weak self] error in
            guard let self = self else { return }
            self.isLoading = false
            self.errorMessage = error
            self.logger.error(tag: "SearchViewModel", message: "Search error: \(error)", throwable: nil)
        }
    }
    
    func loadMore() {
        guard hasMore && !isLoading else { return }
        currentPage += 1
        search(loadMore: true)
    }
    
    func refresh() {
        currentPage = 1
        hasMore = true
        search()
    }
}
