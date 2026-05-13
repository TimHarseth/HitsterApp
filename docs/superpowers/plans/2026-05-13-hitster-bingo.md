# Hitster Bingo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a personal Android app that plays shuffled Spotify songs for Hitster Bingo, hides song identity until revealed, and persists per-playlist progress across sessions.

**Architecture:** MVVM with Repository layer. Spotify PKCE OAuth for Web API access; Spotify App Remote SDK for in-app playback; Room for local track + progress storage; DataStore for token persistence.

**Tech Stack:** Kotlin, Jetpack Compose, Room + KSP, Hilt, Retrofit + Gson, DataStore, Compose Navigation, Spotify App Remote SDK (local AAR), AndroidX Browser (Custom Tabs), MockK + coroutines-test

---

## File Map

```
app/libs/
  spotify-app-remote-release-0.8.0.aar       (downloaded manually)

secrets.properties                            (gitignored — created manually)

gradle/libs.versions.toml                     (modify — add all new deps)
settings.gradle.kts                           (modify — add JitPack)
build.gradle.kts                              (modify — add hilt + ksp plugins)
app/build.gradle.kts                          (modify — add plugins, deps, buildConfig)
app/src/main/AndroidManifest.xml              (modify — permissions, intent filter, singleTop)

app/src/main/java/com/example/hitsterapp/
  HitsterApplication.kt                       (create — @HiltAndroidApp)
  MainActivity.kt                             (modify — auth flow, nav host)

  auth/
    PkceUtil.kt                               (create — code verifier + challenge)
    SpotifyAuthManager.kt                     (create — PKCE launch + callback + token refresh)

  data/
    db/
      HitsterDatabase.kt                      (create — Room database)
      entity/
        PlaylistEntity.kt                     (create)
        TrackEntity.kt                        (create)
        PlayedTrackEntity.kt                  (create)
      dao/
        PlaylistDao.kt                        (create)
        TrackDao.kt                           (create)
        PlayedTrackDao.kt                     (create)
    datastore/
      TokenDataStore.kt                       (create — access/refresh token storage)
    network/
      model/
        PlaylistResponse.kt                   (create — Spotify API response models)
        TokenResponse.kt                      (create)
      AuthInterceptor.kt                      (create — adds Bearer token to API requests)
      SpotifyApiService.kt                    (create — Retrofit interface for Web API)
      SpotifyTokenService.kt                  (create — Retrofit interface for token exchange)
    repository/
      PlaylistRepository.kt                   (create — fetch/save/delete/progress)
      SpotifyRemoteRepository.kt              (create — connect/play/pause/disconnect)

  di/
    DatabaseModule.kt                         (create — Room DAOs)
    NetworkModule.kt                          (create — Retrofit instances)

  util/
    SpotifyUrlParser.kt                       (create — extract playlist ID from URL)

  ui/
    navigation/
      Screen.kt                               (create — route constants)
      HitsterNavGraph.kt                      (create — NavHost)
    home/
      HomeViewModel.kt                        (create)
      HomeScreen.kt                           (create)
    player/
      PlayerViewModel.kt                      (create)
      PlayerScreen.kt                         (create)

app/src/test/java/com/example/hitsterapp/
  util/SpotifyUrlParserTest.kt               (create)
  data/repository/PlaylistRepositoryTest.kt  (create)
  ui/home/HomeViewModelTest.kt               (create)
  ui/player/PlayerViewModelTest.kt           (create)
```

---

## Task 1: Project Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `secrets.properties` (gitignored)
- Create: `app/libs/` directory + App Remote AAR

---

- [ ] **Step 1: Download Spotify App Remote AAR**

Go to https://github.com/spotify/android-sdk/releases/latest and download `spotify-app-remote-release-0.8.0.aar` (or whatever the latest version is). Create the directory and place the file there:

```
C:\Users\Tim\projects\hitster\app\libs\spotify-app-remote-release-0.8.0.aar
```

- [ ] **Step 2: Create secrets.properties**

Create `C:\Users\Tim\projects\hitster\secrets.properties` (this file is already in `.gitignore`):

```properties
SPOTIFY_CLIENT_ID=ec1123c21bcd4befac5e084dfac7ee18
SPOTIFY_REDIRECT_URI=hitsterapp://callback
```

- [ ] **Step 3: Register Android app in Spotify Developer Dashboard**

Go to https://developer.spotify.com/dashboard, open the **Hitster Bingo** app, click **Settings**, and add:
- **Package name:** `com.example.hitsterapp`
- **SHA-1 fingerprint:** run this in a terminal to get it:

```bash
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

Copy the `SHA1:` line from the output and paste it into the dashboard. Save.

- [ ] **Step 4: Update `gradle/libs.versions.toml`**

Replace the entire file with:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.10.0"
activityCompose = "1.11.0"
composeBom = "2024.09.00"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
datastore = "1.1.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
navigationCompose = "2.8.5"
browser = "1.8.0"
mockk = "1.13.12"
coroutinesTest = "1.8.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 5: Update `settings.gradle.kts`** — add JitPack repository

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "HitsterApp"
include(":app")
```

