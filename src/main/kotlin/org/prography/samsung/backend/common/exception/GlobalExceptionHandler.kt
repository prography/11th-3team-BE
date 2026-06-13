package org.prography.samsung.backend.common.exception

import org.prography.samsung.backend.common.response.ApiResponse
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import java.sql.SQLException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException): ResponseEntity<ApiResponse<Nothing>> {
        if (ex.errorCode.httpStatus.is5xxServerError) {
            log.warn("Business exception: code={} message={}", ex.errorCode.code, ex.message)
        } else {
            log.debug("Business exception: code={} message={}", ex.errorCode.code, ex.message)
        }
        return ResponseEntity
            .status(ex.errorCode.httpStatus)
            .body(ApiResponse.onFailure(ex.errorCode, ex.message))
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccess(ex: DataAccessException): ResponseEntity<ApiResponse<Nothing>> {
        val sqlException = ex.mostSpecificCause as? SQLException
        log.error(
            "Database access failed sqlState={} errorCode={} message={}",
            sqlException?.sqlState,
            sqlException?.errorCode,
            ex.mostSpecificCause.message,
            ex,
        )
        return ResponseEntity
            .status(ErrorBaseCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.INTERNAL_SERVER_ERROR))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("Request body is not readable: {}", ex.mostSpecificCause.message)
        return ResponseEntity
            .status(ErrorBaseCode.NOT_READABLE.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.NOT_READABLE))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val fieldErrors =
            ex.bindingResult.fieldErrors.joinToString(", ") { fieldError ->
                "${fieldError.field}=${fieldError.defaultMessage}"
            }
        log.warn("Validation failed: {}", fieldErrors)
        return ResponseEntity
            .status(ErrorBaseCode.MISSING_PARAM.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.MISSING_PARAM))
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(ex: NoHandlerFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("No handler found: {} {}", ex.httpMethod, ex.requestURL)
        return ResponseEntity
            .status(ErrorBaseCode.NOT_FOUND_API.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.NOT_FOUND_API))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception: {}", ex.message, ex)
        return ResponseEntity
            .status(ErrorBaseCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.INTERNAL_SERVER_ERROR))
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
