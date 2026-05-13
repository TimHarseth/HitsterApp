package com.example.hitsterapp.data.repository

import android.content.Context
import com.example.hitsterapp.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SpotifyRemoteRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile private var appRemote: SpotifyAppRemote? = null

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        val params = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote
                if (cont.isActive) cont.resume(true)
            }

            override fun onFailure(error: Throwable) {
                if (cont.isActive) cont.resume(false)
            }
        })

        cont.invokeOnCancellation {
            appRemote?.let { SpotifyAppRemote.disconnect(it) }
            appRemote = null
        }
    }

    fun play(trackId: String) {
        appRemote?.playerApi?.play("spotify:track:$trackId")
    }

    fun pause() {
        appRemote?.playerApi?.pause()
    }

    fun resume() {
        appRemote?.playerApi?.resume()
    }

    fun disconnect() {
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }
}
