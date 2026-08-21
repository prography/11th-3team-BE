-- 모든 curriculum_unit의 hint_notes content_json을 개념(c1~c4) · 오개념(m1~m4) 전체로 보강.
-- 기존에는 INTRO에 c1 하나, REACTION에 오개념 하나만 들어 있었음.
-- (lesson_topic_id, phase) UNIQUE 제약이 있으므로 UPDATE로 content_json을 교체.

---------------------------------------------------------------------
-- 1) frac_concept_01  (FRACTION_CALC seq=1)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 개념"},"sections":[{"id":"c1","title":"분수란?","bodyHtml":"전체를 똑같이 나눈 것 중, <strong>일부분</strong>을 나타내는 수","highlight":false},{"id":"c2","title":"분모","bodyHtml":"전체를 똑같이 나눈 개수를 나타내는 <strong>아래 숫자</strong>","highlight":false},{"id":"c3","title":"분자","bodyHtml":"가지고 있는 조각의 수를 나타내는 <strong>위 숫자</strong>","highlight":false},{"id":"c4","title":"크기 비교","bodyHtml":"분모가 같으면 <strong>분자가 큰 쪽</strong>이 더 큰 분수","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
) AND phase = 'INTRO';

-- REACTION: m1
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 개념"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"분모는 나눈 개수라 <strong>더하지 않고</strong> 그대로 둬요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 1
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 2) frac_add_01  (FRACTION_CALC seq=2)
---------------------------------------------------------------------
-- INTRO: c1~c3
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 덧셈과 뺄셈"},"sections":[{"id":"c1","title":"분모가 같은 덧셈","bodyHtml":"분모가 같으면 <strong>분자끼리</strong>만 더하고 분모는 그대로 둔다","highlight":false},{"id":"c2","title":"분모가 같은 뺄셈","bodyHtml":"분모가 같으면 <strong>분자끼리</strong>만 빼고 분모는 그대로 둔다","highlight":false},{"id":"c3","title":"약분","bodyHtml":"덧셈·뺄셈 결과는 필요하면 <strong>약분</strong>해서 간단히 나타낼 수 있다","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
) AND phase = 'INTRO';

