SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_config ADD COLUMN http_method VARCHAR(255) NULL COMMENT ''请求方式'' AFTER name',
                   'SELECT ''ai_agent_config.http_method exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_config'
      AND COLUMN_NAME = 'http_method'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_config ADD COLUMN header JSON NULL COMMENT ''header参数'' AFTER url',
                   'SELECT ''ai_agent_config.header exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_config'
      AND COLUMN_NAME = 'header'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_config ADD COLUMN `query` JSON NULL COMMENT ''query参数'' AFTER header',
                   'SELECT ''ai_agent_config.query exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_config'
      AND COLUMN_NAME = 'query'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_config ADD COLUMN body JSON NULL COMMENT ''body参数'' AFTER `query`',
                   'SELECT ''ai_agent_config.body exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_config'
      AND COLUMN_NAME = 'body'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_record ADD COLUMN response_content TEXT NULL COMMENT ''响应内容'' AFTER query_content',
                   'SELECT ''ai_agent_record.response_content exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_record'
      AND COLUMN_NAME = 'response_content'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_record ADD COLUMN reply_position INT NOT NULL DEFAULT ''0'' COMMENT ''回答字符位置，-1表示全部回答完成'' AFTER response_content',
                   'SELECT ''ai_agent_record.reply_position exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_record'
      AND COLUMN_NAME = 'reply_position'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_record ADD COLUMN reply_paused TINYINT NOT NULL DEFAULT ''0'' COMMENT ''回答是否暂停'' AFTER reply_position',
                   'SELECT ''ai_agent_record.reply_paused exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_record'
      AND COLUMN_NAME = 'reply_paused'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_record ADD COLUMN deleted TINYINT NOT NULL DEFAULT ''0'' COMMENT ''删除标志'' AFTER agent_config_id',
                   'SELECT ''ai_agent_record.deleted exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_record'
      AND COLUMN_NAME = 'deleted'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_record'
      AND INDEX_NAME = 'idx_ai_agent_record_user_agent_latest'
);

SET @ddl := IF(@index_exists = 0,
               'CREATE INDEX idx_ai_agent_record_user_agent_latest ON ai_agent_record(identity_card_number, deleted, agent_config_id, time DESC, id DESC)',
               'SELECT ''idx_ai_agent_record_user_agent_latest exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS ai_agent_query_approve
(
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    record_id VARCHAR(255) DEFAULT NULL COMMENT 'AI问答记录ID, 引用自ai_agent_record.id',
    approve_no VARCHAR(255) DEFAULT NULL COMMENT '审批单号',
    approve_url VARCHAR(255) DEFAULT NULL COMMENT '审批单据H5 URL地址',
    approve_state VARCHAR(1024) DEFAULT NULL COMMENT '审批状态',
    approve TINYINT DEFAULT NULL COMMENT '审批结果。0：通过、1：不通过',
    approve_description VARCHAR(1024) DEFAULT NULL COMMENT '审批描述',
    approve_time VARCHAR(1024) DEFAULT NULL COMMENT '审批时间',
    approve_user VARCHAR(1024) DEFAULT NULL COMMENT '审批人',
    PRIMARY KEY (id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_0900_ai_ci
    COMMENT='AI问答审批关联信息表';

SET @sql = (
    SELECT IF(
                   COUNT(*) = 0,
                   'ALTER TABLE ai_agent_query_approve ADD COLUMN approve_detail_url VARCHAR(255) DEFAULT NULL COMMENT ''审批详情H5 URL地址'' AFTER approve_url',
                   'SELECT ''ai_agent_query_approve.approve_detail_url exists'''
           )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_query_approve'
      AND COLUMN_NAME = 'approve_detail_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_query_approve'
      AND INDEX_NAME = 'idx_ai_agent_query_approve_record_id'
);

SET @ddl := IF(@index_exists = 0,
               'CREATE UNIQUE INDEX idx_ai_agent_query_approve_record_id ON ai_agent_query_approve(record_id)',
               'SELECT ''idx_ai_agent_query_approve_record_id exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

