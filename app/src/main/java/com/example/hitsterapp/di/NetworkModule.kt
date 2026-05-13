package com.example.hitsterapp.di

import com.example.hitsterapp.data.network.SpotifyTokenService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}
