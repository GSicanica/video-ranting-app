import SwiftUI
import shared

struct FavoritesScreen: View {
    @StateObject private var viewModel = FavoritesViewModel()
    
    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.isLoading {
                    loadingView
                } else if let error = viewModel.errorMessage, viewModel.favoriteVideos.isEmpty {
                    errorView(message: error)
                } else if viewModel.favoriteVideos.isEmpty {
                    emptyState
                } else {
                    favoritesList
                }
            }
            .navigationTitle("Favorites (\(viewModel.favoriteVideos.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.refresh() }) {
                        if viewModel.isSyncing {
                            ProgressView()
                        } else {
                            Image(systemName: "arrow.clockwise")
                        }
                    }
                    .disabled(viewModel.isSyncing)
                }
            }
            .onAppear {
                if viewModel.favoriteVideos.isEmpty && !viewModel.isLoading {
                    viewModel.loadFavoriteVideos()
                }
            }
        }
    }
    
    private var loadingView: some View {
        ProgressView(l10n("loading_favorites"))
    }
    
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 50))
                .foregroundColor(.orange)
            
            Text(l10n("error"))
                .font(.title2)
                .fontWeight(.bold)
            
            Text(message)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Button("Retry") {
                viewModel.refresh()
            }
            .buttonStyle(.borderedProminent)
        }
    }
    
    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "heart.slash")
                .font(.system(size: 50))
                .foregroundColor(.secondary)
            
            Text(l10n("no_favorites_yet"))
                .font(.title2)
                .fontWeight(.bold)
            
            Text(l10n("favorites_hint"))
                .foregroundColor(.secondary)
        }
    }
    
    private var favoritesList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.favoriteVideos, id: \.videoId) { video in
                    FavoriteVideoCard(video: video, viewModel: viewModel)
                }
            }
            .padding()
        }
        .refreshable {
            viewModel.refresh()
        }
    }
}

struct FavoriteVideoCard: View {
    let video: VideoStats
    @ObservedObject var viewModel: FavoritesViewModel
    @State private var showingComments = false
    
    var body: some View {
        NavigationLink(destination: VideoDetailsScreen(videoId: video.videoId)) {
            VStack(alignment: .leading, spacing: 8) {
                // Thumbnail
                AsyncImage(url: URL(string: video.videoThumbnail)) { image in
                    image
                        .resizable()
                        .aspectRatio(16/9, contentMode: .fill)
                } placeholder: {
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .aspectRatio(16/9, contentMode: .fill)
                        .overlay(ProgressView())
                }
                .cornerRadius(12)
                .clipped()
                
                // Title
                Text(video.videoTitle)
                    .font(.headline)
                    .lineLimit(2)
                
                // Ratings
                HStack(spacing: 16) {
                if video.averageLove > 0 {
                    HStack(spacing: 4) {
                        Text("❤️")
                        Text(String(format: "%.1f", video.averageLove))
                            .font(.caption)
                            .fontWeight(.semibold)
                    }
                }
                
                if video.averageFaith > 0 {
                    HStack(spacing: 4) {
                        Text("✝️")
                        Text(String(format: "%.1f", video.averageFaith))
                            .font(.caption)
                            .fontWeight(.semibold)
                    }
                }
                
                if video.averageHope > 0 {
                    HStack(spacing: 4) {
                        Text("⭐")
                        Text(String(format: "%.1f", video.averageHope))
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
            
            // Actions
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
                    viewModel.toggleFavorite(videoId: video.videoId)
                }) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.red)
                }
            }
            .padding(.top, 4)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(16)
        }
        .buttonStyle(PlainButtonStyle())
        .sheet(isPresented: $showingComments) {
            CommentsScreen(videoId: video.videoId, videoTitle: video.videoTitle)
        }
    }
}
