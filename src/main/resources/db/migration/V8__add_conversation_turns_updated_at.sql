ALTER TABLE conversation_turns ADD COLUMN updated_at TIMESTAMP;
UPDATE conversation_turns SET updated_at = created_at;
ALTER TABLE conversation_turns ALTER COLUMN updated_at SET NOT NULL;