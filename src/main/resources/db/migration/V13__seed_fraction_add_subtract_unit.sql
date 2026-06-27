-- Add frac_add_01 unit, relink topics (1 topic = 1 unit), and ensure INTRO+REACTION per topic.

INSERT INTO curriculum_units (unit_id, curriculum_id, unit_json, system_prompt_template, created_at, updated_at)
SELECT
    'frac_add_01',
    c.id,
    '{"unit_id":"frac_add_01","topic":"분수의 덧셈과 뺄셈","student_persona":"초등 4학년 학생. 호기심이 많고 선생님에게 배우려는 태도. 1~2문장으로 짧게 말한다.","concepts":[{"id":"c1","label":"분모가 같은 분수의 덧셈은 분자끼리 더하고 분모는 그대로 둔다","keywords":["분모 같","분자 더","분모 그대로"]},{"id":"c2","label":"분모가 같은 분수의 뺄셈은 분자끼리 빼고 분모는 그대로 둔다","keywords":["분모 같","분자 빼","분모 그대로"]},{"id":"c3","label":"분수 덧셈·뺄셈 결과는 필요하면 약분할 수 있다","keywords":["약분","더 간단","나누기"]}],"misconceptions":[{"id":"m1","pattern":"분모끼리 더한다","correction":"분모는 나눈 개수라 더하지 않고 그대로 둬요"}],"max_concepts":3,"starter_question":"선생님, 2/5 더하기 1/5는 어떻게 구해요?"}',
    '당신은 초등학생 AI 학생입니다. 선생님(유저)의 설명을 듣고 자연스럽게 반응합니다.

## 수업 개념
{{lesson_concepts}}

## 응답 규칙
1. 반드시 아래 JSON 스키마만 출력하세요. 마크다운·설명·코드블록 금지.
2. speak: 1~2문장, 초등학생 말투, 존댓말("요/죠/네요").
3. emotion: curious|confused|thoughtful|aha|happy 중 하나.
4. covered: 이번 턴에 선생님 설명으로 이해한 concept id 배열.
5. missing: 아직 이해 못한 concept id 배열.
6. misconceptions_detected: 감지된 오개념 id 배열 (없으면 []).
7. correction_stage: 0~4 (0=정정 없음, 4=포기하고 넘어감).
8. focus_concept: 지금 집중할 concept id.
9. session_done: covered가 모든 concept를 포함하면 true.

## JSON 스키마
{"speak":"string","emotion":"curious|confused|thoughtful|aha|happy","covered":["c1"],"missing":["c2"],"misconceptions_detected":[],"correction_stage":0,"focus_concept":"c2","session_done":false}

## 금지
- 욕설, 비속어, 성인 콘텐츠
- 선생님을 비하하는 말
- JSON 외 다른 텍스트',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM curriculums c
WHERE c.code = 'FRACTION_CALC'
  AND NOT EXISTS (SELECT 1 FROM curriculum_units WHERE unit_id = 'frac_add_01');

UPDATE lesson_topics
SET curriculum_unit_id = (
    SELECT cu.id
    FROM curriculum_units cu
    JOIN curriculums c ON cu.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND cu.unit_id = 'frac_concept_01'
)
WHERE curriculum_id = (SELECT id FROM curriculums WHERE code = 'FRACTION_CALC')
  AND sequence = 1;

UPDATE lesson_topics
SET curriculum_unit_id = (
    SELECT cu.id
    FROM curriculum_units cu
    JOIN curriculums c ON cu.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND cu.unit_id = 'frac_add_01'
)
WHERE curriculum_id = (SELECT id FROM curriculums WHERE code = 'FRACTION_CALC')
  AND sequence = 2;

ALTER TABLE lesson_topics
    ADD CONSTRAINT uq_lesson_topics_curriculum_unit UNIQUE (curriculum_unit_id);

-- sequence 1 (분수의 개념): REACTION question
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

-- sequence 2 (분수의 덧셈과 뺄셈): INTRO question
INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '선생님, 2/5 더하기 1/5는 어떻게 구해요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
  AND NOT EXISTS (
    SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO'
  );

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '아하! 그럼 2/5 더하기 1/5는 <strong>3/10</strong>이죠?', '3/10', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
  AND NOT EXISTS (
    SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION'
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

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"제 3장","title":"분수의 덧셈과 뺄셈"},"sections":[{"id":"rule-denominator","title":"핵심 규칙","bodyHtml":"분모(아래)는 절대 더하지 않고 그대로 둡니다!","highlight":true},{"id":"rule-numerator","title":"","bodyHtml":"분자(위)끼리만 더해야 합니다","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt
JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
  AND NOT EXISTS (
    SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION'
  );