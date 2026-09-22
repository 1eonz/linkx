package com.chinasoft.cloud.module.aiagent.msip.entity;

import com.chinasoft.cloud.module.aiagent.msip.constant.MSIPConstant;
import com.chinasoft.cloud.module.aiagent.msip.enums.OperationTypeEnum;
import com.chinasoft.cloud.module.aiagent.msip.util.ServletRequestContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
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
//      this.setIp(System.getenv("SERVER_IP"));
        this.setIp(ServletRequestContext.getIp());
    }

}
