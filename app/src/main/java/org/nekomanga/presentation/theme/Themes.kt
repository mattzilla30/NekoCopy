package org.nekomanga.presentation.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import org.nekomanga.R

enum class Themes {
    Monet,
    Neko,
    Purple,
    Green,
    Orange,
    Pink,
    Retro,
    Blue,
    Brown,
    Tako,
    TokyoNight,
    Teal,
    Neon,
    Nord,
    Monochrome;

    fun isDarkTheme() = nightMode() == AppCompatDelegate.MODE_NIGHT_YES

    fun followsSystem() = nightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    // With the way kotlin handles when(enum) and the fact that the main activity uses these methods
    // immediately, we were getting a .clone() error
    // to prevent that we are explictly calling this ==

    @StyleRes
    fun styleRes(): Int {
        return when {
            this == Monet -> R.style.Theme_Kitty_Monet
            this == Retro -> R.style.Theme_Kitty_Retro
            this == Pink -> R.style.Theme_Kitty_Pink
            this == Orange -> R.style.Theme_Kitty_Orange
            this == Teal -> R.style.Theme_Kitty_Teal
            this == Purple -> R.style.Theme_Kitty_Purple
            this == Brown -> R.style.Theme_Kitty_Brown
            this == Tako -> R.style.Theme_Kitty_Tako
            this == TokyoNight -> R.style.Theme_Kitty_TokyoNight
            this == Nord -> R.style.Theme_Kitty_Nord
            this == Green -> R.style.Theme_Kitty_Green
            this == Blue -> R.style.Theme_Kitty_Blue
            this == Monochrome -> R.style.Theme_Kitty_Monochrome
            this == Neon -> R.style.Theme_Kitty_Neon
            else -> R.style.Theme_Kitty // DEFAULT
        }
    }

    fun nightMode(): Int {
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    @StringRes
    fun nameRes(): Int {
        return when {
            this == Monet -> R.string.dynamic
            this == Retro -> R.string.miami_vice
            this == Pink -> R.string.cherry_blossom
            this == Orange -> R.string.citrus_squeeze
            this == Teal -> R.string.teal_ocean
            this == Purple -> R.string.royal
            this == Brown -> R.string.chocolate
            this == Tako -> R.string.tako
            this == TokyoNight -> R.string.tokyo_night
            this == Nord -> R.string.nord
            this == Green -> R.string.jungle
            this == Blue -> R.string.crayon
            this == Monochrome -> R.string.monochrome
            this == Neon -> R.string.neon_nights
            else -> R.string.kitsune_fire // Default
        }
    }
}
