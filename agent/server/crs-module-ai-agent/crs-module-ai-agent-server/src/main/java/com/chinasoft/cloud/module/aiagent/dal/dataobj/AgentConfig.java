package com.chinasoft.cloud.module.aiagent.dal.dataobj;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinasoft.cloud.framework.mybatis.core.dataobject.BaseDO;
import com.chinasoft.cloud.framework.tenant.core.aop.TenantIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@TableName("ai_agent_config")
@KeySequence("ai_agent_config") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@TenantIgnore
public class AgentConfig extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private String httpMethod;

    private String url;

    private String token;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String header;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String query;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String body;

    /**
    /**
     * body 类型，1:raw-json(默认); 2:raw-text; 3:form-data
     */
    private Integer bodyType;

    /**
     * 是否有结束标识，1:有(默认); 0:无
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Integer endFlag;

    private Integer priority;

    private String avatar;

    @TableField("`desc`")
    private String desc;

    private Integer type = 1;

    // 智能体分类ID
    private String categoryIds;
    // 是否涉密 0 否 1 是
    private Integer isRestricted;
    // 是否接收IM消息。0：不接收；1：接收
    private Integer receiveIm;
    // 智能体的作用域。0：所有；1：仅作用于AI智能体问答；2：仅作用于IM
    private Integer scope;

    private String paramScript;

    private String respScript;

    // ========== 多模态能力配置 =======
    private Integer audio;

    private String audioType;

    private Integer video;

    private String videoType;

    private Integer image;

    private String imageType;

    private Integer document;

    private String documentType;

    /**
     * 文件上传接口配置ID，关联 ai_agent_attachment_config.id
     */
    private Long fileInterfaceId;

    public List<Long> categoryIdList() {
        if (categoryIds == null || categoryIds.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(categoryIds.split(",")).filter(a -> !a.isBlank()).map(Long::parseLong).toList();
    }

    public List<String> getAudioTypeList() {
        return parseJsonList(audioType);
    }

    public List<String> getVideoTypeList() {
        return parseJsonList(videoType);
    }

    public List<String> getImageTypeList() {
        return parseJsonList(imageType);
    }

    public List<String> getDocumentTypeList() {
        return parseJsonList(documentType);
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}