package com.chinasoft.cloud.module.aiagent.excel.bo;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
public class ResultBO<T> {
    private List<T> dataList;

    private boolean isEmpty = false;

    private Map<Integer, String> rowIndexToErrorMsgMap = new TreeMap<>();
}
