import SwiftUI
import shared

struct RatedVideosScreen: View {
    @StateObject private var viewModel = RatedVideosViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Filter and Sort Controls
                VStack(spacing: 12) {
                    // Category Filter
                    HStack {
                        Text(l10n("category_label"))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Menu {
                            Button(l10n("all")) { viewModel.changeCategoryAndSort(category: "all", sort: viewModel.sortBy) }
                            Button(l10n("thanks")) { viewModel.changeCategoryAndSort(category: "Thanks", sort: viewModel.sortBy) }
                            Button(l10n("teaching")) { viewModel.changeCategoryAndSort(category: "teaching", sort: viewModel.sortBy) }
                            Button(l10n("testimony")) { viewModel.changeCategoryAndSort(category: "testimony", sort: viewModel.sortBy) }
                            Button(l10n("bible")) { viewModel.changeCategoryAndSort(category: "bible", sort: viewModel.sortBy) }
                            Button(l10n("mark_of_beast")) { viewModel.changeCategoryAndSort(category: "markofbeast", sort: viewModel.sortBy) }
                        } label: {
                            HStack {
                                Text(categoryDisplayName(viewModel.selectedCategory))
                                    .font(.subheadline)
                                Image(systemName: "chevron.down")
                                    .font(.caption)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(.systemGray5))
                            .cornerRadius(8)
                        }
                        
                        Spacer()
                    }
                    
                    // Sort Options
                    HStack {
                        Text(l10n("sort_label") + ":")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Menu {
                            Button(l10n("sort_latest_first")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "latest") }
                            Button(l10n("sort_oldest_first")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "oldest") }
                            Button(l10n("sort_highest_love")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "love") }
                            Button(l10n("sort_highest_faith")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "faith") }
                            Button(l10n("sort_highest_hope")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "hope") }
                            Button(l10n("sort_title_az")) { viewModel.changeCategoryAndSort(category: viewModel.selectedCategory, sort: "title") }
                        } label: {
                            HStack {
                                Text(sortDisplayName(viewModel.sortBy))
                                    .font(.subheadline)
                                Image(systemName: "chevron.down")
                                    .font(.caption)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color(.systemGray5))
                            .cornerRadius(8)
                        }
                        
                        Spacer()
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemBackground))
                
                Divider()
                
                // Content
                ZStack {
                    if viewModel.isLoading && viewModel.ratedVideos.isEmpty {
                        ProgressView(l10n("loading_ratings"))
                    } else if let error = viewModel.errorMessage, viewModel.ratedVideos.isEmpty {
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
                                .padding(.horizontal)
                            
                            Button(l10n("retry")) {
                                viewModel.refresh()
                            }
                            .buttonStyle(.borderedProminent)
                        }
                    } else if viewModel.ratedVideos.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "star.slash")
                                .font(.system(size: 50))
                                .foregroundColor(.secondary)
                            
                            Text(l10n("no_rated_videos"))
                                .font(.title2)
                                .fontWeight(.bold)
                            
                            Text(l10n("no_ratings_yet"))
                                .foregroundColor(.secondary)
                        }
                    } else {
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(Array(viewModel.ratedVideos.enumerated()), id: \.offset) { index, video in
                                    RatedVideoCard(video: video)
                                }
                                
                                // Load More Button
                                if viewModel.hasMore {
                                    if viewModel.isLoading {
                                        ProgressView()
                                            .padding()
                                    } else {
                                        Button(l10n("load_more")) {
                                            viewModel.loadRatedVideos(loadMore: true)
                                        }
                                        .padding()
                                    }
                                }
                            }
                            .padding()
                        }
                        .refreshable {
                            viewModel.refresh()
                        }
                    }
                }
            }
            .navigationTitle(l10n("my_rated_videos"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { viewModel.refresh() }) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .onAppear {
                if viewModel.ratedVideos.isEmpty && !viewModel.isLoading {
                    viewModel.loadRatedVideos()
                }
            }
        }
    }
    
    private func categoryDisplayName(_ category: String) -> String {
        switch category {
        case "all": return l10n("all")
        case "Thanks": return l10n("thanks")
        case "teaching": return l10n("teaching")
        case "testimony": return l10n("testimony")
        case "bible": return l10n("bible")
        case "markofbeast": return l10n("mark_of_beast")
        default: return category.capitalized
        }
    }
    
    private func sortDisplayName(_ sort: String) -> String {
        switch sort {
        case "latest": return l10n("sort_latest_first")
        case "oldest": return l10n("sort_oldest_first")
        case "love": return l10n("sort_highest_love")
        case "faith": return l10n("sort_highest_faith")
        case "hope": return l10n("sort_highest_hope")
        case "title": return l10n("sort_title_az")
        default: return sort.capitalized
        }
    }
}

