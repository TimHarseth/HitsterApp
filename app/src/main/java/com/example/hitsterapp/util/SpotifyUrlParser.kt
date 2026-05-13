package com.example.hitsterapp.util

object SpotifyUrlParser {
    private val PLAYLIST_REGEX = Regex("spotify\\.com/playlist/([A-Za-z0-9]+)")

    fun extractPlaylistId(url: String): String? =
        PLAYLIST_REGEX.find(url)?.groupValues?.get(1)
}
