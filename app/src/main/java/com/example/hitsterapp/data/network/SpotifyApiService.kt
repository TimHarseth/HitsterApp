package com.example.hitsterapp.data.network

import com.example.hitsterapp.data.network.model.PlaylistResponse
import com.example.hitsterapp.data.network.model.TracksPage
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface SpotifyApiService {
    @GET("playlists/{id}")
    suspend fun getPlaylist(
        @Path("id") playlistId: String,
        @Query("market") market: String = "NO"
    ): PlaylistResponse

    @GET
    suspend fun getTracksPage(@Url url: String): TracksPage
}
