package eu.kanade.tachiyomi.source.online.utils

import io.kotest.matchers.shouldBe
import org.junit.Test

class MdUtilTitleTest {

    @Test
    fun `getTitle prefers the main English title`() {
        MdUtil.getTitle(
            titleMap = mapOf("en" to "Attack on Titan"),
            originalLanguage = "ja",
            altTitles = listOf(mapOf("en" to "AoT")),
        ) shouldBe "Attack on Titan"
    }

    @Test
    fun `getTitle falls back to an English alt title before romaji`() {
        MdUtil.getTitle(
            titleMap = mapOf("ja-ro" to "Shingeki no Kyojin"),
            originalLanguage = "ja",
            altTitles = listOf(mapOf("ja" to "進撃の巨人"), mapOf("en" to "Attack on Titan")),
        ) shouldBe "Attack on Titan"
    }

    @Test
    fun `getTitle skips blank English titles`() {
        MdUtil.getTitle(
            titleMap = mapOf("en" to " ", "ja-ro" to "Shingeki no Kyojin"),
            originalLanguage = "ja",
            altTitles = listOf(mapOf("en" to "")),
        ) shouldBe "Shingeki no Kyojin"
    }
}
