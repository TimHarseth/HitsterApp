package com.example.hitsterapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyUrlParserTest {

    @Test
    fun `extracts id from standard playlist url`() {
        val id = SpotifyUrlParser.extractPlaylistId(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        )
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", id)
    }

    @Test
    fun `extracts id from url with query params`() {
        val id = SpotifyUrlParser.extractPlaylistId(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"
        )
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", id)
    }

    @Test
    fun `returns null for invalid url`() {
        assertNull(SpotifyUrlParser.extractPlaylistId("https://open.spotify.com/track/abc"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(SpotifyUrlParser.extractPlaylistId(""))
    }

    @Test
    fun `extracts id containing underscores and hyphens`() {
        val id = SpotifyUrlParser.extractPlaylistId(
            "https://open.spotify.com/playlist/some_playlist-id"
        )
        assertEquals("some_playlist-id", id)
    }
}
