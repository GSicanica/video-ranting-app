import SwiftUI
import shared

struct VideoDetailsScreen: View {
    let videoId: String
    @StateObject private var viewModel = VideoDetailsViewModel()
    @EnvironmentObject var favoritesViewModel: FavoritesViewModel
    @State private var showingComments = false
    @State private var showingRating = false
    @State private var selectedTab = 0
    @State private var playerError: String?
    @State private var playerReloadToken = UUID()
    
    var body: some View {
        ScrollView {
            if viewModel.isLoading {
                VStack(spacing: 20) {
                    ProgressView()
                    Text(l10n("loading_video_details"))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.top, 100)
            } else if let errorMessage = viewModel.errorMessage {
                VStack(spacing: 16) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 50))
                        .foregroundColor(.orange)
                    Text(errorMessage)
                        .font(.headline)
                        .multilineTextAlignment(.center)
                    Button(l10n("retry")) {
                        viewModel.loadVideoDetails(videoId: videoId)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.top, 100)
            } else if let video = viewModel.videoStats {
                VStack(alignment: .leading, spacing: 16) {
                    Text(video.videoTitle)
                        .font(.title2)
                        .fontWeight(.bold)
                        .fixedSize(horizontal: false, vertical: true)

                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(video.channelName ?? l10n("unknown_channel"))
                                .font(.subheadline)
                                .foregroundColor(.secondary)

                            if let category = video.category {
                                Text(viewModel.getCategoryDisplayName(category))
                                    .font(.caption)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.blue.opacity(0.2))
                                    .foregroundColor(.blue)
                                    .cornerRadius(8)
                            }
                        }

                        Spacer()

                        Button(action: {
                            favoritesViewModel.toggleFavorite(videoId: video.videoId)
                        }) {
                            Image(systemName: favoritesViewModel.isFavorite(videoId: video.videoId) ? "heart.fill" : "heart")
                                .foregroundColor(favoritesViewModel.isFavorite(videoId: video.videoId) ? .red : .gray)
                                .font(.title2)
                        }
                    }

                    Picker(l10n("tab"), selection: $selectedTab) {
                        Text(l10n("video")).tag(0)
                        Text(l10n("ratings")).tag(1)
                        Text(l10n("comments")).tag(2)
                    }
                    .pickerStyle(.segmented)

                    if selectedTab == 0 {
                        VStack(spacing: 12) {
                            ZStack {
                                YouTubePlayerView(
                                    videoId: video.videoId,
                                    loadError: $playerError
                                )
                                .id(playerReloadToken)
                                .frame(maxWidth: .infinity)
                                .aspectRatio(16/9, contentMode: .fit)
                                .clipShape(RoundedRectangle(cornerRadius: 12))

                                if let error = playerError {
                                    VStack(spacing: 12) {
                                        Text(error)
                                            .font(.caption)
                                            .foregroundColor(.white)
                                            .multilineTextAlignment(.center)
                                        HStack(spacing: 12) {
                                            Button("Retry") {
                                                playerError = nil
                                                playerReloadToken = UUID()
                                            }
                                            .buttonStyle(.borderedProminent)

                                            Button(l10n("open_youtube")) {
                                                if let url = URL(string: "https://www.youtube.com/watch?v=\(video.videoId)") {
                                                    UIApplication.shared.open(url)
                                                }
                                            }
                                            .buttonStyle(.bordered)
                                        }
                                    }
                                    .padding()
                                    .frame(maxWidth: .infinity)
                                    .background(Color.black.opacity(0.7))
                                }
                            }

                            Button(action: {
                                if let url = URL(string: "https://www.youtube.com/watch?v=\(video.videoId)") {
                                    UIApplication.shared.open(url)
                                }
                            }) {
                                HStack {
                                    Image(systemName: "play.rectangle.fill")
                                    Text(l10n("open_in_youtube"))
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.red)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                            }
                        }
                    } else if selectedTab == 1 {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(l10n("ratings"))
                                .font(.headline)

                            HStack(spacing: 20) {
                                RatingStatBox(
                                    title: l10n("love"),
                                    value: video.averageLove,
                                    icon: "❤️",
                                    color: .red
                                )

                                RatingStatBox(
                                    title: l10n("faith"),
                                    value: video.averageFaith,
                                    icon: "✝️",
                                    color: .blue
                                )

                                RatingStatBox(
                                    title: l10n("hope"),
                                    value: video.averageHope,
                                    icon: "⭐",
                                    color: .green
                                )
                            }

                            Text(l10n("ratings_total", "\(video.totalRatings)"))
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Button(action: {
                                showingRating = true
                            }) {
                                HStack {
                                    Image(systemName: "star.fill")
                                    Text(l10n("rate_this_video"))
                                }
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                            }
                        }
                        .padding()
                        .background(Color(.systemGray6))
                        .cornerRadius(12)
                    } else {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text(l10n("comments"))
                                    .font(.headline)
                                Spacer()
                                if viewModel.isLoadingComments {
                                    ProgressView()
                                        .scaleEffect(0.8)
                                }
                            }

                            if viewModel.comments.isEmpty {
                                VStack(spacing: 8) {
                                    Image(systemName: "bubble.left.and.bubble.right")
                                        .font(.system(size: 40))
                                        .foregroundColor(.gray)
                                    Text(l10n("no_comments_yet"))
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                    Button("Be the first to comment") {
                                        showingComments = true
                                    }
                                    .font(.caption)
                                    .buttonStyle(.borderedProminent)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 30)
                            } else {
                                VStack(alignment: .leading, spacing: 12) {
                                    ForEach(viewModel.comments, id: \.timestamp) { comment in
                                        CommentCard(comment: comment)
                                        Divider()
                                    }
                                }
                            }

                            Button(action: {
                                showingComments = true
                            }) {
                                HStack {
                                    Image(systemName: "plus.circle.fill")
                                    Text(l10n("add_comment"))
                                }
                                .font(.subheadline)
                                .foregroundColor(.blue)
                            }
                            .padding(.top, 4)
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle(l10n("video_details"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    if let url = URL(string: "https://www.youtube.com/watch?v=\(videoId)") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    Image(systemName: "arrow.up.right.square")
                }
            }
        }
        .refreshable {
            viewModel.refresh(videoId: videoId)
        }
        .sheet(isPresented: $showingComments) {
            CommentsScreen(videoId: videoId, videoTitle: viewModel.videoStats?.videoTitle ?? l10n("video"))
        }
        .sheet(isPresented: $showingRating) {
            NavigationView {
                RatingView(viewModel: RatingViewModel(), prefilledVideoId: videoId)
                    .navigationBarItems(trailing: Button(l10n("done")) {
                        showingRating = false
                        viewModel.refresh(videoId: videoId)
                    })
            }
        }
        .onAppear {
            if viewModel.videoStats == nil {
                viewModel.loadVideoDetails(videoId: videoId)
            }
        }
    }
}

struct RatingStatBox: View {
    let title: String
    let value: Double
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Text(icon)
                .font(.title)
            Text(String(format: "%.1f", value))
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(10)
        .shadow(radius: 1)
    }
}

#Preview {
    NavigationView {
        VideoDetailsScreen(videoId: "dQw4w9WgXcQ")
            .environmentObject(FavoritesViewModel())
    }
}
