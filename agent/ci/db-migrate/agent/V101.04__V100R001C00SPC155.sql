ALTER TABLE `ai_agent_query_approve`
    MODIFY COLUMN `approve_url` VARCHAR(1024) DEFAULT NULL COMMENT '审批单据H5 URL地址',
    MODIFY COLUMN `to_leader_url` VARCHAR(1024) DEFAULT NULL COMMENT '给审批领导发送卡片的URL',
    MODIFY COLUMN `approve_detail_url` VARCHAR(1024) DEFAULT NULL COMMENT '审批详情H5 URL地址';