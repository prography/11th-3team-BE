-- Link lesson_topics to curriculum_units so static UI and ai_loop teach share the same unit context.

ALTER TABLE lesson_topics
    ADD COLUMN curriculum_unit_id BIGINT REFERENCES curriculum_units (id);

UPDATE lesson_topics
SET curriculum_unit_id = (
    SELECT MIN(cu.id)
    FROM curriculum_units cu
    WHERE cu.curriculum_id = lesson_topics.curriculum_id
)
WHERE curriculum_unit_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM curriculum_units cu
    WHERE cu.curriculum_id = lesson_topics.curriculum_id
  );

ALTER TABLE lesson_topics
    ALTER COLUMN curriculum_unit_id SET NOT NULL;

CREATE INDEX idx_lesson_topics_curriculum_unit ON lesson_topics (curriculum_unit_id);