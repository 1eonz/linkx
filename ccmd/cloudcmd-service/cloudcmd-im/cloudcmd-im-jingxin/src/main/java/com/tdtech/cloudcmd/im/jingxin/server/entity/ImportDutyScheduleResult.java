package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.tdtech.cloudcmd.im.jingxin.server.excel.DutyScheduleExcel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ImportDutyScheduleResult {

    @Schema(description = "导入失败的数据")
    private Map<Integer, List<String>> errorMap;

    @Schema(description = "导入成功的数据")
    List<DutyScheduleExcel> successList;
}
