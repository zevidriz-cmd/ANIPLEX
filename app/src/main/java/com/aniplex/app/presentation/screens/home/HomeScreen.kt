package com.aniplex.app.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aniplex.app.domain.model.Anime
import com.aniplex.app.domain.model.HistoryItem
import com.aniplex.app.domain.model.HomeData
import com.aniplex.app.domain.model.SpotlightAnime
import com.aniplex.app.presentation.components.AnimeCard
import com.aniplex.app.presentation.components.ContinueWatchingCard
import com.aniplex.app.presentation.components.AnimeRowShimmer
import com.aniplex.app.presentation.components.SpotlightBannerShimmer
import com.aniplex.app.theme.BackgroundVoid
import com.aniplex.app.theme.CrunchyrollOrange
import com.aniplex.app.theme.NetflixRed
import com.aniplex.app.theme.SurfaceDark
import com.aniplex.app.theme.SurfaceDarkVariant
import com.aniplex.app.theme.TextMuted
import com.aniplex.app.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    onEpisodeClick: (String, String, String, Int, String) -> Unit,
    onSearchClick: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val continueWatchingList by viewModel.continueWatchingList.collectAsStateWithLifecycle()
    val isRefreshing = uiState is HomeUiState.Loading && (uiState as? HomeUiState.Success) != null
    
    var dominantColor by remember { mutableStateOf(BackgroundVoid) }
    val animatedColor by animateColorAsState(targetValue = dominantColor, animationSpec = tween(1500))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.35f),
                        BackgroundVoid.copy(alpha = 0.95f),
                        BackgroundVoid
                    )
                )
            )
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SpotlightBannerShimmer()
                        Spacer(modifier = Modifier.height(16.dp))
                        AnimeRowShimmer()
                        AnimeRowShimmer()
                    }
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        homeData = state.homeData,
                        continueWatchingList = continueWatchingList,
                        onAnimeClick = onAnimeClick,
                        onEpisodeClick = onEpisodeClick,
                        onColorExtracted = { dominantColor = it },
                        onSearchClick = onSearchClick,
                        onNavigateToDiscover = onNavigateToDiscover,
                        modifier = Modifier.fillMaxSize(),
                        onRemoveFromHistory = { viewModel.removeFromHistory(it) },
                        onMarkAsFinished = { id, title, poster -> viewModel.markAsWatched(id, title, poster) },
                        onAddToWatchlist = { id, title, poster -> viewModel.addToWatchlist(id, title, poster) },
                        onMarkAsWatched = { id, title, poster -> viewModel.markAsWatched(id, title, poster) }
                    )
                }
                is HomeUiState.Error -> {
                    ErrorLayout(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    homeData: HomeData,
    continueWatchingList: List<HistoryItem>,
    onAnimeClick: (String) -> Unit,
    onEpisodeClick: (String, String, String, Int, String) -> Unit,
    onColorExtracted: (Color) -> Unit,
    onSearchClick: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    modifier: Modifier = Modifier,
    onRemoveFromHistory: ((String) -> Unit)? = null,
    onMarkAsFinished: ((String, String, String) -> Unit)? = null,
    onAddToWatchlist: ((String, String, String) -> Unit)? = null,
    onMarkAsWatched: ((String, String, String) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Spotlight Banner Carousel (Premium Floating Card Pager)
        if (homeData.spotlightAnimes.isNotEmpty()) {
            SpotlightCarousel(
                spotlightList = homeData.spotlightAnimes,
                onAnimeClick = onAnimeClick,
                onColorExtracted = onColorExtracted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(390.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1.5. Premium Continue Watching Row (Direct resume access!)
        if (continueWatchingList.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Continue Watching",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(continueWatchingList) { item ->
                        ContinueWatchingCard(
                            item = item,
                            onClick = {
                                onEpisodeClick(
                                    item.episodeId,
                                    item.animeId,
                                    item.animeTitle,
                                    item.episodeNumber,
                                    "sub"
                                )
                            },
                            onRemoveFromHistory = onRemoveFromHistory,
                            onMarkAsFinished = onMarkAsFinished
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 2. Genres list
        if (homeData.genres.isNotEmpty()) {
            GenreChipsRow(
                genres = homeData.genres,
                onGenreClick = { /* Can pass genre to search in future, for now just open search */ onSearchClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // 3. Trending Now Section (Landscape styled video-tiles like in the screenshot!)
        if (homeData.trendingAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Trending Now",
                animeList = homeData.trendingAnimes,
                onAnimeClick = onAnimeClick,
                isLandscape = true,
                onSeeAllClick = onNavigateToDiscover,
                onAddToWatchlist = onAddToWatchlist,
                onMarkAsWatched = onMarkAsWatched
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Top Airing Section (Portrait)
        if (homeData.topAiringAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Top Airing",
                animeList = homeData.topAiringAnimes,
                onAnimeClick = onAnimeClick,
                isLandscape = false,
                onSeeAllClick = onNavigateToDiscover,
                onAddToWatchlist = onAddToWatchlist,
                onMarkAsWatched = onMarkAsWatched
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Recently Added Section (Landscape)
        if (homeData.recentlyUpdatedAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Recently Added",
                animeList = homeData.recentlyUpdatedAnimes,
                onAnimeClick = onAnimeClick,
                isLandscape = true,
                onSeeAllClick = onNavigateToDiscover,
                onAddToWatchlist = onAddToWatchlist,
                onMarkAsWatched = onMarkAsWatched
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. Most Popular Section (Portrait)
        if (homeData.mostPopularAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Most Popular",
                animeList = homeData.mostPopularAnimes,
                onAnimeClick = onAnimeClick,
                isLandscape = false,
                onSeeAllClick = onNavigateToDiscover,
                onAddToWatchlist = onAddToWatchlist,
                onMarkAsWatched = onMarkAsWatched
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. Top Upcoming Section (Portrait)
        if (homeData.topUpcomingAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Top Upcoming",
                animeList = homeData.topUpcomingAnimes,
                onAnimeClick = onAnimeClick,
                isLandscape = false,
                onSeeAllClick = onNavigateToDiscover,
                onAddToWatchlist = onAddToWatchlist,
                onMarkAsWatched = onMarkAsWatched
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SpotlightCarousel(
    spotlightList: List<SpotlightAnime>,
    onAnimeClick: (String) -> Unit,
    onColorExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { spotlightList.size })
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Extract color for the current page to animate background tint
    LaunchedEffect(pagerState.currentPage) {
        if (spotlightList.isNotEmpty()) {
            val currentPoster = spotlightList[pagerState.currentPage].poster
            if (currentPoster.isNotEmpty()) {
                val request = ImageRequest.Builder(context)
                    .data(currentPoster)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        androidx.palette.graphics.Palette.from(bitmap).generate { palette ->
                            val color = palette?.darkMutedSwatch?.rgb ?: palette?.dominantSwatch?.rgb
                            if (color != null) {
                                onColorExtracted(Color(color))
                            } else {
                                onColorExtracted(BackgroundVoid)
                            }
                        }
                    }
                }
            }
        }
    }

    // Auto-scroll loop
    LaunchedEffect(key1 = true) {
        while (true) {
            delay(5000)
            if (spotlightList.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % spotlightList.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFF2C2A47),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val anime = spotlightList[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onAnimeClick(anime.id) }
                ) {
                    // Background Poster Image
                    AsyncImage(
                        model = anime.poster,
                        contentDescription = anime.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cinematic dark blue-black gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0D0C15).copy(alpha = 0.35f),
                                        Color(0xFF0D0C15).copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    // Card Info Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Season Finale / Spotlight Badge (User screenshot violet style)
                        val badgeText = if (anime.id.contains("solo") || anime.rank == 1) "SEASON FINALE" else "SPOTLIGHT #${anime.rank}"
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFFC5BAFF)) // Premium light lavender purple badge background
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF13111E), // Dark navy text for contrast
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Anime Title Header
                        Text(
                            text = anime.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Badges Info
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            anime.otherInfo.take(3).forEach { info ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF222036))
                                        .border(1.dp, Color(0xFF333054), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = info.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Description Summary
                        if (anime.description.isNotBlank()) {
                            Text(
                                text = anime.description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        // CTA Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onAnimeClick(anime.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Watch",
                                        tint = Color(0xFF13111E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "WATCH NOW",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF13111E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Carousel Indicators top-end
            Row(
                Modifier
                    .height(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(spotlightList.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) CrunchyrollOrange else Color.LightGray.copy(alpha = 0.4f)
                    val size = if (pagerState.currentPage == iteration) 7.dp else 4.dp
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp)
                            .size(size)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
fun GenreChipsRow(
    genres: List<String>,
    onGenreClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(genres) { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark.copy(alpha = 0.6f))
                    .border(1.dp, Color(0xFF222036), RoundedCornerShape(16.dp))
                    .clickable { onGenreClick(genre) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = genre,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AnimeSectionRow(
    title: String,
    animeList: List<Anime>,
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    onSeeAllClick: (() -> Unit)? = null,
    onAddToWatchlist: ((String, String, String) -> Unit)? = null,
    onMarkAsWatched: ((String, String, String) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            if (onSeeAllClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSeeAllClick() }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "See all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrunchyrollOrange
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "See all $title",
                        tint = CrunchyrollOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            items(animeList) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = onAnimeClick,
                    isLandscape = isLandscape,
                    onAddToWatchlist = onAddToWatchlist,
                    onMarkAsWatched = onMarkAsWatched
                )
            }
        }
    }
}

@Composable
fun ErrorLayout(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = NetflixRed,
            modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Oops, something went wrong!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CrunchyrollOrange)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = Color(0xFF13111E)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RETRY",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF13111E)
                )
            }
        }
    }
}
