import SwiftUI
import UIKit
import shared

struct HomeScreen: View {
    @StateObject private var viewModel = HomeViewModel()
    @EnvironmentObject var favoritesViewModel: FavoritesViewModel
    @State private var showFiltersSheet = false
    @State private var ratingSheetItem: RatingSheetItem?

    private let categoryOptions: [(String, String?)] = [
        (l10n("all"), nil),
        (l10n("thanks"), "Thanks"),
        (l10n("teaching"), "Teaching"),
        (l10n("testimony"), "Testimony"),
        (l10n("bible"), "Bible")
    ]

    private let sortOptions: [(String, String)] = [
        (l10n("latest"), "latest"),
        (l10n("sort_best_rated"), "popular"),
        (l10n("sort_highest_love"), "love"),
        (l10n("sort_highest_faith"), "faith"),
        (l10n("sort_highest_hope"), "hope"),
        (l10n("sort_highest_total"), "total")
    ]

    private let languages: [(String, String)] = [
        (l10n("croatian"), "hr"),
        (l10n("english"), "en"),
        (l10n("german"), "de"),
        ("Српски", "sr")
    ]

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(hex: "0F1419"),
                    Color(hex: "1A1E23"),
                    Color(hex: "13171C")
                ]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            content
        }
        .navigationTitle(l10n("home"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showFiltersSheet = true }) {
                    Image(systemName: "line.horizontal.3.decrease.circle")
                        .foregroundColor(Color(hex: "00D4FF"))
                }
            }
        }
        .sheet(isPresented: $showFiltersSheet) {
            HomeFiltersSheet(
                selectedCategory: $viewModel.selectedCategory,
                sortBy: $viewModel.sortBy,
                minLove: $viewModel.minLove,
                minFaith: $viewModel.minFaith,
                minHope: $viewModel.minHope,
                selectedLanguages: $viewModel.selectedLanguages,
                categoryOptions: categoryOptions,
                sortOptions: sortOptions,
                languages: languages,
                onApply: { viewModel.applyFilters() }
            )
        }
        .sheet(item: $ratingSheetItem) { item in
            NavigationView {
                RatingView(
                    viewModel: RatingViewModel(),
                    prefilledVideoId: item.id
                )
            }
        }
        .onAppear { viewModel.loadInitialIfNeeded() }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.selectedFeedTab == .browse && viewModel.isLoading && viewModel.videos.isEmpty {
            LoadingStateView(message: l10n("loading_videos"))
        } else if viewModel.selectedFeedTab == .browse, let error = viewModel.errorMessage, viewModel.videos.isEmpty {
            ErrorStateView(error: error, retryAction: { viewModel.refresh(forceRefresh: true) })
        } else if viewModel.selectedFeedTab == .forYou, viewModel.isForYouLoading && viewModel.forYouVideos.isEmpty {
            LoadingStateView(message: "Loading For You")
        } else if viewModel.selectedFeedTab == .forYou, let error = viewModel.forYouError, viewModel.forYouVideos.isEmpty {
            ErrorStateView(error: error, retryAction: { viewModel.loadForYou(forceRefresh: true) })
        } else {
            ScrollView {
                LazyVStack(spacing: 16) {
                    feedModeRow

                    if viewModel.selectedFeedTab == .browse {
                        searchBar

                        if !activeFilters.isEmpty {
                            ActiveFiltersView(items: activeFilters)
                        }

                        if !viewModel.searchSuggestions.isEmpty {
                            SuggestionChipsView(
                                suggestions: filteredSuggestions,
                                onSelect: { suggestion in
                                    viewModel.updateSearchQuery(suggestion)
                                }
                            )
                        }

                        if !viewModel.featuredVideos.isEmpty {
                            FeaturedRow(videos: viewModel.featuredVideos, onRate: { id in
                                ratingSheetItem = RatingSheetItem(id: id)
                            })
                        }

                        filterRow

                        if viewModel.videos.isEmpty {
                            EmptyStateView(message: l10n("no_videos_found_generic"))
                        } else if viewModel.isGridView {
                            HomeVideoGrid(
                                videos: viewModel.videos,
                                favoritesViewModel: favoritesViewModel,
                                onRate: { id in ratingSheetItem = RatingSheetItem(id: id) }
                            )
                        } else {
                            HomeVideoList(
                                videos: viewModel.videos,
                                favoritesViewModel: favoritesViewModel,
                                onRate: { id in ratingSheetItem = RatingSheetItem(id: id) }
                            )
                        }

                        if viewModel.hasMore {
                            LoadMoreButton(
                                isLoading: viewModel.isLoadingMore,
                                onTap: { viewModel.loadMore() }
                            )
                        }
                    } else {
                        ForYouHeaderView(isColdStart: viewModel.isForYouColdStart)

                        if viewModel.forYouVideos.isEmpty {
                            EmptyStateView(message: "No personalized videos yet")
                        } else if viewModel.isGridView {
                            HomeVideoGrid(
                                videos: viewModel.forYouVideos,
                                favoritesViewModel: favoritesViewModel,
                                onRate: { id in ratingSheetItem = RatingSheetItem(id: id) }
                            )
                        } else {
                            HomeVideoList(
                                videos: viewModel.forYouVideos,
                                favoritesViewModel: favoritesViewModel,
                                onRate: { id in ratingSheetItem = RatingSheetItem(id: id) }
                            )
                        }
                    }
                }
                .padding(.bottom, 100)
            }
            .refreshable {
                if viewModel.selectedFeedTab == .forYou {
                    viewModel.loadForYou(forceRefresh: true)
                } else {
                    viewModel.refresh(forceRefresh: true)
                }
            }
        }
    }

    private var feedModeRow: some View {
        HStack(spacing: 10) {
            Button(action: { viewModel.selectFeedTab(.browse) }) {
                Text("Browse")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(viewModel.selectedFeedTab == .browse ? Color(hex: "00D4FF").opacity(0.24) : Color.white.opacity(0.08))
                    .foregroundColor(viewModel.selectedFeedTab == .browse ? Color(hex: "00D4FF") : .white)
                    .clipShape(Capsule())
            }

            Button(action: { viewModel.selectFeedTab(.forYou) }) {
                Text("For You")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(viewModel.selectedFeedTab == .forYou ? Color(hex: "00D4FF").opacity(0.24) : Color.white.opacity(0.08))
                    .foregroundColor(viewModel.selectedFeedTab == .forYou ? Color(hex: "00D4FF") : .white)
                    .clipShape(Capsule())
            }
            Spacer()
        }
        .padding(.horizontal)
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)

            TextField(
                l10n("search_videos"),
                text: Binding(
                    get: { viewModel.searchQuery },
                    set: { viewModel.updateSearchQuery($0) }
                )
            )
            .textFieldStyle(.plain)

            if !viewModel.searchQuery.isEmpty {
                Button(action: { viewModel.updateSearchQuery("") }) {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.white.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.white.opacity(0.15), lineWidth: 1)
                )
        )
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private var filterRow: some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Menu {
                    ForEach(categoryOptions, id: \.0) { option in
                        Button(action: {
                            viewModel.selectedCategory = option.1
                            viewModel.applyFilters()
                        }) {
                            HStack {
                                Text(option.0)
                                if viewModel.selectedCategory == option.1 {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    FilterPill(
                        title: categoryOptions.first(where: { $0.1 == viewModel.selectedCategory })?.0 ?? l10n("all"),
                        systemImage: "square.grid.2x2"
                    )
                }

                Menu {
                    ForEach(sortOptions, id: \.1) { option in
                        Button(action: {
                            viewModel.sortBy = option.1
                            viewModel.applyFilters()
                        }) {
                            HStack {
                                Text(option.0)
                                if viewModel.sortBy == option.1 {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    FilterPill(
                        title: sortOptions.first(where: { $0.1 == viewModel.sortBy })?.0 ?? l10n("latest"),
                        systemImage: "arrow.up.arrow.down"
                    )
                }

                Spacer()

                Button(action: { viewModel.isGridView.toggle() }) {
                    Image(systemName: viewModel.isGridView ? "rectangle.grid.2x2" : "list.bullet")
                        .foregroundColor(Color(hex: "00D4FF"))
                        .padding(10)
                        .background(Color.white.opacity(0.08))
                        .clipShape(Circle())
                }

                Button(action: { showFiltersSheet = true }) {
                    Image(systemName: "slider.horizontal.3")
                        .foregroundColor(Color(hex: "00D4FF"))
                        .padding(10)
                        .background(Color.white.opacity(0.08))
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(languages, id: \.1) { language in
                        let isSelected = viewModel.selectedLanguages.contains(language.1)
                        Button(action: { viewModel.toggleLanguage(language.1) }) {
                            Text(language.0)
                                .font(.caption)
                                .fontWeight(isSelected ? .semibold : .regular)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(isSelected ? Color(hex: "00D4FF").opacity(0.2) : Color.white.opacity(0.08))
                                .foregroundColor(isSelected ? Color(hex: "00D4FF") : Color.white.opacity(0.8))
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    private var activeFilters: [String] {
        var items: [String] = []
        if let cat = categoryOptions.first(where: { $0.1 == viewModel.selectedCategory })?.0, viewModel.selectedCategory != nil {
            items.append("\(l10n("category_label")) \(cat)")
        }
        if viewModel.minLove > 0 { items.append("\(l10n("love")) ≥ \(viewModel.minLove)") }
        if viewModel.minFaith > 0 { items.append("\(l10n("faith")) ≥ \(viewModel.minFaith)") }
        if viewModel.minHope > 0 { items.append("\(l10n("hope")) ≥ \(viewModel.minHope)") }
        if viewModel.selectedLanguages != Set(["hr"]) {
            let labels = languages.filter { viewModel.selectedLanguages.contains($0.1) }.map { $0.0 }
            if !labels.isEmpty { items.append("\(l10n("languages_label")) \(labels.joined(separator: ", "))") }
        }
        return items
    }

    private var filteredSuggestions: [String] {
        guard !viewModel.searchQuery.isEmpty else { return viewModel.searchSuggestions }
        return viewModel.searchSuggestions.filter { $0.localizedCaseInsensitiveContains(viewModel.searchQuery) }
    }
}

private struct RatingSheetItem: Identifiable {
    let id: String
}

private struct FilterPill: View {
    let title: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: systemImage)
            Text(title)
        }
        .font(.system(size: 12, weight: .semibold))
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color.white.opacity(0.08))
        .foregroundColor(.white)
        .clipShape(Capsule())
    }
}

private struct ActiveFiltersView: View {
    let items: [String]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(items, id: \.self) { item in
                    Text(item)
                        .font(.caption2)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(hex: "00D4FF").opacity(0.2))
                        .foregroundColor(Color(hex: "00D4FF"))
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal)
        }
    }
}

private struct SuggestionChipsView: View {
    let suggestions: [String]
    let onSelect: (String) -> Void

    private let columns = [GridItem(.adaptive(minimum: 90), spacing: 8)]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 8) {
            ForEach(suggestions, id: \.self) { suggestion in
                Button(action: { onSelect(suggestion) }) {
                    Text(suggestion)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.08))
                        .foregroundColor(.white)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(.horizontal)
    }
}

private struct FeaturedRow: View {
    let videos: [VideoSearchResult]
    let onRate: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(l10n("featured"))
                .font(.headline)
                .foregroundColor(.white)
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(videos, id: \.videoId) { video in
                        FeaturedVideoCard(video: video, onRate: onRate)
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

private struct FeaturedVideoCard: View {
    let video: VideoSearchResult
    let onRate: (String) -> Void

    var body: some View {
        NavigationLink(destination: VideoDetailsScreen(videoId: video.videoId)) {
            VStack(alignment: .leading, spacing: 8) {
                ThumbnailView(urlString: video.thumbnail, cornerRadius: 12)

                Text(video.title)
                    .font(.caption)
                    .foregroundColor(.white)
                    .lineLimit(2)

                HStack(spacing: 6) {
                    RatingPill(icon: "❤️", value: video.avgLove)
                    RatingPill(icon: "✝️", value: video.avgFaith)
                    RatingPill(icon: "⭐", value: video.avgHope)
                }

                // whyTag is available in newer shared frameworks; omitted here for compatibility.

                Button(action: { onRate(video.videoId) }) {
                    Text(l10n("quick_rate"))
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                        .background(Color(hex: "00D4FF").opacity(0.2))
                        .foregroundColor(Color(hex: "00D4FF"))
                        .clipShape(Capsule())
                }
            }
            .frame(width: 220)
            .padding(10)
            .background(Color.white.opacity(0.06))
            .cornerRadius(16)
        }
    }
}

private struct HomeVideoGrid: View {
    let videos: [VideoSearchResult]
    let favoritesViewModel: FavoritesViewModel
    let onRate: (String) -> Void

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 16) {
            ForEach(videos, id: \.videoId) { video in
                HomeVideoCard(
                    video: video,
                    favoritesViewModel: favoritesViewModel,
                    onRate: onRate
                )
            }
        }
        .padding(.horizontal)
    }
}

private struct HomeVideoList: View {
    let videos: [VideoSearchResult]
    let favoritesViewModel: FavoritesViewModel
    let onRate: (String) -> Void

    var body: some View {
        LazyVStack(spacing: 16) {
            ForEach(videos, id: \.videoId) { video in
                HomeVideoCard(
                    video: video,
                    favoritesViewModel: favoritesViewModel,
                    onRate: onRate
                )
            }
        }
        .padding(.horizontal)
    }
}

private struct HomeVideoCard: View {
    let video: VideoSearchResult
    let favoritesViewModel: FavoritesViewModel
    let onRate: (String) -> Void
    @State private var isSharePresented = false
    @State private var shareUrl: URL?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ZStack(alignment: .topTrailing) {
                ThumbnailView(urlString: video.thumbnail, cornerRadius: 12)

                Button(action: {
                    favoritesViewModel.toggleFavorite(videoId: video.videoId)
                }) {
                    Image(systemName: favoritesViewModel.isFavorite(videoId: video.videoId) ? "heart.fill" : "heart")
                        .foregroundColor(favoritesViewModel.isFavorite(videoId: video.videoId) ? .red : .white)
                        .padding(8)
                        .background(Color.black.opacity(0.4))
                        .clipShape(Circle())
                        .padding(8)
                }
            }

            Text(video.title)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .lineLimit(2)

            Text(video.channelName)
                .font(.caption)
                .foregroundColor(Color.white.opacity(0.7))

            HStack(spacing: 8) {
                RatingPill(icon: "❤️", value: video.avgLove)
                RatingPill(icon: "✝️", value: video.avgFaith)
                RatingPill(icon: "⭐", value: video.avgHope)
                Spacer()
                HStack(spacing: 4) {
                    Image(systemName: "message")
                    Text("\(video.totalRatings)")
                }
                .font(.caption2)
                .foregroundColor(Color.white.opacity(0.7))
            }

            // whyTag is available in newer shared frameworks; omitted here for compatibility.

            HStack(spacing: 10) {
                NavigationLink(destination: VideoDetailsScreen(videoId: video.videoId)) {
                    Text(l10n("details"))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.08))
                        .foregroundColor(.white)
                        .clipShape(Capsule())
                }

                Button(action: { onRate(video.videoId) }) {
                    Text(l10n("rate"))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color(hex: "00D4FF").opacity(0.2))
                        .foregroundColor(Color(hex: "00D4FF"))
                        .clipShape(Capsule())
                }

                Button(action: {
                    shareUrl = URL(string: "https://www.youtube.com/watch?v=\(video.videoId)")
                    isSharePresented = shareUrl != nil
                }) {
                    Image(systemName: "square.and.arrow.up")
                        .foregroundColor(.white)
                        .padding(8)
                        .background(Color.white.opacity(0.08))
                        .clipShape(Circle())
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.06))
        .cornerRadius(16)
        .sheet(isPresented: $isSharePresented) {
            if let shareUrl = shareUrl {
                ActivityView(activityItems: [shareUrl])
            }
        }
    }
}

