import SwiftUI
import shared

final class HomeViewModel: ObservableObject {
    enum FeedTab {
        case browse
        case forYou
    }

    @Published var videos: [VideoSearchResult] = []
    @Published var featuredVideos: [VideoSearchResult] = []
    @Published var forYouVideos: [VideoSearchResult] = []
    @Published var searchSuggestions: [String] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var isFeaturedLoading = false
    @Published var isForYouLoading = false
    @Published var errorMessage: String?
    @Published var forYouError: String?
    @Published var isForYouColdStart = false
    @Published var selectedFeedTab: FeedTab = .browse

    @Published var searchQuery: String = ""
    @Published var selectedCategory: String?
    @Published var sortBy: String = "latest"
    @Published var minLove: Int = 0
    @Published var minFaith: Int = 0
    @Published var minHope: Int = 0
    @Published var selectedLanguages: Set<String> = ["hr"]
    @Published var isGridView: Bool = true

    @Published var currentPage: Int = 1
    @Published var hasMore: Bool = true
    @Published var totalResults: Int = 0

    private let apiClient: RatingApiClient
    private let logger = Logger.shared
    private var hasLoaded = false
    private var searchDebounceWorkItem: DispatchWorkItem?
    private var userToken: String
    private var isRegistering = false
    private var pendingAfterRegister: [() -> Void] = []

    private let defaults = UserDefaults.standard
    private let defaultsKeyCategory = "ios_home_category"
    private let defaultsKeySort = "ios_home_sort"
    private let defaultsKeyLanguages = "ios_home_languages"
    private let defaultsKeyFilters = "ios_home_filters"
    private let pageSize = 20

    init() {
        apiClient = RatingApiClient(
            baseUrl: "https://tmbv-hms.com/aYOUTUBEocjenivanje",
            enableDebugLogging: true,
            context: nil
        )

        if let storedToken = UserDefaults.standard.string(forKey: "userToken"), !storedToken.isEmpty {
            self.userToken = storedToken
        } else {
            self.userToken = ""
        }

        restorePreferences()
    }

    func loadInitialIfNeeded() {
        guard !hasLoaded else { return }
        hasLoaded = true
        refresh(forceRefresh: false)
        loadFeaturedVideos()
        loadSearchSuggestions()
        loadForYou(forceRefresh: false)
    }

    func refresh(forceRefresh: Bool = false) {
        currentPage = 1
        hasMore = true
        loadBrowseVideos(resetPage: true, forceRefresh: forceRefresh)
    }

    func loadMore() {
        guard hasMore, !isLoadingMore, !isLoading else { return }
        loadBrowseVideos(resetPage: false, forceRefresh: false)
    }

