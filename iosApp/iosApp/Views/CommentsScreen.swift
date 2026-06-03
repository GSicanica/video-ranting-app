import SwiftUI
import shared

struct CommentsScreen: View {
    @StateObject private var viewModel: CommentsViewModel
    @Environment(\.dismiss) var dismiss
    
    init(videoId: String, videoTitle: String) {
        _viewModel = StateObject(wrappedValue: CommentsViewModel(videoId: videoId, videoTitle: videoTitle))
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Comments List
                if viewModel.isLoading && viewModel.comments.isEmpty {
                    VStack(spacing: 16) {
                        ProgressView()
                        Text(l10n("loading_comments"))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = viewModel.errorMessage, viewModel.comments.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.bubble")
                            .font(.system(size: 50))
                            .foregroundColor(.orange)
                        Text(l10n("error"))
                            .font(.title2)
                            .fontWeight(.bold)
                        Text(error)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                        Button(l10n("retry")) {
                            viewModel.loadComments()
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.comments.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "bubble.left")
                            .font(.system(size: 50))
                            .foregroundColor(.secondary)
                        Text(l10n("no_comments"))
                            .font(.title2)
                            .fontWeight(.bold)
                        Text(l10n("be_first_comment"))
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(Array(viewModel.comments.enumerated()), id: \.offset) { index, comment in
                                CommentCard(comment: comment)
                            }
                        }
                        .padding()
                    }
                    .refreshable {
                        viewModel.refresh()
                    }
                }
                
                Divider()
                
                // Comment Input
                HStack(alignment: .top, spacing: 12) {
                    TextEditor(text: $viewModel.newCommentText)
                        .frame(minHeight: 40, maxHeight: 100)
                        .padding(8)
                        .background(Color(.systemGray6))
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                        )
                    
                    Button(action: {
                        viewModel.submitComment()
                    }) {
                        if viewModel.isSubmitting {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(.white)
                        }
                    }
                    .frame(width: 44, height: 44)
                    .background(viewModel.newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? Color.gray : Color.blue)
                    .cornerRadius(22)
                    .disabled(viewModel.newCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isSubmitting)
                }
                .padding()
                .background(Color(.systemBackground))
            }
            .navigationTitle(l10n("comments"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(l10n("close")) {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        viewModel.loadComments()
                    }) {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(viewModel.isLoading)
                }
            }
        }
        .onAppear {
            viewModel.loadComments()
        }
    }
}

struct CommentCard: View {
    let comment: VideoComment
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Device ID (first 8 chars) and timestamp
            HStack {
                Text(String(comment.deviceId.prefix(8)))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.blue)
                
                Spacer()
                
                Text(formatDate(timestamp: comment.timestamp))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Comment text
            Text(comment.text)
                .font(.body)
                .fixedSize(horizontal: false, vertical: true)
            
            // Rating info if available
            if comment.love > 0 || comment.faith > 0 || comment.hope > 0 {
                HStack(spacing: 12) {
                    if comment.love > 0 {
                        HStack(spacing: 2) {
                            Text("❤️")
                                .font(.caption)
                            Text("\(comment.love)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    if comment.faith > 0 {
                        HStack(spacing: 2) {
                            Text("✝️")
                                .font(.caption)
                            Text("\(comment.faith)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    if comment.hope > 0 {
                        HStack(spacing: 2) {
                            Text("⭐")
                                .font(.caption)
                            Text("\(comment.hope)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
    
    private func formatDate(timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let now = Date()
        let calendar = Calendar.current
        
        let components = calendar.dateComponents([.minute, .hour, .day], from: date, to: now)
        
        if let days = components.day, days > 0 {
            if days == 1 {
                return l10n("time_one_day_ago")
            } else if days < 7 {
                return l10n("time_days_ago", "\(days)")
            } else {
                let formatter = DateFormatter()
                formatter.dateFormat = "MMM d, yyyy"
                return formatter.string(from: date)
            }
        } else if let hours = components.hour, hours > 0 {
            return hours == 1 ? l10n("time_one_hour_ago") : l10n("time_hours_ago", "\(hours)")
        } else if let minutes = components.minute, minutes > 0 {
            return minutes == 1 ? l10n("time_one_minute_ago") : l10n("time_minutes_ago", "\(minutes)")
        } else {
            return l10n("time_just_now")
        }
    }
}
