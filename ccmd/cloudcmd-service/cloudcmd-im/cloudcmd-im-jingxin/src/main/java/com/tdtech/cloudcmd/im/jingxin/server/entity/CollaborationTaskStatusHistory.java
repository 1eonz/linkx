package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author cangPeng
 * @date 2025/2/26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_task_status_history")
public class CollaborationTaskStatusHistory {
    public static final String ID = "id";
    public static final String TASK_ID = "task_id";
    public static final String STATUS = "status";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";


    public static final String DEPARTMENT_ID = "department_id";
    public static final String DEPARTMENT_NAME = "department_name";


    public static final String IS_DELETED = "is_deleted";
    public static final String GMT_MODIFIED = "gmt_modified";
    public static final String GMT_CREATED = "gmt_created";
    private static final long serialVersionUID = 1L;
    private Long id;

    /**
     * 协同任务ID
     */
    private Long taskId;

    /**
     * 0:待办；1:忽略；2：跟踪；3：办结；4：答复
     */
    private Integer status;

    /**
     * 答复人ID
     */
    private Long userId;

    /**
     * 答复人姓名
     */
    private String userName;

    /**
     * 答复人部门ID
     */
    private Long departmentId;

    /**
     * 答复人部门名称
     */
    private String departmentName;

    /**
     * 是否删除
     */
    private Integer isDeleted;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 创建时间
     */
    private Date gmtCreated;
}