private struct ForYouHeaderView: View {
    let isColdStart: Bool

    var body: some View {
        Text(isColdStart ? "For You: popular picks while we learn your taste" : "For You: ranked from your taste graph")
            .font(.caption)
            .fontWeight(.semibold)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color.white.opacity(0.08))
            .foregroundColor(.white)
            .clipShape(Capsule())
            .padding(.horizontal)
    }
}

private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct RatingPill: View {
    let icon: String
    let value: Double

    var body: some View {
        HStack(spacing: 4) {
            Text(icon)
            Text(String(format: "%.1f", value))
        }
        .font(.caption2)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.white.opacity(0.08))
        .foregroundColor(.white)
        .clipShape(Capsule())
    }
}

private struct LoadMoreButton: View {
    let isLoading: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                }
                Text(isLoading ? l10n("loading") : l10n("load_more"))
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.white.opacity(0.08))
            .foregroundColor(.white)
            .cornerRadius(12)
        }
        .padding(.horizontal)
    }
}

private struct LoadingStateView: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text(message)
                .foregroundColor(.white.opacity(0.8))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ErrorStateView: View {
    let error: String
    let retryAction: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 40))
                .foregroundColor(.orange)
            Text(error)
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
            Button(l10n("retry"), action: retryAction)
                .buttonStyle(.borderedProminent)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct EmptyStateView: View {
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "video.slash")
                .font(.system(size: 36))
                .foregroundColor(Color.white.opacity(0.7))
            Text(message)
                .foregroundColor(Color.white.opacity(0.8))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }
}

