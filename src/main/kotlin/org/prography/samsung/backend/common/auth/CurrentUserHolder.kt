package org.prography.samsung.backend.common.auth

import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

object CurrentUserHolder {
    const val REQUEST_ATTRIBUTE = "currentUser"

    fun get(): CurrentUser = (
        RequestContextHolder.getRequestAttributes()
            ?.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST)
            as? CurrentUser
        )
        ?: throw IllegalStateException("Current user is not available")
}
