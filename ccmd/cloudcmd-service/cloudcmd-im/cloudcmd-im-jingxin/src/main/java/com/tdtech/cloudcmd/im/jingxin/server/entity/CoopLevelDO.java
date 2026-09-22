package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "tb_coop_level", autoResultMap = true)
@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoopLevelDO implements Serializable {

    private Long id;

    private String name;

    private Long parentId;

    private Integer isDeleted;

    private Date gmtCreated;
}