struct RatedVideoCard: View {
    let video: RatedVideo
    @State private var showingComments = false
    @EnvironmentObject var favoritesViewModel: FavoritesViewModel
    
    var body: some View {
        NavigationLink(destination: VideoDetailsScreen(videoId: video.videoId)) {
            VStack(alignment: .leading, spacing: 8) {
                // Thumbnail
                ThumbnailView(urlString: video.thumbnail, cornerRadius: 12)
                
                // Title
                Text(video.videoTitle)
                    .font(.headline)
                    .lineLimit(2)
                
                // Channel
                Text(video.channelName)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            // My Ratings
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(l10n("my_ratings_title"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack(spacing: 12) {
                        HStack(spacing: 4) {
                            Text("❤️")
                            Text("\(video.myLove)")
                                .font(.caption)
                                .fontWeight(.semibold)
                        }
                        
                        HStack(spacing: 4) {
                            Text("✝️")
                            Text("\(video.myFaith)")
                                .font(.caption)
                                .fontWeight(.semibold)
                        }
                        
                        HStack(spacing: 4) {
                            Text("⭐")
                            Text("\(video.myHope)")
                                .font(.caption)
                                .fontWeight(.semibold)
                        }
                    }
                }
                
                Spacer()
                
                // Average Ratings
                VStack(alignment: .trailing, spacing: 4) {
                    Text(l10n("average"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    let avgLove = video.avgLove?.doubleValue ?? 0
                    let avgFaith = video.avgFaith?.doubleValue ?? 0
                    let avgHope = video.avgHope?.doubleValue ?? 0

                    HStack(spacing: 8) {
                        if avgLove > 0 {
                            Text("❤️ \(String(format: "%.1f", avgLove))")
                                .font(.caption)
                        }
                        if avgFaith > 0 {
                            Text("✝️ \(String(format: "%.1f", avgFaith))")
                                .font(.caption)
                        }
                        if avgHope > 0 {
                            Text("⭐ \(String(format: "%.1f", avgHope))")
                                .font(.caption)
                        }
                    }
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
                        Text(l10n("comments_count", "\(video.totalRatings)"))
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
            .padding(.top, 4)
            
            // Rated timestamp
            Text(l10n("rated_on", formatDate(video.ratedAt)))
                .font(.caption2)
                .foregroundColor(.secondary)
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
    
    private func formatDate(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        guard let date = formatter.date(from: isoString) else {
            return isoString
        }
        
        let now = Date()
        let calendar = Calendar.current
        let components = calendar.dateComponents([.second, .minute, .hour, .day, .weekOfYear, .month, .year], from: date, to: now)
        
        if let years = components.year, years > 0 {
            return years == 1 ? l10n("time_one_year_ago") : l10n("time_years_ago", "\(years)")
        } else if let months = components.month, months > 0 {
            return months == 1 ? l10n("time_one_month_ago") : l10n("time_months_ago", "\(months)")
        } else if let weeks = components.weekOfYear, weeks > 0 {
            return weeks == 1 ? l10n("time_one_week_ago") : l10n("time_weeks_ago", "\(weeks)")
        } else if let days = components.day, days > 0 {
            return days == 1 ? l10n("time_one_day_ago") : l10n("time_days_ago", "\(days)")
        } else if let hours = components.hour, hours > 0 {
            return hours == 1 ? l10n("time_one_hour_ago") : l10n("time_hours_ago", "\(hours)")
        } else if let minutes = components.minute, minutes > 0 {
            return minutes == 1 ? l10n("time_one_minute_ago") : l10n("time_minutes_ago", "\(minutes)")
        } else {
            return l10n("time_just_now")
        }
    }
}
