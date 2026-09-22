package com.tdtech.cloudcmd.im.jingxin.api.entity.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ToString
public class RecordCountReq implements Serializable {

    @Schema(description = "身份证号")
    private List<String> identityCardNumber;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "时间范围")
    private String startTime;

    @Schema(description = "时间范围")
    private String endTime;

    private String personName;

    private String category;

    private List<String> departmentCode;

    @NonNull
    private List<GroupEnum> group;

    public enum GroupEnum implements Serializable{
        agent, person, date
    }

}
