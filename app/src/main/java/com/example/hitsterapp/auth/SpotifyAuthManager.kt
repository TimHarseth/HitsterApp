package com.example.hitsterapp.auth

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.example.hitsterapp.BuildConfig
import com.example.hitsterapp.data.datastore.TokenDataStore
import com.example.hitsterapp.data.network.SpotifyTokenService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthManager @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val spotifyTokenService: SpotifyTokenService
) {
    private var pendingCodeVerifier: String? = null

    fun launchAuth(activity: Activity) {
        val verifier = PkceUtil.generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = PkceUtil.generateCodeChallenge(verifier)

        val uri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", BuildConfig.SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("scope", "streaming playlist-read-private playlist-read-collaborative")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        CustomTabsIntent.Builder().build().launchUrl(activity, uri)
    }

    suspend fun handleCallback(code: String): Boolean {
        val verifier = pendingCodeVerifier ?: return false
        return try {
            val response = spotifyTokenService.exchangeCode(
                code = code,
                redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                codeVerifier = verifier
            )
            tokenDataStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: "",
                expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
            )
            pendingCodeVerifier = null
            true
        } catch (e: Exception) {
            Log.e("SpotifyAuth", "Token exchange failed", e)
            false
        }
    }

    suspend fun hasValidAuth(): Boolean =
        tokenDataStore.getRefreshToken()?.isNotEmpty() == true

    suspend fun getValidAccessToken(): String? {
        val token = tokenDataStore.getAccessToken()
        val expiresAt = tokenDataStore.getExpiresAt()
        if (token != null && System.currentTimeMillis() < expiresAt - 60_000L) return token

        val refreshToken = tokenDataStore.getRefreshToken()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val response = spotifyTokenService.refreshToken(
                refreshToken = refreshToken,
                clientId = BuildConfig.SPOTIFY_CLIENT_ID
            )
            tokenDataStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: refreshToken,
                expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
            )
            response.accessToken
        } catch (e: Exception) {
            Log.e("SpotifyAuth", "Token refresh failed", e)
            null
        }
    }
}