- [ ] **Step 6: Update root `build.gradle.kts`** — add Hilt + KSP plugins

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 7: Update `app/build.gradle.kts`**

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val secrets = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) secrets.load(secretsFile.inputStream())

android {
    namespace = "com.example.hitsterapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hitsterapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SPOTIFY_CLIENT_ID",
            "\"${secrets.getProperty("SPOTIFY_CLIENT_ID", "")}\"")
        buildConfigField("String", "SPOTIFY_REDIRECT_URI",
            "\"${secrets.getProperty("SPOTIFY_REDIRECT_URI", "hitsterapp://callback")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.navigation.compose)
    implementation(libs.browser)

    implementation(fileTree("libs") { include("*.aar") })

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 8: Update `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <queries>
        <package android:name="com.spotify.music" />
    </queries>

    <application
        android:name=".HitsterApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.HitsterApp">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/Theme.HitsterApp">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="hitsterapp" android:host="callback" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 9: Sync Gradle and verify the project builds**

In Android Studio: **File → Sync Project with Gradle Files**. The build should succeed with no errors. If the App Remote AAR is missing, you'll get a compile error — go back to Step 1.

---

## Task 2: Hilt Application + Room Database

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/HitsterApplication.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/entity/PlaylistEntity.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/entity/TrackEntity.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/entity/PlayedTrackEntity.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/dao/PlaylistDao.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/dao/TrackDao.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/dao/PlayedTrackDao.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/db/HitsterDatabase.kt`
- Create: `app/src/main/java/com/example/hitsterapp/di/DatabaseModule.kt`

---

- [ ] **Step 1: Create `HitsterApplication.kt`**

```kotlin
package com.example.hitsterapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HitsterApplication : Application()
```

- [ ] **Step 2: Create `PlaylistEntity.kt`**

```kotlin
package com.example.hitsterapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val totalTrackCount: Int
)
```

- [ ] **Step 3: Create `TrackEntity.kt`**

```kotlin
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
```

- [ ] **Step 4: Create `PlayedTrackEntity.kt`**

```kotlin
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
```

- [ ] **Step 5: Create `PlaylistDao.kt`**

```kotlin
package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithProgress(
    val id: String,
    val name: String,
    val url: String,
    val totalTrackCount: Int,
    val playedCount: Int
)

@Dao
interface PlaylistDao {
    @Query("""
        SELECT p.id, p.name, p.url, p.totalTrackCount,
        COUNT(pt.trackId) AS playedCount
        FROM playlists p
        LEFT JOIN played_tracks pt ON p.id = pt.playlistId
        GROUP BY p.id
        ORDER BY p.name ASC
    """)
    fun observeAllWithProgress(): Flow<List<PlaylistWithProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deleteById(playlistId: String)
}
```

- [ ] **Step 6: Create `TrackDao.kt`**

```kotlin
package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.TrackEntity

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("""
        SELECT * FROM tracks
        WHERE playlistId = :playlistId
        AND id NOT IN (SELECT trackId FROM played_tracks WHERE playlistId = :playlistId)
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomUnplayedTrack(playlistId: String): TrackEntity?

    @Query("SELECT COUNT(*) FROM tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCount(playlistId: String): Int

    @Query("DELETE FROM tracks WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: String)
}
```

- [ ] **Step 7: Create `PlayedTrackDao.kt`**

```kotlin
package com.example.hitsterapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.hitsterapp.data.db.entity.PlayedTrackEntity

@Dao
interface PlayedTrackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playedTrack: PlayedTrackEntity)

    @Query("SELECT COUNT(*) FROM played_tracks WHERE playlistId = :playlistId")
    suspend fun getPlayedCount(playlistId: String): Int

    @Query("DELETE FROM played_tracks WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: String)
}
```

- [ ] **Step 8: Create `HitsterDatabase.kt`**

```kotlin
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
```

- [ ] **Step 9: Create `DatabaseModule.kt`**

```kotlin
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
```

- [ ] **Step 10: Build the project**

In Android Studio click **Build → Make Project**. Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/HitsterApplication.kt \
        app/src/main/java/com/example/hitsterapp/data/db/ \
        app/src/main/java/com/example/hitsterapp/di/DatabaseModule.kt
git commit -m "feat: add Hilt application and Room database"
```

---

## Task 3: DataStore + Spotify Auth

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/data/datastore/TokenDataStore.kt`
- Create: `app/src/main/java/com/example/hitsterapp/auth/PkceUtil.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/network/model/TokenResponse.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/network/SpotifyTokenService.kt`
- Create: `app/src/main/java/com/example/hitsterapp/auth/SpotifyAuthManager.kt`
- Create: `app/src/main/java/com/example/hitsterapp/di/NetworkModule.kt` (partial — token service only)

---

- [ ] **Step 1: Create `TokenDataStore.kt`**

```kotlin
package com.example.hitsterapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tokens")

@Singleton
class TokenDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val expiresAtKey = longPreferencesKey("expires_at")

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
            prefs[expiresAtKey] = expiresAt
        }
    }

    suspend fun getAccessToken(): String? =
        context.dataStore.data.first()[accessTokenKey]

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.first()[refreshTokenKey]

    suspend fun getExpiresAt(): Long =
        context.dataStore.data.first()[expiresAtKey] ?: 0L
}
```

- [ ] **Step 2: Create `PkceUtil.kt`**

```kotlin
package com.example.hitsterapp.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PkceUtil {
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(96)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(
            verifier.toByteArray(Charsets.US_ASCII)
        )
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
```

- [ ] **Step 3: Create `TokenResponse.kt`**

```kotlin
package com.example.hitsterapp.data.network.model

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int
)
```

- [ ] **Step 4: Create `SpotifyTokenService.kt`**

```kotlin
package com.example.hitsterapp.data.network

import com.example.hitsterapp.data.network.model.TokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SpotifyTokenService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun exchangeCode(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): TokenResponse

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): TokenResponse
}
```

- [ ] **Step 5: Create `NetworkModule.kt`** (accounts Retrofit only — API Retrofit added in Task 4)

```kotlin
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
```

- [ ] **Step 6: Create `SpotifyAuthManager.kt`**

```kotlin
package com.example.hitsterapp.auth

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.example.hitsterapp.BuildConfig
import com.example.hitsterapp.data.datastore.TokenDataStore
import com.example.hitsterapp.data.network.SpotifyTokenService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthManager @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val spotifyTokenService: SpotifyTokenService
) {
    private var pendingCodeVerifier: String? = null

    fun launchAuth(activity: Activity) {
        val verifier = PkceUtil.generateCodeVerifier()
        pendingCodeVerifier = verifier
        val challenge = PkceUtil.generateCodeChallenge(verifier)

        val uri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", BuildConfig.SPOTIFY_CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", BuildConfig.SPOTIFY_REDIRECT_URI)
            .appendQueryParameter("scope", "streaming playlist-read-private playlist-read-collaborative")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()

        CustomTabsIntent.Builder().build().launchUrl(activity, uri)
    }

    suspend fun handleCallback(code: String): Boolean {
        val verifier = pendingCodeVerifier ?: return false
        return try {
            val response = spotifyTokenService.exchangeCode(
                code = code,
                redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI,
                clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                codeVerifier = verifier
            )
            tokenDataStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: "",
                expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
            )
            pendingCodeVerifier = null
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasValidAuth(): Boolean =
        tokenDataStore.getRefreshToken()?.isNotEmpty() == true

    suspend fun getValidAccessToken(): String? {
        val token = tokenDataStore.getAccessToken()
        val expiresAt = tokenDataStore.getExpiresAt()
        if (token != null && System.currentTimeMillis() < expiresAt - 60_000L) return token

        val refreshToken = tokenDataStore.getRefreshToken()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val response = spotifyTokenService.refreshToken(
                refreshToken = refreshToken,
                clientId = BuildConfig.SPOTIFY_CLIENT_ID
            )
            tokenDataStore.saveTokens(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken ?: refreshToken,
                expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)
            )
            response.accessToken
        } catch (e: Exception) {
            null
        }
    }
}
```

- [ ] **Step 7: Build to verify**

**Build → Make Project.** Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/auth/ \
        app/src/main/java/com/example/hitsterapp/data/datastore/ \
        app/src/main/java/com/example/hitsterapp/data/network/model/TokenResponse.kt \
        app/src/main/java/com/example/hitsterapp/data/network/SpotifyTokenService.kt \
        app/src/main/java/com/example/hitsterapp/di/NetworkModule.kt
git commit -m "feat: add DataStore token storage and Spotify PKCE auth manager"
```

---

## Task 4: Spotify Web API + URL Parser

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/util/SpotifyUrlParser.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/network/model/PlaylistResponse.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/network/AuthInterceptor.kt`
- Create: `app/src/main/java/com/example/hitsterapp/data/network/SpotifyApiService.kt`
- Modify: `app/src/main/java/com/example/hitsterapp/di/NetworkModule.kt`
- Create: `app/src/test/java/com/example/hitsterapp/util/SpotifyUrlParserTest.kt`

---

- [ ] **Step 1: Write the failing test for `SpotifyUrlParser`**

Create `app/src/test/java/com/example/hitsterapp/util/SpotifyUrlParserTest.kt`:

```kotlin
package com.example.hitsterapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyUrlParserTest {

    @Test
    fun `extracts id from standard playlist url`() {
        val id = SpotifyUrlParser.extractPlaylistId(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        )
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", id)
    }

    @Test
    fun `extracts id from url with query params`() {
        val id = SpotifyUrlParser.extractPlaylistId(
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc123"
        )
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", id)
    }

    @Test
    fun `returns null for invalid url`() {
        assertNull(SpotifyUrlParser.extractPlaylistId("https://open.spotify.com/track/abc"))
    }

    @Test
    fun `returns null for empty string`() {
        assertNull(SpotifyUrlParser.extractPlaylistId(""))
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL**

In Android Studio right-click the test class → Run. Expected: compilation error (class not found).

- [ ] **Step 3: Create `SpotifyUrlParser.kt`**

```kotlin
package com.example.hitsterapp.util

object SpotifyUrlParser {
    private val PLAYLIST_REGEX = Regex("spotify\\.com/playlist/([A-Za-z0-9]+)")

    fun extractPlaylistId(url: String): String? =
        PLAYLIST_REGEX.find(url)?.groupValues?.get(1)
}
```

- [ ] **Step 4: Run the test — expect PASS**

All 4 tests should pass.

- [ ] **Step 5: Create `PlaylistResponse.kt`**

```kotlin
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
```

- [ ] **Step 6: Create `AuthInterceptor.kt`**

```kotlin
package com.example.hitsterapp.data.network

import com.example.hitsterapp.auth.SpotifyAuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val spotifyAuthManager: SpotifyAuthManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { spotifyAuthManager.getValidAccessToken() }
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${token.orEmpty()}")
            .build()
        return chain.proceed(request)
    }
}
```

- [ ] **Step 7: Create `SpotifyApiService.kt`**

```kotlin
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
```

- [ ] **Step 8: Update `NetworkModule.kt`** — add API Retrofit + AuthInterceptor

```kotlin
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
```

- [ ] **Step 9: Build and run tests**

**Build → Make Project.** Expected: BUILD SUCCESSFUL. Re-run `SpotifyUrlParserTest` — all 4 tests pass.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/util/ \
        app/src/main/java/com/example/hitsterapp/data/network/ \
        app/src/main/java/com/example/hitsterapp/di/NetworkModule.kt \
        app/src/test/java/com/example/hitsterapp/util/
git commit -m "feat: add Spotify Web API client and URL parser"
```

