package com.example.hitsterapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.ui.navigation.HitsterNavGraph
import com.example.hitsterapp.ui.theme.HitsterAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class AuthState { Loading, AwaitingCallback, Authenticated }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var spotifyAuthManager: SpotifyAuthManager

    private val authState = mutableStateOf(AuthState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            authState.value = if (spotifyAuthManager.hasValidAuth()) {
                AuthState.Authenticated
            } else {
                spotifyAuthManager.launchAuth(this@MainActivity)
                AuthState.AwaitingCallback
            }
        }

        setContent {
            HitsterAppTheme {
                when (authState.value) {
                    AuthState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    AuthState.AwaitingCallback -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Connecting to Spotify…")
                    }
                    AuthState.Authenticated -> HitsterNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data ?: return
        if (data.scheme == "hitsterapp" && data.host == "callback") {
            val error = data.getQueryParameter("error")
            if (error != null) {
                spotifyAuthManager.launchAuth(this@MainActivity)
                return
            }
            val code = data.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                if (spotifyAuthManager.handleCallback(code)) {
                    authState.value = AuthState.Authenticated
                } else {
                    spotifyAuthManager.launchAuth(this@MainActivity)
                }
            }
        }
    }
}
