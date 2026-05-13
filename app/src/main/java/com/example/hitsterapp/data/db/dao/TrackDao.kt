package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.TrackEntity

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("""
        SELECT * FROM tracks
        WHERE playlistId = :playlistId
        AND id NOT IN (SELECT trackId FROM played_tracks WHERE playlistId = :playlistId)
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomUnplayedTrack(playlistId: String): TrackEntity?

    @Query("SELECT COUNT(*) FROM tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCount(playlistId: String): Int

    @Query("DELETE FROM tracks WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: String)
}
