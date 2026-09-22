package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImArticleVo {

    /**
     * 公众号内容ID
     */
    private Long id;
    /**
     * 内容标题
     */
    private String title;
    /**
     * 缩略图URL
     */
    private String thumbImg;
    /**
     * 内容简介
     */
    private String summary;
    /**
     * 公众号文章url
     */
    private String contentUrl;
    /**
     * 所属公众号ID
     */
    private Long officialaccountId;
    /**
     * 所属公众号名称
     */
    private String officialaccountName;
    /**
     * 所属公众号图标
     */
    private String officialaccountIconUrl;
    /**
     * 发布用户ID
     */
    private Long userId;
    /**
     * 发布用户姓名
     */
    private String userName;
    /**
     * 发布状态，false:未发布 true：已发布。默认取值为false。
     */
    private Boolean isPublish;
    /**
     * 发布时间。UTC时间戳。单位：毫秒
     */
    private Long publishTime;
    /**
     * 发布时间。UTC时间戳。单位：毫秒
     */
    private Long modifiedTime;
    /**
     * 是否删除。false：未删除，true：已删除
     */
    private Boolean isDel;
}
