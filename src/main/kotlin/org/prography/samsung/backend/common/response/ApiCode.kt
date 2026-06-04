package org.prography.samsung.backend.common.response

import org.springframework.http.HttpStatus

interface ApiCode {
    val httpStatus: HttpStatus
    val code: Int
    val message: String
}
