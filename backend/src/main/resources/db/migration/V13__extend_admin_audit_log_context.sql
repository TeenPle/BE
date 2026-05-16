ALTER TABLE admin_audit_log
    ADD COLUMN ip_address VARCHAR(45) NULL AFTER metadata,
    ADD COLUMN user_agent VARCHAR(500) NULL AFTER ip_address;
