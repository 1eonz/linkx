-- 新增ai_agent_record表approval_enabled字段
DELIMITER $$

-- 创建临时存储过程（如果不存在）
CREATE PROCEDURE BATCH_DELETE_PROC()
BEGIN
    -- ai_agent_record表添加approval_enabled字段（如果不存在）
    SET @tb_label_sql = NULL;
SELECT CONCAT('ALTER TABLE ai_agent_record ADD COLUMN `approval_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT \"是否开启审批。0：未开启，1：已开启\";')
INTO @tb_label_sql
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ai_agent_record'
  AND COLUMN_NAME = 'approval_enabled'
HAVING COUNT(1) = 0;

IF @tb_label_sql IS NOT NULL THEN
        PREPARE stmt FROM @tb_label_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;

    SET @tb_label_sql = NULL;
END$$

DELIMITER ;

CALL BATCH_DELETE_PROC();

DROP PROCEDURE BATCH_DELETE_PROC;

-- 新增ai_agent_record表approval_sub_mode字段
DELIMITER $$

-- 创建临时存储过程（如果不存在）
CREATE PROCEDURE BATCH_DELETE_PROC()
BEGIN
    -- ai_agent_record表添加approval_sub_mode字段（如果不存在）
    SET @tb_label_sql = NULL;
SELECT CONCAT('ALTER TABLE ai_agent_record ADD COLUMN `approval_sub_mode` VARCHAR (8) DEFAULT NULL COMMENT \"审批子模式。0：先问后审，1：先审后问\";')
INTO @tb_label_sql
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ai_agent_record'
  AND COLUMN_NAME = 'approval_sub_mode'
HAVING COUNT(1) = 0;

IF @tb_label_sql IS NOT NULL THEN
        PREPARE stmt FROM @tb_label_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;

    SET @tb_label_sql = NULL;
END$$

DELIMITER ;

CALL BATCH_DELETE_PROC();

DROP PROCEDURE BATCH_DELETE_PROC;