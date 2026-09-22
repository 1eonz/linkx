package com.tdtech.cloudcmd.linkx.third.vo;

import cloudcmd.vo.AppInfoResp4RpcVO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AppGroupDetailVo {
    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组类型。1：系统级；2：用户级
     */
    private Integer type;

    /**
     * 分组排序。用户级需要支持排序,越小越靠前
     */
    private Integer sort;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 是否删除。0：否（default）；1：是
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;

    /**
     * 关联应用列表
     */
    private List<AppInfoResp4RpcVO> appList;
}
