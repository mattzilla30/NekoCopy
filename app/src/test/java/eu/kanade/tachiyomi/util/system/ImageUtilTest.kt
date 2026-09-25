package eu.kanade.tachiyomi.util.system

import eu.kanade.tachiyomi.util.system.ImageUtil.ImageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUtilTest {

    private fun header(vararg parts: Any): ByteArray =
        parts
            .flatMap { part ->
                when (part) {
                    is String -> part.toByteArray(Charsets.US_ASCII).toList()
                    is Int -> listOf(part.toByte())
                    is ByteArray -> part.toList()
                    else -> error("unsupported part $part")
                }
            }
            .toByteArray()

    private fun ftyp(vararg brands: String) =
        header(0, 0, 0, 0x20, "ftyp", brands.first(), 0, 0, 0, 0, *brands.drop(1).toTypedArray())

    @Test
    fun `detectImage reads formats from magic bytes`() {
        val cases =
            listOf(
                header(0xFF, 0xD8, 0xFF, 0xE0) to ImageType.JPEG,
                header(0x89, "PNG", 0x0D, 0x0A, 0x1A, 0x0A) to ImageType.PNG,
                header("GIF89a") to ImageType.GIF,
                header("RIFF", 0, 0, 0, 0, "WEBPVP8 ") to ImageType.WEBP,
                header(0xFF, 0x0A) to ImageType.JXL,
                header(0, 0, 0, 0x0C, "JXL ", 0x0D, 0x0A, 0x87, 0x0A) to ImageType.JXL,
                ftyp("avif", "mif1") to ImageType.AVIF,
                ftyp("heic", "mif1") to ImageType.HEIF,
                ftyp("mif1", "heic") to ImageType.HEIF,
            )
        cases.forEach { (bytes, expected) ->
            assertEquals(expected, ImageUtil.detectImage(bytes)?.type)
        }
    }

    @Test
    fun `detectImage returns null for unknown or empty headers`() {
        assertNull(ImageUtil.detectImage(header("hello world")))
        assertNull(ImageUtil.detectImage(ByteArray(0)))
        assertNull(ImageUtil.detectImage(ftyp("isom", "mp41")))
    }

    @Test
    fun `detectImage flags animations`() {
        val animatedWebp = header("RIFF", 0, 0, 0, 0, "WEBPVP8X", 0, 0, 0, 0, 0x02)
        val stillWebp = header("RIFF", 0, 0, 0, 0, "WEBPVP8X", 0, 0, 0, 0, 0x10)
        assertEquals(true, ImageUtil.detectImage(animatedWebp)?.isAnimated)
        assertEquals(false, ImageUtil.detectImage(stillWebp)?.isAnimated)
        assertEquals(true, ImageUtil.detectImage(ftyp("avis", "avif"))?.isAnimated)
        assertEquals(true, ImageUtil.detectImage(ftyp("msf1", "hevc"))?.isAnimated)
        assertEquals(false, ImageUtil.detectImage(ftyp("heic", "mif1"))?.isAnimated)
    }

    @Test
    fun `isAnimatedAndSupported covers the formats Coil animates`() {
        fun detected(type: ImageType, animated: Boolean) =
            ImageUtil.DetectedImage(type, animated).isAnimatedAndSupported()

        assertEquals(true, detected(ImageType.GIF, false))
        assertEquals(true, detected(ImageType.WEBP, true))
        assertEquals(false, detected(ImageType.WEBP, false))
        assertEquals(true, detected(ImageType.HEIF, true))
        assertEquals(false, detected(ImageType.HEIF, false))
        assertEquals(false, detected(ImageType.JPEG, true))
        assertEquals(false, detected(ImageType.PNG, true))
        assertEquals(false, detected(ImageType.AVIF, true))
    }
}
