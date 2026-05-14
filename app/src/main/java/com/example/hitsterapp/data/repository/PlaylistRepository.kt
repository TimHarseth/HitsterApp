package com.example.hitsterapp.data.repository

import androidx.room.withTransaction
import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.data.db.HitsterDatabase
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.TrackDao
import com.example.hitsterapp.data.db.entity.PlayedTrackEntity
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.network.SpotifyApiService
import com.example.hitsterapp.data.network.model.TrackItem
import com.example.hitsterapp.util.SpotifyUrlParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistUiModel(
    val id: String,
    val name: String,
    val url: String,
    val totalCount: Int,
    val playedCount: Int
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val db: HitsterDatabase,
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val playedTrackDao: PlayedTrackDao,
    private val spotifyApiService: SpotifyApiService,
    private val spotifyAuthManager: SpotifyAuthManager
) {
    fun observePlaylists(): Flow<List<PlaylistUiModel>> =
        playlistDao.observeAllWithProgress().map { list ->
            list.map { p ->
                PlaylistUiModel(p.id, p.name, p.url, p.totalTrackCount, p.playedCount)
            }
        }

    suspend fun fetchAndSavePlaylist(url: String) {
        val playlistId = SpotifyUrlParser.extractPlaylistId(url)
            ?: throw IllegalArgumentException("Invalid Spotify playlist URL: $url")

        spotifyAuthManager.getValidAccessToken()
            ?: throw IllegalStateException("Not authenticated with Spotify")

        val playlistInfo = spotifyApiService.getPlaylist(playlistId)

        val allItems = mutableListOf<TrackItem>()
        var nextUrl: String? = null
        playlistInfo.items?.let { firstPage ->
            allItems.addAll(firstPage.items.orEmpty())
            nextUrl = firstPage.next
        }
        while (nextUrl != null) {
            val page = spotifyApiService.getTracksPage(nextUrl!!)
            allItems.addAll(page.items.orEmpty())
            nextUrl = if (page.items.isNullOrEmpty()) null else page.next
        }

        val validTracks = allItems.mapNotNull { it.track }.filter { it.id != null }

        db.withTransaction {
            playlistDao.insert(
                PlaylistEntity(
                    id = playlistId,
                    name = playlistInfo.name,
                    url = url,
                    totalTrackCount = validTracks.size
                )
            )
            trackDao.insertAll(
                validTracks.map { track ->
                    TrackEntity(
                        id = track.id!!,
                        playlistId = playlistId,
                        title = track.name,
                        artists = track.artists.orEmpty().joinToString(", ") { it.name },
                        releaseYear = track.album?.releaseDate?.take(4)?.toIntOrNull() ?: 0
                    )
                }
            )
        }
    }

    suspend fun deletePlaylist(playlistId: String) {
        playedTrackDao.deleteByPlaylistId(playlistId)
        trackDao.deleteByPlaylistId(playlistId)
        playlistDao.deleteById(playlistId)
    }

    suspend fun resetPlaylistProgress(playlistId: String) {
        playedTrackDao.deleteByPlaylistId(playlistId)
    }

    suspend fun getRandomUnplayedTrack(playlistId: String): TrackEntity? =
        trackDao.getRandomUnplayedTrack(playlistId)

    suspend fun getTrackCount(playlistId: String): Int =
        trackDao.getTrackCount(playlistId)

    suspend fun getPlayedCount(playlistId: String): Int =
        playedTrackDao.getPlayedCount(playlistId)

    suspend fun markTrackAsPlayed(playlistId: String, trackId: String) {
        playedTrackDao.insert(PlayedTrackEntity(trackId = trackId, playlistId = playlistId))
    }
}
