package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.text.format.Formatter
import coil3.imageLoader
import coil3.memory.MemoryCache
import eu.kanade.tachiyomi.data.coil.CoilDiskCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.system.toast
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.R
import tachiyomi.core.util.storage.DiskUtil

/**
 * Stores manga covers and the custom covers users set. Names of files are created with the md5 of
 * the thumbnail URL.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class CoverCache(val context: Context) {

    companion object {
        private const val COVERS_DIR = "covers"
        private const val CUSTOM_COVERS_DIR = "covers/custom"
        private const val ONLINE_COVERS_DIR = "online_covers"
    }

    /** Cache directory used for custom cover cache management. */
    val customCoverCacheDir = getCacheDir(CUSTOM_COVERS_DIR)

    /** Cache directory for downloaded covers. */
    val onlineCoverDirectory = File(context.cacheDir, ONLINE_COVERS_DIR).also { it.mkdirs() }

    private var lastClean = 0L

    /**
     * Deletes the covers older versions kept for library manga. They sat beside the custom covers,
     * which stay.
     */
    fun deleteLibraryCovers() {
        getCacheDir(COVERS_DIR).listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }

    /** Clear out online covers */
    suspend fun deleteAllCachedCovers() {
        val directory = onlineCoverDirectory
        var deletedSize = 0L
        val files = directory.listFiles()?.sortedBy { it.lastModified() }?.iterator() ?: return
        while (files.hasNext()) {
            val file = files.next()
            deletedSize += file.length()
            file.delete()
        }
        withContext(Dispatchers.Main) {
            context.toast(
                context.getString(R.string.deleted_, Formatter.formatFileSize(context, deletedSize))
            )
        }
        context.imageLoader.memoryCache?.clear()
        CoilDiskCache.get(context).clear()

        lastClean = System.currentTimeMillis()
    }

    /** Clear out custom covers */
    suspend fun deleteAllCustomCachedCovers() {
        val directory = customCoverCacheDir
        var deletedSize = 0L
        val files = directory.listFiles()?.sortedBy { it.lastModified() }?.iterator() ?: return
        while (files.hasNext()) {
            val file = files.next()
            deletedSize += file.length()
            file.delete()
        }
        withContext(Dispatchers.Main) {
            context.toast(
                context.getString(R.string.deleted_, Formatter.formatFileSize(context, deletedSize))
            )
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param manga the manga.
     * @return cover image.
     */
    fun getCustomCoverFile(manga: Manga): File {
        return getCustomCoverFile(manga.id ?: 0)
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param mangaId the manga id.
     * @return cover image.
     */
    fun getCustomCoverFile(mangaId: Long): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(mangaId.toString()))
    }

    /**
     * Saves the given stream as the manga's custom cover to cache.
     *
     * @param manga the manga.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(manga: Manga, inputStream: InputStream) {
        getCustomCoverFile(manga).outputStream().use {
            inputStream.copyTo(it)
            context.imageLoader.memoryCache
                ?.keys
                ?.filter { it.key.startsWith("${manga.id}-") || it.key.startsWith(manga.key()) }
                ?.forEach { context.imageLoader.memoryCache?.remove(it) }
        }
    }

    /**
     * Saves the given url as the manga's custom cover to cache.
     *
     * @param manga the manga.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(manga: Manga, url: String) {
        val coverFile = getCoverFile(url)
        if (coverFile.exists()) {
            coverFile.inputStream().use { inputStream ->
                getCustomCoverFile(manga).outputStream().use {
                    inputStream.copyTo(it)
                    context.imageLoader.memoryCache
                        ?.keys
                        ?.filter {
                            it.key.startsWith("${manga.id}-") || it.key.startsWith(manga.key())
                        }
                        ?.forEach { context.imageLoader.memoryCache?.remove(it) }
                }
            }
        }
    }

    /**
     * Delete custom cover of the manga from the cache
     *
     * @param manga the manga.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(manga: Manga): Boolean {
        val result = getCustomCoverFile(manga).let { it.exists() && it.delete() }
        context.imageLoader.memoryCache
            ?.keys
            ?.filter { it.key.startsWith("${manga.id}-") || it.key.startsWith(manga.key()) }
            ?.forEach { context.imageLoader.memoryCache?.remove(it) }
        return result
    }

    /**
     * Returns the cover from cache.
     *
     * @param url the url.
     * @return cover image.
     */
    fun getCoverFile(url: String?): File =
        File(onlineCoverDirectory, DiskUtil.hashKeyForDisk(url.orEmpty()))

    fun deleteFromCache(name: String?) {
        if (name.isNullOrEmpty()) return
        val file = getCoverFile(name)
        context.imageLoader.memoryCache?.remove(MemoryCache.Key(file.name))
        if (file.exists()) file.delete()
    }

    /**
     * Delete the cover file from the disk cache and optional from memory cache
     *
     * @param manga the manga.
     * @return status of deletion.
     */
    fun deleteFromCache(manga: Manga, deleteCustom: Boolean = true) {
        // Check if url is empty.
        if (manga.thumbnail_url.isNullOrEmpty()) return

        // Remove file
        val file = getCoverFile(manga.thumbnail_url)
        if (deleteCustom) deleteCustomCover(manga)
        if (file.exists()) {
            context.imageLoader.memoryCache
                ?.keys
                ?.filter { it.key.startsWith("${manga.id}-") || it.key.startsWith(manga.key()) }
                ?.forEach { context.imageLoader.memoryCache?.remove(it) }
            file.delete()
        }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir) ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
