package com.example.hitsterapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val title: String,
    val artists: String,
    val releaseYear: Int
)
