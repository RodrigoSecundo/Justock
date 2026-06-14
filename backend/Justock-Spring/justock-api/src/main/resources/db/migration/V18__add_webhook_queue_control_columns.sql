ALTER TABLE eventos_webhook
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP NULL;

ALTER TABLE eventos_webhook
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;

UPDATE eventos_webhook
   SET attempt_count = 0
 WHERE attempt_count IS NULL;