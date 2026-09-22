alter table ai_agent_config
    add column type tinyint not null default 1;

insert into ai_agent_config
(id, name, url, token, priority, avatar, creator, updater, create_time, update_time, deleted, `desc`, type)
values (1, '海致', 'http://20.232.1.65:12602/v1/chat-messages', 'app-VBGSjiwwDAYuTE1x8ahQXSog', 999, '', '', '',
        now(), now(), 0, '', 0);

ALTER TABLE `agent`.`ai_agent_config`
    ADD COLUMN `category_ids` varchar(255) NULL COMMENT '分类ID' AFTER `type`;

DROP TABLE IF EXISTS `ai_agent_category`;
CREATE TABLE `ai_agent_category`
(
    `id`          bigint(0)                                                     NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL     DEFAULT NULL COMMENT '名称',
    `creator`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL,
    `updater`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NULL     DEFAULT NULL,
    `create_time` datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time` datetime(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    `deleted`     tinyint(0)                                                    NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1976471273947584515
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI代理配置分类表'
  ROW_FORMAT = Dynamic;

INSERT INTO `agent`.`ai_agent_category`(`id`, `name`, `creator`, `updater`, `create_time`, `update_time`, `deleted`)
VALUES (1979064194893023233, '精选', NULL, NULL, '2025-10-17 13:57:28', '2025-10-17 14:09:50', 0);


DROP TABLE IF EXISTS `ai_globals`;
CREATE TABLE `ai_globals`
(
    `id`           bigint(0)                                               NOT NULL COMMENT '主键id',
    `name`         varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '变量名称',
    `value`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '值',
    `remark`       varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL     DEFAULT '' COMMENT '备注',
    `status`       tinyint(0)                                              NOT NULL DEFAULT 0 COMMENT '状态 0-可用 1-禁用',
    `gmt_created`  datetime(0)                                             NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `gmt_modified` datetime(0)                                             NULL     DEFAULT CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_name` (`name`) USING BTREE COMMENT '根据变量名称获取变量值'
) ENGINE = InnoDB
  CHARACTER SET = utf8
  COLLATE = utf8_general_ci COMMENT = '全局变量信息表'
  ROW_FORMAT = Dynamic;

INSERT INTO `agent`.`ai_globals`(`id`, `name`, `value`, `remark`, `status`, `gmt_created`, `gmt_modified`)
VALUES (1, 'MSIP_HOST', 'https://apisix-gateway.platform.svc:9444', 'MSIP上报地址', 0, '2025-11-12 16:23:25', '2025-11-12 16:23:25');



