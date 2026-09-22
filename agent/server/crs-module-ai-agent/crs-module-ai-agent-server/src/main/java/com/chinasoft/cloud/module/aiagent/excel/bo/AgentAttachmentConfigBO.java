package com.chinasoft.cloud.module.aiagent.excel.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = false)
public class AgentAttachmentConfigBO extends AgentCommonBO implements Serializable {
    /**
     * 名称
     */
    @ExcelProperty(value = "名称")
    private String name;

    /**
     * 请求方式
     */
    @ExcelProperty("请求方式")
    private String method;

    /**
     * IP
     */
    @ExcelProperty("IP")
    private String ip;

    /**
     * 端口
     */
    @ExcelProperty("端口")
    private Integer port;

    /**
     * 路径
     */
    @ExcelProperty("路径")
    private String uri;

    /**
     * 文件标识字段
     */
    @ExcelProperty("文件标识字段")
    private String reponseFileFiled;

    /**
     * 描述
     */
    @ExcelProperty("描述")
    private String desc;
}
