package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "tb_coop_level_member", autoResultMap = true)
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoopLevelMemberDO implements Serializable {

    private Long id;

    private Long coopUserId;

    private Long coopLevelId;

    private Integer isDeleted;

    private Long createBy;

    private Date gmtCreated;
}
