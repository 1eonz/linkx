package com.tdtech.cloudcmd.linkx.third.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class AppUsedDto {
    /**
     * 应用ID
     */
    @NotNull(message = "应用ID不能为空")
    private Long appId;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    private Long userId;

    /**
     * 打开的端侧。1：app；2：BS PC；3：CS PC
     */
    @NotNull(message = "打开的端侧不能为空")
    private Integer client;

//    /**
//     * 应用打开时间
//     */
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime time;
}
