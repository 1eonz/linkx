-- 新增ai_agent_record表ask_type字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_record' AND COLUMN_NAME = 'ask_type');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_record ADD COLUMN ask_type INT DEFAULT 1 COMMENT ''提问类型：1-智能体ai助手提问，2-@群ai助手提问''',
    'SELECT ''ask_type column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;