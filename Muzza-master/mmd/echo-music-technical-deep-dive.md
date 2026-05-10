# Echo Music - ANALISIS TEKNIS DETAIL

## 1. API ANALYSIS: Dari Mana Musik Distreaming

### YouTube Music InnerTube API

#### Apa itu InnerTube?
InnerTube adalah API **internal Google** yang digunakan YouTube Music client (official). Ini bukan public API seperti YouTube Data API v3.

#### Cara Kerja
```
┌─────────────────────────────────────┐
│  Official YouTube Music Client      │
│  (Mobile/Web)                       │
└────────────────┬────────────────────┘
                 │
         Makes HTTP requests to
                 │
         ┌───────▼────────┐
         │ InnerTube API  │
         │ music.youtube. │
         │ com/youtubei   │
         └───────┬────────┘
                 │
         Returns JSON response with
         ├─ Video metadata
         ├─ Stream URL
         ├─ Lyrics (cached)
         └─ Recommendations

Echo Music:
└─ Mimics request dari official client
  ├─ Set User-Agent: "Mozilla/5.0... (Android)"
  ├─ Send client context: "ANDROID"
  ├─ Include cookies/session
  └─ Parse response sama seperti client
```

#### Request Example
```
POST /youtubei/v1/search HTTP/1.1
Host: music.youtube.com
Content-Type: application/json
User-Agent: Mozilla/5.0 (Linux; Android...)

{
  "context": {
    "client": {
      "clientName": "ANDROID_MUSIC",
      "clientVersion": "7.08.53"
    }
  },
  "query": "shape of you"
}
```

#### Response Structure
```json
{
  "contents": {
    "singleColumnBrowseResultsRenderer": {
      "tabs": [{
        "tabRenderer": {
          "content": {
            "sectionListRenderer": {
              "contents": [{
                "musicShelfRenderer": {
                  "contents": [
                    {
                      "musicResponsiveListItemRenderer": {
                        "flexColumns": [
                          {"text": {"runs": [{"text": "Shape of You"}]}},
                          {"text": {"runs": [{"text": "Ed Sheeran"}]}}
                        ],
                        "playlistItemData": {
                          "videoId": "JGwWNGJdvx8"
                        }
                      }
                    }
                  ]
                }
              }]
            }
          }
        }
      }]
    }
  }
}
```

#### Endpoints Utama
| Endpoint | Fungsi | Method |
|----------|--------|--------|
| `/youtubei/v1/search` | Search songs | POST |
| `/youtubei/v1/browse` | Browse categories/playlists | POST |
| `/youtubei/v1/player` | Get playback info & URL | POST |
| `/youtubei/v1/getQueue` | Get full playlist/queue | POST |

#### Rate Limiting
- YouTube Music: No strict rate limit untuk client official
- Echo Music: ~50 requests per minute aman
- Jika kena limit: 429 error, retry dengan exponential backoff

#### YouTube Detection & Blocking
```
YouTube sometimes blocks requests yang:
├─ Come dari non-official client
├─ Have abnormal user patterns
├─ Request too many resources
└─ Use suspicious User-Agent

Echo Music Mitigation:
├─ Rotate User-Agent strings
├─ Add realistic timing delays
├─ Limit concurrent requests
└─ Handle 403 errors gracefully
```

---

### 2. STREAMING URL EXTRACTION

#### Masalah
YouTube Music hanya mengembalikan **encrypted playback info**, bukan direct URL.

#### Solusi: youtube_explode_dart Library
```kotlin
// Modified untuk Echo Music
val videoId = "JGwWNGJdvx8"

YouTubeVideo video = youtube.videos.get(videoId)
var audioStreams = video.streams
    .audioOnly()
    .orderByBitrate(ascending: false)

// Ambil bitrate tertinggi
Stream audioStream = audioStreams.first()
Uri audioUrl = audioStream.url
```

#### Flow Extraction
```
1. Get videoId dari search result
   └─ e.g., "JGwWNGJdvx8"

2. Request /youtubei/v1/player
   ├─ Send videoId
   └─ Get signatureCipher untuk streams

3. Decrypt signatureCipher
   ├─ Extract signature code dari HTML
   ├─ Generate client.js URL
   └─ Decrypt using JavaScript decompiling

4. Extract Audio Stream
   ├─ Filter untuk audio-only (itag=251, 251, etc)
   ├─ Get highest bitrate
   └─ Get expirySeconds (usually 5-6 hours)

5. Return playable URL
   └─ Pass ke ExoPlayer
```