---

## Task 5: PlaylistRepository

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/data/repository/PlaylistRepository.kt`
- Create: `app/src/test/java/com/example/hitsterapp/data/repository/PlaylistRepositoryTest.kt`

---

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/example/hitsterapp/data/repository/PlaylistRepositoryTest.kt`:

```kotlin
package com.example.hitsterapp.data.repository

import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.PlaylistWithProgress
import com.example.hitsterapp.data.db.dao.TrackDao
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.network.SpotifyApiService
import com.example.hitsterapp.data.network.model.PlaylistResponse
import com.example.hitsterapp.data.network.model.SpotifyAlbum
import com.example.hitsterapp.data.network.model.SpotifyArtist
import com.example.hitsterapp.data.network.model.SpotifyTrack
import com.example.hitsterapp.data.network.model.TrackItem
import com.example.hitsterapp.data.network.model.TracksPage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PlaylistRepositoryTest {

    private val playlistDao: PlaylistDao = mockk(relaxed = true)
    private val trackDao: TrackDao = mockk(relaxed = true)
    private val playedTrackDao: PlayedTrackDao = mockk(relaxed = true)
    private val spotifyApiService: SpotifyApiService = mockk()
    private val spotifyAuthManager: SpotifyAuthManager = mockk()

    private lateinit var repository: PlaylistRepository

    @Before
    fun setup() {
        repository = PlaylistRepository(
            playlistDao, trackDao, playedTrackDao, spotifyApiService, spotifyAuthManager
        )
    }

    @Test
    fun `fetchAndSavePlaylist saves playlist and tracks to Room`() = runTest {
        coEvery { spotifyAuthManager.getValidAccessToken() } returns "token"
        coEvery { spotifyApiService.getPlaylist("abc123") } returns PlaylistResponse(
            id = "abc123",
            name = "My Playlist",
            tracks = TracksPage(
                items = listOf(
                    TrackItem(SpotifyTrack("t1", "Song One",
                        listOf(SpotifyArtist("Artist A")), SpotifyAlbum("1990-05-01")))
                ),
                next = null
            )
        )

        repository.fetchAndSavePlaylist("https://open.spotify.com/playlist/abc123")

        coVerify {
            playlistDao.insert(PlaylistEntity("abc123", "My Playlist",
                "https://open.spotify.com/playlist/abc123", 1))
        }
        coVerify {
            trackDao.insertAll(listOf(
                TrackEntity("t1", "abc123", "Song One", "Artist A", 1990)
            ))
        }
    }

    @Test
    fun `fetchAndSavePlaylist throws on invalid url`() = runTest {
        var threw = false
        try {
            repository.fetchAndSavePlaylist("https://not-spotify.com/thing")
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assert(threw)
    }

    @Test
    fun `observePlaylists maps DAO data to ui models`() = runTest {
        every { playlistDao.observeAllWithProgress() } returns flowOf(
            listOf(PlaylistWithProgress("p1", "Rock Hits", "https://url", 50, 12))
        )

        val result = mutableListOf<List<PlaylistUiModel>>()
        repository.observePlaylists().collect { result.add(it) }

        assertEquals(1, result.first().size)
        assertEquals("Rock Hits", result.first()[0].name)
        assertEquals(50, result.first()[0].totalCount)
        assertEquals(12, result.first()[0].playedCount)
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Expected: compilation errors (PlaylistRepository and PlaylistUiModel not found).

- [ ] **Step 3: Create `PlaylistRepository.kt`**

```kotlin
package com.example.hitsterapp.data.repository

