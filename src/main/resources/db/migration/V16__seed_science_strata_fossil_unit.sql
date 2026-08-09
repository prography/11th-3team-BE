-- 초등 과학 '지층과 화석' 단원 시드 (scratch_unit1.json 을 앱 스키마에 맞게 증류).
-- 12차시 교과서 원문 → 개념 4개 + 오개념 2개 + 단일 lesson_topic(INTRO/REACTION)으로 압축.
-- 사회 단원 시드(V10/V11)와 동일한 패턴. 모두 NOT EXISTS 가드로 재실행 안전.

-- 1) curriculum
INSERT INTO curriculums (code, name, chapter_label, session_title_template, display_order, updated_at)
SELECT 'SCIENCE_STRATA_FOSSIL', '지층과 화석', '1단원 지층과 화석', '지층과 화석의 세계', 14, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM curriculums WHERE code = 'SCIENCE_STRATA_FOSSIL');

-- 2) curriculum_unit (AI teach unit_json + 시스템 프롬프트 템플릿)
INSERT INTO curriculum_units (unit_id, curriculum_id, unit_json, system_prompt_template)
SELECT
    'science_strata_fossil_01',
    c.id,
    '{"unit_id":"science_strata_fossil_01","title":"지층과 화석","subject":"과학","grade":5,"semester":1,"curriculum_ref":"초등 과학 5~6학년군 지층과 화석","persona":{"name":"지호","age":11,"gender":"neutral","traits":["호기심 많음","돌·화석 구경을 좋아함","과학 처음 배우는 중","쉽게 일반화하는 경향"],"speech_style":"쌤, 헐, 와, 진짜요? 같은 초등 말투","background":"박물관에서 공룡 화석을 본 적은 있지만 지층이나 화석이 어떻게 만들어지는지는 잘 모름"},"concepts":[{"id":"c1","name":"지층의 특징","depth":1,"description":"자갈·모래·진흙 같은 퇴적물이 쌓여 층을 이룬 것. 층마다 두께와 색이 다르고 줄무늬가 보이며 수평·휘어진·끊어진 모양이 있음","key_points":["퇴적물이 쌓여 층을 이룬 것","층마다 두께와 색이 다르고 줄무늬가 보임","수평·휘어진·끊어진 모양이 있음"]},{"id":"c2","name":"지층이 만들어지는 과정","depth":2,"description":"흐르는 물이 퇴적물을 운반해 쌓고 오랜 시간 눌리고 굳어져 지층이 됨. 아래에 있는 층이 먼저 만들어짐","key_points":["흐르는 물이 퇴적물을 운반해 쌓음","오래 쌓이고 눌려 굳어짐","아래에 있는 층이 먼저 만들어짐"]},{"id":"c3","name":"퇴적암과 분류","depth":2,"description":"지층은 주로 퇴적암으로 이루어지며 알갱이 크기에 따라 이암·사암·역암으로 분류함","key_points":["지층은 퇴적암으로 이루어짐","알갱이 크기로 분류함","이암(작은 알갱이)·사암(모래)·역암(자갈)"]},{"id":"c4","name":"화석의 생성과 가치","depth":3,"description":"옛날에 살던 생물의 몸체나 흔적이 지층 속에 남은 것. 과거 생물의 생김새·생활 모습과 살던 환경을 알려 줌","key_points":["옛날 생물의 몸체나 흔적이 지층에 남음","퇴적물이 빠르게 덮여야 잘 만들어짐","과거 생물과 환경을 알려 줌"]}],"misconceptions":[{"id":"m1","desc":"퇴적물이 쌓이고 눌리면 바로 퇴적암이 된다","trigger_example":"퇴적물이 쌓이면 바로 퇴적암이 되는 거죠?","correction_hint":"다짐 작용과 교결 작용을 거쳐 굳어져야 퇴적암이 됨"},{"id":"m2","desc":"동물 뼈는 그대로 화석이다","trigger_example":"동물 뼈는 다 화석이에요?","correction_hint":"뼈를 이루는 물질이 다른 광물로 바뀌는 화석화 과정을 거쳐야 화석이 됨"}],"session_goal":"유저(쌤)가 지층의 특징과 만들어지는 과정, 퇴적암의 분류, 화석의 생성과 가치를 설명할 수 있게 되는 것","max_turns":10,"min_concepts_to_complete":3,"starter_question":"쌤, 저번에 산에 갔을 때 절벽이 줄무늬처럼 층층이 쌓여 있었는데, 그런 층은 어떻게 생긴 거예요?","example_dialog_flow":[{"turn":1,"ai":"쌤, 저번에 산에 갔을 때 절벽이 줄무늬처럼 층층이 쌓여 있었는데, 그런 층은 어떻게 생긴 거예요?","expected_concept":"c1"},{"turn":4,"ai":"(c1, c2 covered 후) 그럼 퇴적물이 쌓이기만 하면 바로 퇴적암이 되는 거예요?","expected_concept":"c3"}]}',
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
- JSON 외 다른 텍스트'
FROM curriculums c
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM curriculum_units WHERE unit_id = 'science_strata_fossil_01');

-- 3) lesson_topic (curriculum_unit_id NOT NULL/UNIQUE 이므로 함께 인서트)
INSERT INTO lesson_topics (curriculum_id, curriculum_unit_id, sequence, title, subtitle, topic_type, gnb_title, created_at, updated_at)
SELECT c.id, cu.id, 1, '지층과 화석', '개념이해', 'CONCEPT', '1단원 지층과 화석', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM curriculums c
JOIN curriculum_units cu ON cu.curriculum_id = c.id AND cu.unit_id = 'science_strata_fossil_01'
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM lesson_topics lt WHERE lt.curriculum_id = c.id);

-- 4) lesson_questions (INTRO 도입 질문 + REACTION 오개념 질문)
INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '쌤, 저번에 산에 갔을 때 절벽이 줄무늬처럼 층층이 쌓여 있었는데, 그런 층은 어떻게 생긴 거예요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO');

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '아하! 그럼 퇴적물이 <strong>쌓이기만 하면 바로</strong> 퇴적암이 되는 거죠?', '쌓이기만 하면 바로', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION');

-- 5) hint_notes (INTRO 핵심 개념 + REACTION 오개념 정정)
INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"1단원","title":"지층의 특징"},"sections":[{"id":"c1","title":"핵심 개념","bodyHtml":"자갈·모래·진흙 같은 퇴적물이 쌓여 층을 이룬 것. <strong>줄무늬</strong>가 보이고 모양이 다양해요","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"1단원","title":"퇴적암이 되는 과정"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"퇴적물은 <strong>다져지고 굳어지는</strong> 과정을 거쳐야 퇴적암이 돼요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION');
