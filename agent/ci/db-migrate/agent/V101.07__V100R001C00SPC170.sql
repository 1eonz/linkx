-- 新增ai_agent_record表answer_time字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_record' AND COLUMN_NAME = 'answer_time');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_record ADD COLUMN answer_time DATETIME DEFAULT NULL COMMENT ''AI响应的开始时间''',
    'SELECT ''answer_time column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;