import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.data.db.dao.PlayedTrackDao
import com.example.hitsterapp.data.db.dao.PlaylistDao
import com.example.hitsterapp.data.db.dao.TrackDao
import com.example.hitsterapp.data.db.entity.PlayedTrackEntity
import com.example.hitsterapp.data.db.entity.PlaylistEntity
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.network.SpotifyApiService
import com.example.hitsterapp.util.SpotifyUrlParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistUiModel(
    val id: String,
    val name: String,
    val url: String,
    val totalCount: Int,
    val playedCount: Int
)

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val trackDao: TrackDao,
    private val playedTrackDao: PlayedTrackDao,
    private val spotifyApiService: SpotifyApiService,
    private val spotifyAuthManager: SpotifyAuthManager
) {
    fun observePlaylists(): Flow<List<PlaylistUiModel>> =
        playlistDao.observeAllWithProgress().map { list ->
            list.map { p ->
                PlaylistUiModel(p.id, p.name, p.url, p.totalTrackCount, p.playedCount)
            }
        }

    suspend fun fetchAndSavePlaylist(url: String) {
        val playlistId = SpotifyUrlParser.extractPlaylistId(url)
            ?: throw IllegalArgumentException("Invalid Spotify playlist URL: $url")

        spotifyAuthManager.getValidAccessToken()
            ?: throw IllegalStateException("Not authenticated with Spotify")

        val playlistInfo = spotifyApiService.getPlaylist(playlistId)

        val allItems = mutableListOf<com.example.hitsterapp.data.network.model.TrackItem>()
        allItems.addAll(playlistInfo.tracks.items)
        var nextPage = playlistInfo.tracks.next
        var offset = playlistInfo.tracks.items.size

        while (nextPage != null) {
            val page = spotifyApiService.getPlaylistTracks(playlistId, offset)
            allItems.addAll(page.items)
            nextPage = page.next
            offset += page.items.size
        }

        val validTracks = allItems.mapNotNull { it.track }

        playlistDao.insert(
            PlaylistEntity(
                id = playlistId,
                name = playlistInfo.name,
                url = url,
                totalTrackCount = validTracks.size
            )
        )

        trackDao.insertAll(
            validTracks.map { track ->
                TrackEntity(
                    id = track.id,
                    playlistId = playlistId,
                    title = track.name,
                    artists = track.artists.joinToString(", ") { it.name },
                    releaseYear = track.album.releaseDate.take(4).toIntOrNull() ?: 0
                )
            }
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        playedTrackDao.deleteByPlaylistId(playlistId)
        trackDao.deleteByPlaylistId(playlistId)
        playlistDao.deleteById(playlistId)
    }

    suspend fun resetPlaylistProgress(playlistId: String) {
        playedTrackDao.deleteByPlaylistId(playlistId)
    }

    suspend fun getRandomUnplayedTrack(playlistId: String): TrackEntity? =
        trackDao.getRandomUnplayedTrack(playlistId)

    suspend fun getTrackCount(playlistId: String): Int =
        trackDao.getTrackCount(playlistId)

    suspend fun getPlayedCount(playlistId: String): Int =
        playedTrackDao.getPlayedCount(playlistId)

    suspend fun markTrackAsPlayed(playlistId: String, trackId: String) {
        playedTrackDao.insert(PlayedTrackEntity(trackId = trackId, playlistId = playlistId))
    }
}
```

- [ ] **Step 4: Run the tests — expect PASS**

All 3 tests should pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/data/repository/PlaylistRepository.kt \
        app/src/test/java/com/example/hitsterapp/data/repository/
git commit -m "feat: add PlaylistRepository with fetch, save, and progress tracking"
```

---

## Task 6: Spotify App Remote Repository

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/data/repository/SpotifyRemoteRepository.kt`

---

- [ ] **Step 1: Create `SpotifyRemoteRepository.kt`**

```kotlin
package com.example.hitsterapp.data.repository

import android.content.Context
import com.example.hitsterapp.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SpotifyRemoteRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var appRemote: SpotifyAppRemote? = null

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        val params = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                appRemote = remote
                if (cont.isActive) cont.resume(true)
            }

            override fun onFailure(error: Throwable) {
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    fun play(trackId: String) {
        appRemote?.playerApi?.play("spotify:track:$trackId")
    }

    fun pause() {
        appRemote?.playerApi?.pause()
    }

    fun resume() {
        appRemote?.playerApi?.resume()
    }

    fun disconnect() {
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }
}
```

- [ ] **Step 2: Build to verify**

**Build → Make Project.** Expected: BUILD SUCCESSFUL. (If the App Remote AAR is missing you'll see `Unresolved reference: SpotifyAppRemote` — check `app/libs/`.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/data/repository/SpotifyRemoteRepository.kt
git commit -m "feat: add SpotifyRemoteRepository wrapping App Remote SDK"
```

