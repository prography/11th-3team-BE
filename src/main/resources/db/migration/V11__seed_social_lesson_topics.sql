-- Seed one lesson_topic per curriculum_unit for elementary social (grade 4) curriculums from V10.
-- Each topic holds INTRO and REACTION questions on the same row (1 topic = 1 unit).

-- SOCIAL_4_1_HERITAGE
INSERT INTO lesson_topics (curriculum_id, sequence, title, subtitle, topic_type, gnb_title, created_at, updated_at)
SELECT c.id, 1, '우리 지역의 문화유산', '개념이해', 'CONCEPT', '1단원 문화유산', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM curriculums c
WHERE c.code = 'SOCIAL_4_1_HERITAGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_topics lt WHERE lt.curriculum_id = c.id);

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '쌤, 저번에 박물관에서 본 오래된 항아리도 문화유산이에요? 문화유산이 정확히 뭐예요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_HERITAGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO');

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '아하! 그럼 문화유산은 <strong>나라에서 알아서 다 지켜 주는</strong> 거죠?', '나라에서 알아서 다 지켜 주는', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_HERITAGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"1단원","title":"문화유산의 의미"},"sections":[{"id":"c1","title":"핵심 개념","bodyHtml":"조상들이 물려준 소중한 것. <strong>유형</strong>(건물·물건)과 <strong>무형</strong>(노래·춤·기술) 문화유산이 있음","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_HERITAGE'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"1단원","title":"문화유산 보호"},"sections":[{"id":"m4","title":"오개념 정정","bodyHtml":"문화유산은 나라만 지키는 것이 아니라, 우리도 함께 아끼고 지켜야 해요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_HERITAGE'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION');

-- SOCIAL_4_1_PUBLIC
INSERT INTO lesson_topics (curriculum_id, sequence, title, subtitle, topic_type, gnb_title, created_at, updated_at)
SELECT c.id, 1, '지역의 공공기관과 주민 참여', '개념이해', 'CONCEPT', '3단원 공공기관', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM curriculums c
WHERE c.code = 'SOCIAL_4_1_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM lesson_topics lt WHERE lt.curriculum_id = c.id);

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '쌤, 우리 동네 도서관이랑 보건소는 누가 만든 거예요? 그냥 가게 같은 거예요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO');

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '그럼 우리 동네에 쓰레기 문제가 생기면 <strong>시청만</strong> 해결하면 되는 거죠?', '시청만', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"3단원","title":"공공기관의 의미"},"sections":[{"id":"c1","title":"핵심 개념","bodyHtml":"주민 <strong>전체</strong>의 이익과 편리를 위해 나라나 지역이 세우고 운영하는 기관","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"3단원","title":"지역 문제와 주민 참여"},"sections":[{"id":"m2","title":"오개념 정정","bodyHtml":"지역 문제는 공공기관만이 아니라 <strong>주민 참여</strong>로 함께 해결해요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_1_PUBLIC'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION');

-- SOCIAL_4_2_PRODUCTION
INSERT INTO lesson_topics (curriculum_id, sequence, title, subtitle, topic_type, gnb_title, created_at, updated_at)
SELECT c.id, 1, '필요한 것의 생산과 교환', '개념이해', 'CONCEPT', '2단원 생산과 교환', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM curriculums c
WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM lesson_topics lt WHERE lt.curriculum_id = c.id);

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '쌤, 제가 어제 편의점에서 과자 사 먹은 것도 소비예요? 소비가 정확히 뭐예요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO');

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '그럼 과자는 <strong>우리 동네에서 다 만든</strong> 거예요?', '우리 동네에서 다 만든', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"2단원","title":"생산과 소비"},"sections":[{"id":"c1","title":"핵심 개념","bodyHtml":"<strong>생산</strong>은 필요한 것을 만들거나 제공하는 활동, <strong>소비</strong>는 사서 쓰는 활동","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"2단원","title":"지역 간 교류"},"sections":[{"id":"m4","title":"오개념 정정","bodyHtml":"지역마다 잘 만드는 것이 달라 서로 <strong>교류</strong>하며 필요한 것을 주고받아요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION');

-- SOCIAL_4_2_SOCIAL_CHANGE
INSERT INTO lesson_topics (curriculum_id, sequence, title, subtitle, topic_type, gnb_title, created_at, updated_at)
SELECT c.id, 1, '사회 변화와 문화 다양성', '개념이해', 'CONCEPT', '3단원 사회변화', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM curriculums c
WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_topics lt WHERE lt.curriculum_id = c.id);

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'INTRO', '쌤, 우리 반에 외국에서 온 친구가 있는데요. 요즘 왜 이렇게 다른 나라 사람도 많고 세상이 빨리 변해요?', NULL, 'curious', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'INTRO');

INSERT INTO lesson_questions (lesson_topic_id, phase, bubble_text, wrong_answer_html, emotion, created_at, updated_at)
SELECT lt.id, 'REACTION', '근데 다른 나라에서 온 친구가 우리랑 다르게 행동하면 <strong>좀 이상한</strong> 거 아니에요?', '좀 이상한', 'confused', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
  AND NOT EXISTS (SELECT 1 FROM lesson_questions lq WHERE lq.lesson_topic_id = lt.id AND lq.phase = 'REACTION');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'INTRO',
    '{"header":{"chapter":"3단원","title":"사회 변화"},"sections":[{"id":"c1","title":"핵심 개념","bodyHtml":"저출산·고령화, <strong>정보화</strong>, <strong>세계화</strong>처럼 우리 생활이 빠르게 변하고 있음","highlight":false}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'INTRO');

INSERT INTO hint_notes (lesson_topic_id, phase, content_json, created_at, updated_at)
SELECT lt.id, 'REACTION',
    '{"header":{"chapter":"3단원","title":"문화 다양성 존중"},"sections":[{"id":"c4","title":"오개념 정정","bodyHtml":"다른 문화는 틀린 게 아니라 <strong>다른 것</strong>. 편견 없이 존중해야 해요!","highlight":true}]}',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM lesson_topics lt JOIN curriculums c ON lt.curriculum_id = c.id
WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
  AND NOT EXISTS (SELECT 1 FROM hint_notes hn WHERE hn.lesson_topic_id = lt.id AND hn.phase = 'REACTION');