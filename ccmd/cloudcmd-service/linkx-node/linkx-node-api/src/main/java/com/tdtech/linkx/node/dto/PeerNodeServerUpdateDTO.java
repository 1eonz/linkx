package com.tdtech.linkx.node.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 更新服务端节点DTO
 */
@Data
public class PeerNodeServerUpdateDTO {

    @NotNull(message = "IP不能为空")
    private String ip;

    private String name;

    private String remark;

    private String tag;
}
