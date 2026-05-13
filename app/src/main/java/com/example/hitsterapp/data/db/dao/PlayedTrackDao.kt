package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.PlayedTrackEntity

@Dao
interface PlayedTrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playedTrack: PlayedTrackEntity)

    @Query("SELECT COUNT(*) FROM played_tracks WHERE playlistId = :playlistId")
    suspend fun getPlayedCount(playlistId: String): Int

    @Query("DELETE FROM played_tracks WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: String)
}
