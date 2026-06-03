package com.youtube.rating.android.domain.usecase.ratings

import com.youtube.rating.android.domain.repository.RatingsGateway
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.RatingResponse
import com.youtube.rating.shared.models.ApiResponse

class SubmitRatingUseCase(
    private val repo: RatingsGateway
) {
    suspend operator fun invoke(request: RatingRequest): RatingResponse = repo.submitRating(request)
}

class UpdateRatingUseCase(
    private val repo: RatingsGateway
) {
    suspend operator fun invoke(request: RatingRequest): RatingResponse = repo.updateRating(request)
}

class DeleteRatingUseCase(
    private val repo: RatingsGateway
) {
    suspend operator fun invoke(videoId: String, userToken: String): ApiResponse =
        repo.deleteRating(videoId, userToken)
}

class BlockUserUseCase(
    private val repo: RatingsGateway
) {
    suspend operator fun invoke(userToken: String, blockedUserToken: String): ApiResponse =
        repo.blockUser(userToken, blockedUserToken)
}

class UnblockUserUseCase(
    private val repo: RatingsGateway
) {
    suspend operator fun invoke(userToken: String, blockedUserToken: String): ApiResponse =
        repo.unblockUser(userToken, blockedUserToken)
}
