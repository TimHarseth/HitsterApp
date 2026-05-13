package com.example.hitsterapp.data.network.model

import com.google.gson.annotations.SerializedName

data class PlaylistResponse(
    val id: String,
    val name: String,
    val tracks: TracksPage
)

data class TracksPage(
    val items: List<TrackItem>,
    val next: String?
)

data class TrackItem(
    val track: SpotifyTrack?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum
)

data class SpotifyArtist(
    val name: String
)

data class SpotifyAlbum(
    @SerializedName("release_date") val releaseDate: String
)
