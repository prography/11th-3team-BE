package org.prography.samsung.backend.common.response

import org.springframework.http.HttpStatus

enum class DomainErrorCode(override val httpStatus: HttpStatus, override val code: Int, override val message: String) :
    ApiCode {
    // 400xx — 요청 값이 틀림
    INVALID_DEVICE_USER_ID(HttpStatus.BAD_REQUEST, 40040, "유효하지 않은 디바이스 사용자 ID입니다."),
    SCHEDULE_DAY_COUNT_MISMATCH(HttpStatus.BAD_REQUEST, 40050, "요일을 frequency 개수만큼 골라주세요."),
    INVALID_LESSON_TIME(HttpStatus.BAD_REQUEST, 40060, "수업 시간은 15:00~20:00 사이 정각만 가능합니다."),
    SCHEDULE_NOT_CONFIGURED(HttpStatus.BAD_REQUEST, 40070, "시간표를 먼저 설정해주세요."),
    CURRICULUM_NOT_SELECTED(HttpStatus.BAD_REQUEST, 40080, "단원을 먼저 선택해주세요."),
    TEACH_EMPTY_USER_TEXT(HttpStatus.BAD_REQUEST, 40090, "유저 발화가 비어 있습니다."),
    SCHEDULE_DUPLICATE_DAY(HttpStatus.BAD_REQUEST, 40091, "중복된 요일이 포함되어 있습니다."),
    CURRICULUM_MISMATCH(HttpStatus.BAD_REQUEST, 40092, "선택한 커리큘럼이 현재 수강 중인 커리큘럼과 다릅니다."),

    // 403xx — 권한 문제
    SESSION_PHASE_MISMATCH(HttpStatus.FORBIDDEN, 40310, "현재 수업 단계와 맞지 않습니다."),
    TEACH_SESSION_NOT_AI_LOOP(HttpStatus.FORBIDDEN, 40320, "AI 대화 모드 세션이 아닙니다."),

    // 404xx — 대상을 못 찾음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40410, "사용자를 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, 40420, "세션을 찾을 수 없습니다."),
    CURRICULUM_NOT_FOUND(HttpStatus.NOT_FOUND, 40430, "커리큘럼을 찾을 수 없습니다."),
    LESSON_TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, 40440, "학습 주제를 찾을 수 없습니다."),
    HINT_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, 40450, "힌트 노트를 찾을 수 없습니다."),

    // 409xx — 현재 상태 때문에 수행 불가
    SESSION_ALREADY_STARTED(HttpStatus.CONFLICT, 40920, "이미 진행 중인 수업이 있습니다."),
    SESSION_NOT_STARTED(HttpStatus.CONFLICT, 40930, "시작되지 않은 수업입니다."),
    SESSION_NOT_IN_INTRO(HttpStatus.CONFLICT, 40940, "설명 단계에서만 진행할 수 있습니다."),
    SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, 40950, "아직 완료되지 않은 수업입니다."),
    ACTIVE_SESSION_EXISTS(HttpStatus.CONFLICT, 40960, "진행 중인 수업이 있어 변경할 수 없습니다."),
    TEACH_TURN_LIMIT_EXCEEDED(HttpStatus.CONFLICT, 40970, "대화 턴 한도를 초과했습니다."),
}
