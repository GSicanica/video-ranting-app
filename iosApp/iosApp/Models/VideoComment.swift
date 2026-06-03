import Foundation

struct VideoComment: Identifiable, Hashable {
    let id: String
    let videoId: String
    let deviceId: String
    let text: String
    let timestamp: Int64
    let love: Int
    let faith: Int
    let hope: Int
}

struct CommentSubmitResponse: Hashable {
    let success: Bool
    let message: String?
}

