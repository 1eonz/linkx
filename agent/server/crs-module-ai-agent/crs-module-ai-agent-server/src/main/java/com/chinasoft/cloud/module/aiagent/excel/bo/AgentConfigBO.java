package com.chinasoft.cloud.module.aiagent.excel.bo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = false)
public class AgentConfigBO extends AgentCommonBO implements Serializable {
    /**
     * 智能体名称
     */
    @ExcelProperty("智能体名称")
    private String name;

    /**
     * 请求方法
     */
    @ExcelProperty("请求方法")
    private String httpMethod;

    /**
     * 服务地址
     */
    @ExcelProperty("服务地址")
    private String url;

    /**
     * 结果脚本
     */
    @ExcelProperty("结果脚本")
    private String respScript;

    /**
     * 关联用户（虚拟用户名称）
     * 备注：参考新增配置逻辑，导入后建立智能体与虚拟用户的绑定关系；为空表示不关联
     */
    @ExcelProperty("关联用户")
    private String virtualUser;

    /**
     * 是否接收IM消息
     * 备注：存储数据库时，需要适配{@link AgentConfig#receiveIm}
     */
    @ExcelProperty("是否接收IM消息")
    private String receiveImStr;

    /**
     * 智能体作用域
     * 备注：存储数据库时，需要适配{@link AgentConfig#scope}
     */
    @ExcelProperty("智能体作用域")
    private String scopeStr;

    /**
     * 优先级
     * 备注：存储数据库时，需要适配{@link AgentConfig#priority}
     */
    @ExcelProperty("优先级")
    private String priorityStr;

    /**
     * 音频支持文件格式
     * 备注：存储数据库时，需要适配{@link AgentConfig#audio}, {@link AgentConfig#audioType}
     */
    @ExcelProperty("音频支持文件格式")
    private String audioTypeStr;

    /**
     * 视频支持文件格式
     * 备注：存储数据库时，需要适配{@link AgentConfig#video}, {@link AgentConfig#videoType}
     */
    @ExcelProperty("视频支持文件格式")
    private String videoTypeStr;

    /**
     * 图片支持文件格式
     * 备注：存储数据库时，需要适配{@link AgentConfig#image}, {@link AgentConfig#imageType}
     */
    @ExcelProperty("图片支持文件格式")
    private String imageTypeStr;

    /**
     * 文档支持文件格式
     * 备注：存储数据库时，需要适配{@link AgentConfig#document}, {@link AgentConfig#documentType}
     */
    @ExcelProperty("文档支持文件格式")
    private String documentTypeStr;

    /**
     * 文件上传接口
     * 备注：存储数据库时，需要适配{@link AgentConfig#fileInterfaceId}
     */
    @ExcelProperty("文件上传接口")
    private String fileInterface;

    /**
     * 智能体说明
     */
    @ExcelProperty("智能体说明")
    private String desc;

    /**
     * 智能体分类
     * 备注：存储数据库时，需要适配{@link AgentConfig#categoryIds}
     */
    @ExcelProperty("智能体分类")
    private String categories;
}