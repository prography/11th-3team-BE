ALTER TABLE tutoring_sessions
    ADD COLUMN lesson_topic_id BIGINT REFERENCES lesson_topics (id);

CREATE INDEX idx_tutoring_sessions_lesson_topic ON tutoring_sessions (lesson_topic_id);