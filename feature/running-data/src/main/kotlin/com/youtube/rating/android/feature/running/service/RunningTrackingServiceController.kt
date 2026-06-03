package com.youtube.rating.android.feature.running.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.youtube.rating.android.feature.running.domain.RunningTrackingServiceController

class RunningTrackingServiceControllerImpl(
    private val context: Context,
) : RunningTrackingServiceController {

    override fun start(): Boolean {
        val intent = Intent(context, RunningTrackingService::class.java).apply {
            action = RunningTrackingService.ACTION_START
        }
        return runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.isSuccess
    }

    override fun stop(): Boolean {
        val intent = Intent(context, RunningTrackingService::class.java).apply {
            action = RunningTrackingService.ACTION_STOP
        }
        return runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.recoverCatching {
            context.startService(intent)
        }.isSuccess
    }
}
