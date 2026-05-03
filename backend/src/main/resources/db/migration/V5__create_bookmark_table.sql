CREATE TABLE bookmark (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    post_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmark_user_post (user_id, post_id),
    INDEX idx_bookmark_user_id (user_id),
    INDEX idx_bookmark_post_id (post_id),
    CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);
