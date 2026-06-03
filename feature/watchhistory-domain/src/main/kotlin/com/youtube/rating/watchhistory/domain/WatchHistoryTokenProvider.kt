package com.youtube.rating.watchhistory.domain

interface WatchHistoryTokenProvider {
    suspend fun getUserToken(): String?
}
