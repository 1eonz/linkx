-- 回填历史数据：将answer_time为空的记录，用time字段值填充answer_time
UPDATE ai_agent_record SET answer_time = `time` WHERE answer_time IS NULL;