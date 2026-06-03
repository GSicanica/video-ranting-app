package com.youtube.rating.android.network

import com.youtube.rating.shared.network.NetworkPolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OkHttpClients {
    val uploadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(NetworkPolicy.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .callTimeout(12, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()
    }

    val debugUploadTestClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
