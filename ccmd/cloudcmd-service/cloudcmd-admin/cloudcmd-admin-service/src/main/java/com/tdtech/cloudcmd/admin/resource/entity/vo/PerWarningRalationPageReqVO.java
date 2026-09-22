package com.tdtech.cloudcmd.admin.resource.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author: S063874
 * @date: 2026-01-13 14:49
 */
@Data
public class PerWarningRalationPageReqVO extends BasePageVO{

    private Integer classify;

    private Long businessId;

    private Integer targetType;

    private Long targetId;

    private String businessName;

    private String targetName;

    private String orgName;
}
