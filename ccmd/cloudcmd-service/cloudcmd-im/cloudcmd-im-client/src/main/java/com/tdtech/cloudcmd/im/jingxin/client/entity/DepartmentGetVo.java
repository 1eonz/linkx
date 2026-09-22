package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

/**
 * @author lsc
 * @date 2025/7/23
 **/
@Data
public class DepartmentGetVo {
    private List<ImDepartment> results;
    private List<DepartmentFailVo> failures;
}
