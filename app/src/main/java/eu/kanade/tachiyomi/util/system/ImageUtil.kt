package eu.kanade.tachiyomi.util.system

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.webkit.MimeTypeMap
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import okio.Buffer
import okio.BufferedSource
import org.nekomanga.logging.TimberKt

object ImageUtil {

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    fun findImageType(stream: InputStream): ImageType? {
        return try {
            getImageType(stream)?.type
        } catch (e: Exception) {
            TimberKt.e(e) { "Error getting image type from stream" }
            null
        }
    }

    fun findImageType(stream: BufferedSource): ImageType? {
        return try {
            getImageType(stream)?.type
        } catch (e: Exception) {
            TimberKt.e(e) { "Error getting image type from stream" }
            null
        }
    }

    fun getExtensionFromMimeType(mime: String?): String {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: SUPPLEMENTARY_MIMETYPE_MAPPING[mime]
            ?: "jpg"
    }

    fun isAnimatedAndSupported(stream: InputStream): Boolean {
        try {
            val type = getImageType(stream) ?: return false
            return type.isAnimatedAndSupported()
        } catch (e: Exception) {
            TimberKt.e(e) { "Error is animated image type" }
        }
        return false
    }

    fun isAnimatedAndSupported(stream: BufferedSource): Boolean {
        try {
            val type = getImageType(stream) ?: return false
            return type.isAnimatedAndSupported()
        } catch (e: Exception) {
            TimberKt.e(e) { "Error is animated image type" }
        }
        return false
    }

    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    /** An image format read from the file header, and whether the image animates. */
    internal data class DetectedImage(val type: ImageType, val isAnimated: Boolean) {
        /** Coil animates GIF, WebP and HEIF images. */
        fun isAnimatedAndSupported(): Boolean =
            when (type) {
                ImageType.GIF -> true
                ImageType.WEBP,
                ImageType.HEIF -> isAnimated
                else -> false
            }
    }

    private const val HEADER_SIZE = 32

    private fun getImageType(stream: InputStream): DetectedImage? {
        val bytes = ByteArray(HEADER_SIZE)

        val length =
            if (stream.markSupported()) {
                stream.mark(bytes.size)
                stream.read(bytes, 0, bytes.size).also { stream.reset() }
            } else {
                stream.read(bytes, 0, bytes.size)
            }

        if (length <= 0) {
            return null
        }

        return detectImage(bytes.copyOf(length))
    }

    private fun getImageType(stream: BufferedSource): DetectedImage? {
        stream.request(HEADER_SIZE.toLong())
        val bytes = stream.peek().readByteArray(minOf(HEADER_SIZE.toLong(), stream.buffer.size))

        if (bytes.isEmpty()) {
            return null
        }

        return detectImage(bytes)
    }

    private val heifBrands = setOf("heic", "heix", "heim", "heis", "hevc", "hevx", "mif1", "msf1")
    private val heifSequenceBrands = setOf("hevc", "hevx", "msf1")

    /** Reads the image format from the magic bytes at the start of a file. */
    internal fun detectImage(header: ByteArray): DetectedImage? {
        fun at(offset: Int, vararg expected: Int): Boolean =
            header.size >= offset + expected.size &&
                expected.indices.all { header[offset + it].toInt() and 0xFF == expected[it] }

        fun ascii(offset: Int, text: String): Boolean =
            at(offset, *text.map { it.code }.toIntArray())

        fun brand(offset: Int): String? =
            if (header.size >= offset + 4) String(header, offset, 4, Charsets.US_ASCII) else null

        return when {
            at(0, 0xFF, 0xD8, 0xFF) -> DetectedImage(ImageType.JPEG, false)
            at(0, 0x89, 0x50, 0x4E, 0x47) -> DetectedImage(ImageType.PNG, false)
            ascii(0, "GIF8") -> DetectedImage(ImageType.GIF, true)
            ascii(0, "RIFF") && ascii(8, "WEBP") -> {
                // An extended VP8X header sets bit 1 of its flags byte for animations.
                val animated =
                    ascii(12, "VP8X") && header.size > 20 && header[20].toInt() and 0x02 != 0
                DetectedImage(ImageType.WEBP, animated)
            }
            at(0, 0xFF, 0x0A) ||
                at(0, 0x00, 0x00, 0x00, 0x0C, 0x4A, 0x58, 0x4C, 0x20, 0x0D, 0x0A, 0x87, 0x0A) ->
                DetectedImage(ImageType.JXL, false)
            ascii(4, "ftyp") -> {
                // The major brand sits at offset 8 and compatible brands follow from offset 16.
                val brands =
                    listOfNotNull(brand(8)) + (16 until header.size - 3 step 4).mapNotNull(::brand)
                when {
                    "avis" in brands -> DetectedImage(ImageType.AVIF, true)
                    "avif" in brands -> DetectedImage(ImageType.AVIF, false)
                    brands.any { it in heifBrands } ->
                        DetectedImage(ImageType.HEIF, brands.any { it in heifSequenceBrands })
                    else -> null
                }
            }
            else -> null
        }
    }