    func updateSearchQuery(_ query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed == searchQuery { return }
        searchQuery = trimmed

        searchDebounceWorkItem?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            self?.refresh(forceRefresh: false)
        }
        searchDebounceWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35, execute: workItem)
    }

    func applyFilters() {
        persistPreferences()
        refresh(forceRefresh: false)
        loadFeaturedVideos()
        loadSearchSuggestions()
        if selectedFeedTab == .forYou {
            loadForYou(forceRefresh: true)
        }
    }

    func selectFeedTab(_ tab: FeedTab) {
        selectedFeedTab = tab
        if tab == .forYou && forYouVideos.isEmpty {
            loadForYou(forceRefresh: false)
        }
    }

    func toggleLanguage(_ code: String) {
        if selectedLanguages.contains(code) {
            if selectedLanguages.count <= 1 { return }
            selectedLanguages.remove(code)
        } else {
            selectedLanguages.insert(code)
        }
        applyFilters()
    }

    private func loadBrowseVideos(resetPage: Bool, forceRefresh: Bool) {
        let page = resetPage ? 1 : currentPage + 1
        let query = searchQuery.isEmpty ? nil : searchQuery
        let languages = resolvedLanguageCodes()

        if resetPage {
            isLoading = true
            errorMessage = nil
        } else {
            isLoadingMore = true
        }

        logger.info(
            tag: "HomeViewModel",
            message: "Home browse: q=\(query ?? "all"), cat=\(selectedCategory ?? "all"), sort=\(sortBy), page=\(page)",
            throwable: nil
        )

        let minLoveValue = max(1, minLove)
        let minFaithValue = max(1, minFaith)
        let minHopeValue = max(1, minHope)

        apiClient.searchVideosV2Ios(
            searchQuery: query,
            minLove: Int32(minLoveValue),
            maxLove: 3,
            minFaith: Int32(minFaithValue),
            maxFaith: 3,
            minHope: Int32(minHopeValue),
            maxHope: 3,
            category: selectedCategory,
            sortBy: sortBy,
            languages: languages,
            page: Int32(page),
            perPage: Int32(pageSize),
            forceRefresh: forceRefresh,
            onSuccess: { [weak self] response in
                guard let self = self else { return }
                if resetPage {
                    self.videos = response.videos
                } else {
                    let existingIds = Set(self.videos.map { $0.videoId })
                    let newVideos = response.videos.filter { !existingIds.contains($0.videoId) }
                    self.videos.append(contentsOf: newVideos)
                }

                self.currentPage = page
                self.hasMore = response.hasMore
                self.totalResults = Int(response.total)
                self.isLoading = false
                self.isLoadingMore = false
            },
            onError: { [weak self] error in
                guard let self = self else { return }
                self.errorMessage = error
                self.isLoading = false
                self.isLoadingMore = false
            }
        )
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
                        self.forYouError = error?.localizedDescription ?? "Unable to load For You"
                        self.pendingAfterRegister.removeAll()
                    }
                }
            }
        }
    }

    func loadForYou(forceRefresh: Bool) {
        ensureUserToken { [weak self] in
            guard let self else { return }
            self.isForYouLoading = true
            self.forYouError = nil

            // Fallback while the packaged shared XCFramework does not expose personalized feed bridge.
            self.apiClient.searchVideosV2Ios(
                searchQuery: nil,
                minLove: 1,
                maxLove: 3,
                minFaith: 1,
                maxFaith: 3,
                minHope: 1,
                maxHope: 3,
                category: self.selectedCategory,
                sortBy: "popular",
                languages: self.resolvedLanguageCodes(),
                page: 1,
                perPage: 50,
                forceRefresh: forceRefresh,
                onSuccess: { [weak self] response in
                    guard let self else { return }
                    DispatchQueue.main.async {
                        self.isForYouLoading = false
                        self.forYouVideos = response.videos
                        // Mark as cold start because this path is popularity-based fallback.
                        self.isForYouColdStart = true
                        self.forYouError = nil
                    }
                },
                onError: { [weak self] error in
                    guard let self else { return }
                    DispatchQueue.main.async {
                        self.isForYouLoading = false
                        self.forYouVideos = []
                        self.isForYouColdStart = false
                        self.forYouError = error
                    }
                }
            )
        }
    }

    private func loadFeaturedVideos() {
        isFeaturedLoading = true
        let languages = resolvedLanguageCodes().filter { $0 != "unknown" }
        let language = languages.count == 1 ? languages.first : nil

        apiClient.getTopVideosIos(
            type: "popular",
            category: selectedCategory,
            language: language,
            limit: 10,
            onSuccess: { [weak self] response in
                guard let self = self else { return }
                self.featuredVideos = response.videos
                self.isFeaturedLoading = false
            },
            onError: { [weak self] _ in
                guard let self = self else { return }
                self.isFeaturedLoading = false
            }
        )
    }

    private func loadSearchSuggestions() {
        let language = resolvedLanguageCodes().first(where: { $0 != "unknown" }) ?? "hr"
        apiClient.getPopularSearchTermsIos(
            limit: 6,
            language: language,
            onSuccess: { [weak self] terms in
                self?.searchSuggestions = terms
            },
            onError: { _ in }
        )
    }

    private func resolvedLanguageCodes() -> [String] {
        var codes = Array(selectedLanguages)
        if !codes.contains("unknown") {
            codes.append("unknown")
        }
        return codes
    }

    private func restorePreferences() {
        if let storedCategory = defaults.string(forKey: defaultsKeyCategory) {
            selectedCategory = storedCategory.isEmpty ? nil : storedCategory
        }
        if let storedSort = defaults.string(forKey: defaultsKeySort), !storedSort.isEmpty {
            sortBy = storedSort
        }
        if let storedLangs = defaults.array(forKey: defaultsKeyLanguages) as? [String], !storedLangs.isEmpty {
            selectedLanguages = Set(storedLangs)
        }
        if let data = defaults.data(forKey: defaultsKeyFilters),
           let stored = try? JSONDecoder().decode([String: Int].self, from: data) {
            minLove = stored["minLove"] ?? 0
            minFaith = stored["minFaith"] ?? 0
            minHope = stored["minHope"] ?? 0
        }
    }

    private func persistPreferences() {
        defaults.set(selectedCategory ?? "", forKey: defaultsKeyCategory)
        defaults.set(sortBy, forKey: defaultsKeySort)
        defaults.set(Array(selectedLanguages), forKey: defaultsKeyLanguages)
        let filters: [String: Int] = [
            "minLove": minLove,
            "minFaith": minFaith,
            "minHope": minHope
        ]
        if let data = try? JSONEncoder().encode(filters) {
            defaults.set(data, forKey: defaultsKeyFilters)
        }
    }
}
