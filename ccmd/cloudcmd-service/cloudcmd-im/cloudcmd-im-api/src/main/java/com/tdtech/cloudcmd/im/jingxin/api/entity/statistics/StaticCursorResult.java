package com.tdtech.cloudcmd.im.jingxin.api.entity.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 统计增量拉取游标分页结果（通用包装）。
 * <p>
 * 各统计表的游标字段不同，但游标语义统一为 (时间字段, 源表唯一键)。
 * 本类将游标抽象为 lastTime + lastId，由 im-jingxin 实现侧从本页最后一条记录填充：
 * <ul>
 *   <li>tb_static_task: lastTime=taskUpdatedTime, lastId=taskId</li>
 *   <li>tb_static_create_group: lastTime=groupUpdatedTime, lastId=groupId</li>
 *   <li>tb_static_task_response: lastTime=responseTime, lastId=responseId</li>
 *   <li>tb_static_coop_duty_switch: lastTime=gmtCreateTime, lastId=attendanceId</li>
 * </ul>
 * dashboard 侧下次拉取时将 lastTime/lastId 作为 timeAfter/idAfter 传入，列表为空时两者为 null。
 *
 * @param <T> 具体的统计 DTO 类型
 */
@Data
@Schema(description = "统计增量拉取游标分页结果")
public class StaticCursorResult<T> implements Serializable {
    private final static long serialVersionUID = 1L;

    @Schema(description = "本页记录列表")
    private List<T> list;

    @Schema(description = "本页最后一条记录的游标时间（作为下次游标，列表为空时为 null）")
    private Date lastTime;

    @Schema(description = "本页最后一条记录的游标ID（作为下次游标，列表为空时为 null）")
    private Long lastId;

    @Schema(description = "是否还有更多数据")
    private Boolean hasMore;
}