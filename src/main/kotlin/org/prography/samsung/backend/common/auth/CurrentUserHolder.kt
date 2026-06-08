package org.prography.samsung.backend.common.auth

object CurrentUserHolder {
    const val REQUEST_ATTRIBUTE = "currentUser"

    fun get(): CurrentUser = (
        org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
            ?.getAttribute(REQUEST_ATTRIBUTE, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST)
            as? CurrentUser
        )
        ?: throw IllegalStateException("Current user is not available")
}
