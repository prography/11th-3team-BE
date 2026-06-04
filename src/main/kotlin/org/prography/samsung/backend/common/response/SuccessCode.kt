package org.prography.samsung.backend.common.response

import org.springframework.http.HttpStatus

enum class SuccessCode(override val httpStatus: HttpStatus, override val code: Int, override val message: String) :
    ApiCode {
    OK(HttpStatus.OK, 200, "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, 201, "요청이 성공했습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, 202, "요청이 접수되었습니다."),
}
