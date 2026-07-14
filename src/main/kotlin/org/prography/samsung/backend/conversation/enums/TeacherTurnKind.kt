package org.prography.samsung.backend.conversation.enums

/** 선생님(유저) 발화의 분류 결과: 단순 확인(AFFIRM), 개념 설명(EXPLAIN), 그 외 잡음/이탈(GARBAGE). */
enum class TeacherTurnKind { AFFIRM, EXPLAIN, GARBAGE }
