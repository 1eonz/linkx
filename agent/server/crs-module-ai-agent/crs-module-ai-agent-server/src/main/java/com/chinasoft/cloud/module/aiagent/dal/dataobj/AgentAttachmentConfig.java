package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * AI智能体文件上传接口配置实体
 */
@TableName("ai_agent_attachement_config")
@KeySequence("ai_agent_attachement_config")
@Data
@EqualsAndHashCode
public class AgentAttachmentConfig {

    @TableId
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 请求方式
     */
    private String method;

    /**
     * 服务器IP地址
     */
    private String ip;

    /**
     * 服务器端口
     */
    private Integer port;

    /**
     * 服务URI路径
     */
    private String uri;

    /**
     * header参数（JSON格式）
     */
    private String header;

    /**
     * query参数（JSON格式）
     */
    private String query;

    /**
     * body参数（JSON格式）
     */
    private String body;

    /**
     * 文件标识字段（响应中提取文件ID的字段名）
     */
    private String reponseFileFiled;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 更新人
     */
    private String updater;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Boolean deleted;

    /**
     * 描述
     */
    @TableField("`desc`")
    private String desc;

    /**
     * 获取响应文件标识字段名
     */
    public String getResponseFileField() {
        return StringUtils.defaultIfBlank(reponseFileFiled, "id");
    }

    public String buildFullUrl() {
        if (StringUtils.isNotBlank(uri) && (uri.startsWith("http://") || uri.startsWith("https://"))) {
            return uri;
        }
        StringBuilder url = new StringBuilder();
        if (StringUtils.isNotBlank(ip)) {
            url.append("http://").append(ip);
            if (port != null && port > 0) {
                url.append(":").append(port);
            }
        }
        if (StringUtils.isNotBlank(uri)) {
            if (!uri.startsWith("/") && !url.isEmpty()) {
                url.append("/");
            }
            url.append(uri);
        }
        return url.toString();
    }
}
