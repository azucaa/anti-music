# Echo Music - Contoh Implementasi Code

## 1. Lyrics Synchronization Implementation

### Model Class
```kotlin
// domain/model/Lyrics.kt
data class Lyrics(
    val id: String,
    val songId: String,
    val title: String,
    val artist: String,
    val lines: List<LyricLine>,
    val isSynced: Boolean = false,
    val language: String = "en"
)

data class LyricLine(
    val timestamp: Long,  // milliseconds
    val text: String,
    val words: List<LyricWord> = emptyList()
)

data class LyricWord(
    val text: String,
    val startTime: Long,
    val duration: Long
)
```

### Repository
```kotlin
// data/repository/LyricsRepository.kt
class LyricsRepository(
    private val betterlyricsAPI: BetterLyricsService,
    private val innerTubeAPI: InnerTubeService,
    private val lyricsDao: LyricsDao,
    private val appDatabase: AppDatabase
) {
    
    suspend fun getLyrics(song: Song): Result<Lyrics> = withContext(Dispatchers.IO) {
        try {
            // First check local cache
            val cached = lyricsDao.getLyricsBySongId(song.id).firstOrNull()
            if (cached != null && cached.isCacheFresh()) {
                return@withContext Result.success(cached.toDomain())
            }
            
            // Try Better Lyrics API (primary)
            betterlyricsAPI.getLyrics(
                artist = song.artist,
                title = song.title
            ).getOrNull()?.let { lrc ->
                val lyrics = parseLRC(lrc, song)
                saveLyricsToCache(lyrics)
                return@withContext Result.success(lyrics)
            }
            
            // Fallback to YouTube Music lyrics
            innerTubeAPI.getLyricsFromSongPage(song.id).getOrNull()?.let { yt ->
                val lyrics = parseYouTubeLyrics(yt, song)
                saveLyricsToCache(lyrics)
                return@withContext Result.success(lyrics)
            }
            
            // Last resort: cached data
            val fallback = lyricsDao.getLyricsBySongId(song.id).firstOrNull()
            if (fallback != null) {
                return@withContext Result.success(fallback.toDomain())
            }
            
            Result.failure(Exception("No lyrics found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseLRC(lrcString: String, song: Song): Lyrics {
        val lines = mutableListOf<LyricLine>()
        
        lrcString.lines().forEach { line ->
            // LRC format: [00:12.50]Lyric text
            val timeRegex = "\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)".toRegex()
            val match = timeRegex.find(line)
            
            if (match != null) {
                val (minutes, seconds, centiseconds, text) = match.destructured
                val timestamp = (minutes.toInt() * 60 + seconds.toInt()) * 1000 + 
                               centiseconds.toInt() * 10
                
                lines.add(
                    LyricLine(
                        timestamp = timestamp,
                        text = text.trim(),
                        words = parseWords(text)
                    )
                )
            }
        }
        
        return Lyrics(
            id = UUID.randomUUID().toString(),
            songId = song.id,
            title = song.title,
            artist = song.artist,
            lines = lines.sortedBy { it.timestamp },
            isSynced = true
        )
    }
    
    private fun parseWords(line: String): List<LyricWord> {
        // Simplified: split by spaces for demo
        // In production: use word-level sync data if available
        val words = line.split(" ")
        val duration = 200L  // milliseconds per word (average)
        
        return words.mapIndexed { index, word ->
            LyricWord(
                text = word,
                startTime = index * duration,
                duration = duration
            )
        }
    }
    
    private suspend fun saveLyricsToCache(lyrics: Lyrics) {
        lyricsDao.insertLyrics(lyrics.toEntity())
    }
}
```

