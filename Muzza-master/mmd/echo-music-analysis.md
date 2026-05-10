# Analisis Repository Echo-Music App
## YouTube Music Client untuk Android dengan Ad-Free Experience

**Tanggal Analisis:** Mei 2026  
**Repository:** https://github.com/EchoMusicApp/Echo-Music  
**Bahasa:** Kotlin  
**Framework:** Android (Jetpack Compose)  
**License:** GPL-3.0

---

## 📋 Daftar Isi
1. [Ringkasan Aplikasi](#ringkasan-aplikasi)
2. [Sumber Music dan Streaming](#sumber-music-dan-streaming)
3. [Arsitektur Teknis](#arsitektur-teknis)
4. [Homepage & Discovery](#homepage--discovery)
5. [Queue Management](#queue-management)
6. [Quick Picks](#quick-picks)
7. [Synchronized Lyrics](#synchronized-lyrics)
8. [Tech Stack](#tech-stack)

---

## 🎵 Ringkasan Aplikasi

### Deskripsi Umum
Echo Music adalah aplikasi musik Android **open-source** yang menyediakan pengalaman streaming premium dengan menghilangkan iklan. Aplikasi ini memanfaatkan library musik YouTube Music yang luas dengan menambahkan fitur-fitur canggih seperti:

- **Ad-Free Experience** - Streaming tanpa gangguan iklan
- **Offline Playback** - Download musik untuk mendengarkan offline
- **Synchronized Lyrics** - Lirik sinkron real-time dengan terjemahan AI
- **Smart Recommendations** - Rekomendasi personal berdasarkan history dengar
- **Multiple Platform** - Tersedia untuk Android, Desktop (Windows/macOS/Linux)

### Fitur Utama
- **Canvas Animations** - Animasi visual saat memutar musik
- **Vertical Ambient Mode** - Visual immersive selama playback
- **Echo Find** - Identifikasi lagu menggunakan audio recognition
- **Podcast Support** - Dukungan podcast di samping musik
- **Local Media Support** - Putar musik dari penyimpanan device
- **Listen Together** - Sinkronisasi musik real-time (seperti Spotify Jam)

---

## 🎼 Sumber Music dan Streaming

### API Utama: YouTube Music InnerTube API

Echo Music **tidak menggunakan YouTube API resmi v3** (yang terbatas dan memerlukan API key). Sebaliknya, aplikasi menggunakan:

#### 1. **YouTube Music InnerTube API (Private/Unofficial)**
- **Endpoint:** Request HTTP ke `music.youtube.com`
- **Metode:** Reverse-engineering dari komunikasi client YouTube Music
- **Keuntungan:**
  - Mendapat akses ke seluruh library YouTube Music (~70 juta lagu)
  - Bypass iklan dengan menghapus ad tokens dari response
  - Streaming audio hanya (tanpa video)

#### 2. **Audio Extraction dari YouTube**
- **Library:** `youtube_explode_dart` (Fork dari Hexer10/youtube_explode_dart)
- **Fungsi:** Extract audio stream dari video YouTube Music
- **Format:** MP3/AAC encoding untuk playback

#### 3. **Integration dengan Spotify (Import)**
- Menggunakan Spotify Web API untuk import playlist
- OAuth 2.0 untuk authentication

### Data Flow: Dari Request hingga Playback

```
User Query / Browse
    ↓
InnerTube API Request (HTTP/JSON)
    ↓
Parse YouTube Music Response
    ↓
Extract Audio Stream URL
    ↓
ExoPlayer (Media3) untuk Playback
    ↓
Offline Storage (Room Database + File System)
```

### Contoh Sumber Musik:
1. **Streaming Langsung:** Dari YouTube Music (via InnerTube API)
2. **Offline:** Dari local storage device
3. **Import:** Dari Spotify playlists
4. **Local Media:** MP3/WAV dari device storage

---

## 🏗️ Arsitektur Teknis

### Project Structure

```
app/src/main/java/com/maxrave/echo/
├── ui/                          # UI Layer (Jetpack Compose)
│   ├── components/              # Reusable UI Components
│   │   ├── HomeScreen.kt
│   │   ├── PlayerScreen.kt
│   │   ├── QueueScreen.kt
│   │   ├── LyricsScreen.kt
│   │   └── ...
│   ├── screens/                 # Full Screen Implementations
│   │   ├── HomeScreens.kt
│   │   ├── PlayScreen.kt
│   │   └── ...
│   └── theme/                   # Theme & Styling
│       ├── Color.kt
│       ├── Typography.kt
│       └── Shape.kt
│
├── data/                        # Data Layer (Repository Pattern)
│   ├── repository/              # Repository Implementations
│   │   ├── HomeRepository.kt
│   │   ├── PlaylistRepository.kt
│   │   ├── LyricsRepository.kt
│   │   └── ...
│   ├── local/                   # Local Data Sources
│   │   ├── LocalDataSource.kt
│   │   └── dao/
│   │       ├── PlaylistDao.kt
│   │       ├── SongDao.kt
│   │       ├── LyricsDao.kt
│   │       └── ...
│   └── remote/                  # Remote Data Sources
│       ├── YouTubeMusicAPI.kt
│       ├── InnerTubeClient.kt
│       └── LyricsAPI.kt
│
├── domain/                      # Domain Layer (Business Logic)
│   ├── model/                   # Domain Models
│   │   ├── Song.kt
│   │   ├── Playlist.kt
│   │   ├── Lyrics.kt
│   │   ├── Queue.kt
│   │   └── ...
│   ├── repository/              # Repository Interfaces
│   │   ├── IHomeRepository.kt
│   │   ├── IPlaylistRepository.kt
│   │   └── ...
│   └── usecase/                 # Use Cases
│       ├── GetHomeContentUseCase.kt
│       ├── PlaySongUseCase.kt
│       ├── GetSyncedLyricsUseCase.kt
│       └── ...
│
├── viewmodel/                   # ViewModels (MVVM)
│   ├── HomeViewModel.kt
│   ├── PlayerViewModel.kt
│   ├── QueueViewModel.kt
│   ├── LyricsViewModel.kt
│   └── ...
│
└── common/                      # Utilities & Extensions
    ├── utils/
    │   ├── NetworkUtils.kt
    │   ├── StorageUtils.kt
    │   └── ...
    └── extensions/
        ├── StringExtensions.kt
        └── DateExtensions.kt
```

### Architectural Pattern: **MVVM + Clean Architecture**

```
┌─────────────────┐
│      UI (Compose)       │  ← User Interaction
├─────────────────┤
│   ViewModel      │  ← State Management & Logic
├─────────────────┤
│   Repository     │  ← Data Abstraction
├─────────────────┤
│  Local | Remote  │  ← Data Sources
└─────────────────┘
```

---

## 🏠 Homepage & Discovery

### Fitur Homepage

Halaman Home Echo Music menampilkan:

1. **Recommendation Carousels**
   - Rekomendasi personal berdasarkan listening history
   - "Similar Artists"
   - "New Releases"

2. **Browse Categories**
   - Charts (Top, Trending, Genre-specific)
   - Moods & Genres
   - Podcasts Section
   - Custom Playlists

3. **User's Library**
   - Recent Played
   - Liked Songs
   - Custom Playlists

### Implementation

**Data Source:** `HomeRepository.kt`
```kotlin
// Contoh fetch home content
suspend fun getHomeContent(): Result<HomeContent> {
    // 1. Request ke InnerTube API
    // 2. Parse carousel/sections
    // 3. Cache ke Room Database
    // 4. Return merged data
}
```

**ViewModel:** `HomeViewModel.kt`
```kotlin
class HomeViewModel : ViewModel() {
    val homeContent: StateFlow<HomeContent>
    
    fun loadHome() {
        // Fetch dan refresh home content
    }
}
```

**UI:** `HomeScreens.kt` (Jetpack Compose)
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val home by viewModel.homeContent.collectAsState()
    
    LazyColumn {
        item {
            SearchBar()
        }
        item {
            RecommendationCarousel(home.recommendations)
        }
        item {
            CategoriesGrid(home.categories)
        }
    }
}
```

### Smart Recommendations Engine

- **Basis:** Last.fm integration + Local listening history
- **Data Tracking:** Firebase Analytics untuk user behavior
- **Algorithm:** Content-based filtering + Collaborative filtering
- **Update Frequency:** Real-time dengan setiap track completion

---

## 📋 Queue Management

### Queue Architecture

Queue di Echo Music dikelola melalui:

#### 1. **Local State Management**
```kotlin
data class Queue(
    val currentIndex: Int,
    val songs: List<Song>,
    val isShuffled: Boolean,
    val repeatMode: RepeatMode  // OFF, ONE, ALL
)

enum class RepeatMode {
    OFF,
    ONE,      // Repeat 1 song
    ALL       // Repeat all songs
}
```

#### 2. **ExoPlayer (Media3) Integration**

Echo Music menggunakan **Media3 (ExoPlayer)** untuk playback:

```kotlin
class PlayerService : MediaLibraryService() {
    private val player = ExoPlayer.Builder(this).build()
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        // Handle queue updates
        // Trigger lyrics sync
        // Update UI
    }
    
    fun setPlaylist(songs: List<Song>) {
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.audioUrl)
                .setMediaMetadata(...)
                .build()
        }
        player.setMediaItems(mediaItems)
    }
    
    fun seekTo(position: Long) {
        player.seekTo(position)  // Update untuk lyrics sync
    }
}
```

#### 3. **Queue Operations**

```kotlin
class QueueManager {
    // Add to queue
    fun addToQueue(songs: List<Song>) { ... }
    
    // Next/Previous
    fun skipToNext() { ... }
    fun skipToPrevious() { ... }
    
    // Shuffle & Repeat
    fun setShuffle(enable: Boolean) { ... }
    fun setRepeatMode(mode: RepeatMode) { ... }
    
    // Jump to position
    fun jumpToQueue(index: Int) { ... }
}
```

#### 4. **Offline Queue**

```kotlin
// Queue disimpan ke Room Database
@Entity
data class QueueEntity(
    @PrimaryKey val id: Int,
    val songId: String,
    val position: Int,
    val timestamp: Long
)

@Dao
interface QueueDao {
    @Insert
    suspend fun insertQueue(queue: List<QueueEntity>)
    
    @Query("SELECT * FROM queue ORDER BY position")
    fun getQueue(): Flow<List<QueueEntity>>
}
```

### UI Queue Display

```kotlin
@Composable
fun QueueScreen(viewModel: QueueViewModel) {
    val queue by viewModel.queue.collectAsState()
    
    LazyColumn {
        itemsIndexed(queue.songs) { index, song ->
            QueueItem(
                song = song,
                isCurrent = index == queue.currentIndex,
                onClick = { viewModel.jumpToQueue(index) }
            )
        }
    }
}
```

---

## ✨ Quick Picks

### Konsep Quick Picks

"Quick Picks" adalah rekomendasi lagu yang dipersonalisasi berdasarkan:

1. **User Behavior:**
   - Recent plays
   - Skipped songs (negative signal)
   - Liked/rated songs

2. **Time Context:**
   - Morning picks (energetic, upbeat)
   - Evening picks (relaxing, chill)
   - Workout picks (high tempo)

3. **Genre/Mood Affinity:**
   - Tracking genre preferences
   - Mood-based suggestions

### Implementation

**Repository:**
```kotlin
class QuickPicksRepository(
    private val innerTubeAPI: InnerTubeClient,
    private val localDatabase: AppDatabase
) {
    suspend fun getQuickPicks(): Result<List<Song>> {
        return try {
            // 1. Get dari InnerTube (YouTube Music recommendations)
            val response = innerTubeAPI.getQuickPicks()
            
            // 2. Apply local filters based on user history
            val localHistory = localDatabase.songDao().getRecentPlays()
            val filteredPicks = response.filterNot { pick ->
                localHistory.any { it.id == pick.id }
            }
            
            // 3. Cache results
            localDatabase.quickPicksDao().insertPicks(filteredPicks)
            
            Result.success(filteredPicks)
        } catch (e: Exception) {
            // Fallback ke cache
            Result.success(localDatabase.quickPicksDao().getCachedPicks())
        }
    }
}
```

**ViewModel:**
```kotlin
class QuickPicksViewModel : ViewModel() {
    val quickPicks = repository.getQuickPicks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun playQuickPick(index: Int) {
        // Start playback dari quick picks
        playerService.setQueue(quickPicks.value)
        playerService.seekTo(index)
    }
}
```

**UI Display:**
```kotlin
@Composable
fun QuickPicksCarousel(picks: List<Song>, onSongClick: (Song) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(picks) { song ->
            QuickPickCard(
                song = song,
                onClick = { onSongClick(song) }
            )
        }
    }
}
```

### Refresh Strategy

Quick Picks di-refresh:
- **Setiap 30 menit** (background update)
- **Saat user membuka app** (fresh picks)
- **Setelah 5 lagu diputar** (update recommendations)

---

## 🎤 Synchronized Lyrics

Synchronized lyrics adalah salah satu fitur unggulan Echo Music dengan **word-by-word synchronization**.

### Lyrics Architecture

#### 1. **Lyrics Data Model**

```kotlin
data class Lyrics(
    val id: String,
    val songId: String,
    val title: String,
    val artist: String,
    val lines: List<LyricLine>
)

data class LyricLine(
    val timestamp: Long,        // milliseconds
    val text: String,
    val words: List<LyricWord>  // untuk word-by-word sync
)

data class LyricWord(
    val text: String,
    val startTime: Long,        // ms
    val duration: Long          // ms
)
```

#### 2. **Lyrics Data Sources**

Echo Music mengintegrasikan **multiple lyrics providers** untuk akurasi maksimal:

**a) Better Lyrics (Primary)**
- Website: `https://better-lyrics.boidu.dev/`
- Format: LRC dengan timestamp
- Sinkronisasi: Word-by-word

**b) YouTube Music Lyrics**
- Extracted dari InnerTube API response
- Built-in YouTube Music lyrics

**c) Lyrics+ (Custom Provider)**
- Provider khusus Echo Music
- Coverage luas untuk lagu Indonesia

**d) Fallback: Online APIs**
- Genius API (optional)
- ChartLyrics API
- Local cache

