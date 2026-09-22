update ai_agent_config set name='海智' where id = 1 and name = '海致';

alter table ai_agent_record
    add department_id varchar(64) null;

alter table ai_agent_record
    add department_code varchar(64) null;

alter table ai_agent_record
    add department_name varchar(64) null;
