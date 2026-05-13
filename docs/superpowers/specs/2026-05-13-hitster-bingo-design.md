# Hitster Bingo App — Design Spec

**Date:** 2026-05-13  
**Platform:** Android (personal use, sideloaded)  
**Min SDK:** 26 (Android 8.0)

---

## Overview

A personal Android app for playing Hitster Bingo — a variant of the Hitster board game where songs are played from a Spotify playlist without needing physical cards or a QR code scanner. The app plays music silently (song identity hidden) and reveals title, artist(s), and release year on demand.

---

## Screens & Navigation

### Home Screen
- Lists all saved playlists as cards
- Each card shows: playlist name and progress (e.g. `12 / 48 songs played`)
- **Add Playlist** button — user pastes a Spotify playlist URL; app fetches and saves all tracks
- Tap a playlist card to open the Player Screen
- Long-press a card to show options: **Reset Progress** or **Delete Playlist**

### Player Screen
- Large central area displaying `?` while the song is hidden
- On Reveal: shows song **title**, **artist(s)**, and **release year**
- Three buttons: **Pause / Play**, **Reveal Song**, **Next Song**
- **Next Song**: marks current track as played, hides reveal panel, picks next random unplayed track, starts playing it
- **Back arrow**: returns to Home, pauses music
- When the last unplayed track is played and the user taps **Next Song**: shows a completion dialog ("Playlist complete! 48 / 48 songs played"), then navigates back to Home where the playlist card shows 48 / 48. The played history is NOT automatically reset — the user must manually reset from Home.

### Navigation
- Compose Navigation: `Home → Player`
- Back from Player always returns to Home

---

## Architecture

**Pattern:** MVVM with Repository layer

### ViewModels
- `HomeViewModel` — loads and manages saved playlists list
- `PlayerViewModel` — manages current playlist state, current track, playback state, reveal state, played-song tracking

### Repository
- `PlaylistRepository` — coordinates between Spotify Web API and Room database
- `SpotifyRemoteRepository` — wraps Spotify App Remote SDK for playback control

---

## Data Layer

### Room Database

**`playlists` table**
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | Spotify playlist ID (extracted from URL) |
| name | TEXT | Playlist display name |
| url | TEXT | Full Spotify playlist URL |
| total_track_count | INTEGER | Total number of tracks |

**`tracks` table**
| Column | Type | Notes |
|---|---|---|
| id | TEXT PK | Spotify track ID |
| playlist_id | TEXT FK | References playlists.id |
| title | TEXT | Track title |
| artists | TEXT | Comma-separated artist name(s) |
| release_year | INTEGER | Year extracted from release_date |

**`played_tracks` table**
| Column | Type | Notes |
|---|---|---|
| track_id | TEXT | Spotify track ID |
| playlist_id | TEXT | References playlists.id |
| PK | | (track_id, playlist_id) composite |

### DataStore (Preferences)
- Spotify OAuth access token
- Spotify OAuth refresh token
- Token expiry timestamp

---

## Spotify Integration

### Authentication
- OAuth2 with PKCE via `spotify-android-auth` library
- Login triggered on first launch or when token is expired
- Tokens stored in DataStore, refresh handled automatically
- **Client ID:** stored in `secrets.properties` (gitignored), injected at build time via `BuildConfig`
- **Redirect URI:** `hitsterapp://callback`

### Spotify Web API (Retrofit)
Used once per playlist to fetch and save all track data:
- `GET /playlists/{id}` — fetches playlist name and track list
- Tracks paginated (max 100 per request); app loops until all tracks fetched
- Metadata saved to Room — no further API calls needed during gameplay

### Spotify App Remote SDK
Used during the Player screen for playback control:
- `connect()` on Player screen entry
- `playTrack(spotifyUri)` — plays `spotify:track:{id}` without switching to Spotify app
- `pause()` / `resume()` for the Pause/Play button
- `disconnect()` on back navigation to Home

---

## Key App Flows

### Adding a Playlist
1. User pastes Spotify URL (e.g. `https://open.spotify.com/playlist/abc123`)
2. App extracts playlist ID from URL
3. Calls Spotify Web API to fetch all tracks (paginated)
4. Saves playlist + tracks to Room
5. Home screen updates with new playlist card showing `0 / N songs played`

### Playing a Session
1. User taps a playlist card → navigates to Player
2. App Remote connects to Spotify in background
3. Picks a random track from `tracks` WHERE NOT IN `played_tracks` for this playlist
4. Sends `spotify:track:{id}` to App Remote → music starts
5. Player shows `?` — song identity hidden
6. **Pause/Play** — toggles App Remote playback
7. **Reveal** — shows title, artist(s), year from Room (no API call)
8. **Next** — inserts current track into `played_tracks`, hides reveal, picks next random unplayed track, plays it
9. If no unplayed tracks remain → shows completion dialog → navigates to Home

### Resuming a Playlist
- Tap a previously started playlist → Player picks up from remaining unplayed tracks
- Played history is preserved across app restarts

### Resetting a Playlist
- Long-press playlist card on Home → "Reset Progress"
- Deletes all rows in `played_tracks` for that playlist
- Playlist card returns to `0 / N songs played`

---

## Dependencies to Add

| Library | Purpose |
|---|---|
| `spotify-android-auth` | OAuth2 Spotify login |
| `spotify-app-remote` | In-app playback control |
| Retrofit + OkHttp | Spotify Web API calls |
| Room | Local database |
| Hilt | Dependency injection |
| Compose Navigation | Screen routing |
| DataStore (Preferences) | Token storage |

---

## Secrets Setup

`secrets.properties` (gitignored, never committed):
```
SPOTIFY_CLIENT_ID=<your_client_id>
SPOTIFY_REDIRECT_URI=hitsterapp://callback
```

Exposed to code via `BuildConfig` using the Secrets Gradle Plugin.

---

## Out of Scope

- iOS / multiplatform support
- Multiple user accounts
- Cloud sync of played history
- Bingo card generation / win detection (the app is just the music player part)