    fun mergeBitmaps(
        imageBitmap: Bitmap,
        imageBitmap2: Bitmap,
        isLTR: Boolean,
        @ColorInt background: Int = Color.WHITE,
        gap: Int = 0,
        progressCallback: ((Int) -> Unit)? = null,
    ): BufferedSource {
        val height = imageBitmap.height
        val width = imageBitmap.width
        val height2 = imageBitmap2.height
        val width2 = imageBitmap2.width
        val maxHeight = max(height, height2)
        val gapInPx = gap.dpToPx

        val result =
            Bitmap.createBitmap(
                width + width2 + gapInPx,
                max(height, height2),
                Bitmap.Config.ARGB_8888,
            )
        val canvas = Canvas(result)
        canvas.drawColor(background)

        val upperPart =
            Rect(
                if (isLTR) 0 else (width2 + gapInPx),
                (maxHeight - imageBitmap.height) / 2,
                (if (isLTR) 0 else (width2 + gapInPx)) + imageBitmap.width,
                imageBitmap.height + (maxHeight - imageBitmap.height) / 2,
            )
        canvas.drawBitmap(imageBitmap, imageBitmap.rect, upperPart, null)

        if (gapInPx != 0) {
            canvas.drawRect(
                (if (isLTR) width2 else width).toFloat(),
                0f,
                ((if (isLTR) width2 else width) + gapInPx).toFloat(),
                maxHeight.toFloat(),
                Paint().apply {
                    color = background
                    style = Paint.Style.FILL
                },
            )
        }

        progressCallback?.invoke(98)
        val bottomPart =
            Rect(
                (if (!isLTR) 0 else (width + gapInPx)),
                (maxHeight - imageBitmap2.height) / 2,
                (if (!isLTR) 0 else (width + gapInPx)) + imageBitmap2.width,
                imageBitmap2.height + (maxHeight - imageBitmap2.height) / 2,
            )
        canvas.drawBitmap(imageBitmap2, imageBitmap2.rect, bottomPart, null)
        progressCallback?.invoke(99)

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 100, output)
        progressCallback?.invoke(100)
        return Buffer().write(output.toByteArray())
    }

    private val Bitmap.rect: Rect
        get() = Rect(0, 0, width, height)

    private val Int.isDark: Boolean
        get() {
            val bgArray = FloatArray(3)
            ColorUtils.colorToHSL(this, bgArray)
            return red < 40 && blue < 40 && green < 40 && alpha > 200 && bgArray[1] <= 0.2f
        }

    /** Used to check an image's dimensions without loading it in the memory. */
    private fun extractImageOptions(
        imageStream: InputStream,
        resetAfterExtraction: Boolean = true,
    ): BitmapFactory.Options {
        imageStream.mark(imageStream.available() + 1)

        val imageBytes = imageStream.readBytes()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        if (resetAfterExtraction) imageStream.reset()
        return options
    }

    fun extractImageOptions(imageSource: BufferedSource): BitmapFactory.Options {
        val imageBytes = imageSource.peek().readByteArray()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        return options
    }

    // Android doesn't include some mappings
    private val SUPPLEMENTARY_MIMETYPE_MAPPING =
        mapOf(
            // https://issuetracker.google.com/issues/182703810
            "image/jxl" to "jxl"
        )
}
