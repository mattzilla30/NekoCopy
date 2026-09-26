package eu.kanade.tachiyomi.ui.setting

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.CoilDiskCache
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.storage.StoragePreferences
import org.nekomanga.presentation.components.UiText
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.injectLazy

class DataStorageSettingsViewModel : ViewModel() {

    val storagePreferences: StoragePreferences by injectLazy()

    val applicationContext: Application by injectLazy()

    val chapterCache: ChapterCache by injectLazy()

    val coverCache: CoverCache by injectLazy()

    val network: NetworkHelper by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText.StringResource>()
    val toastEvent = _toastEvent.asSharedFlow()

    private val _cacheData = MutableStateFlow(CacheData())

    val cacheData = _cacheData.asStateFlow()

    init {
        viewModelScope.launchIO { refreshSizes() }
    }

    /** Measures every cache folder. Runs when the screen opens and after each clear. */
    private fun refreshSizes() {
        val context = applicationContext
        fun size(directory: File) = DiskUtil.readableDiskSize(context, directory)
        val tempFiles =
            context.cacheDir
                .listFiles()
                .orEmpty()
                .filter { it.isFile && it.name.endsWith(Constants.TMP_FILE_SUFFIX) }
                .sumOf { it.length() }
        _cacheData.value =
            CacheData(
                parentCacheSize = size(context.cacheDir),
                chapterDiskCacheSize = size(chapterCache.cacheDir),
                customCoverCacheSize = size(coverCache.customCoverCacheDir),
                onlineCoverCacheSize = size(coverCache.onlineCoverDirectory),
                imageCacheSize = size(File(context.cacheDir, CoilDiskCache.FOLDER_NAME)),
                networkCacheSize = size(network.cacheDir),
                tempFileCacheSize = DiskUtil.readableDiskSize(context, tempFiles),
            )
    }

    fun clearParentCache(cacheType: CacheType) {
        viewModelScope.launchNonCancellable {
            launchIO {
                when (cacheType) {
                    CacheType.Parent ->
                        DiskUtil.cleanupDiskSpace(applicationContext.cacheDir, applicationContext)
                    CacheType.ChapterDisk -> chapterCache.deleteCache()
                    CacheType.CustomCover -> coverCache.deleteAllCustomCachedCovers()
                    CacheType.OnlineCover -> coverCache.deleteAllCachedCovers()
                    CacheType.Image ->
                        DiskUtil.cleanupDiskSpace(
                            File(applicationContext.cacheDir, CoilDiskCache.FOLDER_NAME),
                            applicationContext,
                        )
                    CacheType.Network ->
                        DiskUtil.cleanupDiskSpace(network.cacheDir, applicationContext)
                    CacheType.Temp ->
                        DiskUtil.cleanupDiskSpace(
                            applicationContext.cacheDir,
                            applicationContext,
                            true,
                        )
                }
                refreshSizes()
            }
            _toastEvent.emit(UiText.StringResource(R.string.cache_cleared))
        }
    }
}

enum class CacheType {
    Parent,
    ChapterDisk,
    CustomCover,
    OnlineCover,
    Image,
    Network,
    Temp,
}

@Immutable
data class CacheData(
    val parentCacheSize: String = "",
    val chapterDiskCacheSize: String = "",
    val customCoverCacheSize: String = "",
    val onlineCoverCacheSize: String = "",
    val imageCacheSize: String = "",
    val networkCacheSize: String = "",
    val tempFileCacheSize: String = "",
)
