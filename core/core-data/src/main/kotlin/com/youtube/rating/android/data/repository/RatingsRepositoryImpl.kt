package com.youtube.rating.android.data.repository

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.RatingResponse

class RatingsRepositoryImpl(
    private val apiClient: RatingApiClient
) : RatingsRepository {
    override suspend fun submitRating(request: RatingRequest): RatingResponse =
        apiClient.submitRating(request)

    override suspend fun updateRating(request: RatingRequest): RatingResponse =
        apiClient.updateRating(request)

    override suspend fun deleteRating(videoId: String, userToken: String): ApiResponse =
        apiClient.deleteRating(videoId, userToken)

    override suspend fun blockUser(userToken: String, blockedUserToken: String): ApiResponse =
        apiClient.blockUser(userToken, blockedUserToken)

    override suspend fun unblockUser(userToken: String, blockedUserToken: String): ApiResponse =
        apiClient.unblockUser(userToken, blockedUserToken)

}
