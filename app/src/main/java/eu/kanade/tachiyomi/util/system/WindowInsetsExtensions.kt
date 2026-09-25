package eu.kanade.tachiyomi.util.system

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars

val View.rootWindowInsetsCompat
    get() = rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }

val WindowInsetsCompat.ignoredSystemInsets: Insets
    get() = getInsetsIgnoringVisibility(systemBars())
