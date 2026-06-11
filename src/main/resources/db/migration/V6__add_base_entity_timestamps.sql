-- curriculums: add updated_at
ALTER TABLE curriculums ADD COLUMN updated_at TIMESTAMP;
UPDATE curriculums SET updated_at = created_at;
ALTER TABLE curriculums ALTER COLUMN updated_at SET NOT NULL;

-- badge_levels: add created_at, updated_at
ALTER TABLE badge_levels ADD COLUMN created_at TIMESTAMP;
ALTER TABLE badge_levels ADD COLUMN updated_at TIMESTAMP;
UPDATE badge_levels SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE badge_levels ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE badge_levels ALTER COLUMN updated_at SET NOT NULL;

-- lesson_topics: add created_at, updated_at
ALTER TABLE lesson_topics ADD COLUMN created_at TIMESTAMP;
ALTER TABLE lesson_topics ADD COLUMN updated_at TIMESTAMP;
UPDATE lesson_topics SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE lesson_topics ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE lesson_topics ALTER COLUMN updated_at SET NOT NULL;

-- lesson_questions: add created_at, updated_at
ALTER TABLE lesson_questions ADD COLUMN created_at TIMESTAMP;
ALTER TABLE lesson_questions ADD COLUMN updated_at TIMESTAMP;
UPDATE lesson_questions SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE lesson_questions ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE lesson_questions ALTER COLUMN updated_at SET NOT NULL;

-- hint_notes: add created_at, updated_at
ALTER TABLE hint_notes ADD COLUMN created_at TIMESTAMP;
ALTER TABLE hint_notes ADD COLUMN updated_at TIMESTAMP;
UPDATE hint_notes SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE hint_notes ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE hint_notes ALTER COLUMN updated_at SET NOT NULL;

-- users: add updated_at
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;
UPDATE users SET updated_at = created_at;
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;

-- user_profiles: add created_at
ALTER TABLE user_profiles ADD COLUMN created_at TIMESTAMP;
UPDATE user_profiles SET created_at = updated_at;
ALTER TABLE user_profiles ALTER COLUMN created_at SET NOT NULL;

-- user_curriculums: add created_at
ALTER TABLE user_curriculums ADD COLUMN created_at TIMESTAMP;
UPDATE user_curriculums SET created_at = updated_at;
ALTER TABLE user_curriculums ALTER COLUMN created_at SET NOT NULL;

-- user_schedules: add created_at
ALTER TABLE user_schedules ADD COLUMN created_at TIMESTAMP;
UPDATE user_schedules SET created_at = updated_at;
ALTER TABLE user_schedules ALTER COLUMN created_at SET NOT NULL;

-- user_schedule_days: add created_at, updated_at
ALTER TABLE user_schedule_days ADD COLUMN created_at TIMESTAMP;
ALTER TABLE user_schedule_days ADD COLUMN updated_at TIMESTAMP;
UPDATE user_schedule_days SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE user_schedule_days ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE user_schedule_days ALTER COLUMN updated_at SET NOT NULL;

-- tutoring_sessions: add updated_at
ALTER TABLE tutoring_sessions ADD COLUMN updated_at TIMESTAMP;
UPDATE tutoring_sessions SET updated_at = created_at;
ALTER TABLE tutoring_sessions ALTER COLUMN updated_at SET NOT NULL;

-- session_topic_snapshots: add created_at, updated_at
ALTER TABLE session_topic_snapshots ADD COLUMN created_at TIMESTAMP;
ALTER TABLE session_topic_snapshots ADD COLUMN updated_at TIMESTAMP;
UPDATE session_topic_snapshots SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP;
ALTER TABLE session_topic_snapshots ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE session_topic_snapshots ALTER COLUMN updated_at SET NOT NULL;

-- coin_ledger_entries: add updated_at
ALTER TABLE coin_ledger_entries ADD COLUMN updated_at TIMESTAMP;
UPDATE coin_ledger_entries SET updated_at = created_at;
ALTER TABLE coin_ledger_entries ALTER COLUMN updated_at SET NOT NULL;

-- device_tokens: add created_at
ALTER TABLE device_tokens ADD COLUMN created_at TIMESTAMP;
UPDATE device_tokens SET created_at = updated_at;
ALTER TABLE device_tokens ALTER COLUMN created_at SET NOT NULL;

-- notification_schedules: add updated_at
ALTER TABLE notification_schedules ADD COLUMN updated_at TIMESTAMP;
UPDATE notification_schedules SET updated_at = created_at;
ALTER TABLE notification_schedules ALTER COLUMN updated_at SET NOT NULL;