### ViewModel
```kotlin
// viewmodel/LyricsViewModel.kt
class LyricsViewModel(
    private val lyricsRepository: LyricsRepository,
    private val playerService: PlayerService,
    private val currentSongFlow: Flow<Song>
) : ViewModel() {
    
    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics = _lyrics.asStateFlow()
    
    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex = _currentLineIndex.asStateFlow()
    
    private val _currentWordIndex = MutableStateFlow(-1)
    val currentWordIndex = _currentWordIndex.asStateFlow()
    
    private val _selectedAnimation = MutableStateFlow(LyricsAnimationStyle.FADE)
    val selectedAnimation = _selectedAnimation.asStateFlow()
    
    private val _syncEnabled = MutableStateFlow(true)
    val syncEnabled = _syncEnabled.asStateFlow()
    
    init {
        loadLyricsForCurrentSong()
        startSyncLoop()
    }
    
    private fun loadLyricsForCurrentSong() {
        viewModelScope.launch {
            currentSongFlow.collect { song ->
                val result = lyricsRepository.getLyrics(song)
                _lyrics.value = result.getOrNull()
            }
        }
    }
    
    private fun startSyncLoop() {
        viewModelScope.launch {
            while (isActive && _syncEnabled.value) {
                val currentPos = playerService.getCurrentPosition()
                updateLyricsPosition(currentPos)
                delay(50)  // Update every 50ms for smooth animation
            }
        }
    }
    
    private fun updateLyricsPosition(positionMs: Long) {
        val lyricsData = _lyrics.value ?: return
        val lines = lyricsData.lines
        
        // Binary search for efficiency
        val lineIndex = lines.binarySearch { line ->
            when {
                line.timestamp < positionMs -> -1
                line.timestamp > positionMs -> 1
                else -> 0
            }
        }.let { index ->
            if (index < 0) -index - 2 else index
        }.coerceIn(0, lines.size - 1)
        
        if (lineIndex != _currentLineIndex.value) {
            _currentLineIndex.value = lineIndex
        }
        
        // Update word-by-word sync
        val currentLine = lines[lineIndex]
        val wordOffsetMs = positionMs - currentLine.timestamp
        
        val wordIndex = currentLine.words.binarySearch { word ->
            when {
                word.startTime < wordOffsetMs -> -1
                word.startTime > wordOffsetMs -> 1
                else -> 0
            }
        }.let { index ->
            if (index < 0) -index - 2 else index
        }.coerceIn(-1, currentLine.words.size - 1)
        
        if (wordIndex != _currentWordIndex.value) {
            _currentWordIndex.value = wordIndex
        }
    }
    
    fun translateLyrics(targetLanguage: String) {
        viewModelScope.launch {
            val current = _lyrics.value ?: return@launch
            // Translate and update UI
        }
    }
    
    fun setAnimationStyle(style: LyricsAnimationStyle) {
        _selectedAnimation.value = style
    }
    
    fun setSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
    }
}
```

### UI Component
```kotlin
// ui/components/LyricsScreen.kt
@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()
    val currentWordIndex by viewModel.currentWordIndex.collectAsState()
    val animationStyle by viewModel.selectedAnimation.collectAsState()
    
    if (lyrics == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No lyrics available", color = Color.White)
        }
        return
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        itemsIndexed(lyrics!!.lines) { lineIndex, line ->
            val isCurrent = lineIndex == currentLineIndex
            
            LyricLineView(
                line = line,
                isCurrent = isCurrent,
                currentWordIndex = if (isCurrent) currentWordIndex else -1,
                animationStyle = animationStyle,
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
    animationStyle: LyricsAnimationStyle,
    modifier: Modifier = Modifier
) {
    val fontSize by animateDpAsState(
        targetValue = if (isCurrent) 28.sp else 16.sp
    )
    val alpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.4f
    )
    
    Row(
        modifier = modifier
            .alpha(alpha)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.Center
    ) {
        line.words.forEachIndexed { wordIndex, word ->
            val isCurrentWord = isCurrent && wordIndex <= currentWordIndex
            val wordColor = if (isCurrentWord) Color.Cyan else Color.White
            
            Text(
                text = word.text,
                fontSize = fontSize,
                color = wordColor,
                fontWeight = if (isCurrentWord) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .padding(4.dp)
                    .animateContentSize()
            )
        }
    }
}
```

---

## 2. Queue Management Implementation

### Queue Data Models
```kotlin
// domain/model/Queue.kt
data class Queue(
    val currentIndex: Int = 0,
    val songs: List<Song> = emptyList(),
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val currentSong: Song? = songs.getOrNull(currentIndex)
    val hasNext: Boolean = currentIndex < songs.size - 1
    val hasPrevious: Boolean = currentIndex > 0
}

enum class RepeatMode {
    OFF,    // No repeat
    ONE,    // Repeat current song
    ALL     // Repeat entire queue
}
```

