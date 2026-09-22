package com.tdtech.cloudcmd.msip.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.web.utils.ServletRequestContext;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class OperationLog {
    // 操作类型
    private String operationType;

    // 操作描述
    private String operation;

    // 操作人
    private String operator = "LinkX";

    // 操作动作，例如：删除、创建等
    private String action;

    // 被操作的对象
    private String operationResource;

    // 操作结果：0成功，1失败
    private Integer status = MSIPConstant.OPERATION_SUCCESS;

    // Pod所在服务器的IP
    private String ip;

    public OperationLog() {
        super();
    }

    public OperationLog(OperationTypeEnum operationTypeEnum) {
        String classificationName = operationTypeEnum.getParent().getParent().getName();
        String subClassificationName = operationTypeEnum.getParent().getName();
        String name = operationTypeEnum.getName();
        String desc = operationTypeEnum.getDesc();

        this.setOperation(desc);
        this.setOperationResource(subClassificationName);
        this.setIp(resolveIp());
    }

    /**
     * 解析操作来源IP。
     * Web请求线程取真实请求IP；Dubbo/异步/定时任务等非Web线程无绑定请求，
     * 回退到环境变量SERVER_IP，仍无则返回unknown，避免抛出No thread-bound request found
     */
    private String resolveIp() {
        try {
            return ServletRequestContext.getIp();
        } catch (Exception e) {
            String serverIp = System.getenv("SERVER_IP");
            return (serverIp == null || serverIp.isEmpty()) ? "unknown" : serverIp;
        }
    }

}
