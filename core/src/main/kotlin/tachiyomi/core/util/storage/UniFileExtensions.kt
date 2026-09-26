package tachiyomi.core.util.storage

import com.hippo.unifile.UniFile

val UniFile.extension: String?
    get() = name?.substringAfterLast('.')

val UniFile.displayablePath: String
    get() = filePath ?: uri.toString()
