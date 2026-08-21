-- V1이 시드한 미사용 커리큘럼 7개 삭제.
--
-- 대상: INTEGERS_RATIONALS(정수와 유리수), LINEAR_EQUATIONS(일차방정식),
--       GEOMETRY_BASICS(도형의 기초), PROPORTION(비례와 반비례),
--       STATISTICS(통계의 기초), DECIMAL_DIVISION(소수의 나눗셈),
--       RULES_CORRESPONDENCE(규칙과 대응)
-- 이들은 V1에서 curriculums 행만 시드되었고 lesson_topics/curriculum_units가 없다.
--
-- 유지: FRACTION_CALC(분수의 계산), 사회 4학년 4개(V10/V11), 과학 지층과 화석(V16).
--
-- 콘텐츠 테이블은 현재 해당 행이 없지만, 운영 데이터(세션·유저 선택)는 존재할 수 있어
-- FK 역순으로 함께 삭제한다. 재실행 안전(멱등).

-- 1) 코인 원장: 삭제 대상 커리큘럼의 세션에 달린 항목
DELETE FROM coin_ledger_entries
WHERE session_id IN (
    SELECT ts.id FROM tutoring_sessions ts
    JOIN curriculums c ON ts.curriculum_id = c.id
    WHERE c.code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 2) 대화 턴
DELETE FROM conversation_turns
WHERE session_id IN (
    SELECT ts.id FROM tutoring_sessions ts
    JOIN curriculums c ON ts.curriculum_id = c.id
    WHERE c.code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 3) 세션 토픽 스냅샷
DELETE FROM session_topic_snapshots
WHERE session_id IN (
    SELECT ts.id FROM tutoring_sessions ts
    JOIN curriculums c ON ts.curriculum_id = c.id
    WHERE c.code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 4) 세션
DELETE FROM tutoring_sessions
WHERE curriculum_id IN (
    SELECT id FROM curriculums
    WHERE code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 5) 사용자 커리큘럼 선택 (해당 유저는 미선택 상태가 됨)
DELETE FROM user_curriculums
WHERE curriculum_id IN (
    SELECT id FROM curriculums
    WHERE code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 6) 콘텐츠: hint_notes → lesson_questions → lesson_topics
DELETE FROM hint_notes
WHERE lesson_topic_id IN (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

DELETE FROM lesson_questions
WHERE lesson_topic_id IN (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

DELETE FROM lesson_topics
WHERE curriculum_id IN (
    SELECT id FROM curriculums
    WHERE code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 7) AI 대화용 유닛
DELETE FROM curriculum_units
WHERE curriculum_id IN (
    SELECT id FROM curriculums
    WHERE code IN (
        'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
        'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
    )
);

-- 8) 커리큘럼
DELETE FROM curriculums
WHERE code IN (
    'INTEGERS_RATIONALS', 'LINEAR_EQUATIONS', 'GEOMETRY_BASICS',
    'PROPORTION', 'STATISTICS', 'DECIMAL_DIVISION', 'RULES_CORRESPONDENCE'
);

-- 9) 남은 커리큘럼 노출 순서 재정렬 (분수 1, 사회 2~5, 과학 6)
UPDATE curriculums SET display_order = 1 WHERE code = 'FRACTION_CALC';
UPDATE curriculums SET display_order = 2 WHERE code = 'SOCIAL_4_1_HERITAGE';
UPDATE curriculums SET display_order = 3 WHERE code = 'SOCIAL_4_1_PUBLIC';
UPDATE curriculums SET display_order = 4 WHERE code = 'SOCIAL_4_2_PRODUCTION';
UPDATE curriculums SET display_order = 5 WHERE code = 'SOCIAL_4_2_SOCIAL_CHANGE';
UPDATE curriculums SET display_order = 6 WHERE code = 'SCIENCE_STRATA_FOSSIL';
