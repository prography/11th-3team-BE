package org.prography.samsung.backend.common.response

import org.springframework.http.HttpStatus

enum class ErrorBaseCode(override val httpStatus: HttpStatus, override val code: Int, override val message: String) :
    ApiCode {
    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),
    MISSING_PARAM(HttpStatus.BAD_REQUEST, 40010, "필수 파라미터가 존재하지 않습니다."),
    NOT_READABLE(HttpStatus.BAD_REQUEST, 40020, "JSON 혹은 REQUEST BODY 필드 오류입니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, 41300, "업로드 가능 용량을 초과했습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 41500, "지원하지 않는 Content-Type입니다."),

    // 401 UNAUTHORIZED
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 40100, "토큰이 만료되었습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40110, "인증되지 않은 사용자입니다."),

    // 403 FORBIDDEN
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "리소스 접근이 거부되었습니다."),

    // 404 NOT FOUND
    NOT_FOUND_API(HttpStatus.NOT_FOUND, 40400, "잘못된 API 요청입니다."),
    NOT_FOUND_ENTITY(HttpStatus.NOT_FOUND, 40410, "대상을 찾을 수 없습니다."),

    // 405 METHOD NOT ALLOWED
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 40500, "잘못된 HTTP METHOD 요청입니다."),

    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, 40900, "이미 존재하는 리소스입니다."),
    DB_CONFLICT(HttpStatus.CONFLICT, 40910, "DB 관련 충돌 문제입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버 내부 오류입니다."),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, 50100, "지원되지 않거나 미구현된 기능입니다."),
}