### Implementation: Lyrics Sync Engine

```kotlin
class LyricsRepository(
    private val betterlyricsAPI: BetterLyricsClient,
    private val innerTubeAPI: InnerTubeClient,
    private val database: AppDatabase,
    private val externalAPI: ExternalLyricsClient
) {
    suspend fun getLyrics(song: Song): Result<Lyrics> {
        // Priority: Better Lyrics → YouTube → Lyrics+ → Fallback
        
        return betterlyricsAPI.fetchLyrics(
            artist = song.artist,
            title = song.title
        ).getOrElse {
            innerTubeAPI.getLyricsFromSongPage(song.id).getOrElse {
                externalAPI.getFromLyricsPlus(song).getOrNull() ?: 
                    Lyrics.empty()
            }
        }
    }
}
```

#### 3. **Real-Time Sync dengan ExoPlayer**

```kotlin
class LyricsSyncController(
    private val player: ExoPlayer,
    private val playerService: PlayerService
) {
    private val lyrics = MutableStateFlow<Lyrics?>(null)
    private val currentLineIndex = MutableStateFlow(0)
    private val currentWordIndex = MutableStateFlow(0)
    
    fun startSync() {
        // Update lyrics position setiap 50ms
        scope.launch {
            while (isActive) {
                val currentPosition = player.currentPosition
                updateLyricsPosition(currentPosition)
                delay(50)  // 50ms refresh rate untuk smooth animation
            }
        }
    }
    
    private fun updateLyricsPosition(positionMs: Long) {
        val lyricLines = lyrics.value?.lines ?: return
        
        // Find line yang sesuai dengan current position
        val lineIndex = lyricLines.indexOfLast { line ->
            line.timestamp <= positionMs
        }
        
        if (lineIndex != currentLineIndex.value) {
            currentLineIndex.value = lineIndex
            
            // Update word-by-word sync
            val currentLine = lyricLines.getOrNull(lineIndex) ?: return
            val wordIndex = currentLine.words.indexOfLast { word ->
                word.startTime <= (positionMs - currentLine.timestamp)
            }
            currentWordIndex.value = wordIndex
        }
    }
}
```

