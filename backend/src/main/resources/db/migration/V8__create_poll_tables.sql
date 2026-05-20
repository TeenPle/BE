CREATE TABLE IF NOT EXISTS poll
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_poll_post UNIQUE (post_id),
    CONSTRAINT fk_poll_post FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE IF NOT EXISTS poll_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poll_id BIGINT NOT NULL,
    text VARCHAR(100) NOT NULL,
    display_order INT NOT NULL,
    vote_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_poll_option_poll_id (poll_id),
    CONSTRAINT fk_poll_option_poll FOREIGN KEY (poll_id) REFERENCES poll (id)
);

CREATE TABLE IF NOT EXISTS poll_vote (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poll_id BIGINT NOT NULL,
    poll_option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_poll_vote_poll_user UNIQUE (poll_id, user_id),
    INDEX idx_poll_vote_poll_id (poll_id),
    INDEX idx_poll_vote_option_id (poll_option_id),
    INDEX idx_poll_vote_user_id (user_id),
    CONSTRAINT fk_poll_vote_poll FOREIGN KEY (poll_id) REFERENCES poll (id),
    CONSTRAINT fk_poll_vote_option FOREIGN KEY (poll_option_id) REFERENCES poll_option (id),
    CONSTRAINT fk_poll_vote_user FOREIGN KEY (user_id) REFERENCES user (id)
);
