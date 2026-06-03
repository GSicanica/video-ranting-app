package com.youtube.rating.android.repository

data class PrayerPage(
    val items: List<PrayerRequest>,
    val hasMore: Boolean
)

data class PrayerRequest(
    val id: String,
    val authorName: String?,
    val text: String,
    val createdAtEpochMs: Long,
    val prayedCount: Int,
    val encouragementCount: Int,
    val iPrayed: Boolean,
    val tags: List<String> = emptyList()
)

data class Encouragement(
    val id: String,
    val requestId: String,
    val authorName: String?,
    val message: String,
    val createdAtEpochMs: Long
)



interface PrayerRepository {
    suspend fun loadRequests(page: Int, pageSize: Int, forceRefresh: Boolean = false): PrayerPage
    suspend fun createRequest(text: String, authorName: String?): PrayerRequest
    suspend fun togglePrayed(requestId: String, prayed: Boolean): PrayerRequest
    suspend fun loadEncouragements(requestId: String): List<Encouragement>
    suspend fun addEncouragement(requestId: String, message: String, authorName: String?): Encouragement
    suspend fun deleteRequest(requestId: String, adminToken: String): Boolean
}