#### Stream Formats Available
```
Itag  Codec      Bitrate  Format
───────────────────────────────────
251   opus       ~128kbps mp4a/WebM
250   opus       ~128kbps mp4a/WebM
141   AAC        ~256kbps m4a
140   AAC        ~128kbps m4a
```

**Echo Music choice:** `opus 128kbps` (best quality + size ratio)

---

### 3. LYRIC SOURCES

#### Better Lyrics API
**Website:** https://better-lyrics.boidu.dev/

**Karakteristik:**
- Format: LRC (Lyric format standard)
- Word-level sync: ✅ YES
- Coverage: ~2 juta lagu
- Akurasi: HIGH
- Source: Community-maintained database

**Request:**
```http
GET /lyrics?artist=Ed%20Sheeran&title=Shape%20of%20You
```

**Response Format:**
```
[00:00.00]Shape of the You - Ed Sheeran
[00:00.50]by Ed Sheeran
[00:15.00]Eh, eh
[00:15.50]The club isn't the best place to find a lover
[00:20.00]So the bar is where I go
[00:24.00]Me and my friends at the table doing shots
[00:28.50]Drinking fast and then we talk slow
[00:31.50]Come over and start up a conversation
...
[03:15.00]⎎
```

LRC Format Explanation:
```
[MM:SS.CC] text
└─ MM = Minutes
└─ SS = Seconds
└─ CC = Centiseconds (0-99)
└─ text = Lyric text
```

#### YouTube Music Lyrics (Built-in)
**Source:** InnerTube API response

**Kelebihan:**
- Always available untuk streaming songs
- Integrated di response player

**Kekurangan:**
- Format tidak konsisten
- Timing kadang tidak akurat
- Word-sync tidak selalu tersedia

**Fallback Strategy:**
```kotlin
try {
    // 1. Better Lyrics (best quality)
    val lyrics = betterlyricsAPI.get(artist, title)
} catch {
    // 2. YouTube Music built-in
    val lyrics = innerTubeAPI.getLyricsFromPlayerResponse()
} catch {
    // 3. Local cache
    val lyrics = roomDatabase.getLyrics(songId)
} finally {
    // 4. Show "No lyrics" message
}
```

---

## 4. OFFLINE STORAGE ARCHITECTURE

### Room Database Schema

```sql
-- Main tables
CREATE TABLE songs (
    id TEXT PRIMARY KEY,
    videoId TEXT UNIQUE,
    title TEXT NOT NULL,
    artist TEXT NOT NULL,
    album TEXT,
    duration INTEGER,
    thumbnailUrl TEXT,
    isDownloaded BOOLEAN DEFAULT 0,
    downloadedDate LONG,
    filePath TEXT,
    fileSize INTEGER,
    isLiked BOOLEAN DEFAULT 0
);

CREATE TABLE playlists (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    createdDate LONG,
    isOffline BOOLEAN DEFAULT 0,
    coverUrl TEXT
);

CREATE TABLE playlist_songs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    playlistId TEXT NOT NULL,
    songId TEXT NOT NULL,
    position INTEGER,
    FOREIGN KEY(playlistId) REFERENCES playlists(id),
    FOREIGN KEY(songId) REFERENCES songs(id),
    UNIQUE(playlistId, songId)
);

CREATE TABLE lyrics (
    id TEXT PRIMARY KEY,
    songId TEXT UNIQUE,
    content TEXT,  -- JSON string of LyricLine array
    isSynced BOOLEAN DEFAULT 0,
    provider TEXT,
    language TEXT DEFAULT 'en',
    cachedDate LONG
);

CREATE TABLE queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    songId TEXT NOT NULL,
    position INTEGER NOT NULL,
    timestamp LONG,
    FOREIGN KEY(songId) REFERENCES songs(id)
);

CREATE TABLE play_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    songId TEXT NOT NULL,
    playedDate LONG,
    durationPlayed INTEGER,
    isSkipped BOOLEAN DEFAULT 0,
    isLiked BOOLEAN DEFAULT 0,
    FOREIGN KEY(songId) REFERENCES songs(id)
);

CREATE TABLE home_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sectionKey TEXT UNIQUE,
    content TEXT,  -- JSON cache
    cachedDate LONG,
    expiresDate LONG
);

CREATE TABLE quick_picks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    songId TEXT NOT NULL,
    score REAL,
    generatedDate LONG,
    FOREIGN KEY(songId) REFERENCES songs(id)
);
```

