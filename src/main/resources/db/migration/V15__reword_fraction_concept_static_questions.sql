-- 정적 수업(FRACTION_CALC 토픽1 '분수란?')의 학생 대사를 자연스럽게 정리.
-- V2/V13은 이미 적용된 마이그레이션이라 편집 불가(Flyway 체크섬 불변) → 새 마이그레이션으로 UPDATE.
--
-- ① INTRO 질문: "분수는 그냥 숫자랑 어떻게 달라요?" → "분수가 뭐예요?"
-- ② REACTION 질문: "1/4는 4보다 작은 수라서 덜 중요한 거죠?"
--    → "그럼 분모를 분자가 나누는 거예요?" (분모/분자의 나눗셈 방향을 반대로 아는 오개념)
--    → 정정 카드도 올바른 방향(분자 ÷ 분모)을 짚도록 교체.

-- ① 토픽1 INTRO 질문
UPDATE lesson_questions
SET bubble_text = '분수가 뭐예요?',
    updated_at = CURRENT_TIMESTAMP
WHERE phase = 'INTRO'
  AND lesson_topic_id IN (
    SELECT lt.id
    FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  );

-- ② 토픽1 REACTION 질문 (오개념 문구 교체)
UPDATE lesson_questions
SET bubble_text = '아하! 그럼 <strong>분모를 분자가 나누는</strong> 거예요?',
    wrong_answer_html = '분모를 분자가 나누는',
    updated_at = CURRENT_TIMESTAMP
WHERE phase = 'REACTION'
  AND lesson_topic_id IN (
    SELECT lt.id
    FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  );

-- ② 토픽1 REACTION 정정 카드 (올바른 나눗셈 방향: 분자 ÷ 분모)
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 개념"},"sections":[{"id":"compare","title":"오개념 정정","bodyHtml":"분수는 위에 있는 <strong>분자</strong>를 아래 <strong>분모</strong>로 나눈 수예요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE phase = 'REACTION'
  AND lesson_topic_id IN (
    SELECT lt.id
    FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  );