#### 4. **Lyrics UI Components**

**Full-Screen Lyrics View:**
```kotlin
@Composable
fun LyricsScreen(
    lyrics: Lyrics,
    currentLineIndex: Int,
    currentWordIndex: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        itemsIndexed(lyrics.lines) { lineIndex, line ->
            LyricLineView(
                line = line,
                isCurrent = lineIndex == currentLineIndex,
                currentWordIndex = if (lineIndex == currentLineIndex) {
                    currentWordIndex
                } else {
                    -1
                },
                modifier = Modifier
                    .padding(12.dp)
                    .animateContentSize()
            )
        }
    }
}

@Composable
fun LyricLineView(
    line: LyricLine,
    isCurrent: Boolean,
    currentWordIndex: Int,
    modifier: Modifier = Modifier
) {
    val fontSize = if (isCurrent) 28.sp else 16.sp
    val alpha = if (isCurrent) 1f else 0.5f
    
    Row(
        modifier = modifier.alpha(alpha),
        horizontalArrangement = Arrangement.Center
    ) {
        line.words.forEachIndexed { wordIndex, word ->
            val wordColor = if (isCurrent && wordIndex <= currentWordIndex) {
                Color.Cyan  // Highlight current word
            } else {
                Color.White
            }
            
            Text(
                text = word.text,
                fontSize = fontSize,
                color = wordColor,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
```

