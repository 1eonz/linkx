package com.tdtech.cloudcmd.im.jingxin.client.entity;

/**
 * @author lsc
 * @date 2025/7/14
 **/
import lombok.Data;

@Data
public class BindUserVo {

    private Long userId; // 用户ID
    private String name; // 人员姓名
    private String departmentName; // 所在主部门名称
    private String departmentFullPath; // 所在主部门全路径名称
    private Integer serviceStatus; // 服务支撑状态 0-未支撑（备选人员）1-支撑人员（正式人员）
    private Integer groupNum; // 该协同岗人员支撑的群组数
}