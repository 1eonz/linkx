UPDATE agent.ai_agent_config SET avatar=REPLACE(avatar,'/admin-api/infra/','/admin-api/proxy/ai/v1/infra/') WHERE avatar LIKE '%/admin-api/infra/%';

UPDATE agent.infra_file SET url=REPLACE(url,'/admin-api/infra/','/admin-api/proxy/ai/v1/infra/') WHERE url LIKE '%/admin-api/infra/%';