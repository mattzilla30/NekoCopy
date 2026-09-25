package org.nekomanga.presentation.screens.about

import androidx.compose.runtime.Immutable

@Immutable
data class AboutScreenState(
    val incognitoMode: Boolean = false,
    val buildTime: String,
)
