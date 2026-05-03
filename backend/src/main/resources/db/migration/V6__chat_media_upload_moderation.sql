ALTER TABLE media
    ADD COLUMN s3_key VARCHAR(500) NULL,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ATTACHED',
    ADD COLUMN moderation_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN moderation_labels TEXT NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN storage_deleted_at DATETIME(6) NULL;

ALTER TABLE media
    MODIFY url VARCHAR(1000) NOT NULL,
    MODIFY target_type VARCHAR(255) NULL,
    MODIFY target_id BIGINT NULL;

CREATE INDEX idx_media_status_deleted
    ON media (status, deleted_at);