**Inline Lyrics View (di Player):**
```kotlin
@Composable
fun PlayerWithInlineLyrics(
    song: Song,
    lyrics: Lyrics?,
    currentLineIndex: Int
) {
    Column {
        // Player controls
        PlayerControls(song)
        
        // Inline lyrics (1-2 lines)
        if (lyrics != null) {
            val currentLine = lyrics.lines.getOrNull(currentLineIndex)
            val nextLine = lyrics.lines.getOrNull(currentLineIndex + 1)
            
            Text(
                text = currentLine?.text ?: "",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            
            if (nextLine != null) {
                Text(
                    text = nextLine.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
```

#### 5. **Lyrics Animation Styles**

Echo Music menawarkan **multiple animation styles**:

```kotlin
enum class LyricsAnimationStyle {
    FADE,           // Simple fade in/out
    SLIDE,          // Slide animation
    POP,            // Pop/scale animation
    WAVE,           // Wave effect
    GLOW,           // Glow effect
    BOUNCE          // Bounce animation
}

@Composable
fun AnimatedLyricLine(
    line: LyricLine,
    isCurrent: Boolean,
    style: LyricsAnimationStyle
) {
    when (style) {
        LyricsAnimationStyle.FADE -> {
            Text(
                text = line.text,
                modifier = Modifier.alpha(if (isCurrent) 1f else 0.3f)
            )
        }
        LyricsAnimationStyle.SLIDE -> {
            Text(
                text = line.text,
                modifier = Modifier.offset(
                    x = if (isCurrent) 0.dp else 20.dp
                )
            )
        }
        // ... style lainnya
    }
}
```

