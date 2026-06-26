-- Migrate seed curriculum unit prompt template from raw unit_json placeholder to formatted lesson_concepts.
-- Do not edit V7: Flyway checksum is immutable after apply.

UPDATE curriculum_units
SET system_prompt_template = REPLACE(
    REPLACE(system_prompt_template, '## 단원 정보', '## 수업 개념'),
    '{{unit_json}}',
    '{{lesson_concepts}}'
)
WHERE system_prompt_template LIKE '%{{unit_json}}%'
   OR system_prompt_template LIKE '%## 단원 정보%';