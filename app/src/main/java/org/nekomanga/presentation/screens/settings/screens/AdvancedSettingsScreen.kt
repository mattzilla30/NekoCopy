package org.nekomanga.presentation.screens.settings.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.getActivity
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dialog.ClearDatabaseDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm
import tachiyomi.core.network.PREF_DOH_360
import tachiyomi.core.network.PREF_DOH_ADGUARD
import tachiyomi.core.network.PREF_DOH_ALIDNS
import tachiyomi.core.network.PREF_DOH_CLOUDFLARE
import tachiyomi.core.network.PREF_DOH_CONTROLD
import tachiyomi.core.network.PREF_DOH_DNSPOD
import tachiyomi.core.network.PREF_DOH_GOOGLE
import tachiyomi.core.network.PREF_DOH_MULLVAD
import tachiyomi.core.network.PREF_DOH_NJALLA
import tachiyomi.core.network.PREF_DOH_QUAD101
import tachiyomi.core.network.PREF_DOH_QUAD9
import tachiyomi.core.network.PREF_DOH_SHECAN
import tachiyomi.core.util.system.setDefaultSettings

internal class AdvancedSettingsScreen(
    incognitoMode: Boolean,
    val preferences: PreferencesHelper,
    val networkPreferences: NetworkPreferences,
    val readerPreferences: ReaderPreferences,
    val toastEvent: SharedFlow<UiText>,
    val clearNetworkCookies: () -> Unit,
    val clearDatabase: (Boolean) -> Unit,
    onNavigationIconClick: (() -> Unit)?,
) : SearchableSettings(onNavigationIconClick, incognitoMode) {

    override fun getTitleRes(): Int = R.string.advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current

        LaunchedEffect(Unit) { toastEvent.collect { event -> context.toast(event) } }

        return listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(R.string.dump_crash_logs),
                subtitle = stringResource(R.string.saves_error_logs),
                onClick = {
                    context.getActivity()?.lifecycleScope?.launchIO {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            Preference.PreferenceItem.SwitchPreference(
                pref = networkPreferences.verboseLogging(),
                title = stringResource(R.string.verbose_logging),
                subtitle = stringResource(R.string.verbose_logging_summary),
                onValueChanged = {
                    context.toast(R.string.requires_app_restart)
                    true
                },
            ),
            systemGroup(context),
            networkGroup(context, clearNetworkCookies),
            dataGroup(clearDatabase),
            getReaderGroup(readerPreferences),
        )
    }

    @Composable
    fun systemGroup(context: Context): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.system),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.supported_links),
                        subtitle = stringResource(R.string.supported_links_summary),
                        onClick = {
                            val intent =
                                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            context.startActivity(intent)
                        },
                    )
                ),
        )
    }

    @Composable
    fun networkGroup(
        context: Context,
        clearNetworkCookies: () -> Unit,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(R.string.network),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.clear_cookies),
                        onClick = { clearNetworkCookies() },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.pref_clear_webview_data),
                        onClick = {
                            try {
                                val webview = WebView(context)
                                webview.setDefaultSettings()
                                webview.clearCache(true)
                                webview.clearFormData()
                                webview.clearHistory()
                                webview.clearSslPreferences()
                                WebStorage.getInstance().deleteAllData()
                                context.applicationInfo.dataDir?.let {
                                    File("$it/app_webview/").deleteRecursively()
                                }
                                context.toast(R.string.webview_data_deleted)
                            } catch (e: Throwable) {
                                TimberKt.e(e) { "Error clearing webview data" }
                                context.toast(R.string.cache_delete_error)
                            }
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        pref = networkPreferences.dohProvider(),
                        title = stringResource(R.string.doh),
                        entries =
                            mapOf(
                                    -1 to stringResource(R.string.disabled),
                                    PREF_DOH_CLOUDFLARE to stringResource(R.string.cloudflare),
                                    PREF_DOH_GOOGLE to stringResource(R.string.google),
                                    PREF_DOH_ADGUARD to stringResource(R.string.ad_guard),
                                    PREF_DOH_QUAD9 to stringResource(R.string.quad9),
                                    PREF_DOH_ALIDNS to stringResource(R.string.aliDNS),
                                    PREF_DOH_DNSPOD to stringResource(R.string.dnsPod),
                                    PREF_DOH_360 to stringResource(R.string.dns_360),
                                    PREF_DOH_QUAD101 to stringResource(R.string.quad_101),
                                    PREF_DOH_MULLVAD to stringResource(R.string.mullvad),
                                    PREF_DOH_CONTROLD to stringResource(R.string.control_d),
                                    PREF_DOH_NJALLA to stringResource(R.string.njalla),
                                    PREF_DOH_SHECAN to stringResource(R.string.shecan),
                                )
                                .toMap(),
                        onValueChanged = {
                            context.toast(R.string.requires_app_restart)
                            true
                        },
                    ),
                ),
        )
    }

    @Composable
    fun getReaderGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val chooseColorProfile =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
                uri ->
                uri?.let {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    readerPreferences.displayProfile().set(uri.toString())
                }
            }
        return Preference.PreferenceGroup(
            title = stringResource(R.string.reader),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.display_profile),
                        subtitle = readerPreferences.displayProfile().get(),
                        onClick = { chooseColorProfile.launch(arrayOf("*/*")) },
                    )
                ),
        )
    }

    @Composable
    fun dataGroup(clearDatabase: (Boolean) -> Unit): Preference.PreferenceGroup {
        var showClearDatabaseDialog by rememberSaveable { mutableStateOf(false) }

        if (showClearDatabaseDialog) {
            ClearDatabaseDialog(
                onDismiss = { showClearDatabaseDialog = false },
                onConfirm = clearDatabase,
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(R.string.data_management),
            preferenceItems =
                listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(R.string.clear_database),
                        subtitle = stringResource(R.string.clear_database_summary),
                        onClick = { showClearDatabaseDialog = true },
                    )
                ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): List<SearchTerm> {
            return listOf(
                SearchTerm(
                    title = stringResource(R.string.dump_crash_logs),
                    subtitle = stringResource(R.string.saves_error_logs),
                ),
                SearchTerm(
                    title = stringResource(R.string.verbose_logging),
                    subtitle = stringResource(R.string.verbose_logging_summary),
                ),
                SearchTerm(
                    title = stringResource(R.string.supported_links),
                    subtitle = stringResource(R.string.supported_links_summary),
                    group = stringResource(R.string.system),
                ),
                SearchTerm(
                    title = stringResource(R.string.clear_cookies),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.pref_clear_webview_data),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.doh),
                    group = stringResource(R.string.network),
                ),
                SearchTerm(
                    title = stringResource(R.string.clear_database),
                    subtitle = stringResource(R.string.clear_database_summary),
                    group = stringResource(R.string.data_management),
                ),
                SearchTerm(
                    title = stringResource(R.string.display_profile),
                    group = stringResource(R.string.reader),
                ),
            )
        }
    }
}
