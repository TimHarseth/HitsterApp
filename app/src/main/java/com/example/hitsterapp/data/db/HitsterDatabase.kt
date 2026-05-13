package com.example.hitsterapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.TrackDao
import com.example.hitsterapp.data.db.entity.PlayedTrackEntity
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import com.example.hitsterapp.data.db.entity.TrackEntity

@Database(
    entities = [PlaylistEntity::class, TrackEntity::class, PlayedTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HitsterDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun trackDao(): TrackDao
    abstract fun playedTrackDao(): PlayedTrackDao
}
