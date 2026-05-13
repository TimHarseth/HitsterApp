package com.example.hitsterapp.data.network

import com.example.hitsterapp.auth.SpotifyAuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val spotifyAuthManager: SpotifyAuthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { spotifyAuthManager.getValidAccessToken() }
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${token.orEmpty()}")
            .build()
        return chain.proceed(request)
    }
}
