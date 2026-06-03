import SwiftUI
import shared

struct RatingView: View {
    @ObservedObject var viewModel: RatingViewModel
    var prefilledVideoId: String? = nil
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // YouTube URL Input
                VStack(alignment: .leading, spacing: 8) {
                    Text(l10n("youtube_link"))
                        .font(.headline)
                    
                    HStack {
                        TextField("https://youtube.com/watch?v=...", text: $viewModel.videoUrl)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .autocapitalization(.none)
                            .keyboardType(.URL)
                            .disabled(prefilledVideoId != nil)
                        
                        if !viewModel.videoUrl.isEmpty {
                            Button(action: {
                                viewModel.videoUrl = ""
                                viewModel.clearVideo()
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                            }
                            .disabled(prefilledVideoId != nil)
                        }
                    }
                    
                    Button(l10n("load_video")) {
                        viewModel.loadVideoInfo()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(viewModel.videoUrl.isEmpty || viewModel.isLoading)
                }
                
                // Loading indicator
                if viewModel.isLoading {
                    HStack {
                        ProgressView()
                        Text(l10n("loading_video_info"))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                }
                
                // Error message
                if let error = viewModel.errorMessage {
                    HStack {
                        Image(systemName: "exclamationmark.triangle")
                            .foregroundColor(.orange)
                        Text(error)
                            .foregroundColor(.secondary)
                            .font(.caption)
                    }
                    .padding()
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(8)
                }
                
                // Video Info Card
                if let videoInfo = viewModel.videoInfo {
                    VStack(alignment: .leading, spacing: 12) {
                        // Thumbnail
                        AsyncImage(url: URL(string: videoInfo.thumbnail)) { image in
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
                        Text(videoInfo.title)
                            .font(.headline)
                            .lineLimit(2)
                        
                        // Channel
                        if !videoInfo.channelName.isEmpty {
                            Text(videoInfo.channelName)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        Divider()
                        
                        // Rating Section
                        Text(l10n("rate_this_video"))
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        // Love Rating
                        HeartRatingSelector(
                            title: "❤️ Love",
                            rating: $viewModel.loveRating
                        )
                        
                        // Faith Rating
                        HeartRatingSelector(
                            title: "✝️ Faith",
                            rating: $viewModel.faithRating
                        )
                        
                        // Hope Rating
                        HeartRatingSelector(
                            title: "⭐ Hope",
                            rating: $viewModel.hopeRating
                        )
                        
                        // Comment
                        VStack(alignment: .leading, spacing: 8) {
                            Text(l10n("comment_optional"))
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            TextEditor(text: $viewModel.comment)
                                .frame(height: 100)
                                .padding(4)
                                .background(Color(.systemGray6))
                                .cornerRadius(8)
                        }
                        
                        // Submit Button
                        Button(action: {
                            viewModel.submitRating()
                        }) {
                            HStack {
                                if viewModel.isSubmitting {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "checkmark.circle.fill")
                                    Text(l10n("submit_rating"))
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(viewModel.canSubmit ? Color.blue : Color.gray)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                        }
                        .disabled(!viewModel.canSubmit || viewModel.isSubmitting)
                        
                        // Success message
                        if viewModel.submitSuccess {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                Text(l10n("rating_submitted_success"))
                                    .foregroundColor(.green)
                            }
                            .padding()
                            .background(Color.green.opacity(0.1))
                            .cornerRadius(8)
                        }
                    }
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(16)
                }
            }
            .padding()
        }
        .navigationTitle(l10n("rate_video"))
        .onAppear {
            onAppearHandler()
        }
    }
}

struct HeartRatingSelector: View {
    let title: String
    @Binding var rating: Int32
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                if rating > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "heart.fill")
                            .foregroundColor(.red)
                            .font(.caption)
                        Text("\(rating)/6")
                            .font(.caption)
                            .fontWeight(.bold)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(ratingColor.opacity(0.2))
                    .cornerRadius(12)
                }
            }
            
            HStack(spacing: 8) {
                ForEach(1...6, id: \.self) { index in
                    Button(action: {
                        rating = Int32(index)
                    }) {
                        Image(systemName: index <= rating ? "heart.fill" : "heart")
                            .font(.title3)
                            .foregroundColor(index <= rating ? .red : .gray.opacity(0.3))
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
    }
    
    private var ratingColor: Color {
        switch rating {
        case 5...6: return .green
        case 3...4: return .blue
        default: return .orange
        }
    }
}

extension RatingView {
    // Add onAppear to handle prefilled video
    func onAppearHandler() {
        if let videoId = prefilledVideoId, viewModel.videoUrl.isEmpty {
            viewModel.prefillVideoId(videoId)
        }
    }
}
