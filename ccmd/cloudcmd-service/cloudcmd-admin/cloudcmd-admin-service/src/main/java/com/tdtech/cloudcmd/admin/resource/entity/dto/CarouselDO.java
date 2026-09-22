package com.tdtech.cloudcmd.admin.resource.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarouselDO {

    /**
     * 编号
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 图片URL
     */
    private String pciUrl;
    /**
     * 链接地址
     */
    private String url;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 公众号ID
     */
    private Long officialAccountId;

    /**
     * 公众号名称
     */
    private String officialAccountName;

    /**
     * 文章ID
     */
    private Long articleId;

}
