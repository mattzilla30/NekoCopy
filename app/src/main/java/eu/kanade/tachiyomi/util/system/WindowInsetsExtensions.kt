package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.mandatorySystemGestures
import androidx.core.view.WindowInsetsCompat.Type.systemBars

/** returns if device using gesture nav and supports true edge to edge */
fun WindowInsetsCompat.isBottomTappable() =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        getInsetsIgnoringVisibility(systemBars()).bottom !=
            getInsetsIgnoringVisibility(mandatorySystemGestures()).bottom

val View.rootWindowInsetsCompat
    get() = rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }

val WindowInsetsCompat.ignoredSystemInsets: Insets
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getInsetsIgnoringVisibility(systemBars())
        } else {
            getInsets(systemBars())
        }
