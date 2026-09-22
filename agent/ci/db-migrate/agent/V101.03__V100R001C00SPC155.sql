ALTER TABLE `ai_agent_config`
    ADD COLUMN `audio` TINYINT(1) DEFAULT 0 COMMENT '音频支持能力 0-不支持 1-支持',
    ADD COLUMN `audio_type` JSON DEFAULT NULL COMMENT '支持的音频文件格式，如[".mp3",".aac",".pcm",".wav",".amr"]',
    ADD COLUMN `video` TINYINT(1) DEFAULT 0 COMMENT '视频支持能力 0-不支持 1-支持',
    ADD COLUMN `video_type` JSON DEFAULT NULL COMMENT '支持的视频文件格式，如[".mp4",".mov",".webm"]',
    ADD COLUMN `image` TINYINT(1) DEFAULT 0 COMMENT '图片支持能力 0-不支持 1-支持',
    ADD COLUMN `image_type` JSON DEFAULT NULL COMMENT '支持的图片格式，如[".jpg",".jpeg",".gif",".png",".bmp",".webp"]',
    ADD COLUMN `document` TINYINT(1) DEFAULT 0 COMMENT '文档支持能力 0-不支持 1-支持',
    ADD COLUMN `document_type` JSON DEFAULT NULL COMMENT '支持的文档格式，如[".md",".doc",".docx",".pdf",".xlsx",".xls",".ppt",".pptx",".txt",".html"]',
    ADD COLUMN `file_interface_id` BIGINT DEFAULT NULL COMMENT '文件上传接口配置ID，关联 ai_agent_attachement_config.id';

CREATE TABLE `ai_agent_attachement_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) DEFAULT NULL COMMENT '名称',
    `method` VARCHAR(255) DEFAULT NULL COMMENT '请求方式',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '服务器IP地址',
    `port` INT DEFAULT NULL COMMENT '服务器端口',
    `uri` VARCHAR(1024) DEFAULT NULL COMMENT '服务URI路径',
    `header` JSON DEFAULT NULL COMMENT 'header参数',
    `query` JSON DEFAULT NULL COMMENT 'query参数',
    `body` JSON DEFAULT NULL COMMENT 'body参数',
    `reponse_file_filed` VARCHAR(100) DEFAULT NULL COMMENT '文件标识字段',
    `creator` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `updater` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    `desc` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI智能体前置文件上传接口';

ALTER TABLE `ai_agent_record`
    ADD COLUMN `attachement` VARCHAR(255) DEFAULT NULL COMMENT '文件标识',
    ADD COLUMN `attachement_path` VARCHAR(255) DEFAULT NULL COMMENT '文件资源路径';

INSERT IGNORE INTO agent.ai_globals (`id`, `name`, `value`, `remark`, `status`, `gmt_created`, `gmt_modified`) VALUES (2, 'LINKX_HOST', '192.168.1.101:30843', 'Linkx服务地址', 0, now(), now());
INSERT IGNORE INTO agent.ai_globals (`id`, `name`, `value`, `remark`, `status`, `gmt_created`, `gmt_modified`) VALUES (3, 'LINKX_CLIENT_ID', 'agent', 'Linkx登录账号', 0, now(), now());
INSERT IGNORE INTO agent.ai_globals (`id`, `name`, `value`, `remark`, `status`, `gmt_created`, `gmt_modified`) VALUES (4, 'LINKX_CLIENT_SECRET', 'ljCckuoWS', 'Linkx登录密码', 0, now(), now());
