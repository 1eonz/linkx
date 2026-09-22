alter table ai_agent_config
    add column is_restricted int null default 0 comment '是否涉密 0 否 1 是';