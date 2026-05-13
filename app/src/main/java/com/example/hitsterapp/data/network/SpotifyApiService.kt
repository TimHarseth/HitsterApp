package com.example.hitsterapp.data.network

import com.example.hitsterapp.data.network.model.PlaylistResponse
import com.example.hitsterapp.data.network.model.TracksPage
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyApiService {
    @GET("playlists/{id}")
    suspend fun getPlaylist(@Path("id") playlistId: String): PlaylistResponse

    @GET("playlists/{id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("id") playlistId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 100
    ): TracksPage
}
