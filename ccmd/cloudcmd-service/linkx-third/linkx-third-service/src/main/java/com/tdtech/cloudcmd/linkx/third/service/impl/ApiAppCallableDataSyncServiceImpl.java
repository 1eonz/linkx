package com.tdtech.cloudcmd.linkx.third.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.api.dto.KeyValue;
import com.tdtech.cloudcmd.linkx.third.dto.ApiPullDataDto;
import com.tdtech.cloudcmd.linkx.third.service.AbstractAppCallableDataSyncServiceImpl;
import com.tdtech.cloudcmd.linkx.third.utils.AssertUtils;
import com.tdtech.cloudcmd.linkx.third.utils.DateFormatFieldUtils;
import com.tdtech.cloudcmd.linkx.third.utils.KeyValueUtils;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.DateFormatFieldInfo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.util.json.JsonUtil;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 南向应用数据同步-api类型
 * <p>
 * 覆写5参数 pullData 以支持时间表达式替换，
 * 分页拉取、建表、插入等公共逻辑由抽象类 {@link AbstractAppCallableDataSyncServiceImpl#doSync} 统一处理。
 * </p>
 */
@Service("apiAppCallableDataSyncServiceImpl")
@Slf4j
public class ApiAppCallableDataSyncServiceImpl extends AbstractAppCallableDataSyncServiceImpl {
    /**
     * json根路径
     */
    private static final String JSON_ROOT = "$";

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private HttpClient httpClient;

    @Override
    public AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long page, Long pageSize) throws SQLException {
        // 3参数版本委托给5参数版本（无时间范围）
        return pullData(detailVo, page, pageSize, null, null);
    }

    /**
     * 带时间范围的 pullData，覆写接口默认实现
     * <p>
     * 当 timeRangeStart/timeRangeEnd 不为 null 时，会使用 transReq 替换请求参数中的时间表达式；
     * 为 null 时走原有逻辑（兼容 DB 类型的 doSync 流程）。
     * </p>
     *
     * @param detailVo       南向应用详情
     * @param page           页码
     * @param pageSize       页面大小
     * @param timeRangeStart 时间范围起始（可为null，表示不替换时间表达式）
     * @param timeRangeEnd   时间范围结束（可为null，表示不替换时间表达式）
     * @return 拉取数据结果
     */
    @Override
    public AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long page, Long pageSize,
                                   LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) throws SQLException {
        List<ColumnInfo> columnInfoList = new ArrayList<>();
        ApiPullDataDto pullDataDto = ApiPullDataDto.builder()
                .app(detailVo).columnInfoList(columnInfoList)
                .page(page).pageSize(pageSize)
                .timeRangeStart(timeRangeStart).timeRangeEnd(timeRangeEnd)
                .build();
        // 拉取数据（refactorDataList 内部同时计算 min/max 写入 dto）
        List<JSONObject> dataList = queryData(pullDataDto);

        log.info("拉取数据完成, 应用id={}, 数据条数={}, minDateTimeSign={}, maxDateTimeSign={}",
                detailVo.getId(), dataList.size(), pullDataDto.getMinDateTimeSign(), pullDataDto.getMaxDateTimeSign());

        return AppDataToDbDto.builder()
                .columnInfoList(columnInfoList)
                .resultDataList(dataList)
                .minDateTimeSign(pullDataDto.getMinDateTimeSign())
                .maxDateTimeSign(pullDataDto.getMaxDateTimeSign())
                .build();
    }

    /**
     * 将请求参数中的时间表达式替换为实际时间值
     * <p>
     * 优先从 reqParam 中提取日期格式字段，若 reqParam 无日期字段则从 reqBody 中提取。
     * 将第一个日期字段替换为 timeRangeStart，第二个日期字段替换为 timeRangeEnd。
     * </p>
     *
     * @param reqParam       请求参数 JSON 字符串
     * @param reqBody        请求体 JSON 字符串
     * @param timeRangeStart 时间范围起始
     * @param timeRangeEnd   时间范围结束
     * @return 替换后的 paramMap 和 reqBody
     */
    private TransReqResult transReq(String reqParam, String reqBody,
                                   LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        JsonObject paramMap = JsonUtil.parseJson(reqParam);
        if (Objects.isNull(paramMap)) {
            paramMap = new JsonObject();
        }
        JsonObject bodyMap = JsonUtil.parseJson(StringUtils.isBlank(reqBody) ? "{}" : reqBody);
        if (Objects.isNull(bodyMap)) {
            bodyMap = new JsonObject();
        }

        // 优先从 reqParam 中提取日期字段，没有再从 reqBody 中提取
        List<DateFormatFieldInfo> dateFields = DateFormatFieldUtils.extractDateFormatFields(reqParam);
        boolean fromParam =  CollectionUtils.isNotEmpty(dateFields) && dateFields.size() >=2;
        if (!fromParam) {
            dateFields = DateFormatFieldUtils.extractDateFormatFields(reqBody);
        }
        if (CollectionUtils.isEmpty(dateFields) || dateFields.size() < 2) {
            log.info("请求参数中日期格式字段不足2个, 跳过时间表达式替换");
            return new TransReqResult(paramMap, bodyMap);
        }

        DateFormatFieldInfo startField = dateFields.get(0);
        DateFormatFieldInfo endField = dateFields.get(1);
        String startValue = DateFormatUtil.format(timeRangeStart, startField.getDateFormat());
        String endValue = DateFormatUtil.format(timeRangeEnd, endField.getDateFormat());

        // 替换到对应的 JSON 对象中
        JsonObject target = fromParam ? paramMap : bodyMap;
        target.put(startField.getFieldName(), startValue);
        target.put(endField.getFieldName(), endValue);
        log.info("时间表达式替换完成, 来源={}, {}={}, {}={}", fromParam ? "reqParam" : "reqBody",
                startField.getFieldName(), startValue, endField.getFieldName(), endValue);
        return new TransReqResult(paramMap, bodyMap);
    }

    @Data
    @AllArgsConstructor
    static class TransReqResult {
        private JsonObject paramMap;
        private JsonObject reqBody;
    }

    private List<JSONObject> queryData(ApiPullDataDto dto) {
        AppCallableDetailVo app = dto.getApp();
        boolean pageable = app.getPagenation() == 1;
        boolean hasTimeRange = Objects.nonNull(dto.getTimeRangeStart()) && Objects.nonNull(dto.getTimeRangeEnd());

        // 解析请求参数：有时间范围时 transReq 内部自动判断时间表达式来源（优先reqParam）
        JsonObject paramMap;
        JsonObject reqBody;
        if (hasTimeRange) {
            TransReqResult result = transReq(app.getReqParam(), app.getReqBody(), dto.getTimeRangeStart(), dto.getTimeRangeEnd());
            paramMap = result.getParamMap();
            reqBody = result.getReqBody();
        } else {
            paramMap = JsonUtil.parseJson(app.getReqParam());
            if (Objects.isNull(paramMap)) {
                paramMap = new JsonObject();
            }
            reqBody = JsonUtil.parseJson(StringUtils.isBlank(app.getReqBody()) ? "{}" : app.getReqBody());
            if (Objects.isNull(reqBody)) {
                reqBody = new JsonObject();
            }
        }
        if (pageable && app.getPageParamLocation() == 0) {
            paramMap.put(app.getPageFieldName(), dto.getPage());
            paramMap.put(app.getPageSizeFieldName(), dto.getPageSize());
        }
        // 构建query参数
        var uri = HttpClient.buildUri(
                app.getProtocol() + "://" + app.getIp() + ":" + app.getPort()
                        + app.getUri(), paramMap);

        if (pageable && app.getPageParamLocation() == 1) {
            reqBody.put(app.getPageFieldName(), dto.getPage());
            reqBody.put(app.getPageSizeFieldName(), dto.getPageSize());
        }
        Map<String, String> reqHeader = JsonUtil.parseJson(StringUtils.isBlank(app.getReqHeader()) ? "{}" : app.getReqHeader(),
                new TypeReference<>() {});

        // 记录请求信息
        log.info("API请求, 应用id={}, url={}, body={}, header={}", app.getId(), uri, reqBody, reqHeader);

        // 执行查询
        var resp = httpClient.sendJsonRequest(uri, HttpClient.HttpMethodEnum.valueOf(app.getMethod()),
                reqHeader, reqBody, Duration.ofSeconds(30L));
        AssertUtils.check(resp.statusCode() == 200, "查询失败, 失败状态码:" + resp.statusCode());
        byte[] bodyBytes = resp.body();
        if (bodyBytes == null || bodyBytes.length == 0) {
            log.warn("查询到的数据为空!");
            return Collections.emptyList();
        }

        // 提取返回值的data
        String respPath = StringUtils.isBlank(app.getResponseDataPath()) ? JSON_ROOT
                : JSON_ROOT + "." + app.getResponseDataPath();
        Object records = JSONPath.read(new String(resp.body()), respPath);
        if (!(records instanceof JSONArray)) {
            log.warn("数据响应配置错误，必须为对象列表格式");
            return Collections.emptyList();
        }

        List<JSONObject> dataList = ((JSONArray) records).toJavaList(JSONObject.class);
        // 处理数据
        if (CollectionUtils.isEmpty(dataList)) {
            log.warn("查询到的数据为空!");
            return Collections.emptyList();
        }
        // 记录第一条数据样例
        log.info("API响应, 应用id={}, 数据条数={}, 第一条数据样例={}", app.getId(), dataList.size(), dataList.get(0));
        List<KeyValue> keyValues = JSON.parseArray(app.getMapper(), KeyValue.class);
        Map<String, String> columnMapper = KeyValueUtils.keyvalueToMap(keyValues);

        // 以第一条数据提取字段信息
        List<ColumnInfo> columnInfoList = dto.getColumnInfoList();
        getColumnInfo(dataList.get(0), app.getUniqueId(), columnMapper, dto.getColumnInfoList());
        if (CollectionUtils.isEmpty(columnInfoList)) {
            log.warn("数据无任何字段信息，无法同步!");
            return Collections.emptyList();
        }
        // 重构数据，同时计算 dateTimeSign 的 min/max 写入 dto
        refactorDataList(dataList, columnInfoList, app.getDateTimeSign(), dto);
        return dataList;
    }

    /**
     * 重构数据并同步计算 dateTimeSign 的最早/最晚值
     * <p>
     * 在已有遍历中同时追踪 min/max，避免二次遍历数据列表。
     * 计算结果写入 dto 的 minDateTimeSign / maxDateTimeSign。
     * </p>
     *
     * @param dataList       数据列表
     * @param columnInfoList 列信息
     * @param dateTimeSign   时间字段名（可为空，为空时不计算 min/max）
     * @param dto            拉取数据 DTO，用于输出 min/max
     */
    private void refactorDataList(List<JSONObject> dataList, List<ColumnInfo> columnInfoList,
                                  String dateTimeSign, ApiPullDataDto dto) {
        Map<String, String> columnTypeMap = columnInfoList.stream()
                .collect(Collectors.
                        toMap(ColumnInfo::getColumnName, ColumnInfo::getColumnType, (k1, k2) -> k1));
        Set<String> columns = columnTypeMap.keySet();

        // 追踪 dateTimeSign 的最早和最晚值
        String min = null;
        String max = null;
        boolean hasDateTimeSign = StringUtils.isNotBlank(dateTimeSign);

        for (JSONObject data : dataList) {
            // 重构字段
            for (String column : columns) {
                String columnType = columnTypeMap.get(column);
                Object value = data.get(column);
                if (StringUtils.equals("TEXT", columnType)) {
                    value = Objects.isNull(value) ? StringUtils.EMPTY :
                            isStringOrNumber(value) ? value : JSONObject.toJSONString(value);
                    data.put(column, value);
                }
                if (StringUtils.equals("JSON", columnType)) {
                    data.put(column, Objects.isNull(value) ? "{}" : JSONObject.toJSONString(value));
                }
            }
            // 添加必要参数
            Date date = new Date();
            data.put(LINKX_ID_COLUMN, idWorker.nextId());
            data.put(CREATED_TIME_COLUMN, date);
            data.put(UPDATE_TIME_COLUMN, date);

            // 在同一次遍历中计算 min/max
            if (hasDateTimeSign) {
                Object signValue = data.get(dateTimeSign);
                if (Objects.nonNull(signValue)) {
                    String strVal = String.valueOf(signValue);
                    if (min == null || strVal.compareTo(min) < 0) {
                        min = strVal;
                    }
                    if (max == null || strVal.compareTo(max) > 0) {
                        max = strVal;
                    }
                }
            }
        }

        // 将 min/max 写入 dto
        dto.setMinDateTimeSign(min);
        dto.setMaxDateTimeSign(max);
    }

    private void getColumnInfo(JSONObject data, String uniqueKey,
                               Map<String, String> columnMapper, List<ColumnInfo> columnInfoList) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            columnInfoList.add(ColumnInfo.builder()
                    .columnName(entry.getKey())
                    .columnType(StringUtils.equals(uniqueKey, entry.getKey()) ? "VARCHAR(255)" : getColumnType(entry.getValue()))
                    .comment(columnMapper.getOrDefault(entry.getKey(), StringUtils.EMPTY))
                    .build());
        }
        addExtraColumn(columnInfoList);
    }

    private static String getColumnType(Object value) {
        if (value == null || isStringOrNumber(value)) {
            return "TEXT";
        }
        return "JSON";
    }

    public static boolean isStringOrNumber(Object value) {
        if (value == null) {
            return false;
        }
        return (value instanceof String)
                || (value instanceof Integer)
                || (value instanceof Long)
                || (value instanceof Double)
                || (value instanceof Float)
                || (value instanceof Short)
                || (value instanceof Byte)
                || (value instanceof Boolean);
    }
}