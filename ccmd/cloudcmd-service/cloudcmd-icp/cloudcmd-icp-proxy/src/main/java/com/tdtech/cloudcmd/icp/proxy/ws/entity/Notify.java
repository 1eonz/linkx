package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@Schema(description = "通用通知信息")
public class Notify<T> {
    @Schema(description = "操作类型", example = "add")
    private String opt;
    
    @Schema(description = "命令类型", example = "notify")
    private String cmd;
    
    @Schema(description = "数据列表")
    private List<T> list;
    
    @Schema(description = "单个数据")
    private T value;
}
