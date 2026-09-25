package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import androidx.annotation.StringRes
import java.io.Serializable
import org.nekomanga.R
import org.nekomanga.domain.category.CategoryItem.Companion.SYSTEM_CATEGORY
import org.nekomanga.presentation.screens.library.LibrarySort

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    var mangaOrder: List<Long>

    var mangaSort: Char?

    var isAlone: Boolean

    var isHidden: Boolean

    var isDynamic: Boolean

    var sourceId: Long?

    fun isAscending(): Boolean {
        return ((mangaSort?.minus('a') ?: 0) % 2) != 1
    }

    val isDragAndDrop
        get() =
            (mangaSort == null || mangaSort == LibrarySort.DragAndDrop.categoryValue) && !isDynamic

    @StringRes fun sortRes(): Int = LibrarySort.valueOf(mangaSort).stringRes(isDynamic)

    fun changeSortTo(sort: Int) {
        mangaSort = LibrarySort.valueOf(sort).categoryValue
    }

    companion object {
        fun create(name: String): Category = CategoryImpl().apply { this.name = name }

        fun createSystemCategory(): Category = create(SYSTEM_CATEGORY).apply { id = 0 }

        fun createSystemCategory(context: Context): Category =
            create(context.getString(R.string.default_value)).apply { id = 0 }

        fun createCustom(name: String, libSort: Int, ascending: Boolean): Category =
            create(name).apply {
                val librarySort = LibrarySort.valueOf(libSort)
                changeSortTo(librarySort.mainValue)
                if (mangaSort != LibrarySort.DragAndDrop.categoryValue && !ascending) {
                    mangaSort = mangaSort?.plus(1)
                }
                isDynamic = true
            }

        fun createAll(context: Context, libSort: Int, ascending: Boolean): Category =
            createCustom(context.getString(R.string.all), libSort, ascending).apply {
                id = -1
                order = -1
                isAlone = true
            }
    }
}
