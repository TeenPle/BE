SET @schema_name = DATABASE();

SET @post_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = @schema_name
      AND table_name = 'post'
);

SET @post_comment_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'post'
      AND column_name = 'comment_count'
);

SET @sql = IF(
    @post_table_exists = 1 AND @post_comment_count_exists = 0,
    'ALTER TABLE post ADD COLUMN comment_count INT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @post_comment_count_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'post'
      AND column_name = 'comment_count'
);

SET @comment_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = @schema_name
      AND table_name = 'comment'
);

SET @sql = IF(
    @post_table_exists = 1 AND @comment_table_exists = 1 AND @post_comment_count_exists = 1,
    'UPDATE post p SET comment_count = (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id AND c.comment_status <> ''DELETED'')',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'post'
      AND index_name = 'idx_post_board_status_created'
);
SET @sql = IF(
    @post_table_exists = 1 AND @index_exists = 0,
    'CREATE INDEX idx_post_board_status_created ON post (board_id, post_status, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'post'
      AND index_name = 'idx_post_board_status_comment'
);
SET @sql = IF(
    @post_table_exists = 1 AND @post_comment_count_exists = 1 AND @index_exists = 0,
    'CREATE INDEX idx_post_board_status_comment ON post (board_id, post_status, comment_count)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @notification_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = @schema_name
      AND table_name = 'notification'
);
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'notification'
      AND index_name = 'idx_notification_user_read_created'
);
SET @sql = IF(
    @notification_table_exists = 1 AND @index_exists = 0,
    'CREATE INDEX idx_notification_user_read_created ON notification (user_id, is_read, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
