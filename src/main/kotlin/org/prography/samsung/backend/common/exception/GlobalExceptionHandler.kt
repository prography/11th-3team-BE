package org.prography.samsung.backend.common.exception

import org.prography.samsung.backend.common.response.ApiResponse
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException): ResponseEntity<ApiResponse<Nothing>> = ResponseEntity
        .status(ex.errorCode.httpStatus)
        .body(ApiResponse.onFailure(ex.errorCode, ex.message))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> = ResponseEntity
        .status(ErrorBaseCode.NOT_READABLE.httpStatus)
        .body(ApiResponse.onFailure(ErrorBaseCode.NOT_READABLE))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> = ResponseEntity
        .status(ErrorBaseCode.MISSING_PARAM.httpStatus)
        .body(ApiResponse.onFailure(ErrorBaseCode.MISSING_PARAM))

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(ex: NoHandlerFoundException): ResponseEntity<ApiResponse<Nothing>> = ResponseEntity
        .status(ErrorBaseCode.NOT_FOUND_API.httpStatus)
        .body(ApiResponse.onFailure(ErrorBaseCode.NOT_FOUND_API))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiResponse<Nothing>> = ResponseEntity
        .status(ErrorBaseCode.INTERNAL_SERVER_ERROR.httpStatus)
        .body(ApiResponse.onFailure(ErrorBaseCode.INTERNAL_SERVER_ERROR))
}
