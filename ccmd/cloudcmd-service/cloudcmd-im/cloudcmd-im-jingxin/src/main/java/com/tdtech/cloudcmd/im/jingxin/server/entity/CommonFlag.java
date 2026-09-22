package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 公共标记位表实体
 * @author syf
 * @date 2025/7/23
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_common_flag")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonFlag implements Serializable {

    private Long id;
    /**
     *  标记位key
     */
    private String flagKey;
    /**
     *  标记位value
     */
    private String flagValue;
    /**
     *  操作人员id
     */
    private Long operatorId;
    /**
     *  操作人员名称
     */
    private String operatorName;
    /**
     *  操作时间
     */
    private Date operateTime;
    /**
     *  更新时间
     */
    private Date updateTime;
    /**
     *  备注
     */
    private String remark;
}
