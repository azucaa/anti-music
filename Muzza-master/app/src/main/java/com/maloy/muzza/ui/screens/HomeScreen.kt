package com.maloy.muzza.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.maloy.innertube.models.AlbumItem
import com.maloy.innertube.models.Artist
import com.maloy.innertube.models.ArtistItem
import com.maloy.innertube.models.PlaylistItem
import com.maloy.innertube.models.SongItem
import com.maloy.innertube.models.WatchEndpoint
import com.maloy.innertube.utils.parseCookieString
import com.maloy.muzza.LocalDatabase
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.LocalPlayerConnection
import com.maloy.muzza.R
import com.maloy.muzza.constants.GridThumbnailHeight
import com.maloy.muzza.constants.InnerTubeCookieKey
import com.maloy.muzza.constants.ListItemHeight
import com.maloy.muzza.extensions.togglePlayPause
import com.maloy.muzza.models.toMediaMetadata
import com.maloy.muzza.playback.queues.YouTubeAlbumRadio
import com.maloy.muzza.playback.queues.YouTubeQueue
import com.maloy.muzza.ui.component.AlbumSmallGridItem
import com.maloy.muzza.ui.component.ArtistSmallGridItem
import com.maloy.muzza.ui.component.HideOnScrollFAB
import com.maloy.muzza.ui.component.LocalMenuState
import com.maloy.muzza.ui.component.NavigationTile
import com.maloy.muzza.ui.component.NavigationTitle
import com.maloy.muzza.ui.component.SongListItem
import com.maloy.muzza.ui.component.SongSmallGridItem
import com.maloy.muzza.ui.component.YouTubeGridItem
import com.maloy.muzza.ui.component.YouTubeSmallGridItem
import com.maloy.muzza.ui.menu.ArtistMenu
import com.maloy.muzza.ui.menu.SongMenu
import com.maloy.muzza.ui.menu.YouTubeAlbumMenu
import com.maloy.muzza.ui.menu.YouTubeArtistMenu
import com.maloy.muzza.ui.menu.YouTubePlaylistMenu
import com.maloy.muzza.ui.menu.YouTubeSongMenu
import com.maloy.muzza.ui.theme.CardBackground
import com.maloy.muzza.ui.theme.CardBesar
import com.maloy.muzza.ui.theme.CardKecil
import com.maloy.muzza.ui.theme.ElevatedBackground
import com.maloy.muzza.ui.theme.MuzzaTheme
import com.maloy.muzza.ui.theme.PillChip
import com.maloy.muzza.ui.theme.SpotifyGreen
import com.maloy.muzza.ui.theme.TextSecondary
import com.maloy.muzza.ui.utils.SnapLayoutInfoProvider
import com.maloy.muzza.utils.rememberPreference
import com.maloy.muzza.viewmodels.HomeViewModel
import java.util.Calendar
import kotlin.random.Random

