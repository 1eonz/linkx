package com.tdtech.cloudcmd.im.jingxin.client.ai;

import lombok.Data;

import java.util.List;

/**
 * ai-agent 核查记录游标分页返回结果（对应 ai-agent 服务侧 AgentRecordCursorVO）。
 * <p>
 * 仅含统计同步所需字段：源表 id、身份证号、user_name、部门、核查时间。
 * <p>
 * 时间字段统一用 Long（epoch 毫秒）承载：ai-agent 服务端返回的是毫秒时间戳，
 * 用 LocalDateTime 反序列化会因缺少时区信息报 MismatchedInputException，
 * 故在 DTO 层保留原始数值，由调用方按系统时区转 Date。
 */
@Data
public class AgentRecordCursorResult {

    private List<AgentRecordItem> list;

    private Long lastTime;

    private Long lastId;

    private Boolean hasMore;

    @Data
    public static class AgentRecordItem {
        private Long id;
        private String userName;
        private String identityCardNumber;
        private String departmentId;
        private String departmentName;
        private Long time;
    }
}