---

## Task 7: Navigation + MainActivity

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/example/hitsterapp/ui/navigation/HitsterNavGraph.kt`
- Modify: `app/src/main/java/com/example/hitsterapp/MainActivity.kt`

---

- [ ] **Step 1: Create `Screen.kt`**

```kotlin
package com.example.hitsterapp.ui.navigation

object Screen {
    const val HOME = "home"
    const val PLAYER = "player/{playlistId}"

    fun player(playlistId: String) = "player/$playlistId"
}
```

- [ ] **Step 2: Create `HitsterNavGraph.kt`**

This is a stub — the Home and Player screens will be filled in Tasks 8 and 9. Use placeholder composables for now.

```kotlin
package com.example.hitsterapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun HitsterNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.HOME) {
        composable(Screen.HOME) {
            // Replaced in Task 8
            androidx.compose.material3.Text("Home — coming soon")
        }
        composable(
            route = Screen.PLAYER,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
        ) {
            // Replaced in Task 9
            androidx.compose.material3.Text("Player — coming soon")
        }
    }
}
```

- [ ] **Step 3: Replace `MainActivity.kt`**

```kotlin
package com.example.hitsterapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.hitsterapp.auth.SpotifyAuthManager
import com.example.hitsterapp.ui.navigation.HitsterNavGraph
import com.example.hitsterapp.ui.theme.HitsterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private enum class AuthState { Loading, AwaitingCallback, Authenticated }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var spotifyAuthManager: SpotifyAuthManager

    private val authState = mutableStateOf(AuthState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            authState.value = if (spotifyAuthManager.hasValidAuth()) {
                AuthState.Authenticated
            } else {
                spotifyAuthManager.launchAuth(this@MainActivity)
                AuthState.AwaitingCallback
            }
        }

        setContent {
            HitsterTheme {
                when (authState.value) {
                    AuthState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    AuthState.AwaitingCallback -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("Connecting to Spotify…")
                    }
                    AuthState.Authenticated -> HitsterNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data ?: return
        if (data.scheme == "hitsterapp" && data.host == "callback") {
            val code = data.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                if (spotifyAuthManager.handleCallback(code)) {
                    authState.value = AuthState.Authenticated
                }
            }
        }
    }
}
```

- [ ] **Step 4: Build and run on device/emulator**

**Run the app.** Expected flow:
1. Loading spinner briefly
2. Chrome Custom Tab opens Spotify login (or grants access silently if already logged in)
3. Browser redirects back to app
4. "Home — coming soon" placeholder text is shown

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/MainActivity.kt \
        app/src/main/java/com/example/hitsterapp/ui/navigation/
git commit -m "feat: add navigation scaffold and Spotify auth flow in MainActivity"
```

