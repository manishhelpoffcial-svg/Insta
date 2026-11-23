package com.example.data.network

import android.content.Context
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object MediaClient {

    @Volatile
    private var sharedOkHttpClient: OkHttpClient? = null

    @Volatile
    private var sharedImageLoader: ImageLoader? = null

    /**
     * OkHttpClient configured with no-referrer policy and clean mobile User-Agent
     * so Instagram CDN images & videos load without 403/hotlink blocks.
     */
    fun getOkHttpClient(): OkHttpClient {
        return sharedOkHttpClient ?: synchronized(this) {
            sharedOkHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .removeHeader("Referer")
                        .removeHeader("Origin")
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                        )
                        .build()
                    chain.proceed(request)
                }
                .build()
                .also { sharedOkHttpClient = it }
        }
    }

    fun getImageLoader(context: Context): ImageLoader {
        return sharedImageLoader ?: synchronized(this) {
            sharedImageLoader ?: ImageLoader.Builder(context.applicationContext)
                .okHttpClient { getOkHttpClient() }
                .crossfade(true)
                .build()
                .also { sharedImageLoader = it }
        }
    }
}
