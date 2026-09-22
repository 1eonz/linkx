package com.tdtech.cloudcmd.admin.resource.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@TableName("carousel")
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Carousel {

    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String PCI_URL = "pci_url";
    public static final String URL = "url";
    public static final String SORT = "sort";
    public static final String OFFICIAL_ACCOUNT_ID = "official_account_id";
    public static final String OFFICIAL_ACCOUNT_NAME = "official_account_name";
    public static final String ARTICLE_ID = "article_id";

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 标题
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;
    /**
     * 图片URL
     */
    private String pciUrl;
    /**
     * 链接地址
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String url;
    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 公众号ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long officialAccountId;

    /**
     * 公众号名称
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String officialAccountName;

    /**
     * 文章ID
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long articleId;


}
