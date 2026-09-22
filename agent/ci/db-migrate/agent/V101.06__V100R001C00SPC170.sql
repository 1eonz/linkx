-- 新增ai_agent_config表body_type字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_config' AND COLUMN_NAME = 'body_type');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_config ADD COLUMN body_type TINYINT DEFAULT 1 COMMENT ''Body参数类型。1：raw-json;2:raw-text;3:form-data''',
    'SELECT ''body_type column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新增ai_agent_config表end_flag字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_config' AND COLUMN_NAME = 'end_flag');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_config ADD COLUMN end_flag TINYINT NOT NULL DEFAULT 1 COMMENT ''是否有结束标识。0：无标识；1：有标识''',
    'SELECT ''end_flag column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;