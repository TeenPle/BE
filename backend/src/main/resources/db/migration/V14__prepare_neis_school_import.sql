SET @school_name_unique_index := (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'school'
      AND column_name = 'name'
      AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 1
    LIMIT 1
);

SET @drop_school_name_unique_sql := IF(
    @school_name_unique_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE school DROP INDEX `', REPLACE(@school_name_unique_index, '`', '``'), '`')
);

PREPARE drop_school_name_unique_stmt FROM @drop_school_name_unique_sql;
EXECUTE drop_school_name_unique_stmt;
DEALLOCATE PREPARE drop_school_name_unique_stmt;

SET @school_name_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'school'
      AND index_name = 'idx_school_name'
);

SET @add_school_name_index_sql := IF(
    @school_name_index_exists > 0,
    'SELECT 1',
    'ALTER TABLE school ADD INDEX idx_school_name (name)'
);

PREPARE add_school_name_index_stmt FROM @add_school_name_index_sql;
EXECUTE add_school_name_index_stmt;
DEALLOCATE PREPARE add_school_name_index_stmt;

SET @school_neis_unique_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'school'
      AND index_name = 'uq_school_neis_codes'
);

SET @add_school_neis_unique_sql := IF(
    @school_neis_unique_exists > 0,
    'SELECT 1',
    'ALTER TABLE school ADD CONSTRAINT uq_school_neis_codes UNIQUE (neis_office_code, neis_school_code)'
);

PREPARE add_school_neis_unique_stmt FROM @add_school_neis_unique_sql;
EXECUTE add_school_neis_unique_stmt;
DEALLOCATE PREPARE add_school_neis_unique_stmt;
