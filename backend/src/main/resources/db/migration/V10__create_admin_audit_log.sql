CREATE TABLE IF NOT EXISTS admin_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500),
    metadata TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_admin_audit_admin_created (admin_id, created_at),
    INDEX idx_admin_audit_target (target_type, target_id),
    INDEX idx_admin_audit_action_created (action, created_at),
    CONSTRAINT fk_admin_audit_log_admin
        FOREIGN KEY (admin_id) REFERENCES `user` (id)
);
