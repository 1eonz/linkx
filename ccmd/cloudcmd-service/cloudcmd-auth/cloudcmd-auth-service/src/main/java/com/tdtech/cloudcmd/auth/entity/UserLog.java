package com.tdtech.cloudcmd.auth.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户操作日志记录表（目前只记录登陆日志。安全审计使用）
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@TableName("tb_user_log")
public class UserLog implements Serializable {

    public static final String ID = "id";
    public static final String USER_ID = "user_id";
    public static final String OPERATION_TYPE = "operation_type";
    public static final String OPERATION_TIME = "operation_time";
    public static final String CONTENT = "content";
    public static final String RESULT = "result";
    public static final String EQUIPMENT_ID = "equipment_id";
    public static final String IP = "ip";
    public static final String GMT_CREATED = "gmt_created";
    public static final String GMT_MODIFIED = "gmt_modified";
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 操作类型：0-未知 1-登录 2-登出 3-锁定、解锁 4-冻结、解冻 操作类型（1:增加，2:修改，3:删除，4:查询5:登录）
     */
    private Integer operationType;
    /**
     * 操作时间
     */
    private Date operationTime;
    /**
     * 操作内容
     */
    private String content;
    /**
     * 操作结果 0-成功 1-失败
     */
    private Integer result;
    /**
     * '登陆装备ID'
     */
    private Long equipmentId;
    private String ip;
    private Date gmtCreated;
    private Date gmtModified;

}
