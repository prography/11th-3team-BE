package org.prography.samsung.backend.common.web

import org.prography.samsung.backend.common.response.ApiResponse
import org.prography.samsung.backend.common.response.SuccessCode
import org.springframework.http.ResponseEntity

object ApiResponseFactory {
    fun <T> success(successCode: SuccessCode, data: T? = null): ResponseEntity<ApiResponse<T>> =
        ResponseEntity.status(successCode.httpStatus).body(ApiResponse.onSuccess(successCode, data))
}