@SuppressLint("UnrememberedMutableState")
@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val forgottenFavorite by viewModel.forgottenFavorite.collectAsState()
    val homeFirstAlbumRecommendation by viewModel.homeFirstAlbumRecommendation.collectAsState()
    val homeSecondAlbumRecommendation by viewModel.homeSecondAlbumRecommendation.collectAsState()

    val homeFirstArtistRecommendation by viewModel.homeFirstArtistRecommendation.collectAsState()
    val homeSecondArtistRecommendation by viewModel.homeSecondArtistRecommendation.collectAsState()
    val homeThirdArtistRecommendation by viewModel.homeThirdArtistRecommendation.collectAsState()
    val home by viewModel.home.collectAsState()

    val keepListeningSongs by viewModel.keepListeningSongs.collectAsState()
    val keepListeningAlbums by viewModel.keepListeningAlbums.collectAsState()
    val keepListeningArtists by viewModel.keepListeningArtists.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()

    val homeFirstContinuation by viewModel.homeFirstContinuation.collectAsState()
    val homeSecondContinuation by viewModel.homeSecondContinuation.collectAsState()
    val homeThirdContinuation by viewModel.homeThirdContinuation.collectAsState()

    val youtubePlaylists by viewModel.youtubePlaylists.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mostPlayedLazyGridState = rememberLazyGridState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val listenAgainLazyGridState = rememberLazyGridState()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State Sapaan Berdasarkan Waktu Lokal
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Selamat Pagi"
            hour < 15 -> "Selamat Siang"
            hour < 18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    // State Filter Pills Lokal
    var selectedFilter by remember { mutableStateOf("Musik") }
    val filters = listOf("Musik", "Podcast", "Live", "Album")

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = viewModel::refresh,
        indicatorPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val snapLayoutInfoProviderQuickPicks =
                remember(mostPlayedLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = mostPlayedLazyGridState,
                    )
                }
            val snapLayoutInfoProviderForgottenFavorite =
                remember(forgottenFavoritesLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = forgottenFavoritesLazyGridState,
                    )
                }

            Column(
                modifier = Modifier.verticalScroll(scrollState),
            ) {
                Spacer(
                    Modifier.height(
                        LocalPlayerAwareWindowInsets.current
                            .asPaddingValues()
                            .calculateTopPadding(),
                    ),
                )

                // 1. HEADER (Sapaan & Ikon Settings / Discover di Kanan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = greeting,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { /* Action discover tune */ }) {
                            Icon(
                                painter = painterResource(R.drawable.discover_tune),
                                contentDescription = "Discover",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                painter = painterResource(R.drawable.settings),
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 2. FILTER PILLS
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(PillChip)
                                .background(if (isSelected) SpotifyGreen else ElevatedBackground)
                                .combinedClickable(
                                    onClick = { selectedFilter = filter }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Row of NavigationTiles (History, Stats, Account)
                Row(
                    modifier =
                    Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                ) {
                    NavigationTile(
                        title = stringResource(R.string.history),
                        icon = R.drawable.history,
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.weight(1f),
                    )

                    NavigationTile(
                        title = stringResource(R.string.stats),
                        icon = R.drawable.trending_up,
                        onClick = { navController.navigate("stats") },
                        modifier = Modifier.weight(1f),
                    )

                    if (isLoggedIn) {
                        NavigationTile(
                            title = stringResource(R.string.account),
                            icon = R.drawable.person,
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // 3. QUICK PICKS (2-COLUMN GRID - Spotify Aesthetic)
                NavigationTitle(
                    title = stringResource(R.string.quick_picks),
                )

                quickPicks?.let { list ->
                    if (list.isEmpty()) {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4),
                        ) {
                            Text(
                                text = stringResource(R.string.quick_picks_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    } else {
                        val gridItems = list.take(6)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) // Sized perfectly to host 3 rows static
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false // Let parent scroll
                        ) {
                            items(gridItems) { originalSong ->
                                val song by database
                                    .song(originalSong.id)
                                    .collectAsState(initial = originalSong)

                                val isCurrent = song!!.id == mediaMetadata?.id

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(CardKecil)
                                        .background(CardBackground)
                                        .combinedClickable(
                                            onClick = {
                                                if (song!!.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song!!.id),
                                                            song!!.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                ) {
                                    AsyncImage(
                                        model = song!!.song.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = song!!.song.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) SpotifyGreen else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song!!.artists.joinToString { it.name },
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurrent && isPlaying) {
                                        Icon(
                                            painter = painterResource(R.drawable.graphic_eq),
                                            contentDescription = null,
                                            tint = SpotifyGreen,
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. TRENDING SECTION (Gradients & 120dp Cover Layout)
                val trendingAlbums = explorePage?.newReleaseAlbums?.take(10) ?: emptyList()
                if (trendingAlbums.isNotEmpty()) {
                    NavigationTitle(
                        title = "Trending Sekarang",
                        onClick = { navController.navigate("new_release") }
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingAlbums) { album ->
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CardBesar)
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = album.thumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                                startY = 60f
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = album.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artists?.joinToString { it.name } ?: "YouTube Music",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // YouTube playlists
                if (youtubePlaylists?.isNotEmpty() == true) {
                    NavigationTitle(
                        title = stringResource(R.string.your_ytb_playlists),
                        onClick = {
                            navController.navigate("account")
                        },
                    )
                    LazyRow(
                        contentPadding =
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(
                            items = youtubePlaylists.orEmpty(),
                            key = { it.id },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("online_playlist/${item.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                            )
                        }
                    }
                }

                // Keep Listening / Listen Again
                if (keepListening?.isNotEmpty() == true) {
                    keepListening?.let {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                        )

                        LazyHorizontalGrid(
                            state = listenAgainLazyGridState,
                            rows = GridCells.Fixed(if (keepListening!!.size > 6) 2 else 1),
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(GridThumbnailHeight * if (keepListening!!.size > 6) 2.4f else 1.2f),
                        ) {
                            keepListening?.forEach {
                                when (it) {
                                    in 0..4 ->
                                        item {
                                            ArtistSmallGridItem(
                                                artist = keepListeningArtists!![it],
                                                modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("artist/${keepListeningArtists!![it].id}")
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress,
                                                            )
                                                            menuState.show {
                                                                ArtistMenu(
                                                                    originalArtist = keepListeningArtists!![it],
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ),
                                            )
                                        }

                                    in 5..9 ->
                                        item {
                                            AlbumSmallGridItem(
                                                song = keepListeningAlbums!![it - 5],
                                                modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate(
                                                                "album/${keepListeningAlbums!![it - 5].song.albumId}",
                                                            )
                                                        },
                                                    ),
                                            )
                                        }

                                    in 10..19 ->
                                        item {
                                            SongSmallGridItem(
                                                song = keepListeningSongs!![it - 10],
                                                modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (keepListeningSongs!![it - 10].id == mediaMetadata?.id) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = keepListeningSongs!![it - 10].id),
                                                                        keepListeningSongs!![it - 10].toMediaMetadata(),
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress,
                                                            )
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = keepListeningSongs!![it - 10],
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ),
                                                isActive = keepListeningSongs!![it - 10].song.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                            )
                                        }
                                }
                            }
                        }
                    }
                }

                homeFirstArtistRecommendation?.let { albums ->
                    if (albums.listItem.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.similar_to) + " " + albums.artistName,
                        )

                        LazyRow(
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = albums.listItem,
                                key = { it.id },
                            ) { item ->
                                if (!item.title.contains("Presenting")) {
                                    YouTubeSmallGridItem(
                                        item = item,
                                        isActive = mediaMetadata?.album?.id == item.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is PlaylistItem ->
                                                            navController.navigate(
                                                                "online_playlist/${item.id}",
                                                            )

                                                        is SongItem -> {
                                                            if (item.id == mediaMetadata?.id) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata(),
                                                                    ),
                                                                )
                                                            }
                                                        }

                                                        is AlbumItem -> navController.navigate("album/${item.id}")

                                                        else -> navController.navigate("artist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        when (item) {
                                                            is PlaylistItem ->
                                                                YouTubePlaylistMenu(
                                                                    playlist = item,
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss,
                                                                )

                                                            is ArtistItem -> {
                                                                YouTubeArtistMenu(
                                                                    artist = item,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is SongItem -> {
                                                                YouTubeSongMenu(
                                                                    song = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is AlbumItem -> {
                                                                YouTubeAlbumMenu(
                                                                    albumItem = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            else -> {
                                                            }
                                                        }
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                forgottenFavorite?.let { forgottenFav ->
                    if (forgottenFav.isNotEmpty() && forgottenFav.size > 5) {
                        NavigationTitle(
                            title = stringResource(R.string.forgotten_favorites),
                        )

                        LazyHorizontalGrid(
                            state = forgottenFavoritesLazyGridState,
                            rows = GridCells.Fixed(4),
                            flingBehavior =
                            rememberSnapFlingBehavior(
                                snapLayoutInfoProviderForgottenFavorite,
                            ),
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4),
                        ) {
                            items(
                                items = forgottenFav,
                                key = { it.id },
                            ) { originalSong ->
                                val song by database
                                    .song(originalSong.id)
                                    .collectAsState(initial = originalSong)
                                SongListItem(
                                    song = song!!,
                                    showInLibraryIcon = true,
                                    isActive = song!!.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                    Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song!!.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song!!.id),
                                                            song!!.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song!!,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                home?.forEach { homePlaylists ->
                    if (homePlaylists.playlists.isNotEmpty()) {
                        homePlaylists.let { playlists ->
                            NavigationTitle(
                                title = playlists.playlistName,
                            )

                            LazyRow(
                                contentPadding =
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                            ) {
                                items(
                                    items = playlists.playlists,
                                    key = { it.id },
                                ) { playlist ->
                                    playlist.author ?: run {
                                        playlist.author =
                                            Artist(name = "YouTube Music", id = null)
                                    }
                                    YouTubeGridItem(
                                        item = playlist,
                                        isActive = mediaMetadata?.album?.id == playlist.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${playlist.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeFirstAlbumRecommendation?.albums?.let { albums ->
                    if (albums.recommendationAlbum.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.similar_to) + " " + albums.recommendedAlbum.name,
                        )

                        LazyRow(
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = albums.recommendationAlbum,
                                key = { it.id },
                            ) { album ->
                                if (!album.title.contains("Presenting")) {
                                    YouTubeGridItem(
                                        item = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = album,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeFirstContinuation?.forEach { homePlaylists ->
                    if (homePlaylists.playlists.isNotEmpty()) {
                        homePlaylists.let { playlists ->
                            NavigationTitle(
                                title = playlists.playlistName,
                            )

                            LazyRow(
                                contentPadding =
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                            ) {
                                items(
                                    items = playlists.playlists,
                                    key = { it.id },
                                ) { playlist ->
                                    playlist.author ?: run {
                                        playlist.author =
                                            Artist(name = "YouTube Music", id = null)
                                    }
                                    YouTubeGridItem(
                                        item = playlist,
                                        isActive = mediaMetadata?.album?.id == playlist.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${playlist.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeSecondArtistRecommendation?.let { albums ->
                    if (albums.listItem.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.similar_to) + " " + albums.artistName,
                        )

                        LazyRow(
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = albums.listItem,
                                key = { it.id },
                            ) { item ->
                                if (!item.title.contains("Presenting")) {
                                    YouTubeSmallGridItem(
                                        item = item,
                                        isActive = mediaMetadata?.album?.id == item.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is PlaylistItem ->
                                                            navController.navigate(
                                                                "online_playlist/${item.id}",
                                                            )

                                                        is SongItem -> {
                                                            if (item.id == mediaMetadata?.id) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata(),
                                                                    ),
                                                                )
                                                            }
                                                        }

                                                        is AlbumItem -> navController.navigate("album/${item.id}")

                                                        else -> navController.navigate("artist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        when (item) {
                                                            is PlaylistItem ->
                                                                YouTubePlaylistMenu(
                                                                    playlist = item,
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss,
                                                                )

                                                            is ArtistItem -> {
                                                                YouTubeArtistMenu(
                                                                    artist = item,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is SongItem -> {
                                                                YouTubeSongMenu(
                                                                    song = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is AlbumItem -> {
                                                                YouTubeAlbumMenu(
                                                                    albumItem = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            else -> {
                                                            }
                                                        }
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeSecondContinuation?.forEach { homePlaylists ->
                    if (homePlaylists.playlists.isNotEmpty()) {
                        homePlaylists.let { playlists ->
                            NavigationTitle(
                                title = playlists.playlistName,
                            )

                            LazyRow(
                                contentPadding =
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                            ) {
                                items(
                                    items = playlists.playlists,
                                    key = { it.id },
                                ) { playlist ->
                                    playlist.author ?: run {
                                        playlist.author =
                                            Artist(name = "YouTube Music", id = null)
                                    }
                                    YouTubeGridItem(
                                        item = playlist,
                                        isActive = mediaMetadata?.album?.id == playlist.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${playlist.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeSecondAlbumRecommendation?.albums?.let { albums ->
                    if (albums.recommendationAlbum.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.similar_to) + " " + albums.recommendedAlbum.name,
                        )

                        LazyRow(
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = albums.recommendationAlbum,
                                key = { it.id },
                            ) { album ->
                                if (!album.title.contains("Presenting")) {
                                    YouTubeGridItem(
                                        item = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = album,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeThirdContinuation?.forEach { homePlaylists ->
                    if (homePlaylists.playlists.isNotEmpty()) {
                        homePlaylists.let { playlists ->
                            NavigationTitle(
                                title = playlists.playlistName,
                            )

                            LazyRow(
                                contentPadding =
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                            ) {
                                items(
                                    items = playlists.playlists,
                                    key = { it.id },
                                ) { playlist ->
                                    playlist.author ?: run {
                                        playlist.author =
                                            Artist(name = "YouTube Music", id = null)
                                    }
                                    YouTubeGridItem(
                                        item = playlist,
                                        isActive = mediaMetadata?.album?.id == playlist.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("online_playlist/${playlist.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        YouTubePlaylistMenu(
                                                            playlist = playlist,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                homeThirdArtistRecommendation?.let { albums ->
                    if (albums.listItem.isNotEmpty()) {
                        NavigationTitle(
                            title = stringResource(R.string.similar_to) + " " + albums.artistName,
                        )

                        LazyRow(
                            contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                        ) {
                            items(
                                items = albums.listItem,
                                key = { it.id },
                            ) { item ->
                                if (!item.title.contains("Presenting")) {
                                    YouTubeSmallGridItem(
                                        item = item,
                                        isActive = mediaMetadata?.album?.id == item.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is PlaylistItem ->
                                                            navController.navigate(
                                                                "online_playlist/${item.id}",
                                                            )

                                                        is SongItem -> {
                                                            if (item.id == mediaMetadata?.id) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata(),
                                                                    ),
                                                                )
                                                            }
                                                        }

                                                        is AlbumItem -> navController.navigate("album/${item.id}")

                                                        else -> navController.navigate("artist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress,
                                                    )
                                                    menuState.show {
                                                        when (item) {
                                                            is PlaylistItem ->
                                                                YouTubePlaylistMenu(
                                                                    playlist = item,
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss,
                                                                )

                                                            is ArtistItem -> {
                                                                YouTubeArtistMenu(
                                                                    artist = item,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is SongItem -> {
                                                                YouTubeSongMenu(
                                                                    song = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            is AlbumItem -> {
                                                                YouTubeAlbumMenu(
                                                                    albumItem = item,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }

                                                            else -> {
                                                            }
                                                        }
                                                    }
                                                },
                                            ).animateItemPlacement(),
                                    )
                                }
                            }
                        }
                    }
                }

                explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
                        onClick = {
                            navController.navigate("new_release")
                        },
                    )

                    LazyRow(
                        contentPadding =
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                    ) {
                        items(
                            items = newReleaseAlbums,
                            key = { it.id },
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("album/${album.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ).animateItemPlacement(),
                            )
                        }
                    }
                }
                Spacer(
                    Modifier.height(
                        LocalPlayerAwareWindowInsets.current
                            .asPaddingValues()
                            .calculateBottomPadding(),
                    ),
                )
            }

            HideOnScrollFAB(
                visible =
                !quickPicks.isNullOrEmpty() || !forgottenFavorite.isNullOrEmpty() || explorePage?.newReleaseAlbums?.isNotEmpty() == true,
                scrollState = scrollState,
                icon = R.drawable.casino,
                onClick = {
                    if (Random.nextBoolean() && !quickPicks.isNullOrEmpty()) {
                        val song = quickPicks!!.random()
                        playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
                    } else if (explorePage?.newReleaseAlbums?.isNotEmpty() == true) {
                        val album = explorePage?.newReleaseAlbums!!.random()
                        playerConnection.playQueue(YouTubeAlbumRadio(album.playlistId))
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
fun HomeScreenPreview() {
    MuzzaTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D0D))
                .padding(16.dp)
        ) {
            Text(
                text = "Selamat Malam",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}