-- REACTION: m1
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"제 3장","title":"분수의 덧셈과 뺄셈"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"분모(아래)는 절대 더하지 않고 그대로 둡니다! <strong>분자(위)끼리만</strong> 더해야 해요","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'FRACTION_CALC' AND lt.sequence = 2
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 3) social_4_1_heritage_01  (SOCIAL_4_1_HERITAGE)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"1단원","title":"문화유산의 의미"},"sections":[{"id":"c1","title":"문화유산이란?","bodyHtml":"조상들이 물려준 소중한 것. <strong>유형</strong>(건물·물건)과 <strong>무형</strong>(노래·춤·기술) 문화유산이 있음","highlight":false},{"id":"c2","title":"문화유산 조사 방법","bodyHtml":"직접 찾아가는 <strong>답사</strong>, 전문가에게 묻는 <strong>면담</strong>, 책·인터넷으로 <strong>자료 찾기</strong>","highlight":false},{"id":"c3","title":"문화유산 보호","bodyHtml":"한번 훼손되면 되살리기 어려우므로 <strong>함께 아끼고 지켜야</strong> 함","highlight":false},{"id":"c4","title":"지역의 역사적 인물","bodyHtml":"지역마다 본받을 만한 <strong>역사적 인물</strong>이 있고, 그들의 삶과 정신도 소중한 유산","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_1_HERITAGE'
) AND phase = 'INTRO';

-- REACTION: m1~m4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"1단원","title":"문화유산 보호"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"문화유산은 건물·물건뿐 아니라 판소리·탈춤 같은 <strong>무형 문화유산</strong>도 있어요!","highlight":true},{"id":"m2","title":"오개념 정정","bodyHtml":"박물관뿐 아니라 우리 동네 성곽, 옛집, 축제처럼 <strong>가까운 곳</strong>에도 문화유산이 있어요!","highlight":true},{"id":"m3","title":"오개념 정정","bodyHtml":"옛날 것이라 쓸모없는 게 아니라 조상의 <strong>지혜와 역사</strong>가 담겨 있어 보호하고 배울 가치가 있어요!","highlight":true},{"id":"m4","title":"오개념 정정","bodyHtml":"문화유산은 나라만 지키는 것이 아니라, <strong>우리도 함께</strong> 아끼고 지켜야 해요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_1_HERITAGE'
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 4) social_4_1_publicinstitution_01  (SOCIAL_4_1_PUBLIC)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"3단원","title":"공공기관의 의미"},"sections":[{"id":"c1","title":"공공기관이란?","bodyHtml":"주민 <strong>전체</strong>의 이익과 편리를 위해 나라나 지역이 세우고 운영하는 기관","highlight":false},{"id":"c2","title":"공공기관의 종류","bodyHtml":"<strong>경찰서</strong>(안전·질서), <strong>소방서</strong>(화재·구조), <strong>도서관·보건소</strong>(배움·건강) 등","highlight":false},{"id":"c3","title":"지역 문제와 주민 참여","bodyHtml":"쓰레기·교통·안전 같은 문제는 공공기관 혼자가 아니라 <strong>주민도 함께</strong> 참여해야 해결됨","highlight":false},{"id":"c4","title":"민주적 문제 해결","bodyHtml":"<strong>대화와 타협</strong>으로 의견을 모으고, 다수결로 정하되 <strong>소수 의견도 존중</strong>","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_1_PUBLIC'
) AND phase = 'INTRO';

-- REACTION: m1~m4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"3단원","title":"지역 문제와 주민 참여"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"공공기관은 돈을 벌려는 회사가 아니라 <strong>주민 전체의 편리와 안전</strong>을 위해 일하는 곳이에요!","highlight":true},{"id":"m2","title":"오개념 정정","bodyHtml":"지역 문제는 공공기관만이 아니라 <strong>주민 참여</strong>로 함께 해결해요!","highlight":true},{"id":"m3","title":"오개념 정정","bodyHtml":"다수결은 좋은 방법이지만 <strong>소수의 의견</strong>도 듣고 존중해야 해요!","highlight":true},{"id":"m4","title":"오개념 정정","bodyHtml":"불만만 말하는 것이 아니라 서명, 주민 회의처럼 <strong>직접 참여</strong>해야 문제가 해결돼요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_1_PUBLIC'
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 5) social_4_2_production_01  (SOCIAL_4_2_PRODUCTION)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"2단원","title":"생산과 소비"},"sections":[{"id":"c1","title":"생산과 소비","bodyHtml":"<strong>생산</strong>은 필요한 것을 만들거나 제공하는 활동, <strong>소비</strong>는 사서 쓰는 활동","highlight":false},{"id":"c2","title":"생산 활동의 종류","bodyHtml":"자연에서 얻기(농사·고기잡이), <strong>만들기</strong>(공장·요리), <strong>서비스</strong>(배달·미용·치료)","highlight":false},{"id":"c3","title":"현명한 소비","bodyHtml":"돈은 한정되어 있으므로 <strong>필요·가격·품질</strong>을 따져 보고 계획해서 소비해야 함","highlight":false},{"id":"c4","title":"지역 간 교류","bodyHtml":"지역마다 자연환경·자원·기술이 달라 서로 필요한 것을 <strong>주고받으며</strong> 모두 이익을 얻음","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
) AND phase = 'INTRO';

-- REACTION: m1~m4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"2단원","title":"지역 간 교류"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"물건을 사는 것만 소비가 아니라 병원 진료, 버스 타기처럼 <strong>서비스 이용</strong>도 소비예요!","highlight":true},{"id":"m2","title":"오개념 정정","bodyHtml":"공장에서만 생산하는 게 아니라 농사·고기잡이, 배달·미용 같은 <strong>서비스 제공</strong>도 생산이에요!","highlight":true},{"id":"m3","title":"오개념 정정","bodyHtml":"비싼 물건이 항상 좋은 게 아니라 <strong>필요와 품질</strong>을 함께 따져 선택하는 것이 현명한 소비예요!","highlight":true},{"id":"m4","title":"오개념 정정","bodyHtml":"지역마다 잘 만드는 것이 달라 서로 <strong>교류</strong>하며 필요한 것을 주고받아요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_2_PRODUCTION'
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 6) social_4_2_socialchange_01  (SOCIAL_4_2_SOCIAL_CHANGE)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"3단원","title":"사회 변화"},"sections":[{"id":"c1","title":"저출산·고령화","bodyHtml":"태어나는 아이는 줄고 노인 인구는 늘어나는 변화. <strong>학교·일터·복지</strong>에 큰 영향을 줌","highlight":false},{"id":"c2","title":"정보화","bodyHtml":"컴퓨터·인터넷·스마트폰으로 정보를 빠르게 주고받는 사회. 편리하지만 <strong>개인정보 유출·악성 댓글</strong> 같은 문제도 있음","highlight":false},{"id":"c3","title":"세계화","bodyHtml":"세계 여러 나라가 가까워져 물건·문화·사람이 <strong>활발히 오가는</strong> 변화","highlight":false},{"id":"c4","title":"문화 다양성 존중","bodyHtml":"다른 문화는 틀린 게 아니라 <strong>다른 것</strong>. 편견 없이 존중해야 해요","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
) AND phase = 'INTRO';

-- REACTION: m1~m4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"3단원","title":"문화 다양성 존중"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"고령화는 할머니·할아버지 일이 아니라 일할 사람·세금·복지가 달라져 <strong>우리 생활과 미래</strong>에도 영향을 줘요!","highlight":true},{"id":"m2","title":"오개념 정정","bodyHtml":"인터넷은 편하기만 한 게 아니라 <strong>개인정보 유출, 악성 댓글, 가짜 정보</strong> 같은 문제도 함께 생겨요!","highlight":true},{"id":"m3","title":"오개념 정정","bodyHtml":"다른 나라 문화는 이상한 게 아니라 환경과 역사에 따라 생긴 <strong>다른 방식</strong>일 뿐이에요!","highlight":true},{"id":"m4","title":"오개념 정정","bodyHtml":"겉모습이나 언어가 달라도 <strong>함께 사는 우리 사회의 구성원</strong>이에요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SOCIAL_4_2_SOCIAL_CHANGE'
) AND phase = 'REACTION';