### File Storage Structure
```
/data/data/com.maxrave.echo/
├── files/
│   ├── music/
│   │   ├── JGwWNGJdvx8.m4a     (Song file)
│   │   ├── yt_kffXn2FrPFw.m4a
│   │   └── ...
│   ├── thumbnails/
│   │   ├── JGwWNGJdvx8.jpg
│   │   └── ...
│   └── lyrics/
│       ├── JGwWNGJdvx8.lrc
│       └── ...
├── databases/
│   └── echo_music.db          (Room database)
└── shared_prefs/
    └── user_preferences.xml
```

### Caching Strategy

#### Cache Invalidation
```kotlin
// TTL (Time To Live) per section
val CACHE_DURATION = mapOf(
    "home_feed" to 30.minutes,
    "search_results" to 1.hours,
    "playlists" to 24.hours,
    "lyrics" to 7.days,
    "thumbnails" to 30.days
)

// Check if cache valid
fun isCacheValid(lastCachedTime: Long, type: String): Boolean {
    val age = System.currentTimeMillis() - lastCachedTime
    val ttl = CACHE_DURATION[type] ?: 1.hours
    return age < ttl.inMilliseconds
}
```

#### Smart Refresh
```kotlin
// Online mode
if (isNetworkAvailable) {
    // Fetch fresh data
    val fresh = fetchFromAPI()
    if (isCacheValid(...)) {
        // Cache still valid, but refresh in background
        refreshInBackground()
        return cached  // Instant display
    } else {
        return fresh   // Update immediately
    }
}

// Offline mode
return cached ?: Result.failure("No cached data")
```

---

## 5. JETPACK COMPOSE IMPLEMENTATION DETAILS

### Recomposition & Performance
```kotlin
// BAD: Triggers recomposition untuk semua children
@Composable
fun PlaylistScreen() {
    val songs = viewModel.songs.collectAsState()
    
    LazyColumn {
        items(songs.value) { song ->  // ❌ Recomposes all items
            SongItem(song = song)
        }
    }
}

// GOOD: Deriving state properly
@Composable
fun PlaylistScreen() {
    val songs by viewModel.songs.collectAsState()
    
    LazyColumn {
        items(
            items = songs,
            key = { it.id }  // ✅ Stable key untuk reuse
        ) { song ->
            SongItem(song = song)
        }
    }
}

// BETTER: Using value class untuk stable reference
@Composable
fun SongItem(
    song: Song,
    onPlayClick: (Song) -> Unit = {}
) {
    // Only recomposes when song.id changes
    val songId by rememberUpdatedState(song.id)
    
    Row(...) {
        // ...
    }
}
```

### State Hoisting Pattern (MVVM + Compose)
```kotlin
// ViewModel provides state
class PlayerViewModel : ViewModel() {
    private val _playerState = MutableStateFlow<PlayerState>(...)
    val playerState = _playerState.asStateFlow()
    
    fun play() { _playerState.update { it.copy(isPlaying = true) } }
    fun pause() { _playerState.update { it.copy(isPlaying = false) } }
}

// Screen collects state and passes events
@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    
    PlayerUI(
        state = playerState,
        onPlayClick = { viewModel.play() },
        onPauseClick = { viewModel.pause() }
    )
}

// Stateless UI component
@Composable
fun PlayerUI(
    state: PlayerState,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    Button(onClick = if (state.isPlaying) onPauseClick else onPlayClick) {
        Text(if (state.isPlaying) "Pause" else "Play")
    }
}
```

---

## 6. MEDIA3 / ExoPlayer CONFIGURATION

### ExoPlayer Setup untuk Echo Music
```kotlin
// Initialize in Application or MainActivity
val player = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build(),
        handleAudioFocus = true  // Auto-pause saat call masuk
    )
    .setBufferingBackoffStrategy(
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                min = 15000,       // 15 seconds minimum buffer
                max = 50000,       // 50 seconds max buffer
                playback = 5000,   // Resume play saat buffered
                backoff = 5000     // Time antara retry
            )
            .build()
    )
    .build()

// Configure crossfade
player.setTrackSelectionParameters(...)
```

