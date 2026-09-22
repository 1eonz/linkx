package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tdtech.cloudcmd.im.jingxin.client.entity.BindUserVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserListVo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_collaboration_post")
@Data
public class CollaborationPost implements Serializable {
    private Long id;
    /**
     * 协同岗名称
     */
    private String postName;

    private String fileId;
    /**
     * 图标URL
     */
    private String iconUrl;
    /**
     * 所属组织ID
     */
    private Long orgId;

    private String orgCode;
    /**
     * 所属组织名称
     */
    private String orgName;
    /**
     * 关联人员IDs
     */
    private String relatedUserIds;
    /**
     * 关联人员名称
     */
    private String relatedUserNames;
    /**
     * 操作类型 1为创建，2为修改
     */
    private Integer operationType;
    /**
     * 数据来源：0-平台操作，1-im同步
     */
    private Integer source;
    /**
     * 操作人ID
     */
    private Long operatorId;
    /**
     * 操作人姓名
     */
    private String operatorName;
    /**
     * 操作时间
     */
    private Date operateTime;
    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 0 普通 1 1：14E
     */
    private Integer type;

    @TableLogic // 逻辑删除标记（0-未删除，1-已删除）
    @TableField("deleted")
    private Integer deleted = 0;

    public static CollaborationPost from(UserListVo userListVo) {
        CollaborationPost post = new CollaborationPost();
        post.setId(Long.valueOf(userListVo.getId()));
        post.setPostName(userListVo.getName());
        post.setFileId(userListVo.getAvatar());
        post.setOperateTime(userListVo.getGmtCreated() == null ? null : new Date(userListVo.getGmtCreated()));
        post.setUpdateTime(userListVo.getGmtModified() == null ? null : new Date(userListVo.getGmtModified()));
        post.setOperatorName("系统同步");
        post.setSource(1);
        if (userListVo.getDepartment() != null) {
            post.setOrgId(userListVo.getDepartment().getDepartmentId());
            post.setOrgName(userListVo.getDepartment().getDepartmentName());
        }
        if (CollectionUtils.isNotEmpty(userListVo.getBindUsers())) {
            post.setRelatedUserIds(userListVo.getBindUsers().stream().map(BindUserVo::getUserId)
                .filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(",")));
            post.setRelatedUserNames(userListVo.getBindUsers().stream().map(BindUserVo::getName)
                .filter(StringUtils::isNotBlank).collect(Collectors.joining(",")));
        }
        return post;
    }

    public Map<Long, String> toUidNameMap() {
        var idAndNameMap = new LinkedHashMap<Long, String>();
        var uids = getRelatedUserIds().split(",");
        var unames = getRelatedUserNames().split(",");
        for (int i = 0; i < uids.length; i++) {
            idAndNameMap.put(Long.parseLong(uids[i]), unames[i]);
        }
        return idAndNameMap;
    }

    @JsonIgnore
    public List<Long> getUids() {
        if (relatedUserIds == null) {
            return null;
        }
        var uids = getRelatedUserIds().split(",");
        return Arrays.stream(uids).filter(a -> !a.isBlank()).map(Long::parseLong).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<String> getUnames() {
        if (relatedUserNames == null) {
            return null;
        }
        var uids = getRelatedUserNames().split(",");
        return Arrays.stream(uids).filter(a -> !a.isBlank()).collect(Collectors.toList());
    }
}