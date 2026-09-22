SET @stmt = (SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE ai_agent_query_approve ADD COLUMN to_leader_url VARCHAR(1024) DEFAULT NULL COMMENT ''给审批领导发送卡片的URL'' AFTER approve_detail_url',
    'SELECT ''ai_agent_query_approve.to_leader_url exists'''
)
             FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'ai_agent_query_approve'
               AND COLUMN_NAME = 'to_leader_url');
PREPARE s1 FROM @stmt;
EXECUTE s1;
DEALLOCATE PREPARE s1;
