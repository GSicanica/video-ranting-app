package com.youtube.rating.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.domain.usecase.ratings.BlockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.DeleteRatingUseCase
import com.youtube.rating.android.domain.usecase.ratings.UnblockUserUseCase
import com.youtube.rating.android.domain.usecase.ratings.UpdateRatingUseCase
import com.youtube.rating.shared.models.RatedVideo
import com.youtube.rating.shared.models.RatingRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.youtube.rating.android.data.prefs.RatingsPrefs
import com.youtube.rating.core.coroutines.makeIOCall

/**
 * ViewModel for displaying user's rated videos and managing them in debug mode.
 * Uses singleton API client for better resource management.
 */
class RatedVideosViewModel(
    private val updateRatingUseCase: UpdateRatingUseCase,
    private val deleteRatingUseCase: DeleteRatingUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val userTokenManager: UserTokenManager
) : ViewModel() {
    private var appContext: Context? = null

    private val _videos = MutableStateFlow<List<RatedVideo>>(emptyList())
    val videos: StateFlow<List<RatedVideo>> = _videos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var observeJob: Job? = null

    private fun setError(where: String, message: String, e: Exception? = null) {
        _error.value = message
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e ?: Exception(message),
            tags = mapOf("where" to where)
        )
    }

    private suspend fun requireUserToken(where: String): String? {
        val token = userTokenManager.getUserTokenAsync()
        if (token != null) return token
        setError(where, "User token nije postavljen. Molimo postavite token u postavkama.")
        return null
    }

    fun loadVideos(context: Context? = null, forceRefresh: Boolean = false) {
        // Keep API signature for UI call sites, but make it simple:
        // - no local cache invalidation
        // - no seeding from server
        // - always reflect RatingsPrefs as the source of truth for "my ratings"
        val ctx = context?.applicationContext
        if (ctx == null) {
            _error.value = "Context needed to load rated videos"
            return
        }
        appContext = ctx

        if (observeJob?.isActive == true && !forceRefresh) return

        observeJob?.cancel()
        _isLoading.value = true
        _error.value = null

        observeJob = makeIOCall {
            var first = true
            RatingsPrefs.myRatingsFlow(ctx).collect { list ->
                _videos.value = list
                if (first) {
                    first = false
                    _isLoading.value = false
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Block a user by their user token
     */
    suspend fun blockUser(blockedUserToken: String) {
        val myUserToken = requireUserToken("RatedVideosViewModel.blockUser") ?: return
        val response = blockUserUseCase(myUserToken, blockedUserToken)
        if (!response.success) {
            setError(
                where = "RatedVideosViewModel.blockUser",
                message = response.message ?: "Failed to block user"
            )
        }
    }

    /**
     * Unblock a user by their user token
     */
    suspend fun unblockUser(blockedUserToken: String) {
        val myUserToken = requireUserToken("RatedVideosViewModel.unblockUser") ?: return
        val response = unblockUserUseCase(myUserToken, blockedUserToken)
        if (!response.success) {
            setError(
                where = "RatedVideosViewModel.unblockUser",
                message = response.message ?: "Failed to unblock user"
            )
        }
    }

    /**
     * Update a rating via API call
     */
    fun updateRating(videoId: String, love: Int, faith: Int, hope: Int) {
        makeIOCall {
            try {
                _isLoading.value = true
                val userToken = requireUserToken("RatedVideosViewModel.updateRating") ?: return@makeIOCall

                val ratingRequest = RatingRequest(
                    videoId = videoId,
                    videoTitle = "", // Not needed for update
                    videoThumbnail = "", // Not needed for update
                    videoChannel = "", // Not needed for update
                    love = love,
                    faith = faith,
                    hope = hope,
                    userToken = userToken,
                    language = "hr" // Default language
                )

                val response = updateRatingUseCase(ratingRequest)

                if (response.success) {
                    // Update local state to reflect the change
                    _videos.value = _videos.value.map { video ->
                        if (video.videoId == videoId) {
                            video.copy(
                                myLove = love,
                                myFaith = faith,
                                myHope = hope
                            )
                        } else {
                            video
                        }
                    }
                    // Persist the updated "my rating" locally.
                    val current = _videos.value.firstOrNull { it.videoId == videoId }
                    val ctx = appContext
                    if (ctx != null) {
                        RatingsPrefs.upsertMyRating(
                            context = ctx,
                            videoId = videoId,
                            videoTitle = current?.videoTitle.orEmpty(),
                            channelName = current?.channelName.orEmpty(),
                            thumbnail = current?.thumbnail.orEmpty(),
                            category = current?.category,
                            myLove = love,
                            myFaith = faith,
                            myHope = hope,
                            ratedAt = current?.ratedAt
                        )
                    }
                    _error.value = null
                } else {
                    _error.value = response.message ?: "Failed to update rating"
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a rating via API call
     */
    fun deleteRating(videoId: String) {
        makeIOCall {
            try {
                _isLoading.value = true
                val userToken = requireUserToken("RatedVideosViewModel.deleteRating") ?: return@makeIOCall

                val response = deleteRatingUseCase(videoId, userToken)

                if (response.success) {
                    // Remove from local state
                    _videos.value = _videos.value.filter { it.videoId != videoId }
                    appContext?.let { RatingsPrefs.removeMyRating(it, videoId) }
                    _error.value = null
                } else {
                    _error.value = response.message ?: "Failed to delete rating"
                }
            } catch (e: Exception) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(e)
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

}

/**
 * Factory for creating RatedVideosViewModel with singleton API client.
 */
class RatedVideosViewModelFactory(
    private val updateRatingUseCase: UpdateRatingUseCase,
    private val deleteRatingUseCase: DeleteRatingUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val unblockUserUseCase: UnblockUserUseCase,
    private val userTokenManager: UserTokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RatedVideosViewModel::class.java)) {
            return RatedVideosViewModel(
                updateRatingUseCase,
                deleteRatingUseCase,
                blockUserUseCase,
                unblockUserUseCase,
                userTokenManager
            ) as T
        }
        val e = IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        com.youtube.rating.android.sentry.SentryLogger.captureException(
            e,
            tags = mapOf("where" to "RatedVideosViewModelFactory.create")
        )
        error(e.message ?: "Unknown ViewModel class")
    }
}
