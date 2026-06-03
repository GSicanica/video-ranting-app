package com.youtube.rating.android.data.repository

import com.youtube.rating.shared.api.RatingApiClient
import com.youtube.rating.shared.models.LiveKitTokenResponse
import com.youtube.rating.shared.models.PsalmHighlightsSyncResponse
import com.youtube.rating.shared.models.PsalmAvailabilityListResponse
import com.youtube.rating.shared.models.PsalmAvailabilitySetResponse
import com.youtube.rating.shared.models.PsalmRoomResponse

class CallsRepository(
    private val apiClient: RatingApiClient
) {
    fun getCallsUrl(): String = "https://rtc.tmbv-hms.com"

    suspend fun syncPsalmHighlights(
        userToken: String,
        highlights: List<String>,
        displayName: String? = null,
        gender: String? = null,
        notMarried: Boolean? = null
    ): PsalmHighlightsSyncResponse {
        return apiClient.syncPsalmHighlights(
            userToken = userToken,
            highlights = highlights,
            displayName = displayName,
            gender = gender,
            notMarried = notMarried
        )
    }

    suspend fun getPsalmCallRoom(
        userToken: String,
        displayName: String,
        gender: String,
        notMarried: Boolean,
        selectedTokens: List<String>,
        favoritePsalm: String? = null
    ): PsalmRoomResponse {
        return apiClient.getPsalmCallRoom(
            userToken = userToken,
            displayName = displayName,
            gender = gender,
            notMarried = notMarried,
            selectedTokens = selectedTokens,
            favoritePsalm = favoritePsalm
        )
    }

    suspend fun getLiveKitToken(
        userToken: String,
        room: String,
        identity: String,
        name: String
    ): LiveKitTokenResponse {
        return apiClient.getLiveKitToken(
            userToken = userToken,
            room = room,
            identity = identity,
            name = name
        )
    }

    suspend fun setPsalmCallAvailability(
        userToken: String,
        gender: String,
        notMarried: Boolean,
        favoritePsalm: String,
        availableFrom: String,
        displayName: String,
        ageYears: Int
    ): PsalmAvailabilitySetResponse {
        return apiClient.setPsalmCallAvailability(
            userToken = userToken,
            gender = gender,
            notMarried = notMarried,
            favoritePsalm = favoritePsalm,
            availableFrom = availableFrom,
            displayName = displayName,
            ageYears = ageYears
        )
    }

    suspend fun listPsalmCallAvailability(
        userToken: String,
        gender: String,
        favoritePsalm: String
    ): PsalmAvailabilityListResponse {
        return apiClient.listPsalmCallAvailability(
            userToken = userToken,
            gender = gender,
            favoritePsalm = favoritePsalm
        )
    }
}
