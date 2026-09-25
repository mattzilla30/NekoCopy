package org.nekomanga.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.toShape
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import eu.kanade.tachiyomi.ui.main.ObserveAsEvents
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.LATEST_COMMIT_URL
import eu.kanade.tachiyomi.util.REPO_URL
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.icons.GithubIcon
import org.nekomanga.presentation.components.listcard.ExpressiveListCard
import org.nekomanga.presentation.components.listcard.ListCardType
import org.nekomanga.presentation.components.scaffold.ChildScreenScaffold
import org.nekomanga.presentation.components.snackbar.NekoSnackbarHost
import org.nekomanga.presentation.screens.about.AboutScreenState
import org.nekomanga.presentation.screens.about.AboutTopAppBar
import org.nekomanga.presentation.screens.about.AboutViewModel
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

@Composable
fun AboutScreen(
    aboutViewModel: AboutViewModel,
    windowSizeClass: WindowSizeClass,
    onBackPressed: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
) {

    val screenState by aboutViewModel.aboutScreenState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-implementation of ObserveAsEvents from MainActivity
    ObserveAsEvents(flow = aboutViewModel.appSnackbarManager.events, snackbarHostState) { event ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result =
                snackbarHostState.showSnackbar(
                    message = event.getFormattedMessage(context),
                    actionLabel = event.getFormattedActionLabel(context),
                    duration = event.snackbarDuration,
                    withDismissAction = true,
                )
            // We don't need to handle the result for AboutScreen's snackbars
        }
    }

    AboutWrapper(
        aboutScreenState = screenState,
        windowSizeClass = windowSizeClass,
        onVersionClicked = { aboutViewModel.onVersionClicked() },
        onVersionLongClicked = { context ->
            aboutViewModel.onVersionLongClicked()
            val deviceInfo = CrashLogUtil(context).getDebugInfo()
            val clipboard = context.getSystemService<ClipboardManager>()!!
            val appInfo = context.getString(R.string.app_info)
            clipboard.setPrimaryClip(ClipData.newPlainText(appInfo, deviceInfo))
        },
        onClickLicenses = { onNavigateTo(Screens.License) },
        onBackPressed = onBackPressed,
        snackbarHost = { NekoSnackbarHost(snackbarHostState = snackbarHostState) },
    )
}

@Composable
private fun AboutWrapper(
    aboutScreenState: AboutScreenState,
    windowSizeClass: WindowSizeClass,
    onVersionClicked: () -> Unit,
    onVersionLongClicked: (Context) -> Unit,
    onClickLicenses: () -> Unit,
    onBackPressed: () -> Unit,
    snackbarHost: @Composable () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    ChildScreenScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHost = snackbarHost,
        topBar = {
            AboutTopAppBar(
                incognitoMode = aboutScreenState.incognitoMode,
                scrollBehavior = scrollBehavior,
                onNavigationClicked = onBackPressed,
            )
        },
    ) { contentPadding ->
        val isTablet = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

        if (isTablet) {
            Row(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(0.45f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KittyLogo(size = Size.extraExtraHuge * 3)
                    Spacer(modifier = Modifier.size(Size.large))
                    Text(
                        text = stringResource(R.string.app_name),
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(Size.medium))
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(Size.medium, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val iconModifier = Modifier.size(Size.extraLarge)
                        LinkIcon(
                            modifier = iconModifier,
                            label = "GitHub",
                            icon = GithubIcon,
                            url = REPO_URL,
                        )
                    }
                }

                LazyColumn(
                    modifier =
                        Modifier.weight(0.55f).fillMaxHeight().padding(horizontal = Size.medium),
                    verticalArrangement =
                        Arrangement.spacedBy(Size.tiny, Alignment.CenterVertically),
                ) {
                    item {
                        ExpressiveListCard(listCardType = ListCardType.Top) {
                            TextPreferenceWidget(
                                title = stringResource(R.string.version),
                                subtitle =
                                    when {
                                        BuildConfig.DEBUG -> {
                                            "Debug ${BuildConfig.COMMIT_SHA} (${aboutScreenState.buildTime})"
                                        }

                                        else -> {
                                            "Stable ${BuildConfig.VERSION_NAME} (${aboutScreenState.buildTime})"
                                        }
                                    },
                                onPreferenceClick = { onVersionClicked() },
                                onPreferenceLongClick = { onVersionLongClicked(context) },
                            )
                        }
                    }
                    item {
                        ExpressiveListCard(listCardType = ListCardType.Center) {
                            TextPreferenceWidget(
                                title = stringResource(R.string.whats_new),
                                onPreferenceClick = { uriHandler.openUri(LATEST_COMMIT_URL) },
                            )
                        }
                    }

                    item {
                        ExpressiveListCard(listCardType = ListCardType.Bottom) {
                            TextPreferenceWidget(
                                title = stringResource(R.string.open_source_licenses),
                                onPreferenceClick = onClickLicenses,
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.padding(horizontal = Size.medium),
                verticalArrangement = Arrangement.spacedBy(Size.tiny),
            ) {
                item { LogoHeader() }
                item { Spacer(modifier = Modifier.size(Size.large)) }
                item {
                    ExpressiveListCard(listCardType = ListCardType.Top) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.version),
                            subtitle =
                                when {
                                    BuildConfig.DEBUG -> {
                                        "Debug ${BuildConfig.COMMIT_SHA} (${aboutScreenState.buildTime})"
                                    }

                                    else -> {
                                        "Stable ${BuildConfig.VERSION_NAME} (${aboutScreenState.buildTime})"
                                    }
                                },
                            onPreferenceClick = { onVersionClicked() },
                            onPreferenceLongClick = { onVersionLongClicked(context) },
                        )
                    }
                }
                item {
                    ExpressiveListCard(listCardType = ListCardType.Center) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.whats_new),
                            onPreferenceClick = { uriHandler.openUri(LATEST_COMMIT_URL) },
                        )
                    }
                }

                item {
                    ExpressiveListCard(listCardType = ListCardType.Bottom) {
                        TextPreferenceWidget(
                            title = stringResource(R.string.open_source_licenses),
                            onPreferenceClick = onClickLicenses,
                        )
                    }
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(Size.medium),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        val modifier = Modifier.size(Size.extraLarge)
                        LinkIcon(
                            modifier = modifier,
                            label = "GitHub",
                            icon = GithubIcon,
                            url = REPO_URL,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoHeader() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = Size.huge),
        contentAlignment = Alignment.Center,
    ) {
        KittyLogo(size = Size.extraExtraHuge * 2)
    }
}

/** The kitty face on a 12-sided cookie, Kitty's app mark. */
@Composable
private fun KittyLogo(size: Dp) {
    Box(
        modifier =
            Modifier.size(size)
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_kitty),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.62f),
            contentDescription = null,
        )
    }
}

@Composable
private fun LinkIcon(
    modifier: Modifier = Modifier,
    label: String,
    painter: Painter? = null,
    icon: ImageVector? = null,
    url: String,
) {
    val uriHandler = LocalUriHandler.current

    ToolTipButton(
        label,
        iconModifier = Modifier.size(24.dp),
        icon = icon,
        painter = painter,
        enabledTint = MaterialTheme.colorScheme.primary.copy(alpha = .7f),
        onClick = { uriHandler.openUri(url) },
    )
}
