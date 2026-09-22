package com.tdtech.cloudcmd.admin.util;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.tdtech.cloudcmd.admin.config.PlaceholderResolver;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: back-new-huawei
 * @description: I18nCellWriteHandler
 * @author: yj
 * @date: 2024-08-16
 **/

@Slf4j
@RequiredArgsConstructor
public class I18nCellWriteHandler implements CellWriteHandler {

    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 Row row, Head head, Integer integer, Integer integer1, Boolean aBoolean) {
        final List<String> originHeadNames = head.getHeadNameList();
        if (CollectionUtils.isEmpty(originHeadNames)) {
            return;
        }
        List<String> newHeadNames = originHeadNames.stream().
                map(headName -> PlaceholderResolver.getDefaultResolver()
                        .resolveByRule(headName, this::getMessage)).
                collect(Collectors.toList());
        head.setHeadNameList(newHeadNames);
    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                Cell cell, Head head, Integer integer, Boolean aBoolean) {}

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, WriteCellData<?> cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {}

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 List cellDataList, Cell cell, Head head, Integer relativeRowIndex,
                                 Boolean isHead) {}

    public String getMessage(String code) {
        return I18nUtil.get(code);
    }

}
