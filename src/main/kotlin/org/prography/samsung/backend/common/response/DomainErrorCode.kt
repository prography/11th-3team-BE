package org.prography.samsung.backend.common.response

import org.springframework.http.HttpStatus

enum class DomainErrorCode(override val httpStatus: HttpStatus, override val code: Int, override val message: String) :
    ApiCode {
    INVALID_DEVICE_USER_ID(ErrorBaseCode.BAD_REQUEST.httpStatus, ErrorBaseCode.BAD_REQUEST.code, "잘못된 요청입니다."),
    SCHEDULE_DAY_COUNT_MISMATCH(
        ErrorBaseCode.MISSING_PARAM.httpStatus,
        ErrorBaseCode.MISSING_PARAM.code,
        "요일을 frequency 개수만큼 골라주세요.",
    ),
    INVALID_LESSON_TIME(
        ErrorBaseCode.BAD_REQUEST.httpStatus,
        ErrorBaseCode.BAD_REQUEST.code,
        "수업 시간은 15:00~20:00 사이 정각만 가능합니다.",
    ),
    SCHEDULE_NOT_CONFIGURED(ErrorBaseCode.BAD_REQUEST.httpStatus, ErrorBaseCode.BAD_REQUEST.code, "시간표를 먼저 설정해주세요."),
    SESSION_PHASE_MISMATCH(ErrorBaseCode.FORBIDDEN.httpStatus, ErrorBaseCode.FORBIDDEN.code, "현재 수업 단계와 맞지 않습니다."),
    SESSION_ALREADY_STARTED(ErrorBaseCode.CONFLICT.httpStatus, ErrorBaseCode.CONFLICT.code, "이미 진행 중인 수업이 있습니다."),
    SESSION_NOT_STARTED(ErrorBaseCode.CONFLICT.httpStatus, ErrorBaseCode.CONFLICT.code, "시작되지 않은 수업입니다."),
    SESSION_NOT_IN_INTRO(ErrorBaseCode.CONFLICT.httpStatus, ErrorBaseCode.CONFLICT.code, "설명 단계에서만 진행할 수 있습니다."),
    SESSION_NOT_COMPLETED(ErrorBaseCode.CONFLICT.httpStatus, ErrorBaseCode.CONFLICT.code, "아직 완료되지 않은 수업입니다."),
    CURRICULUM_NOT_SELECTED(ErrorBaseCode.BAD_REQUEST.httpStatus, ErrorBaseCode.BAD_REQUEST.code, "단원을 먼저 선택해주세요."),
    ACTIVE_SESSION_EXISTS(ErrorBaseCode.CONFLICT.httpStatus, ErrorBaseCode.CONFLICT.code, "진행 중인 수업이 있어 변경할 수 없습니다."),
}