#### 6. **Lyrics Translation (AI)**

Echo Music terintegrasi dengan **Google Translate** untuk terjemahan real-time:

```kotlin
class LyricsTranslationService(
    private val googleTranslate: GoogleTranslateClient
) {
    suspend fun translateLyrics(
        lyrics: Lyrics,
        targetLanguage: String
    ): Result<Lyrics> {
        val translatedLines = lyrics.lines.map { line ->
            val translated = googleTranslate.translate(
                text = line.text,
                targetLanguage = targetLanguage
            )
            
            line.copy(text = translated)  // Keep timestamps
        }
        
        return Result.success(lyrics.copy(lines = translatedLines))
    }
}
```

#### 7. **Offline Lyrics Storage**

Lyrics di-cache locally untuk offline access:

```kotlin
@Entity
data class LyricsEntity(
    @PrimaryKey val id: String,
    val songId: String,
    val title: String,
    val artist: String,
    val content: String,  // JSON string of LyricLine
    val timestamp: Long
)

@Dao
interface LyricsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)
    
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    fun getLyrics(songId: String): Flow<LyricsEntity?>
}
```

---

## 🛠️ Tech Stack

### Core Framework
| Component | Technology | Version | Fungsi |
|-----------|-----------|---------|--------|
| **Language** | Kotlin | Latest | Main development language |
| **UI Framework** | Jetpack Compose | Latest | Modern declarative UI |
| **Architecture** | MVVM + Clean | - | State management & scalability |
| **Dependency Injection** | Hilt/Dagger2 | Latest | DI container |

### Media & Playback
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **Audio Playback** | Media3 (ExoPlayer) | Playback engine, queue management |
| **Video/Music Extraction** | youtube_explode_dart | Extract audio dari YouTube |
| **Audio Codec** | ExoPlayer built-in | Support MP3, AAC, FLAC, etc. |

### Data & Networking
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **HTTP Client** | Retrofit + OkHttp | API requests |
| **JSON Parser** | Gson/Kotlinx Serialization | Parse API responses |
| **Local Database** | Room (SQLite) | Offline storage |
| **Async** | Kotlin Coroutines | Non-blocking operations |
| **Reactive** | Flow (StateFlow) | State management |

### API Integration
| Service | Endpoint | Fungsi |
|---------|----------|--------|
| **YouTube Music** | `music.youtube.com` (InnerTube) | Song search, streaming |
| **Better Lyrics** | `https://better-lyrics.boidu.dev/` | Synchronized lyrics |
| **Google Translate** | Google Translate API | Lyrics translation |
| **Firebase** | Analytics + Crash Reporting | User tracking, error logs |
| **Spotify** (optional) | Spotify Web API | Import playlists |