### Queue Implementation via MediaItems
```kotlin
// Create playlist dari database
val songs = database.songDao().getPlaylist(playlistId)

val mediaItems = songs.map { song ->
    MediaItem.Builder()
        .setUri(song.audioUrl)  // InnerTube extracted URL
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(
                    if (isCached(song.thumbnailId)) {
                        getCachedThumbnailUri(song.thumbnailId)
                    } else {
                        song.thumbnailUrl.toUri()
                    }
                )
                .setDisplayTitle(song.title)
                .build()
        )
        .build()
}

player.setMediaItems(mediaItems, autoPlay = true)

// Listen untuk state changes
player.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> updateUI()
            Player.STATE_BUFFERING -> showLoading()
            Player.STATE_ENDED -> handleQueueEnd()
        }
    }
    
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        // Sync lyrics ke lagu baru
        viewModel.loadLyricsForCurrentSong()
    }
})
```

---

## 7. NETWORKING: RETROFIT + OkHttp

### HTTP Client Configuration
```kotlin
val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    
    // Add interceptors untuk InnerTube
    .addInterceptor { chain ->
        val original = chain.request()
        
        // Add headers
        val request = original.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
            .header("Accept-Language", "en-US")
            .header("Accept-Encoding", "gzip, deflate")
            .build()
        
        chain.proceed(request)
    }
    
    // Error handling
    .addInterceptor(ErrorHandlingInterceptor())
    
    // Logging (debug only)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    })
    
    // Retry
    .retryOnConnectionFailure()
    
    .build()

// Create Retrofit instance
val retrofit = Retrofit.Builder()
    .baseUrl("https://music.youtube.com/")
    .client(httpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val youtubeAPI = retrofit.create(YouTubeMusicService::class.java)
```

### Service Interface
```kotlin
interface YouTubeMusicService {
    @POST("/youtubei/v1/search")
    suspend fun search(
        @Body request: SearchRequest
    ): Response<SearchResponse>
    
    @POST("/youtubei/v1/player")
    suspend fun getPlayerInfo(
        @Body request: PlayerRequest
    ): Response<PlayerResponse>
    
    @POST("/youtubei/v1/browse")
    suspend fun browse(
        @Body request: BrowseRequest
    ): Response<BrowseResponse>
}
```

---

## 8. PERFORMANCE METRICS

### Benchmark Results (Typical)
```
Operation               Time        Memory   Notes
─────────────────────────────────────────────────────────
App Startup            ~2-3s       ~100MB   Cold start
Load Homepage           ~500ms      +20MB    Includes API
Search Query            ~1-2s       +15MB    Network dependent
Load Lyrics             ~100-500ms  +5MB     Local/API dependent
Queue Navigation        ~50ms       ~0MB     In-memory operation
Playback Start          ~1-2s       +30MB    Download + buffer

Cache Hit (Homepage)    ~100ms      +5MB     From Room DB
Offline Mode            ~50ms       ~0MB     No network
Lyrics Sync Loop        ~5ms        ~0MB     Per 50ms tick (negligible)
```

### Memory Usage
```
Base App:        ~80-100 MB
With Library:    ~120-150 MB (depends on playlist size)
Downloading:     ~150-200 MB (streaming buffer)
UI Heavy (Player): ~200-250 MB (bitmaps, animations)

Garbage Collection: Auto, ~100-200ms pause saat GC
```

---

## 9. DEPENDENCY TREE (Key Libraries)

```
app/build.gradle.kts:

dependencies {
    // Core Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-common:1.1.1")
    implementation("androidx.media3:media3-exoplayer:1.1.1")
    implementation("androidx.media3:media3-ui:1.1.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.1.1")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    
    // DI
    implementation("com.google.dagger:hilt-android:2.46.1")
    kapt("com.google.dagger:hilt-compiler:2.46.1")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    
    // Firebase
    implementation("com.google.firebase:firebase-analytics-ktx:21.3.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.4.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.7")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
}
```

---

## 10. POTENTIAL IMPROVEMENTS & CHALLENGES

### Challenges
1. **YouTube Blocking:** InnerTube API can be blocked at any time
2. **Lyrics Accuracy:** Better Lyrics coverage still <50% dari semua songs
3. **Network Dependency:** Streaming requires stable internet
4. **Legal Gray Area:** Not officially licensed from YouTube

### Future Improvements
```
Potential:
├─ Add Genius API as lyrics fallback
├─ Implement video lyrics (for music videos)
├─ Add audio fingerprinting untuk identification
├─ Support untuk multiple audio codecs
├─ Desktop sync (play di device A, control from B)
├─ Improve recommendation algorithm
├─ Add podcast integration untuk full audio platform
└─ Multi-language UI
```

---

**Document Version:** 1.0  
**Last Updated:** May 2026  
**Scope:** Technical deep-dive untuk developers

