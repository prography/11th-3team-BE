package org.prography.samsung.backend.common.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.prography.samsung.backend.common.exception.CustomException
import org.prography.samsung.backend.common.response.ApiResponse
import org.prography.samsung.backend.common.response.DomainErrorCode
import org.prography.samsung.backend.common.response.ErrorBaseCode
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(1)
class DeviceUserAuthFilter(private val userUpsertService: UserUpsertService, private val objectMapper: ObjectMapper) :
    OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/api-docs")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val authorization = request.getHeader("Authorization")
            if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ")) {
                writeError(response, CustomException(ErrorBaseCode.UNAUTHORIZED))
                return
            }

            val token = authorization.removePrefix("Bearer ").trim()
            if (!isValidUuid(token)) {
                writeError(response, CustomException(DomainErrorCode.INVALID_DEVICE_USER_ID))
                return
            }

            val currentUser = userUpsertService.upsertByExternalId(token)
            request.setAttribute(CurrentUserHolder.REQUEST_ATTRIBUTE, currentUser)
            filterChain.doFilter(request, response)
        } catch (ex: CustomException) {
            writeError(response, ex)
        }
    }

    private fun isValidUuid(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess

    private fun writeError(response: HttpServletResponse, ex: CustomException) {
        response.status = ex.errorCode.httpStatus.value()
        response.contentType = "${MediaType.APPLICATION_JSON_VALUE};charset=UTF-8"
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiResponse.onFailure<Nothing>(ex.errorCode, ex.message),
            ),
        )
    }
}
