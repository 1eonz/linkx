package com.tdtech.cloudcmd.im.jingxin.server.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tdtech.cloudcmd.im.jingxin.client.entity.BindUserVo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserListVo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author syf
 * @date 2025/7/17
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_collaboration_attendance_switch")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationAttendanceSwitch implements Serializable {
    private Long id;
    /**
     *  人员ID
     */
    private Long personId;
    /**
     *  上下岗类型：0-上岗、1-下岗
     */
    private Integer switchStatus;
    /**
     *  im状态：0-离线 1-在线 2-忙碌 3-离开
     */
    private Integer imStatus;
    /**
     * 切换类型：0手工切换（default），1IM状态变化切换，2值班自动上下岗，3其他
     * @see com.tdtech.cloudcmd.im.jingxin.server.enums.SwitchTypeEnum
     */
    private Integer switchType;
    /**
     *  更新时间
     */
    private Date updateTime;
    /**
     *  备注
     */
    private String remark;
    /**
     *  创建时间
     */
    private Date gmtCreated;
    /**
     * 最后手动上下岗操作时间（仅记录用户点击或管理员点击操作的时间）
     * 用于解决排班自动上岗误判问题：updateTime会被IM状态变化等非手动操作更新，导致误判该值班区间有手动操作
     */
    private Date lastManualOperationTime;

    public static List<CollaborationAttendanceSwitch> batchFrom(UserListVo userListVo){
        var now = new Date();
        return Optional.ofNullable(userListVo.getBindUsers()).stream().flatMap(Collection::stream).map(BindUserVo::getUserId)
                .filter(Objects::nonNull).map(uid -> CollaborationAttendanceSwitch.builder().id(uid).personId(uid)
                        .switchStatus(0).updateTime(now).build())
                .collect(Collectors.toList());
    }
}