### Entity for Database
```kotlin
// data/local/entity/QueueEntity.kt
@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val id: Int = 0,
    val songId: String,
    val position: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position")
    fun getQueue(): Flow<List<QueueEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueue(items: List<QueueEntity>)
    
    @Query("DELETE FROM queue")
    suspend fun clearQueue()
    
    @Query("UPDATE queue SET position = position - 1 WHERE position > :pos")
    suspend fun shiftPositions(pos: Int)
}
```

### Queue Manager Service
```kotlin
// data/service/QueueManager.kt
class QueueManager(
    private val exoPlayer: ExoPlayer,
    private val queueDao: QueueDao,
    private val songRepository: SongRepository
) {
    
    private val _queue = MutableStateFlow(Queue())
    val queue = _queue.asStateFlow()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()
    
    suspend fun setQueue(songs: List<Song>) = withContext(Dispatchers.Main) {
        // Clear old queue
        queueDao.clearQueue()
        
        // Create media items for ExoPlayer
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.audioUrl)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.thumbnailUrl.toUri())
                    .build()
                )
                .build()
        }
        
        exoPlayer.setMediaItems(mediaItems, 0, 0)
        
        // Save to database
        val entities = songs.mapIndexed { index, song ->
            QueueEntity(
                id = 0,
                songId = song.id,
                position = index
            )
        }
        queueDao.insertQueue(entities)
        
        // Update state
        _queue.value = Queue(songs = songs)
        updateCurrentSong()
    }
    
    fun skipToNext() {
        if (_queue.value.hasNext) {
            exoPlayer.seekToNextMediaItem()
            updateCurrentSong()
        }
    }
    
    fun skipToPrevious() {
        if (_queue.value.hasPrevious) {
            exoPlayer.seekToPreviousMediaItem()
            updateCurrentSong()
        }
    }
    
    fun jumpToQueue(index: Int) {
        if (index in _queue.value.songs.indices) {
            exoPlayer.seekToDefaultPosition(index)
            updateCurrentSong()
        }
    }
    
    fun toggleShuffle() {
        val isShuffled = !_queue.value.isShuffled
        
        if (isShuffled) {
            // Shuffle the queue
            val shuffled = _queue.value.songs.shuffled()
            setQueue(shuffled)
        } else {
            // Restore original order
            _queue.value = _queue.value.copy(isShuffled = false)
        }
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        _queue.value = _queue.value.copy(repeatMode = mode)
        
        // Configure ExoPlayer repeat
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }
    
    private fun updateCurrentSong() {
        val index = exoPlayer.currentMediaItemIndex
        if (index in _queue.value.songs.indices) {
            _queue.value = _queue.value.copy(currentIndex = index)
            _currentSong.value = _queue.value.songs[index]
        }
    }
}
```

### UI - Queue Screen
```kotlin
// ui/screens/QueueScreen.kt
@Composable
fun QueueScreen(
    viewModel: QueueViewModel,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.queue.collectAsState()
    
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(queue.songs) { index, song ->
            QueueItemCard(
                song = song,
                isCurrent = index == queue.currentIndex,
                isShuffled = queue.isShuffled,
                repeatMode = queue.repeatMode,
                onItemClick = { viewModel.jumpToQueue(index) },
                onRemoveClick = { viewModel.removeSongFromQueue(index) }
            )
        }
    }
}

@Composable
fun QueueItemCard(
    song: Song,
    isCurrent: Boolean,
    isShuffled: Boolean,
    repeatMode: RepeatMode,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .background(if (isCurrent) Color.DarkGray else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrent) {
                Icon(
                    painter = painterResource(R.drawable.ic_playing),
                    contentDescription = "Now playing",
                    tint = Color.Cyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) Color.White else Color.Gray
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.Gray
            )
        }
    }
}
```

---

## 3. Homepage Content Loading

