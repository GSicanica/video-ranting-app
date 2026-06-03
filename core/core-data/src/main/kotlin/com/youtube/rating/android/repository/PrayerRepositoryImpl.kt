package com.youtube.rating.android.repository

import com.youtube.rating.core.coroutines.ioDispatcher

import com.youtube.rating.shared.data.AddEncouragementBody
import com.youtube.rating.shared.data.CreatePrayerRequestBody
import com.youtube.rating.shared.data.SetPrayerPrayedBody
import com.youtube.rating.android.network.RatingApi
import com.youtube.rating.shared.data.EncouragementDto
import com.youtube.rating.shared.data.PrayerRequestDto
import com.youtube.rating.android.utils.UserTokenManager
import com.youtube.rating.android.utils.TextDecoding
import com.youtube.rating.android.utils.CoroutineTracing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrayerRepositoryImpl(
    private val api: RatingApi,
    private val userTokenManager: UserTokenManager
) : PrayerRepository {

    private suspend fun getUserTokenOrNull(): String? {
        return CoroutineTracing.traceSuspend("PrayerRepository.getUserTokenOrNull") {
            withContext(ioDispatcher) {
                runCatching { userTokenManager.getUserTokenAsync() }.getOrNull()
            }
        }
    }

    private fun tokenErrorRequest(message: String): PrayerRequest {
        val now = System.currentTimeMillis()
        return PrayerRequest(
            id = "error_$now",
            authorName = null,
            text = message,
            createdAtEpochMs = now,
            prayedCount = 0,
            encouragementCount = 0,
            iPrayed = false,
            tags = emptyList()
        )
    }

    private fun tokenErrorEncouragement(requestId: String, message: String): Encouragement {
        val now = System.currentTimeMillis()
        return Encouragement(
            id = "error_$now",
            requestId = requestId,
            authorName = null,
            message = message,
            createdAtEpochMs = now
        )
    }

    override suspend fun loadRequests(page: Int, pageSize: Int, forceRefresh: Boolean): PrayerPage {
        return CoroutineTracing.traceSuspend("PrayerRepository.loadRequests") {
            withContext(ioDispatcher) {
            val userToken = getUserTokenOrNull().orEmpty()
            if (userToken.isBlank()) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(IllegalStateException("User token is not available"))
                return@withContext PrayerPage(items = emptyList(), hasMore = false)
            }
            val dto = api.getPrayerRequests(
                userToken = userToken,
                page = page,
                pageSize = pageSize,
                forceRefresh = forceRefresh
            )

            PrayerPage(
                items = dto.items.map { it.toModel() },
                hasMore = dto.hasMore
            )
            }
        }
    }

    override suspend fun createRequest(text: String, authorName: String?): PrayerRequest {
        return CoroutineTracing.traceSuspend("PrayerRepository.createRequest") {
            withContext(ioDispatcher) {
            val userToken = getUserTokenOrNull().orEmpty()
            if (userToken.isBlank()) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(IllegalStateException("User token is not available"))
                return@withContext tokenErrorRequest("⚠️ Nema user token-a (ne mogu poslati molitvu).")
            }
            val dto = api.createPrayerRequest(
                body = CreatePrayerRequestBody(
                    userToken = userToken,
                    text = text,
                    authorName = authorName
                )
            )
            dto.toModel()
            }
        }
    }

    override suspend fun togglePrayed(requestId: String, prayed: Boolean): PrayerRequest {
        return CoroutineTracing.traceSuspend("PrayerRepository.togglePrayed") {
            withContext(ioDispatcher) {
            val userToken = getUserTokenOrNull().orEmpty()
            if (userToken.isBlank()) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(IllegalStateException("User token is not available"))
                return@withContext tokenErrorRequest("⚠️ Nema user token-a (ne mogu označiti molitvu).")
            }
            com.youtube.rating.shared.utils.Logger.info("PrayerRepository", "togglePrayed requestId=$requestId prayed=$prayed")
            val dto = api.setPrayerPrayed(
                requestId = requestId,
                body = SetPrayerPrayedBody(
                    userToken = userToken,
                    prayed = prayed
                )
            )
            dto.toModel()
            }
        }
    }

    override suspend fun loadEncouragements(requestId: String): List<Encouragement> {
        return CoroutineTracing.traceSuspend("PrayerRepository.loadEncouragements") {
            withContext(ioDispatcher) {
            val userToken = getUserTokenOrNull().orEmpty()
            if (userToken.isBlank()) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(IllegalStateException("User token is not available"))
                return@withContext emptyList()
            }
            val list = api.getPrayerEncouragements(
                requestId = requestId,
                userToken = userToken
            )
            list.map { it.toModel() }
            }
        }
    }

    override suspend fun addEncouragement(
        requestId: String,
        message: String,
        authorName: String?
    ): Encouragement {
        return CoroutineTracing.traceSuspend("PrayerRepository.addEncouragement") {
            withContext(ioDispatcher) {
            val userToken = getUserTokenOrNull().orEmpty()
            if (userToken.isBlank()) {
                com.youtube.rating.android.sentry.SentryLogger.captureException(IllegalStateException("User token is not available"))
                return@withContext tokenErrorEncouragement(
                    requestId = requestId,
                    message = "⚠️ Nema user token-a (ne mogu poslati poruku)."
                )
            }
            val dto = api.addPrayerEncouragement(
                requestId = requestId,
                body = AddEncouragementBody(
                    userToken = userToken,
                    message = message,
                    authorName = authorName
                )
            )
            dto.toModel()
            }
        }
    }

    override suspend fun deleteRequest(requestId: String, adminToken: String): Boolean {
        return CoroutineTracing.traceSuspend("PrayerRepository.deleteRequest") {
            withContext(ioDispatcher) {
            val response = api.deletePrayerRequest(requestId, adminToken)
            response.success
            }
        }
    }
}

fun PrayerRequestDto.toModel(): PrayerRequest = PrayerRequest(
    id = id,
    authorName = authorName,
    text = TextDecoding.decodePossiblyEncodedText(text),
    createdAtEpochMs = createdAtEpochMs,
    prayedCount = prayedCount,
    encouragementCount = encouragementCount,
    iPrayed = iPrayed,
    tags = tags
)

fun EncouragementDto.toModel(): Encouragement = Encouragement(
    id = id,
    requestId = requestId,
    authorName = authorName,
    message = TextDecoding.decodePossiblyEncodedText(message),
    createdAtEpochMs = createdAtEpochMs
)
