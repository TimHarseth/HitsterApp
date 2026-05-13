package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithProgress(
    val id: String,
    val name: String,
    val url: String,
    val totalTrackCount: Int,
    val playedCount: Int
)

@Dao
interface PlaylistDao {
    @Query("""
        SELECT p.id, p.name, p.url, p.totalTrackCount,
        COUNT(pt.trackId) AS playedCount
        FROM playlists p
        LEFT JOIN played_tracks pt ON p.id = pt.playlistId
        GROUP BY p.id
        ORDER BY p.name ASC
    """)
    fun observeAllWithProgress(): Flow<List<PlaylistWithProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: String)
}
