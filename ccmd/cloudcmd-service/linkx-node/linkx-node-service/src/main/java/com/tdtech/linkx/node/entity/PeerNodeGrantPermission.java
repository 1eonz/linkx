package com.tdtech.linkx.node.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多节点数据访问路由控制表
 */
@Data
@TableName("tb_peer_node_grant_permission")
public class PeerNodeGrantPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限控制项（org/dashboard/coopuser）
     */
    private String name;

    /**
     * 对应权限控制项允许访问的URI地址（支持Ant风格通配符）
     */
    private String uris;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;
}
