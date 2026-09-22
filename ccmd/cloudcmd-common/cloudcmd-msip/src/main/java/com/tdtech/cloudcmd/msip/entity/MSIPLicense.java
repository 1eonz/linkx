package com.tdtech.cloudcmd.msip.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * License授权类
 */
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MSIPLicense {

    // 没有接口文档，无法标注字段备注
    @Schema(description = "lsn")
    private String lsn;

    @Schema(description = "productName")
    private String productName;

    @Schema(description = "esn")
    private String esn;

    @Schema(description = "licType")
    private String licType;

    @Schema(description = "expireDate")
    private String expireDate;

    @Schema(description = "证书状态：0 未激活，1 激活，2 即将过期，3 已过期，4 失效可试用，5 失效")
    // 证书状态：0 未激活，1 激活，2 即将过期，3 已过期，4 失效可试用，5 失效
    private String status;

    @Schema(description = "firstActDate")
    private String firstActDate;

    @Schema(description = "disableDate")
    private String disableDate;

    @Schema(description = "revokeCode")
    private String revokeCode;

    @Schema(description = "授权信息")
    private List<MSIPLicenseItem> itemList;

    @Schema(description = "secretID")
    private String secretID;

    @Schema(description = "startTime")
    private String startTime;

    /**
     * license是否可用
     * @return true：可用，false：不可用
     */
    public boolean available() {
        // 状态可用
        boolean isLegalStatus = Arrays.asList(1, 2, 4).contains(Integer.valueOf(this.getStatus()));
        Date parseDate = DateFormatUtil.parseDate(this.getExpireDate());
        Date now = new Date();
        // 日期正常
        boolean isLegalDate = parseDate.after(now);
        return isLegalStatus && isLegalDate;
    }

}
