package com.example.hitsterapp.di

import com.example.hitsterapp.data.network.AuthInterceptor
import com.example.hitsterapp.data.network.SpotifyApiService
import com.example.hitsterapp.data.network.SpotifyTokenService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("accounts")
    fun provideAccountsRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://accounts.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSpotifyTokenService(@Named("accounts") retrofit: Retrofit): SpotifyTokenService =
        retrofit.create(SpotifyTokenService::class.java)

    @Provides
    @Singleton
    @Named("api")
    fun provideApiRetrofit(authInterceptor: AuthInterceptor): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.spotify.com/v1/")
            .client(OkHttpClient.Builder().addInterceptor(authInterceptor).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSpotifyApiService(@Named("api") retrofit: Retrofit): SpotifyApiService =
        retrofit.create(SpotifyApiService::class.java)
}