private struct HomeFiltersSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selectedCategory: String?
    @Binding var sortBy: String
    @Binding var minLove: Int
    @Binding var minFaith: Int
    @Binding var minHope: Int
    @Binding var selectedLanguages: Set<String>
    let categoryOptions: [(String, String?)]
    let sortOptions: [(String, String)]
    let languages: [(String, String)]
    let onApply: () -> Void

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(l10n("category"))) {
                    Picker(l10n("category"), selection: Binding(
                        get: { selectedCategory ?? "" },
                        set: { selectedCategory = $0.isEmpty ? nil : $0 }
                    )) {
                        ForEach(categoryOptions, id: \.0) { option in
                            Text(option.0).tag(option.1 ?? "")
                        }
                    }
                }

                Section(header: Text(l10n("sort_label"))) {
                    Picker(l10n("sort_by"), selection: $sortBy) {
                        ForEach(sortOptions, id: \.1) { option in
                            Text(option.0).tag(option.1)
                        }
                    }
                }

                Section(header: Text(l10n("minimum_ratings"))) {
                    Stepper(value: $minLove, in: 0...3) {
                        Text("Love ≥ \(minLove == 0 ? "Any" : "\(minLove)")")
                    }
                    Stepper(value: $minFaith, in: 0...3) {
                        Text("Faith ≥ \(minFaith == 0 ? "Any" : "\(minFaith)")")
                    }
                    Stepper(value: $minHope, in: 0...3) {
                        Text("Hope ≥ \(minHope == 0 ? "Any" : "\(minHope)")")
                    }
                }

                Section(header: Text(l10n("languages_label"))) {
                    ForEach(languages, id: \.1) { language in
                        let isSelected = selectedLanguages.contains(language.1)
                        Button(action: {
                            if isSelected {
                                if selectedLanguages.count > 1 {
                                    selectedLanguages.remove(language.1)
                                }
                            } else {
                                selectedLanguages.insert(language.1)
                            }
                        }) {
                            HStack {
                                Text(language.0)
                                Spacer()
                                if isSelected {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(l10n("filters"))
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(l10n("apply")) {
                        onApply()
                        dismiss()
                    }
                }
            }
        }
    }
}
