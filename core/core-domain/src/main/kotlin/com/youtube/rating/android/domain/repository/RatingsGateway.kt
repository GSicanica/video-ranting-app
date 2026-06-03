package com.youtube.rating.android.domain.repository

import com.youtube.rating.shared.models.ApiResponse
import com.youtube.rating.shared.models.RatingRequest
import com.youtube.rating.shared.models.RatingResponse

interface RatingsGateway {
    suspend fun submitRating(request: RatingRequest): RatingResponse
    suspend fun updateRating(request: RatingRequest): RatingResponse
    suspend fun deleteRating(videoId: String, userToken: String): ApiResponse
    suspend fun blockUser(userToken: String, blockedUserToken: String): ApiResponse
    suspend fun unblockUser(userToken: String, blockedUserToken: String): ApiResponse
}
