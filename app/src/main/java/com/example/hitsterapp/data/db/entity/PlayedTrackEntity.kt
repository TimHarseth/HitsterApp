package com.example.hitsterapp.data.db.entity

import androidx.room.Entity

@Entity(
    tableName = "played_tracks",
    primaryKeys = ["trackId", "playlistId"]
)
data class PlayedTrackEntity(
    val trackId: String,
    val playlistId: String
)
