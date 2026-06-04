package org.prography.samsung.backend.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class ApiResponse<T>(
    @field:JsonProperty("code") val code: Int,
    @field:JsonProperty("message") val message: String,
    @field:JsonProperty("data") @field:JsonInclude(JsonInclude.Include.NON_NULL) val data: T? = null,
) {
    companion object {
        fun <T> onSuccess(successCode: SuccessCode, data: T): ApiResponse<T> =
            ApiResponse(successCode.code, successCode.message, data)

        fun <T> onSuccess(successCode: SuccessCode): ApiResponse<T> = ApiResponse(successCode.code, successCode.message)

        fun <T> onFailure(errorCode: ApiCode): ApiResponse<T> = ApiResponse(errorCode.code, errorCode.message)

        fun <T> onFailure(errorCode: ApiCode, message: String?): ApiResponse<T> =
            ApiResponse(errorCode.code, message?.takeIf { it.isNotBlank() } ?: errorCode.message)
    }
}