---------------------------------------------------------------------
-- 7) science_strata_fossil_01  (SCIENCE_STRATA_FOSSIL)
---------------------------------------------------------------------
-- INTRO: c1~c4
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"1단원","title":"지층과 화석"},"sections":[{"id":"c1","title":"지층의 특징","bodyHtml":"자갈·모래·진흙 같은 퇴적물이 쌓여 층을 이룬 것. <strong>줄무늬</strong>가 보이고 모양이 다양해요","highlight":false},{"id":"c2","title":"지층이 만들어지는 과정","bodyHtml":"흐르는 물이 퇴적물을 운반해 쌓고, 오랜 시간 <strong>눌리고 굳어져</strong> 지층이 됨. 아래 층이 먼저 만들어짐","highlight":false},{"id":"c3","title":"퇴적암의 분류","bodyHtml":"지층은 주로 퇴적암으로 이루어지며, 알갱이 크기에 따라 <strong>이암</strong>(작은 알갱이)·<strong>사암</strong>(모래)·<strong>역암</strong>(자갈)으로 분류","highlight":false},{"id":"c4","title":"화석의 생성과 가치","bodyHtml":"옛날 생물의 몸체나 흔적이 지층에 남은 것. 과거 생물의 생김새와 <strong>살던 환경</strong>을 알려 줌","highlight":false}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
) AND phase = 'INTRO';

-- REACTION: m1~m2
UPDATE hint_notes
SET content_json = '{"header":{"chapter":"1단원","title":"퇴적암과 화석"},"sections":[{"id":"m1","title":"오개념 정정","bodyHtml":"퇴적물은 쌓이기만 하면 안 되고, <strong>다져지고 굳어지는</strong> 과정을 거쳐야 퇴적암이 돼요!","highlight":true},{"id":"m2","title":"오개념 정정","bodyHtml":"동물 뼈가 그대로 화석이 아니라, 뼈를 이루는 물질이 다른 <strong>광물로 바뀌는 화석화 과정</strong>을 거쳐야 화석이 돼요!","highlight":true}]}',
    updated_at = CURRENT_TIMESTAMP
WHERE lesson_topic_id = (
    SELECT lt.id FROM lesson_topics lt
    JOIN curriculums c ON lt.curriculum_id = c.id
    WHERE c.code = 'SCIENCE_STRATA_FOSSIL'
) AND phase = 'REACTION';
