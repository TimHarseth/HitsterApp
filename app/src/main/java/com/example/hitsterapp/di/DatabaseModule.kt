package com.example.hitsterapp.di

import android.content.Context
import androidx.room.Room
import com.example.hitsterapp.data.db.HitsterDatabase
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HitsterDatabase =
        Room.databaseBuilder(context, HitsterDatabase::class.java, "hitster.db").build()

    @Provides
    fun providePlaylistDao(db: HitsterDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideTrackDao(db: HitsterDatabase): TrackDao = db.trackDao()

    @Provides
    fun providePlayedTrackDao(db: HitsterDatabase): PlayedTrackDao = db.playedTrackDao()
}
