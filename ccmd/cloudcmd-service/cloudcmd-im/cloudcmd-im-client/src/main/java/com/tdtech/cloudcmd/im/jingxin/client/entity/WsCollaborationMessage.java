package com.tdtech.cloudcmd.im.jingxin.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class WsCollaborationMessage {
    @NotNull
    @JsonProperty("userId")
    private Long postId;

    @NotNull
    private Integer operType;

    //没啥用
    @JsonProperty("realUserId")
    private Long supportUserId;

    @NotNull
    private Long groupId;

}
