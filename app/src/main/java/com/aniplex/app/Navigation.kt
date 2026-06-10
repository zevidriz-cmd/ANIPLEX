package com.aniplex.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.aniplex.app.presentation.screens.auth.LoginScreen
import com.aniplex.app.presentation.screens.browse.BrowseScreen
import com.aniplex.app.presentation.screens.detail.DetailScreen
import com.aniplex.app.presentation.screens.home.HomeScreen
import com.aniplex.app.presentation.screens.player.PlayerScreen
import com.aniplex.app.presentation.screens.profile.ProfileScreen
import com.aniplex.app.presentation.screens.profile.ProfileSelectionScreen
import com.aniplex.app.presentation.screens.schedule.ScheduleScreen
import com.aniplex.app.presentation.screens.watchlist.WatchlistScreen
import com.aniplex.app.presentation.screens.history.HistoryScreen
import com.aniplex.app.presentation.screens.search.SearchScreen
import com.aniplex.app.presentation.screens.splash.SplashScreen
import com.aniplex.app.presentation.screens.downloads.DownloadsScreen
import com.aniplex.app.theme.CrunchyrollOrange
import com.aniplex.app.theme.SurfaceDark

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Splash)

    androidx.activity.compose.BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Splash> {
                SplashScreen(
                    onNavigate = { nextScreen ->
                        backStack.removeLastOrNull() // Pop Splash
                        backStack.add(nextScreen)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Login> {
                LoginScreen(
                    onLoginSuccess = {
                        backStack.removeLastOrNull() // Pop Login
                        backStack.add(ProfileSelection(showBack = false))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<ProfileSelection> { key ->
                ProfileSelectionScreen(
                    onProfileSelected = {
                        if (key.showBack) {
                            backStack.removeLastOrNull()
                        } else {
                            backStack.removeLastOrNull() // Pop ProfileSelection
                            backStack.add(Home)
                        }
                    },
                    onSignOut = {
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Home> {
                DashboardShell(
                    onAnimeClick = { animeId ->
                        backStack.add(Detail(animeId))
                    },
                    onEpisodeClick = { epId, animId, title, epNum, cat ->
                        backStack.add(Player(epId, animId, title, epNum, cat, resumePlayback = false))
                    },
                    onSearchClick = {
                        backStack.add(Search)
                    },
                    onSignOut = {
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    onWatchlistClick = {
                        backStack.add(Watchlist)
                    },
                    onHistoryClick = {
                        backStack.add(History)
                    },
                    onSwitchProfile = {
                        backStack.add(ProfileSelection(showBack = true))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Search> {
                SearchScreen(
                    onAnimeClick = { animeId ->
                        backStack.add(Detail(animeId))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Watchlist> {
                WatchlistScreen(
                    onAnimeClick = { animeId ->
                        backStack.add(Detail(animeId))
                    },
                    onEpisodeClick = { epId, animId, title, epNum, cat ->
                        backStack.add(Player(epId, animId, title, epNum, cat, resumePlayback = true))
                    },
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<History> {
                HistoryScreen(
                    onAnimeClick = { animeId ->
                        backStack.add(Detail(animeId))
                    },
                    onEpisodeClick = { epId, animId, title, epNum, cat ->
                        backStack.add(Player(epId, animId, title, epNum, cat, resumePlayback = true))
                    },
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Detail> { key ->
                DetailScreen(
                    animeId = key.animeId,
                    onBackClick = { backStack.removeLastOrNull() },
                    onPlayClick = { epId, animId, title, epNum, cat ->
                        backStack.add(Player(epId, animId, title, epNum, cat, resumePlayback = false))
                    },
                    onRecommendationClick = { recId ->
                        backStack.add(Detail(recId))
                    },
                    onSeasonSelect = { newId ->
                        backStack.removeLastOrNull()
                        backStack.add(Detail(newId))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Player> { key ->
                PlayerScreen(
                    episodeId = key.episodeId,
                    animeId = key.animeId,
                    animeTitle = key.animeTitle,
                    episodeNumber = key.episodeNumber,
                    category = key.category,
                    resumePlayback = key.resumePlayback,
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Downloads> {
                DownloadsScreen(
                    onPlayClick = { epId, animId, title, epNum, cat ->
                        backStack.add(Player(epId, animId, title, epNum, cat, resumePlayback = false))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}

enum class DashboardTab {
    HOME, MY_LISTS, DOWNLOADS, BROWSE, SIMULCASTS, ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardShell(
    onAnimeClick: (String) -> Unit,
    onEpisodeClick: (String, String, String, Int, String) -> Unit,
    onSearchClick: () -> Unit,
    onSignOut: () -> Unit,
    onWatchlistClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }

    Scaffold(
        modifier = modifier,
        topBar = {
            when (selectedTab) {
                DashboardTab.HOME -> {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_aniplex_logo),
                                    contentDescription = "ANIPLEX Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ANIPLEX",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        ),
                        modifier = Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                    )
                }
                DashboardTab.BROWSE -> {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Browse",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                }
                DashboardTab.SIMULCASTS -> {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Simulcasts",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                }
                DashboardTab.ACCOUNT -> {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Account",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                    )
                }
                else -> {
                    // Downloads and My Lists screens handle their own TopAppBars natively
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = Color.LightGray
            ) {
                val activeColor = CrunchyrollOrange
                
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.HOME,
                    onClick = { selectedTab = DashboardTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.MY_LISTS,
                    onClick = { selectedTab = DashboardTab.MY_LISTS },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "My Lists") },
                    label = { Text("My Lists", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.BROWSE,
                    onClick = { selectedTab = DashboardTab.BROWSE },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Browse") },
                    label = { Text("Browse", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SIMULCASTS,
                    onClick = { selectedTab = DashboardTab.SIMULCASTS },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Simulcasts") },
                    label = { Text("Simulcasts", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.ACCOUNT,
                    onClick = { selectedTab = DashboardTab.ACCOUNT },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        val topPadding = if (selectedTab == DashboardTab.HOME) 0.dp else innerPadding.calculateTopPadding()
        val screenModifier = Modifier.padding(
            top = topPadding,
            bottom = innerPadding.calculateBottomPadding()
        )
        
        when (selectedTab) {
            DashboardTab.HOME -> HomeScreen(onAnimeClick = onAnimeClick, modifier = screenModifier)
            DashboardTab.MY_LISTS -> WatchlistScreen(
                onAnimeClick = onAnimeClick,
                onEpisodeClick = onEpisodeClick,
                onBackClick = null,
                modifier = screenModifier
            )
            DashboardTab.DOWNLOADS -> DownloadsScreen(onPlayClick = onEpisodeClick, modifier = screenModifier)
            DashboardTab.BROWSE -> BrowseScreen(onAnimeClick = onAnimeClick, modifier = screenModifier)
            DashboardTab.SIMULCASTS -> ScheduleScreen(onAnimeClick = onAnimeClick, modifier = screenModifier)
            DashboardTab.ACCOUNT -> ProfileScreen(
                onSignOut = onSignOut,
                onWatchlistClick = onWatchlistClick,
                onHistoryClick = onHistoryClick,
                onSwitchProfile = onSwitchProfile,
                modifier = screenModifier
            )
        }
    }
}