### Repository
```kotlin
// data/repository/HomeRepository.kt
class HomeRepository(
    private val innerTubeAPI: InnerTubeService,
    private val homeDao: HomeDao,
    private val appDatabase: AppDatabase
) {
    
    suspend fun getHomeContent(): Result<HomeContent> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = homeDao.getHomeContent().firstOrNull()
            if (cached != null && cached.isCacheFresh()) {
                return@withContext Result.success(cached.toDomain())
            }
            
            // Fetch fresh content
            val response = innerTubeAPI.getHomeFeed()
            val homeContent = response.toDomainModel()
            
            // Cache result
            homeDao.insertHomeContent(homeContent.toEntity())
            
            Result.success(homeContent)
        } catch (e: Exception) {
            // Fallback to cache if network fails
            val cached = homeDao.getHomeContent().firstOrNull()
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }
}
```

### ViewModel
```kotlin
// viewmodel/HomeViewModel.kt
class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {
    
    private val _homeContent = MutableStateFlow<HomeContent?>(null)
    val homeContent = _homeContent.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    init {
        loadHome()
    }
    
    fun loadHome() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = homeRepository.getHomeContent()
            
            result.onSuccess { content ->
                val enrichedContent = enrichWithRecommendations(content)
                _homeContent.value = enrichedContent
                _isLoading.value = false
            }
            
            result.onFailure { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun enrichWithRecommendations(
        content: HomeContent
    ): HomeContent {
        val recommendations = recommendationEngine.generateQuickPicks()
        return content.copy(
            quickPicks = recommendations
        )
    }
    
    fun refreshHome() {
        loadHome()
    }
}
```

### UI - Home Screen
```kotlin
// ui/screens/HomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val homeContent by viewModel.homeContent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (error != null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: ${error}")
                Button(onClick = { viewModel.refreshHome() }) {
                    Text("Retry")
                }
            }
        }
        return
    }
    
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SearchBar(modifier = Modifier.fillMaxWidth())
        }
        
        homeContent?.recommendations?.forEach { carousel ->
            item {
                CarouselSection(
                    title = carousel.title,
                    songs = carousel.songs
                )
            }
        }
        
        item {
            Text("Quick Picks", style = MaterialTheme.typography.headlineSmall)
        }
        
        item {
            QuickPicksCarousel(
                picks = homeContent?.quickPicks ?: emptyList()
            )
        }
    }
}
```

---

## 4. Quick Picks Implementation

### Use Case
```kotlin
// domain/usecase/GetQuickPicksUseCase.kt
class GetQuickPicksUseCase(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val recommendationRepository: RecommendationRepository
) {
    
    suspend operator fun invoke(): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            // Get user profile
            val userProfile = userRepository.getUserProfile()
            
            // Get listening history
            val recentPlays = userRepository.getRecentPlays(limit = 100)
            
            // Fetch recommendations from InnerTube
            val apiRecommendations = recommendationRepository
                .getRecommendations(userProfile)
            
            // Filter: remove already played, liked, or skipped
            val played = recentPlays.map { it.songId }.toSet()
            val filtered = apiRecommendations.filterNot { pick ->
                pick.id in played
            }
            
            // Rank by relevance
            val ranked = rankByRelevance(filtered, userProfile)
            
            // Limit to top 10
            Result.success(ranked.take(10))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun rankByRelevance(
        picks: List<Song>,
        profile: UserProfile
    ): List<Song> {
        return picks.sortedByDescending { song ->
            val genreScore = if (song.genre in profile.preferredGenres) 0.5f else 0f
            val popularityScore = song.popularity / 100f * 0.3f
            val freshnessScore = if (song.isNew) 0.2f else 0f
            
            genreScore + popularityScore + freshnessScore
        }
    }
}
```

### Repository
```kotlin
// data/repository/RecommendationRepository.kt
class RecommendationRepository(
    private val innerTubeAPI: InnerTubeService,
    private val quickPicksDao: QuickPicksDao
) {
    
    suspend fun getRecommendations(
        profile: UserProfile
    ): List<Song> = withContext(Dispatchers.IO) {
        val response = innerTubeAPI.getQuickPicks(
            userId = profile.id,
            preferences = profile.toApiModel()
        )
        
        response.picks.map { pick ->
            Song(
                id = pick.videoId,
                title = pick.title,
                artist = pick.artist,
                thumbnailUrl = pick.thumbnail,
                audioUrl = pick.streamUrl
            )
        }
    }
}
```

---

Dokumentasi implementasi ini menunjukkan pola desain real-world yang digunakan dalam Echo Music dengan Kotlin, Jetpack Compose, dan architecture patterns modern.

