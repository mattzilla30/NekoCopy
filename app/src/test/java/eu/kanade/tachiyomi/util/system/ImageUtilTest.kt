package eu.kanade.tachiyomi.util.system

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageType

class ImageUtilTest {

    @Test
    fun testToImageUtilType() {
        with(ImageUtil) {
            fun mockType(fmt: Format): ImageType {
                val m = mockk<ImageType>()
                every { m.format } returns fmt
                return m
            }

            val typeMappings =
                mapOf(
                    Format.Avif to ImageUtil.ImageType.AVIF,
                    Format.Gif to ImageUtil.ImageType.GIF,
                    Format.Heif to ImageUtil.ImageType.HEIF,
                    Format.Jpeg to ImageUtil.ImageType.JPEG,
                    Format.Jxl to ImageUtil.ImageType.JXL,
                    Format.Png to ImageUtil.ImageType.PNG,
                    Format.Webp to ImageUtil.ImageType.WEBP,
                )

            typeMappings.forEach { (format, expectedType) ->
                assertEquals(expectedType, mockType(format).toImageUtilType())
            }
        }
    }

    @Test
    fun testIsAnimatedAndSupported() {
        with(ImageUtil) {
            fun mockType(fmt: Format, animated: Boolean): ImageType {
                val m = mockk<ImageType>()
                every { m.format } returns fmt
                every { m.isAnimated } returns animated
                return m
            }

            listOf(
                    TestCase(Format.Gif, true, true, "Animated GIF"),
                    TestCase(Format.Webp, true, true, "Animated WebP"),
                    TestCase(Format.Webp, false, false, "Non-animated WebP"),
                    TestCase(Format.Heif, true, true, "Animated HEIF"),
                    TestCase(Format.Heif, false, false, "Non-animated HEIF"),
                    // Other formats never animate
                    TestCase(Format.Jpeg, true, false, "Animated JPEG"),
                    TestCase(Format.Png, true, false, "Animated PNG"),
                )
                .forEach { (format, isAnimated, expected, description) ->
                    val mock = mockType(format, isAnimated)
                    val actual = mock.isAnimatedAndSupported()
                    assertEquals("Test case '$description' failed", expected, actual)
                }
        }
    }

    data class TestCase(
        val format: Format,
        val isAnimated: Boolean,
        val expected: Boolean,
        val description: String,
    )
}
