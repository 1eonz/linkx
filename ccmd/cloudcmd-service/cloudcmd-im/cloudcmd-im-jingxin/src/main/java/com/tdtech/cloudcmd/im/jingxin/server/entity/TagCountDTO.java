package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.Data;

/**
 * @author ChinasoftPortal
 * @date 2025/9/17
 * @Describe：
 */
@Data
public class TagCountDTO {
    private Long tagId;
    private String tagName;
    private String icon;
    private String color;
    private Integer deleted;
    private Long count;
    /**
     * 已归档的数量
     */
    private Integer archivedCount;
    /**
     * 未归档的数量
     */
    private Integer unArchivedCount;
}
