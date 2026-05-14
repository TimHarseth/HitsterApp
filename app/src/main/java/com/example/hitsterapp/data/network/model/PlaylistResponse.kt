package com.example.hitsterapp.data.network.model

import com.google.gson.annotations.SerializedName

data class PlaylistResponse(
    val name: String,
    val items: TracksPage?
)

data class TracksPage(
    val items: List<TrackItem>?,
    val next: String?
)

data class TrackItem(
    @SerializedName("item") val track: SpotifyTrack?
)

data class SpotifyTrack(
    val id: String?,
    val name: String,
    val artists: List<SpotifyArtist>?,
    val album: SpotifyAlbum?,
    @SerializedName("external_urls") val externalUrls: SpotifyExternalUrls?
)

data class SpotifyArtist(
    val name: String
)

data class SpotifyAlbum(
    @SerializedName("release_date") val releaseDate: String
)

data class SpotifyExternalUrls(
    val spotify: String?
)
