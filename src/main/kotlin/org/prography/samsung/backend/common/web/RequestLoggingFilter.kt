package org.prography.samsung.backend.common.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.prography.samsung.backend.common.auth.CurrentUser
import org.prography.samsung.backend.common.auth.CurrentUserHolder
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(0)
class RequestLoggingFilter : OncePerRequestFilter() {
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
        val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()

        MDC.put(REQUEST_ID_KEY, requestId)
        MDC.put(HTTP_METHOD_KEY, request.method)
        MDC.put(HTTP_PATH_KEY, request.requestURI)

        try {
            log.info("request started query={}", request.queryString.orEmpty().ifBlank { "-" })
            filterChain.doFilter(request, response)
        } finally {
            (request.getAttribute(CurrentUserHolder.REQUEST_ATTRIBUTE) as? CurrentUser)?.let { currentUser ->
                MDC.put(USER_ID_KEY, currentUser.userId.toString())
            }

            val durationMs = System.currentTimeMillis() - startedAt
            log.info(
                "request completed status={} durationMs={}",
                response.status,
                durationMs,
            )

            MDC.remove(REQUEST_ID_KEY)
            MDC.remove(HTTP_METHOD_KEY)
            MDC.remove(HTTP_PATH_KEY)
            MDC.remove(USER_ID_KEY)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val REQUEST_ID_KEY = "requestId"
        private const val HTTP_METHOD_KEY = "httpMethod"
        private const val HTTP_PATH_KEY = "httpPath"
        private const val USER_ID_KEY = "userId"
    }
}
