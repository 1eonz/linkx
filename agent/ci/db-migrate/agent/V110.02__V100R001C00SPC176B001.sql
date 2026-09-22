-- ============================================================
-- 本批次新增 IM 通道接入相关字段
-- 1. ai_agent_config: receive_im / scope
-- 2. ai_agent_record: im_session_id / ask_type 注释补充
-- ============================================================

-- 新增 ai_agent_config 表 receive_im 字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_config' AND COLUMN_NAME = 'receive_im');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_config ADD COLUMN receive_im TINYINT DEFAULT 0 COMMENT ''是否接收IM消息。0：不接收；1：接收'' AFTER is_restricted',
    'SELECT ''receive_im column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新增 ai_agent_config 表 scope 字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_config' AND COLUMN_NAME = 'scope');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_config ADD COLUMN scope TINYINT DEFAULT 1 COMMENT ''智能体的作用域。0：所有；1：仅作用于AI智能体问答；2：仅作用于IM'' AFTER receive_im',
    'SELECT ''scope column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 新增 ai_agent_record 表 im_session_id 字段
SET @exists = (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_record' AND COLUMN_NAME = 'im_session_id');
SET @sql = IF(@exists = 0,
    'ALTER TABLE ai_agent_record ADD COLUMN im_session_id BIGINT DEFAULT NULL COMMENT ''IM会话ID''',
    'SELECT ''im_session_id column already exists, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 更新 ai_agent_record 表 ask_type 字段注释（补充 3-单聊智能体）
SET @current_comment = (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_agent_record' AND COLUMN_NAME = 'ask_type');
SET @sql = IF(@current_comment = '提问类型：1-智能体ai助手提问，2-@群ai助手提问',
    'ALTER TABLE ai_agent_record MODIFY COLUMN ask_type INT DEFAULT 1 COMMENT ''提问类型：1-智能体ai助手提问，2-@群ai助手提问，3-单聊智能体''',
    'SELECT ''ask_type comment already updated or not matched, skipped''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;