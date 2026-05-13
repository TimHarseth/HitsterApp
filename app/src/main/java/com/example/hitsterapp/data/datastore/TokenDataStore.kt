package com.example.hitsterapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tokens")

@Singleton
class TokenDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val expiresAtKey = longPreferencesKey("expires_at")

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
            prefs[expiresAtKey] = expiresAt
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[accessTokenKey]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[refreshTokenKey]

    suspend fun getExpiresAt(): Long =
        context.dataStore.data.first()[expiresAtKey] ?: 0L
}
