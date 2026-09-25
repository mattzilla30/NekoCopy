package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response after login */
@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable data class ErrorResponse(val result: String, val errors: List<ErrorResult>)

@Serializable data class ErrorResult(val status: Int, val title: String?, val detail: String?)
