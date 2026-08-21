-- 분수 커리큘럼 토픽별 INTRO/REACTION 보강 (운영 DB 백필).
--
-- 배경: 77af720에서 "1 topic = 1 unit, INTRO/REACTION은 같은 topic의 phase" 모델로
-- 전환하며 V13에 동일한 보강을 넣었으나, V13이 이미 적용된 환경에는 반영되지 않았다.
-- 그 결과 topic 1(분수의 개념)은 REACTION이, topic 2(분수의 덧셈과 뺄셈)는 INTRO가
-- 없어 해당 phase 조회가 HINT_NOTE_NOT_FOUND(40450)로 실패한다.
--
-- V13과 동일한 값을 사용해 두 경로(V13 신규 적용 / V17 백필)가 같은 상태로 수렴하게 한다.
-- 모두 NOT EXISTS 가드로 재실행 안전.

-- sequence 1 (분수의 개념): REACTION
INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '아하! 그럼 1/4는 <strong>4보다 작은</strong> 수라서 덜 중요한 거죠?', '4보다 작은', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  AND NOT EXISTS (
    SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION'
  );

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"제 3장","title":"분수의 개념"},"sections":[{"id":"compare","title":"오개념 정정","bodyHtml":"분수는 전체를 똑같이 나눈 <strong>일부분</strong>을 나타내는 수예요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  AND NOT EXISTS (
    SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION'
  );

-- sequence 2 (분수의 덧셈과 뺄셈): INTRO
INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '선생님, 2/5 더하기 1/5는 어떻게 구해요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
  AND NOT EXISTS (
    SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO'
  );

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"제 3장","title":"분수의 덧셈과 뺄셈"},"sections":[{"id":"rule","title":"핵심 규칙","bodyHtml":"분모가 같으면 <strong>분자끼리</strong>만 더하거나 빼요","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
  AND NOT EXISTS (
    SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO'
  );
