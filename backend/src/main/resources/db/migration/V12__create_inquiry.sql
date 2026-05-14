CREATE TABLE inquiry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    admin_answer VARCHAR(2000) NULL,
    answered_by BIGINT NULL,
    answered_at DATETIME(6) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_inquiry_user_created (user_id, created_at),
    INDEX idx_inquiry_status_created (status, created_at),
    CONSTRAINT fk_inquiry_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_inquiry_answered_by FOREIGN KEY (answered_by) REFERENCES `user` (id)
);
