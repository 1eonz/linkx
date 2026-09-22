package cloudcmd.enums;

import com.tdtech.cloudcmd.enums.BaseEnum;

/**
 * @program: back-new-huawei
 * @description: DashboardCodeEnum
 * @author: yj
 * @date: 2024-06-13
 **/
public enum DashboardCodeEnum implements BaseEnum {

    PUBLISHED(101, "DashboardErrorEnum_ERROR_1", "看板已发布状态，无法删除"),
    IN_PUBLISHED_DASHBOARD(102, "DashboardErrorEnum_ERROR_2", "图表在已发布状态的看板引用，无法删除"),
    DO_NOT_EXPORT(103, "DashboardErrorEnum_ERROR_3", "该图表不支持导出"),
    NO_OPERATE_AUTH(105, "DashboardErrorEnum_ERROR_4", "没有该组织的操作权限"),
    BUILT_IN_DATA(106, "DashboardErrorEnum_ERROR_5", "内置数据不能删除"),
    CHART_NOT_EXIST(104, "DashboardErrorEnum_ERROR_6", "图表不存在");

    private int code;

    private String msgKey;

    private String msg;

    DashboardCodeEnum(int code, String msgKey, String msg) {
        this.code = code;
        this.msgKey = msgKey;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsgKey() {
        return msgKey;
    }

    @Override
    public String getMsg() {
        return msg;
    }

}
