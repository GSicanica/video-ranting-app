package com.youtube.rating.android.data.repository

import com.youtube.rating.android.domain.repository.RatingsGateway
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.RatingResponse
import com.youtube.rating.shared.models.ApiResponse

interface RatingsRepository : RatingsGateway {
    override suspend fun submitRating(request: RatingRequest): RatingResponse
    override suspend fun updateRating(request: RatingRequest): RatingResponse
    override suspend fun deleteRating(videoId: String, userToken: String): ApiResponse
    override suspend fun blockUser(userToken: String, blockedUserToken: String): ApiResponse
    override suspend fun unblockUser(userToken: String, blockedUserToken: String): ApiResponse
}
