package com.chinasoft.cloud.module.aiagent.dal.mysql;

import com.chinasoft.cloud.module.aiagent.controller.app.vo.UserAgentRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.chinasoft.cloud.framework.mybatis.core.mapper.BaseMapperX;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentRecord;

import java.util.List;

@Mapper
public interface AgentRecordMapper extends BaseMapperX<AgentRecord> {

    // 先过滤该用户的记录，再按智能体分组取最新一条问答记录。
    @Select("""
        SELECT
            cfg.id AS agentId,
            cfg.id AS agentConfigId,
            cfg.name AS agentName,
            cfg.token AS token,
            cfg.avatar AS avatar,
            cfg.`desc` AS `desc`,
            cfg.priority AS priority,
            cfg.is_restricted AS isRestricted,
            latest_record.time AS latestTime,
            latest_record.id AS latestRecordId,
            latest_record.query_content AS latestQueryContent,
            latest_record.response_content AS latestResponseContent,
            latest_record.reply_position AS latestReplyPosition,
            latest_record.reply_paused AS latestReplyPaused
        FROM ai_agent_config cfg
        INNER JOIN (
            SELECT *
            FROM (
                SELECT
                    r.id,
                    r.agent_config_id,
                    r.query_content,
                    r.response_content,
                    r.reply_position,
                    r.reply_paused,
                    r.time,
                    ROW_NUMBER() OVER (PARTITION BY r.agent_config_id ORDER BY r.time DESC, r.id DESC) AS row_num
                FROM ai_agent_record r
                WHERE r.identity_card_number = #{userId}
                  AND r.deleted = 0
                  AND r.ask_type = 1 -- 1=ai助手提问
            ) ranked_record
            WHERE ranked_record.row_num = 1
        ) latest_record ON cfg.id = latest_record.agent_config_id
        WHERE cfg.deleted = 0
        ORDER BY latest_record.time DESC, latest_record.id DESC
        """)
    List<UserAgentRecordVO> selectLatestUserAgentRecords(@Param("userId") String userId);
}