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
import com.aniplex.app.domain.model.HomeData
import com.aniplex.app.domain.model.SpotlightAnime
import com.aniplex.app.presentation.components.AnimeCard
import com.aniplex.app.presentation.components.AnimeRowShimmer
import com.aniplex.app.presentation.components.SpotlightBannerShimmer
import com.aniplex.app.theme.BackgroundVoid
import com.aniplex.app.theme.CrunchyrollOrange
import com.aniplex.app.theme.NetflixRed
import com.aniplex.app.theme.SurfaceDark
import com.aniplex.app.theme.SurfaceDarkVariant
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnimeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing = uiState is HomeUiState.Loading && (uiState as? HomeUiState.Success) != null
    
    var dominantColor by remember { mutableStateOf(BackgroundVoid) }
    val animatedColor by animateColorAsState(targetValue = dominantColor, animationSpec = tween(1500))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.4f),
                        BackgroundVoid,
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
                    // Show premium shimmer skeletons while loading
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
                        onAnimeClick = onAnimeClick,
                        onColorExtracted = { dominantColor = it },
                        modifier = Modifier.fillMaxSize()
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
    onAnimeClick: (String) -> Unit,
    onColorExtracted: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Spotlight Banner Carousel (Auto-scrolling Pager)
        if (homeData.spotlightAnimes.isNotEmpty()) {
            SpotlightCarousel(
                spotlightList = homeData.spotlightAnimes,
                onAnimeClick = onAnimeClick,
                onColorExtracted = onColorExtracted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Trending Section
        if (homeData.trendingAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Trending Now",
                animeList = homeData.trendingAnimes,
                onAnimeClick = onAnimeClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Top Airing Section
        if (homeData.topAiringAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Top Airing",
                animeList = homeData.topAiringAnimes,
                onAnimeClick = onAnimeClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Recently Added Section (between Airing and Popular)
        if (homeData.recentlyUpdatedAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Recently Added",
                animeList = homeData.recentlyUpdatedAnimes,
                onAnimeClick = onAnimeClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. Most Popular Section
        if (homeData.mostPopularAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Most Popular",
                animeList = homeData.mostPopularAnimes,
                onAnimeClick = onAnimeClick
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. Top Upcoming Section
        if (homeData.topUpcomingAnimes.isNotEmpty()) {
            AnimeSectionRow(
                title = "Top Upcoming",
                animeList = homeData.topUpcomingAnimes,
                onAnimeClick = onAnimeClick
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
    
    // Extract color for the current page
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
            delay(4000)
            if (spotlightList.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % spotlightList.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = modifier) {
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

                // Dark ambient gradient overlay (Netflix cinema feel)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    BackgroundVoid
                                )
                            )
                        )
                )

                // Content Panel Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Rank Badge
                    if (anime.rank > 0) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${anime.rank} Spotlight",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Anime Name Title
                    Text(
                        text = anime.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Badges Info (Sub, Dub, Type, etc.)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        anime.otherInfo.forEach { info ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = info.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Summary Description
                    if (anime.description.isNotBlank()) {
                        Text(
                            text = anime.description,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Watch Now and Bookmark CTA Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onAnimeClick(anime.id) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Watching E1",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { /* Add to Watchlist action */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.BookmarkBorder,
                                contentDescription = "Add to List",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Pager Indicators
        Row(
            Modifier
                .height(30.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(spotlightList.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) CrunchyrollOrange else Color.LightGray.copy(alpha = 0.5f)
                val width = if (pagerState.currentPage == iteration) 24.dp else 12.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(width)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun GenreChipsRow(
    genres: List<String>,
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
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceDarkVariant, RoundedCornerShape(16.dp))
                    .clickable { /* Genre click logic can be added later */ }
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(animeList) { anime ->
                AnimeCard(
                    anime = anime,
                    onClick = onAnimeClick
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
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RETRY",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
