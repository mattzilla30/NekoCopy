package eu.kanade.tachiyomi.util.system

import android.view.View
import androidx.core.view.WindowInsetsCompat

val View.rootWindowInsetsCompat
    get() = rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }
