package org.prography.samsung.backend.common.exception

import org.prography.samsung.backend.common.response.ApiCode

class CustomException(val errorCode: ApiCode, message: String = errorCode.message) : RuntimeException(message)
