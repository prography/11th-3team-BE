-- 정적 수업(FRACTION_CALC 토픽1 '분수란?')의 학생 대사를 자연스럽게 정리.
-- V2/V13은 이미 적용된 마이그레이션이라 편집 불가(Flyway 체크섬 불변) → 새 마이그레이션으로 UPDATE.
--
-- ① INTRO 질문: "분수는 그냥 숫자랑 어떻게 달라요?" (분수도 '수'라 전제가 모호)
--    → "분수는 무엇을 나타내는 수예요?" (INTRO 정정 카드 '전체를 똑같이 나눈 일부분'과 자연스럽게 연결)
-- ② REACTION 질문: "1/4는 4보다 작은 수라서 덜 중요한 거죠?" (작다→덜 중요 비약, 정정 카드와 핀트 어긋남)
--    → "꼭 똑같이 안 나눠도 분수인 거죠?" (실제 학생이 흔히 놓치는 '똑같이' 오개념)
--    → 정정 카드도 '똑같이'를 정조준하도록 강조점 정렬.

-- ① 토픽1 INTRO 질문
UPDATE lesson_questions
SET bubble_text = '선생님, 분수는 무엇을 나타내는 수예요?',
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
SET bubble_text = '아하! 그럼 꼭 <strong>똑같이 안 나눠도</strong> 분수인 거죠?',
    wrong_answer_html = '똑같이 안 나눠도',
    updated_at = CURRENT_TIMESTAMP
WHERE phase = 'REACTION'
  AND lesson_topic_id IN (
    SELECT lt.id
    FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  );

-- ② 토픽1 REACTION 정정 카드 ('똑같이' 강조로 오개념 정조준)
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 개념"},"sections":[{"id":"compare","title":"오개념 정정","bodyHtml":"분수는 전체를 <strong>똑같이</strong> 나눈 일부분을 나타내는 수예요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE phase = 'REACTION'
  AND lesson_topic_id IN (
    SELECT lt.id
    FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
  );
