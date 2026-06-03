package com.youtube.rating.android.domain.usecase

import java.util.concurrent.TimeUnit

class HomeRandomRequestUseCase {
    enum class Outcome {
        LOCKED,
        WAIT_FOR_LOAD,
        ALREADY_SHUFFLING,
        NOT_ENOUGH_VIDEOS,
        SHUFFLE
    }

    data class Result(
        val outcome: Outcome,
        val timestamps: List<Long>,
        val shouldLock: Boolean
    )

    fun execute(
        isLocked: Boolean,
        timestamps: List<Long>,
        nowMs: Long,
        hasMore: Boolean,
        isShuffling: Boolean,
        listSize: Int
    ): Result {
        if (isLocked) {
            return Result(
                outcome = Outcome.LOCKED,
                timestamps = timestamps,
                shouldLock = true
            )
        }

        val cutoff = nowMs - TimeUnit.MINUTES.toMillis(2)
        val recent = timestamps.filter { it >= cutoff }.toMutableList()
        recent.add(nowMs)

        if (recent.size > 80) {
            return Result(
                outcome = Outcome.LOCKED,
                timestamps = recent,
                shouldLock = true
            )
        }

        val outcome = when {
            hasMore -> Outcome.WAIT_FOR_LOAD
            isShuffling -> Outcome.ALREADY_SHUFFLING
            listSize <= 1 -> Outcome.NOT_ENOUGH_VIDEOS
            else -> Outcome.SHUFFLE
        }
        return Result(
            outcome = outcome,
            timestamps = recent,
            shouldLock = false
        )
    }
}

