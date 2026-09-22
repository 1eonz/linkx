package com.tdtech.cloudcmd.linkx.dashboard.support;

import lombok.Data;

import java.util.Date;

/**
 * 统计同步水位线（双游标）。
 * <p>
 * 各统计表统一用 (time, id) 双游标：time 精度到秒，同秒可能多条记录，
 * 靠 id 二级游标保证不漏不重。
 * <p>
 * 各表游标字段：
 * <ul>
 *   <li>tb_static_task: (task_updated_time, task_id)</li>
 *   <li>tb_static_create_group: (group_updated_time, group_id)</li>
 *   <li>tb_static_task_response: (response_time, response_id)</li>
 *   <li>tb_static_photo_check: (gmt_create_time, check_data_id)</li>
 *   <li>tb_static_coop_duty_switch: (gmt_create_time, attendance_id)</li>
 * </ul>
 */
@Data
public class SyncWatermark {

    private Date time;

    private Long id;

    public SyncWatermark() {
    }

    public SyncWatermark(Date time, Long id) {
        this.time = time;
        this.id = id;
    }

    public boolean isEmpty() {
        return time == null && id == null;
    }
}