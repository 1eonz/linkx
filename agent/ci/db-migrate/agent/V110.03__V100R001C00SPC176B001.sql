-- ============================================================
-- 本脚本更新 ai_agent_record 表 ask_type 字段注释
-- 补充 4-群聊普通消息（非@），与实体 AgentRecord / 枚举 AgentRecordAskType 对齐
-- ============================================================

-- 更新 ai_agent_record 表 ask_type 字段注释（补充 4-群聊普通消息（非@））
SET @current_comment = (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_record' AND COLUMN_NAME = 'ask_type');
SET @sql = IF(@current_comment = '提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体',
    'ALTER TABLE ai_agent_record MODIFY COLUMN ask_type INT DEFAULT 1 COMMENT ''提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体，4-群聊普通消息（非@）''',
    'SELECT ''ask_type comment already updated or not matched, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;