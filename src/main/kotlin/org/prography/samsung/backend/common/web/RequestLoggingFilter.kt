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
        val endpoint = buildEndpoint(request)

        MDC.put(REQUEST_ID_KEY, requestId)
        MDC.put(HTTP_METHOD_KEY, request.method)
        MDC.put(HTTP_PATH_KEY, request.requestURI)
        MDC.put(ENDPOINT_KEY, endpoint)

        try {
            log.info("access started endpoint={}", endpoint)
            filterChain.doFilter(request, response)
        } finally {
            val currentUser = request.getAttribute(CurrentUserHolder.REQUEST_ATTRIBUTE) as? CurrentUser
            val userId = currentUser?.userId?.toString() ?: MDC.get(USER_ID_KEY) ?: ANONYMOUS
            val externalUserId = currentUser?.externalId ?: MDC.get(EXTERNAL_USER_ID_KEY) ?: ANONYMOUS

            MDC.put(USER_ID_KEY, userId)
            MDC.put(EXTERNAL_USER_ID_KEY, externalUserId)

            val durationMs = System.currentTimeMillis() - startedAt
            log.info(
                "access completed endpoint={} userId={} externalUserId={} status={} durationMs={}",
                endpoint,
                userId,
                externalUserId,
                response.status,
                durationMs,
            )

            MDC.remove(REQUEST_ID_KEY)
            MDC.remove(HTTP_METHOD_KEY)
            MDC.remove(HTTP_PATH_KEY)
            MDC.remove(ENDPOINT_KEY)
            MDC.remove(USER_ID_KEY)
            MDC.remove(EXTERNAL_USER_ID_KEY)
        }
    }

    private fun buildEndpoint(request: HttpServletRequest): String {
        val query = request.queryString?.takeIf { it.isNotBlank() }
        return if (query == null) {
            "${request.method} ${request.requestURI}"
        } else {
            "${request.method} ${request.requestURI}?$query"
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val ANONYMOUS = "-"
        private const val REQUEST_ID_KEY = "requestId"
        private const val HTTP_METHOD_KEY = "httpMethod"
        private const val HTTP_PATH_KEY = "httpPath"
        private const val ENDPOINT_KEY = "endpoint"
        private const val USER_ID_KEY = "userId"
        private const val EXTERNAL_USER_ID_KEY = "externalUserId"
    }
}