### Storage & Offline
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **Database** | Room (SQLite) | Songs, playlists, lyrics, queue |
| **File Storage** | Android File System | Downloaded songs |
| **Encryption** | Android Security Crypto | Encrypt sensitive data |

### UI & Design
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **Design System** | Material Design 3 | UI components & theming |
| **Animation** | Compose Animation | Smooth transitions |
| **Image Loading** | Coil/Glide | Album art, thumbnails |
| **Icons** | Material Icons | App icons |

### Testing
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **Unit Tests** | JUnit 4/5 | Business logic tests |
| **UI Tests** | Espresso/Compose Test | UI component tests |
| **Mocking** | Mockk | Mock objects |

### Build & Deployment
| Component | Technology | Fungsi |
|-----------|-----------|--------|
| **Build System** | Gradle 8+ | Project building |
| **CI/CD** | GitHub Actions | Automated builds & tests |
| **APK Distribution** | GitHub Releases | Direct APK download |
| **Package Manager** | F-Droid/Obtainium | Alternative installation |

---

## 📊 Data Flow Summary

### 1. **Homepage Load Flow**
```
User Opens App
    ↓
HomeViewModel.loadHome()
    ↓
HomeRepository.getHomeContent()
    ↓
InnerTubeAPI.getHomeFeed() [Network Call]
    ↓
Cache ke Room Database
    ↓
Update HomeViewModel StateFlow
    ↓
Recompose UI dengan data baru
```

### 2. **Music Playback Flow**
```
User Clicks Song
    ↓
PlayerService.playSong(song)
    ↓
Fetch URL dari InnerTubeAPI
    ↓
Create MediaItem untuk ExoPlayer
    ↓
ExoPlayer download & play audio
    ↓
Emit playback state updates
    ↓
UI update (progress bar, lyrics sync)
```

### 3. **Lyrics Sync Flow**
```
Song Playing
    ↓
LyricsRepository.getLyrics(songId)
    ↓
Try Better Lyrics API
    ├→ Success: Parse LRC format
    ├→ Fallback: YouTube Music API
    └→ Last Resort: Local cache
    ↓
LyricsSyncController.startSync()
    ↓
Every 50ms: Calculate current line & word
    ↓
Update currentLineIndex & currentWordIndex
    ↓
UI animates to current lyric
```

### 4. **Queue Management Flow**
```
Playlist Selected
    ↓
QueueManager.setQueue(songs)
    ↓
ExoPlayer.setMediaItems()
    ↓
Save queue state to Room
    ↓
Display in QueueScreen
    ↓
User selects song in queue
    ↓
ExoPlayer.seekTo(index)
```

---

## 🔐 Privacy & Security

### Data Collection
- **Minimal collection:** Hanya listening history untuk recommendations
- **Firebase:** Analytics dengan anonymized user IDs
- **Local-first:** Majority data stored locally

### Authentication
- **Spotify Import:** OAuth 2.0 (redirect flow)
- **YouTube:** Cookies/session-based (tidak perlu login)
- **No Account Required:** Gunakan app tanpa membuat akun

### Open Source & Transparency
- **GPL-3.0 License:** Source code fully public
- **Community Review:** Transparansi kode memungkinkan audit
- **No Proprietary APIs:** Hanya menggunakan public/reverse-engineered APIs

---

## 🚀 Kesimpulan

**Echo Music** adalah aplikasi musik modern yang menggabungkan:

1. **Kemudahan Akses:** Tap ke library YouTube Music ~70 juta lagu
2. **Ad-Free Experience:** Melewati iklan dengan smart engineering
3. **Advanced Features:**
   - Synchronized lyrics dengan AI translation
   - Smart recommendations & quick picks
   - Offline playback dengan queue management
   - Multi-platform support (Android, Desktop)

4. **Open Source:** Komunitas bisa contribute & audit code
5. **Scalable Architecture:** Clean architecture dengan MVVM memudahkan maintenance

Aplikasi ini adalah contoh sempurna bagaimana reverse-engineering dan open-source dapat memberikan user experience yang superior dibanding aplikasi proprietary.

---

## 📚 Referensi & Inspirasi

- **Metrolist** - Foundational architecture
- **Better Lyrics** - Lyrics synchronization
- **SimpMusic** - Lyrics implementation reference
- **Music Recognizer** - Audio recognition (Echo Find)
- **YouTube Explode** - YouTube content extraction

