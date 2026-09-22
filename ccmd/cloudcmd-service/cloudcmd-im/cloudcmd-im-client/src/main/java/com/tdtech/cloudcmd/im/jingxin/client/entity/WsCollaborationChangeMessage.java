package com.tdtech.cloudcmd.im.jingxin.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
public class WsCollaborationChangeMessage {

    @NotNull
    private Long groupId;

    @NotNull
    private Long operateId;

    @NotNull
    private Integer operationType;

    @NotNull
    private Long tag;

    /**
     * 群组资料变更时存在
     */
    private NewGroupProfile newGroupProfile;

    @NotNull
    private List<WsCollaborationChangeMember> memberList;

    /**
     * 群公告变更时存在
     */
    private Object noticeDto;

    /**
     * 群未决变更时存在
     */
    private Object groupApplications;

}
