DROP TABLE if exists ai_agent_record;
CREATE TABLE ai_agent_record
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_name            VARCHAR(255) COMMENT '用户名',
    identity_card_number VARCHAR(255) COMMENT '身份证号',
    query_content        text COMMENT '查询内容',
    time                 DATETIME COMMENT '时间',
    agent_name           VARCHAR(255) COMMENT '智能体名称',
    agent_config_id      VARCHAR(255) COMMENT '智能体配置ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='统计信息表';

DROP TABLE if exists ai_agent_config;
CREATE TABLE `ai_agent_config`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(255)  NULL     DEFAULT NULL COMMENT '名称',
    `url`         VARCHAR(1024) NULL     DEFAULT NULL COMMENT '服务地址',
    `token`       VARCHAR(1024) NULL     DEFAULT NULL COMMENT '认证Token',
    `priority`    INT           NULL     DEFAULT NULL COMMENT '优先级',
    `avatar`      VARCHAR(1024) NULL     DEFAULT NULL COMMENT '头像地址',
    `creator`     varchar(64)   null     default null comment '',
    `updater`     varchar(64)   null     default null comment '',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT       NOT NULL DEFAULT '0' COMMENT '逻辑删除标志',
    `desc`        VARCHAR(255)  null     default null COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI代理配置表';

truncate table infra_file_config;

INSERT INTO agent.infra_file_config (id, name, storage, remark, master, config, creator, create_time, updater,
                                     update_time, deleted)
VALUES (1, '本地存储（示例）', 10, '', true,
        '{"@class":"com.chinasoft.cloud.module.infra.framework.file.core.client.local.LocalFileClientConfig","basePath":"/home/agent/upload","domain":""}',
        '1', now(), '1', now(), false);
