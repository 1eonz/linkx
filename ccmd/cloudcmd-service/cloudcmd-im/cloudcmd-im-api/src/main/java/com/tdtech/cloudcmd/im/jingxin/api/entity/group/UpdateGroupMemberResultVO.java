package com.tdtech.cloudcmd.im.jingxin.api.entity.group;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "更新群组成员结果")
public class UpdateGroupMemberResultVO implements Serializable {

    @Schema(description = "IM 返回的错误码，0 表示成功")
    private Integer code = 0;

    @Schema(description = "IM 返回的错误描述，成功时为空")
    private String msg;

    @Schema(description = "群组ID")
    private Long groupId;

    @Schema(description = "操作人用户标识（警信用户ID）")
    private String operatorUserIdentity;

    @Schema(description = "成员操作结果列表")
    private List<MemberResult> memberResults;

    @Data
    @Schema(description = "成员操作结果")
    public static class MemberResult implements Serializable {

        @Schema(description = "该成员的 IM 错误码，0 表示成功")
        private Integer code;

        @Schema(description = "错误描述")
        private String msg;

        @Schema(description = "用户标识（警信用户ID或身份证号）")
        private String userIdentity;
    }
}
