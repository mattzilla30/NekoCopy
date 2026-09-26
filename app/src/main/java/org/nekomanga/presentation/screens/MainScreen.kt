package org.nekomanga.presentation.screens

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.ui.source.latest.DisplayViewModel
import eu.kanade.tachiyomi.ui.source.latest.toDomain
import eu.kanade.tachiyomi.ui.source.latest.toSerializable
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.AppBar
import org.nekomanga.presentation.screens.about.AboutViewModel
import org.nekomanga.presentation.screens.browse.BrowseViewModel
import org.nekomanga.presentation.screens.deepLink.DeepLinkScreen
import org.nekomanga.presentation.screens.deepLink.DeepLinkViewModel
import org.nekomanga.presentation.screens.feed.FeedViewModel
import org.nekomanga.presentation.screens.similar.SimilarViewModel

@Composable
fun MainScreen(
    backStack: NavBackStack<NavKey>,
    windowSizeClass: WindowSizeClass,
    incognitoMode: Boolean,
    incognitoClick: () -> Unit,
    onboardingCompleted: () -> Unit,
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
) {

    var mainDropdownShowing by remember { mutableStateOf(false) }

    val mainDropDown =
        AppBar.MainDropdown(
            incognitoMode = incognitoMode,
            incognitoModeClick = incognitoClick,
            settingsClick = { backStack.add(Screens.Settings.Main()) },
            aboutClick = { backStack.add(Screens.About) },
            menuShowing = { mainDropdownShowing = it },
        )

    // Screen transitions follow the M3 Expressive motion scheme: spatial springs move the
    // screen and effects springs fade it.
    val animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val slideInTransition =
        slideInHorizontally(animationSpec = animationSpec, initialOffsetX = { it / 4 }) +
            fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)

    val slideOutTransition =
        fadeIn(animationSpec = fadeSpec) togetherWith
            slideOutHorizontally(animationSpec = animationSpec, targetOffsetX = { it / 4 }) +
                fadeOut(animationSpec = fadeSpec)

    // The new fade-only animation for top-level screens
    val fadeTransition =
        fadeIn(animationSpec = fadeSpec) togetherWith fadeOut(animationSpec = fadeSpec)

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val goBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onBackPressedDispatcher?.onBackPressed()
        }
        Unit
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            transitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideInTransition
                }
            },
            popTransitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideOutTransition
                }
            },
            predictivePopTransitionSpec = {
                val initialIsTop = isTopLevel(initialState.key)
                val targetIsTop = isTopLevel(targetState.key)

                if (initialIsTop && targetIsTop) {
                    fadeTransition
                } else {
                    slideOutTransition
                }
            },
            entryProvider =
                entryProvider {
                    entry<Screens.Loading> { LoadingScreen(it.showLoadingIndicator) }
                    entry<Screens.DeepLink> {
                        val deepLinkViewModel: DeepLinkViewModel = viewModel()
                        TimberKt.d { "CESCO DeepLinkScreen ${it.host}" }
                        DeepLinkScreen(
                            onNavigate = { screens ->
                                backStack.clear()
                                backStack.add(screens.last())
                                backStack.addAll(0, screens.dropLast(1))
                            },
                            host = it.host,
                            path = it.path,
                            id = it.id,
                            deepLinkViewModel = deepLinkViewModel,
                        )
                    }
                    entry<Screens.Onboarding> {
                        OnboardingScreen(
                            finishedOnBoarding = {
                                backStack.clear()
                                backStack.add(Screens.Browse())
                                onboardingCompleted()
                            }
                        )
                    }

                    entry<Screens.History> {
                        FeedScreen(
                            feedViewModel = viewModel { FeedViewModel() },
                            mainDropdown = mainDropDown,
                            mainDropdownShowing = mainDropdownShowing,
                            openManga = { mangaId -> backStack.add(Screens.Manga(mangaId)) },
                            windowSizeClass = windowSizeClass,
                            navigationRail = navigationRail,
                            bottomBar = bottomBar,
                        )
                    }
                    entry<Screens.Browse> { screen ->
                        val browseViewModel: BrowseViewModel = viewModel()
                        if (screen.title != null) {
                            browseViewModel.initSearch(screen.title)
                        }
                        BrowseScreen(
                            browseViewModel = browseViewModel,
                            mainDropdown = mainDropDown,
                            mainDropdownShowing = mainDropdownShowing,
                            onNavigateTo = { screen -> backStack.add(screen) },
                            windowSizeClass = windowSizeClass,
                            navigationRail = navigationRail,
                            bottomBar = bottomBar,
                        )
                    }

                    entry<Screens.Manga> { screen ->
                        val mangaViewModel: MangaViewModel = viewModel {
                            MangaViewModel(screen.mangaId)
                        }

                        MangaScreen(
                            mangaViewModel = mangaViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigate = { screen -> backStack.add(screen) },
                            onSearchMangaDex = { displayType ->
                                backStack.add(Screens.Display(displayType.toSerializable()))
                            },
                        )
                    }

                    entry<Screens.WebView> { screen ->
                        WebViewScreen(
                            title = screen.title,
                            url = screen.url,
                            onBackPressed = goBack,
                        )
                    }

                    entry<Screens.Display> { screen ->
                        val displayViewModel: DisplayViewModel = viewModel {
                            DisplayViewModel(screen.displayScreenType.toDomain())
                        }

                        DisplayScreen(
                            viewModel = displayViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigateTo = { screen -> backStack.add(screen) },
                        )
                    }

                    entry<Screens.Similar> { screen ->
                        val similarViewModel: SimilarViewModel = viewModel {
                            SimilarViewModel(screen.mangaUUID)
                        }

                        SimilarScreen(
                            viewModel = similarViewModel,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigateTo = { screen -> backStack.add(screen) },
                        )
                    }

                    entry<Screens.Settings.Main> { screen ->
                        SettingsScreen(
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            deepLink = screen.deepLink,
                        )
                    }

                    entry<Screens.About> {
                        val aboutView: AboutViewModel = viewModel()
                        AboutScreen(
                            aboutViewModel = aboutView,
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                            onNavigateTo = { backStack.add(Screens.License) },
                        )
                    }

                    entry<Screens.License> {
                        LicenseScreen(
                            windowSizeClass = windowSizeClass,
                            onBackPressed = goBack,
                        )
                    }
                },
        )
    }
}

private fun isTopLevel(key: Any?): Boolean {
    if (key == null) return false
    val keyString = key.toString()
    return keyString.contains("Library") ||
        keyString.contains("Updates") ||
        keyString.contains("History") ||
        keyString.contains("Browse")
}
