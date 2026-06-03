import Foundation
import shared

extension RatingApiClient {
    func getVideoCommentsIos(
        videoId: String,
        onSuccess: @escaping ([VideoComment]) -> Void,
        onError: @escaping (String) -> Void
    ) {
        // Backend doesn't currently expose a public "list comments" endpoint used by iOS.
        // Keep the app runnable by returning empty data instead of failing compilation.
        onSuccess([])
    }

    func getVideoCommentsIos(
        videoId: String,
        onSuccess: @escaping ([VideoComment]) -> Void
    ) {
        getVideoCommentsIos(videoId: videoId, onSuccess: onSuccess, onError: { _ in })
    }

    func addCommentIos(
        videoId: String,
        deviceId: String,
        commentText: String,
        onSuccess: @escaping (CommentSubmitResponse) -> Void,
        onError: @escaping (String) -> Void
    ) {
        onSuccess(CommentSubmitResponse(success: false, message: "Comments are not available on iOS yet"))
    }
}

