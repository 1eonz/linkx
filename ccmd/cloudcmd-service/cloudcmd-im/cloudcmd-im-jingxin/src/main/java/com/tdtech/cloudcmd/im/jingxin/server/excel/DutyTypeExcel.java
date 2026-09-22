package com.tdtech.cloudcmd.im.jingxin.server.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 排班类型模板 sheet。
 */
@Data
@NoArgsConstructor
public class DutyTypeExcel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排班类型标识，导入排班时填写该值。
     */
    @ExcelProperty("排班类型*")
    private Long type;

    /**
     * 排班类型名称，用于辅助用户识别。
     */
    @ExcelProperty("排班类型名称")
    private String name;

    public DutyTypeExcel(DutyType dutyType) {
        this.type = dutyType.getType();
        this.name = dutyType.getName();
    }
}
