import SwiftUI
import shared

struct SearchScreen: View {
    @StateObject private var viewModel = SearchViewModel()
    @State private var showFilters = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                HStack {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        
                        TextField(l10n("search_videos"), text: $viewModel.searchQuery)
                            .textFieldStyle(PlainTextFieldStyle())
                            .autocapitalization(.none)
                        
                        if !viewModel.searchQuery.isEmpty {
                            Button(action: {
                                viewModel.searchQuery = ""
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                    .padding(10)
                    .background(Color(.systemGray6))
                    .cornerRadius(10)
                    
                    Button(action: {
                        viewModel.search()
                    }) {
                        Text(l10n("search"))
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(viewModel.isLoading)
                }
                .padding()
                
                // Filter Bar
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        // Category Filter
                        Menu {
                            ForEach(viewModel.categories, id: \.0) { category in
                                Button(action: {
                                    viewModel.selectedCategory = category.1
                                    viewModel.search()
                                }) {
                                    HStack {
                                        Text(category.0)
                                        if viewModel.selectedCategory == category.1 {
                                            Image(systemName: "checkmark")
                                        }
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                Image(systemName: "line.3.horizontal.decrease.circle")
                                Text(viewModel.categories.first(where: { $0.1 == viewModel.selectedCategory })?.0 ?? l10n("all"))
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(.systemGray6))
                            .cornerRadius(20)
                        }
                        
                        // Sort Filter
                        Menu {
                            ForEach(viewModel.sortOptions, id: \.0) { option in
                                Button(action: {
                                    viewModel.sortBy = option.1
                                    viewModel.search()
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
                            HStack {
                                Image(systemName: "arrow.up.arrow.down")
                                Text(viewModel.sortOptions.first(where: { $0.1 == viewModel.sortBy })?.0 ?? l10n("latest"))
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(.systemGray6))
                            .cornerRadius(20)
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.bottom, 8)
                
                Divider()
                
                // Results
                if viewModel.isLoading && viewModel.videos.isEmpty {
                    VStack(spacing: 16) {
                        ProgressView()
                        Text(l10n("searching"))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.errorMessage {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 50))
                            .foregroundColor(.orange)
                        Text(l10n("error"))
                            .font(.title2)
                            .fontWeight(.bold)
                        Text(error)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                        Button(l10n("retry")) {
                            viewModel.search()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.videos.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        Text(l10n("no_results"))
                            .font(.title2)
                            .fontWeight(.bold)
                        Text(l10n("try_different_search"))
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(viewModel.videos, id: \.videoId) { video in
                                SearchVideoCard(video: video)
                            }
                            
                            // Load More
                            if viewModel.hasMore {
                                Button(action: {
                                    viewModel.loadMore()
                                }) {
                                    HStack {
                                        if viewModel.isLoading {
                                            ProgressView()
                                                .progressViewStyle(CircularProgressViewStyle())
                                        }
                                        Text(viewModel.isLoading ? l10n("loading") : l10n("load_more"))
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color(.systemGray6))
                                    .cornerRadius(12)
                                }
                                .padding(.horizontal)
                            }
                        }
                        .padding()
                    }
                    .refreshable {
                        viewModel.refresh()
                    }
                }
            }
            .navigationTitle(l10n("search_videos"))
            .navigationBarTitleDisplayMode(.inline)
        }
        .onAppear {
            if viewModel.videos.isEmpty {
                viewModel.search()
            }
        }
    }
}

struct SearchVideoCard: View {
    let video: VideoSearchResult
    @State private var showingComments = false
    @EnvironmentObject var favoritesViewModel: FavoritesViewModel
    
    var body: some View {
        NavigationLink(destination: VideoDetailsScreen(videoId: video.videoId)) {
            VStack(alignment: .leading, spacing: 8) {
                // Thumbnail
                ThumbnailView(urlString: video.thumbnail, cornerRadius: 12)
                
                // Title
                Text(video.title)
                    .font(.headline)
                    .lineLimit(2)
                
                // Channel
                Text(video.channelName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            
            // Stats
            HStack(spacing: 16) {
                if video.avgLove > 0 {
                    HStack(spacing: 4) {
                        Text("❤️")
                        Text(String(format: "%.1f", video.avgLove))
                            .font(.caption)
                            .fontWeight(.semibold)
                    }
                }
                
                if video.avgFaith > 0 {
                    HStack(spacing: 4) {
                        Text("✝️")
                        Text(String(format: "%.1f", video.avgFaith))
                            .font(.caption)
                            .fontWeight(.semibold)
                    }
                }
                
                if video.avgHope > 0 {
                    HStack(spacing: 4) {
                        Text("⭐")
                        Text(String(format: "%.1f", video.avgHope))
                            .font(.caption)
                            .fontWeight(.semibold)
                    }
                }
                
                Spacer()
                
                if video.totalRatings > 0 {
                    Text("\(video.totalRatings) ratings")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.top, 4)
            
            // Open in YouTube button
            HStack(spacing: 12) {
                Button(action: {
                    if let url = URL(string: "https://www.youtube.com/watch?v=\(video.videoId)") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    HStack {
                        Image(systemName: "play.rectangle.fill")
                        Text(l10n("watch"))
                    }
                    .font(.caption)
                    .foregroundColor(.blue)
                }
                
                Button(action: {
                    showingComments = true
                }) {
                    HStack {
                        Image(systemName: "bubble.left.and.bubble.right.fill")
                        Text(l10n("comments"))
                    }
                    .font(.caption)
                    .foregroundColor(.green)
                }
                
                Spacer()
                
                Button(action: {
                    favoritesViewModel.toggleFavorite(videoId: video.videoId)
                }) {
                    Image(systemName: favoritesViewModel.isFavorite(videoId: video.videoId) ? "heart.fill" : "heart")
                        .foregroundColor(favoritesViewModel.isFavorite(videoId: video.videoId) ? .red : .gray)
                        .font(.title3)
                }
            }
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingComments) {
            CommentsScreen(videoId: video.videoId, videoTitle: video.title)
        }
    }
}
