package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.Serializable

@Serializable data class ErrorResponse(val result: String, val errors: List<ErrorResult>)

@Serializable data class ErrorResult(val status: Int, val title: String?, val detail: String?)
