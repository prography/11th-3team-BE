package org.prography.samsung.backend.common.exception

import jakarta.servlet.http.HttpServletRequest
import org.prography.samsung.backend.common.alert.DiscordErrorNotifier
import org.prography.samsung.backend.common.auth.CurrentUser
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.prography.samsung.backend.common.response.ApiResponse
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import java.sql.SQLException

@RestControllerAdvice
class GlobalExceptionHandler(private val discordErrorNotifier: DiscordErrorNotifier) {
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        if (ex.errorCode.httpStatus.is5xxServerError) {
            log.warn("Business exception: code={} message={}", ex.errorCode.code, ex.message)
            notifyDiscord(ex, request)
        } else {
            log.debug("Business exception: code={} message={}", ex.errorCode.code, ex.message)
        }
        return ResponseEntity
            .status(ex.errorCode.httpStatus)
            .body(ApiResponse.onFailure(ex.errorCode, ex.message))
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccess(ex: DataAccessException, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        val sqlException = ex.mostSpecificCause as? SQLException
        log.error(
            "Database access failed sqlState={} errorCode={} message={}",
            sqlException?.sqlState,
            sqlException?.errorCode,
            ex.mostSpecificCause.message,
            ex,
        )
        notifyDiscord(ex, request)
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
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception: {}", ex.message, ex)
        notifyDiscord(ex, request)
        return ResponseEntity
            .status(ErrorBaseCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(ApiResponse.onFailure(ErrorBaseCode.INTERNAL_SERVER_ERROR))
    }

    private fun notifyDiscord(throwable: Throwable, request: HttpServletRequest) {
        val uid = runCatching { CurrentUserHolder.get().userId.toString() }
            .getOrNull()
            ?: (request.getAttribute(CurrentUserHolder.REQUEST_ATTRIBUTE) as? CurrentUser)?.userId?.toString()

        val path = request.requestURI
        val method = request.method
        val traceId = MDC.get("requestId") ?: request.getHeader("X-Request-Id")

        val errorCode =
            (throwable as? CustomException)
                ?.let {
                    "${it.errorCode.code} ${it.errorCode::class.simpleName}"
                } ?: throwable::class.simpleName

        discordErrorNotifier.notifyError(
            throwable = throwable,
            uid = uid,
            path = path,
            method = method,
            errorCode = errorCode,
            traceId = traceId,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
