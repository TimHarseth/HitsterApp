package com.example.hitsterapp.data.repository

import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.data.db.HitsterDatabase
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.PlaylistWithProgress
import com.example.hitsterapp.data.db.dao.TrackDao
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.network.SpotifyApiService
import com.example.hitsterapp.data.network.model.PlaylistResponse
import com.example.hitsterapp.data.network.model.SpotifyAlbum
import com.example.hitsterapp.data.network.model.SpotifyArtist
import com.example.hitsterapp.data.network.model.SpotifyTrack
import com.example.hitsterapp.data.network.model.TrackItem
import com.example.hitsterapp.data.network.model.TracksPage
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaylistRepositoryTest {

    private val db: HitsterDatabase = mockk(relaxed = true)
    private val playlistDao: PlaylistDao = mockk(relaxed = true)
    private val trackDao: TrackDao = mockk(relaxed = true)
    private val playedTrackDao: PlayedTrackDao = mockk(relaxed = true)
    private val spotifyApiService: SpotifyApiService = mockk()
    private val spotifyAuthManager: SpotifyAuthManager = mockk()

    private lateinit var repository: PlaylistRepository

    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (args[1] as suspend () -> Unit).invoke()
        }
        repository = PlaylistRepository(
            db, playlistDao, trackDao, playedTrackDao, spotifyApiService, spotifyAuthManager
        )
    }

    @Test
    fun `fetchAndSavePlaylist saves playlist and tracks to Room`() = runTest {
        coEvery { spotifyAuthManager.getValidAccessToken() } returns "token"
        coEvery { spotifyApiService.getPlaylist("abc123") } returns PlaylistResponse(
            id = "abc123",
            name = "My Playlist",
            tracks = TracksPage(
                items = listOf(
                    TrackItem(SpotifyTrack("t1", "Song One",
                        listOf(SpotifyArtist("Artist A")), SpotifyAlbum("1990-05-01")))
                ),
                next = null
            )
        )

        repository.fetchAndSavePlaylist("https://open.spotify.com/playlist/abc123")

        coVerify {
            playlistDao.insert(PlaylistEntity("abc123", "My Playlist",
                "https://open.spotify.com/playlist/abc123", 1))
        }
        coVerify {
            trackDao.insertAll(listOf(
                TrackEntity("t1", "abc123", "Song One", "Artist A", 1990)
            ))
        }
    }

    @Test
    fun `fetchAndSavePlaylist throws on invalid url`() = runTest {
        var threw = false
        try {
            repository.fetchAndSavePlaylist("https://not-spotify.com/thing")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assert(threw)
    }

    @Test
    fun `observePlaylists maps DAO data to ui models`() = runTest {
        every { playlistDao.observeAllWithProgress() } returns flowOf(
            listOf(PlaylistWithProgress("p1", "Rock Hits", "https://url", 50, 12))
        )

        val result = mutableListOf<List<PlaylistUiModel>>()
        repository.observePlaylists().collect { result.add(it) }

        assertEquals(1, result.first().size)
        assertEquals("Rock Hits", result.first()[0].name)
        assertEquals(50, result.first()[0].totalCount)
        assertEquals(12, result.first()[0].playedCount)
    }
}
