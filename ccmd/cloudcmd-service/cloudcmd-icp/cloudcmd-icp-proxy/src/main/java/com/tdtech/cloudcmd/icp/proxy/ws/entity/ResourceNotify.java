package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tdtech.cloudcmd.util.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Schema(description = "资源通知信息")
public class ResourceNotify {

    @JsonProperty("isdn")
    @Schema(description = "ISDN号码", example = "123456789")
    private String isdn;

    @JsonProperty("cmd")
    @Schema(description = "命令", example = "status")
    private String cmd;

    @JsonProperty("seq")
    @Schema(description = "序列号", example = "1")
    private Long seq;

    @JsonProperty("rid")
    @Schema(description = "资源ID", example = "resource001")
    private String rid;

    @JsonProperty("rsp")
    @Schema(description = "响应", example = "ok")
    private String rsp;

    @JsonProperty("session")
    @Schema(description = "会话", example = "session001")
    private String session;

    @JsonProperty("opt")
    @Schema(description = "操作", example = "add")
    private String opt;

    @JsonProperty("from")
    @Schema(description = "来源", example = "sender001")
    private String from;

    @JsonProperty("to")
    @Schema(description = "目标", example = "receiver001")
    private String to;

    @JsonProperty("attaching")
    @Schema(description = "附加信息", example = "attachment")
    private String attaching;

    @JsonProperty("statustype")
    @Schema(description = "状态类型", example = "online")
    private String statustype;

    @JsonProperty("statusvalue")
    @Schema(description = "状态值", example = "active")
    private String statusvalue;

    @JsonProperty("value")
    @Schema(description = "值对象")
    private Value value;

    @JsonProperty("list")
    @Schema(description = "状态列表")
    private List<Status> list;

    public List<Status> normalizeToStatus() {
        if (list == null || list.isEmpty()) {
            if (StringUtils.isNotBlank(statustype)) {
                Status sdkStatusListValue = new Status();
                sdkStatusListValue.setIsdn(rid);
                sdkStatusListValue.setStatusType(statustype);
                sdkStatusListValue.setStatusValue(statusvalue);
                sdkStatusListValue.setSeq(seq);
                return Collections.singletonList(sdkStatusListValue);
            } else {
                return Collections.emptyList();
            }
        } else if (StringUtils.isNotBlank(statustype)) {
            Status sdkStatusListValue = new Status();
            sdkStatusListValue.setIsdn(rid);
            sdkStatusListValue.setStatusType(statustype);
            sdkStatusListValue.setStatusValue(statusvalue);
            list.add(sdkStatusListValue);
            list.forEach(a -> a.setSeq(seq));
            return list;
        } else {
            list.forEach(a -> a.setSeq(seq));
            return list;
        }
    }

    @Getter
    @Setter
    @Schema(description = "值对象")
    public static class Value {
        // 2：点呼。
        // 3：视频点呼。
        // 4：视频回传。
        // 5：视频分发。
        // 6：视频上墙。
        // 8：环境侦听。
        // 9：半双工点呼。
        @JsonProperty("calltype")
        @Schema(description = "呼叫类型", example = "2")
        private String callType;
        
        @Schema(description = "方向", example = "incoming")
        private String direction;
        
        @Schema(description = "加密", example = "true")
        private String encrypt;
        
        @Schema(description = "ISDN", example = "123456789")
        private String isdn;
        
        @JsonProperty("peerid")
        @Schema(description = "对端ID", example = "peer001")
        private String peerId;
        
        @JsonProperty("speakid")
        @Schema(description = "说话人ID", example = "speaker001")
        private String speakId;
    }

    @Getter
    @Setter
    @ToString
    @Schema(description = "状态对象")
    public static class Status {

        @JsonProperty("statusvalue")
        @Schema(description = "状态值", example = "online")
        private String statusValue;

        @JsonProperty("statustype")
        @Schema(description = "状态类型", example = "call")
        private String statusType;

        @JsonProperty("attaching")
        @Schema(description = "附加信息", example = "extra")
        private String attaching;

        @JsonProperty("isdn")
        @Schema(description = "ISDN", example = "123456789")
        private String isdn;

        @JsonProperty("seq")
        @Schema(description = "序列号", example = "1")
        private Long seq;

    }
}