---

## Task 8: Home Screen

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/example/hitsterapp/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/hitsterapp/ui/navigation/HitsterNavGraph.kt`
- Create: `app/src/test/java/com/example/hitsterapp/ui/home/HomeViewModelTest.kt`

---

- [ ] **Step 1: Write the failing ViewModel tests**

Create `app/src/test/java/com/example/hitsterapp/ui/home/HomeViewModelTest.kt`:

```kotlin
package com.example.hitsterapp.ui.home

import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.PlaylistUiModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PlaylistRepository = mockk(relaxed = true)
    private lateinit var viewModel: HomeViewModel

    private val samplePlaylists = listOf(
        PlaylistUiModel("p1", "Rock Hits", "https://url1", 50, 10),
        PlaylistUiModel("p2", "Pop Mix", "https://url2", 30, 0)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observePlaylists() } returns flowOf(samplePlaylists)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `playlists are exposed from repository`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(samplePlaylists, viewModel.playlists.value)
    }

    @Test
    fun `addPlaylist delegates to repository`() = runTest {
        viewModel.addPlaylist("https://open.spotify.com/playlist/abc")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.fetchAndSavePlaylist("https://open.spotify.com/playlist/abc") }
    }

    @Test
    fun `deletePlaylist delegates to repository`() = runTest {
        viewModel.deletePlaylist("p1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.deletePlaylist("p1") }
    }

    @Test
    fun `resetPlaylist delegates to repository`() = runTest {
        viewModel.resetPlaylist("p1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.resetPlaylistProgress("p1") }
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Expected: compilation error (HomeViewModel not found).

- [ ] **Step 3: Create `HomeViewModel.kt`**

```kotlin
package com.example.hitsterapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.PlaylistUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistUiModel>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPlaylist(url: String) {
        viewModelScope.launch { repository.fetchAndSavePlaylist(url) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun resetPlaylist(playlistId: String) {
        viewModelScope.launch { repository.resetPlaylistProgress(playlistId) }
    }
}
```

- [ ] **Step 4: Run the tests — expect PASS**

All 4 tests should pass.

- [ ] **Step 5: Create `HomeScreen.kt`**

```kotlin
package com.example.hitsterapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hitsterapp.data.repository.PlaylistUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onPlaylistSelected: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var menuPlaylist by remember { mutableStateOf<PlaylistUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hitster Bingo") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add playlist")
            }
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text("No playlists yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = { onPlaylistSelected(playlist.id) },
                        onLongClick = { menuPlaylist = playlist }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlaylistDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url ->
                viewModel.addPlaylist(url)
                showAddDialog = false
            }
        )
    }

    menuPlaylist?.let { playlist ->
        PlaylistOptionsMenu(
            playlistName = playlist.name,
            onReset = {
                viewModel.resetPlaylist(playlist.id)
                menuPlaylist = null
            },
            onDelete = {
                viewModel.deletePlaylist(playlist.id)
                menuPlaylist = null
            },
            onDismiss = { menuPlaylist = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistCard(
    playlist: PlaylistUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${playlist.playedCount} / ${playlist.totalCount} songs played",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddPlaylistDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Spotify Playlist") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste Spotify playlist link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }, enabled = url.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PlaylistOptionsMenu(
    playlistName: String,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlistName) },
        text = {
            Column {
                TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset Progress")
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Playlist", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
```

- [ ] **Step 6: Wire `HomeScreen` into `HitsterNavGraph.kt`**

Replace the `composable(Screen.HOME)` block:

```kotlin
composable(Screen.HOME) {
    HomeScreen(
        onPlaylistSelected = { playlistId ->
            navController.navigate(Screen.player(playlistId))
        }
    )
}
```

- [ ] **Step 7: Run on device**

Launch the app. Expected:
- Home screen with "No playlists yet" message
- Tap + → dialog appears, paste a Spotify playlist URL → tap Add
- Playlist card appears with name and "0 / N songs played"
- Long-press a card → options menu shows Reset / Delete

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/hitsterapp/ui/home/ \
        app/src/main/java/com/example/hitsterapp/ui/navigation/HitsterNavGraph.kt \
        app/src/test/java/com/example/hitsterapp/ui/home/
git commit -m "feat: add Home screen with playlist list, add, delete, and reset"
```

---

## Task 9: Player Screen

**Files:**
- Create: `app/src/main/java/com/example/hitsterapp/ui/player/PlayerViewModel.kt`
- Create: `app/src/main/java/com/example/hitsterapp/ui/player/PlayerScreen.kt`
- Modify: `app/src/main/java/com/example/hitsterapp/ui/navigation/HitsterNavGraph.kt`
- Create: `app/src/test/java/com/example/hitsterapp/ui/player/PlayerViewModelTest.kt`

---

- [ ] **Step 1: Write the failing ViewModel tests**

Create `app/src/test/java/com/example/hitsterapp/ui/player/PlayerViewModelTest.kt`:

```kotlin
package com.example.hitsterapp.ui.player

import androidx.lifecycle.SavedStateHandle
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.SpotifyRemoteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: PlaylistRepository = mockk(relaxed = true)
    private val remoteRepository: SpotifyRemoteRepository = mockk(relaxed = true)

    private val sampleTrack = TrackEntity("t1", "p1", "Song One", "Artist A", 1990)

    private fun buildViewModel(): PlayerViewModel {
        val savedState = SavedStateHandle(mapOf("playlistId" to "p1"))
        return PlayerViewModel(repository, remoteRepository, savedState)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { remoteRepository.connect() } returns true
        coEvery { repository.getRandomUnplayedTrack("p1") } returns sampleTrack
        coEvery { repository.getPlayedCount("p1") } returns 0
        coEvery { repository.getTrackCount("p1") } returns 10
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with hidden song and playing state`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.uiState.value.isRevealed)
        assertTrue(vm.uiState.value.isPlaying)
        assertEquals("t1", vm.uiState.value.currentTrack?.id)
    }

    @Test
    fun `reveal sets isRevealed to true`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.reveal()
        assertTrue(vm.uiState.value.isRevealed)
    }

    @Test
    fun `togglePlayPause pauses when playing`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.togglePlayPause()
        assertFalse(vm.uiState.value.isPlaying)
        coVerify { remoteRepository.pause() }
    }

    @Test
    fun `next marks track as played and loads next`() = runTest {
        val secondTrack = TrackEntity("t2", "p1", "Song Two", "Artist B", 2000)
        coEvery { repository.getRandomUnplayedTrack("p1") } returnsMany listOf(sampleTrack, secondTrack)

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.markTrackAsPlayed("p1", "t1") }
        assertEquals("t2", vm.uiState.value.currentTrack?.id)
        assertFalse(vm.uiState.value.isRevealed)
    }

    @Test
    fun `next sets isComplete when no more tracks`() = runTest {
        coEvery { repository.getRandomUnplayedTrack("p1") } returnsMany listOf(sampleTrack, null)
        coEvery { repository.getTrackCount("p1") } returns 10
        coEvery { repository.getPlayedCount("p1") } returns 10

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.next()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isComplete)
        assertEquals(10, vm.uiState.value.totalCount)
    }
}
```

- [ ] **Step 2: Run the tests — expect FAIL**

Expected: compilation error (PlayerViewModel not found).

- [ ] **Step 3: Create `PlayerViewModel.kt`**

```kotlin
package com.example.hitsterapp.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitsterapp.data.db.entity.TrackEntity
import com.example.hitsterapp.data.repository.PlaylistRepository
import com.example.hitsterapp.data.repository.SpotifyRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackUiModel(
    val id: String,
    val title: String,
    val artists: String,
    val releaseYear: Int
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isRevealed: Boolean = false,
    val currentTrack: TrackUiModel? = null,
    val isComplete: Boolean = false,
    val playedCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val remoteRepository: SpotifyRemoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val connected = remoteRepository.connect()
            if (connected) loadAndPlayNextTrack()
        }
    }

    private suspend fun loadAndPlayNextTrack() {
        val track = repository.getRandomUnplayedTrack(playlistId)
        if (track == null) {
            val total = repository.getTrackCount(playlistId)
            _uiState.update { it.copy(isComplete = true, totalCount = total, playedCount = total) }
            return
        }
        val played = repository.getPlayedCount(playlistId)
        val total = repository.getTrackCount(playlistId)
        _uiState.update {
            it.copy(
                currentTrack = track.toUiModel(),
                isRevealed = false,
                isPlaying = true,
                playedCount = played,
                totalCount = total
            )
        }
        remoteRepository.play(track.id)
    }

    fun togglePlayPause() {
        val playing = _uiState.value.isPlaying
        if (playing) remoteRepository.pause() else remoteRepository.resume()
        _uiState.update { it.copy(isPlaying = !playing) }
    }

    fun reveal() {
        _uiState.update { it.copy(isRevealed = true) }
    }

    fun next() {
        viewModelScope.launch {
            val currentId = _uiState.value.currentTrack?.id ?: return@launch
            repository.markTrackAsPlayed(playlistId, currentId)
            loadAndPlayNextTrack()
        }
    }

    override fun onCleared() {
        remoteRepository.disconnect()
        super.onCleared()
    }
}

private fun TrackEntity.toUiModel() = TrackUiModel(
    id = id,
    title = title,
    artists = artists,
    releaseYear = releaseYear
)
```

- [ ] **Step 4: Run the tests — expect PASS**

All 5 tests should pass.

- [ ] **Step 5: Create `PlayerScreen.kt`**

```kotlin
package com.example.hitsterapp.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isComplete) {
        LaunchedEffect(Unit) {
            // Delay so the dialog is visible before navigation
        }
        PlaylistCompleteDialog(
            playedCount = state.totalCount,
            onDismiss = onNavigateBack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.togglePlayPause()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${state.playedCount} / ${state.totalCount}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Song reveal area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (state.isRevealed && state.currentTrack != null) {
                    val track = state.currentTrack
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = track.artists,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = track.releaseYear.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pause / Play
                IconButton(
                    onClick = viewModel::togglePlayPause,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reveal
                    OutlinedButton(
                        onClick = viewModel::reveal,
                        enabled = !state.isRevealed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reveal Song")
                    }

                    // Next
                    Button(
                        onClick = viewModel::next,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next Song")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistCompleteDialog(playedCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playlist Complete!") },
        text = { Text("You've played all $playedCount / $playedCount songs. Head back home to reset and play again.") },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Go to Home") }
        }
    )
}
```

- [ ] **Step 6: Wire `PlayerScreen` into `HitsterNavGraph.kt`**

Replace the `composable(Screen.PLAYER, ...)` block:

```kotlin
composable(
    route = Screen.PLAYER,
    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
) {
    PlayerScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 7: Run on device — full end-to-end test**

1. Home screen loads → tap a playlist card
2. Player screen opens → `?` is shown, music starts playing in background
3. Tap **Pause** → music pauses, icon changes to Play
4. Tap **Play** → music resumes
5. Tap **Reveal Song** → song title, artist, and year appear
6. Tap **Next Song** → `?` reappears, next song starts
7. After all songs played → completion dialog shows correct count → tapping "Go to Home" returns to Home screen with 48 / 48
8. Back arrow on Player → returns to Home, music pauses

- [ ] **Step 8: Commit and push**

```bash
git add app/src/main/java/com/example/hitsterapp/ui/player/ \
        app/src/main/java/com/example/hitsterapp/ui/navigation/HitsterNavGraph.kt \
        app/src/test/java/com/example/hitsterapp/ui/player/
git commit -m "feat: add Player screen with play/pause, reveal, next, and completion dialog"
git push
```

---

## Self-Review Checklist

| Spec requirement | Covered in task |
|---|---|
| Spotify PKCE OAuth | Task 3 |
| Playlist URL → fetch tracks → save to Room | Task 4 + 5 |
| Multiple saved playlists | Task 5 + 8 |
| Per-playlist played progress persisted | Task 2 (PlayedTrackEntity) + Task 5 |
| Shuffle, no repeats | Task 2 (TrackDao.getRandomUnplayedTrack) |
| In-app playback (no Spotify switch) | Task 6 |
| Pause / Play toggle | Task 9 |
| Reveal: title, artist(s), year | Task 9 |
| Next song | Task 9 |
| Completion dialog → back to Home | Task 9 |
| Reset progress | Task 8 |
| Delete playlist | Task 8 |
| Home screen progress display (X/Y) | Task 8 |
| Secrets not committed to git | Task 1 |
