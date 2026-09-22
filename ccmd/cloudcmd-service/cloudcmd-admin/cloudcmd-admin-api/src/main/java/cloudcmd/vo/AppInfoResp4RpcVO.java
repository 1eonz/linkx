package cloudcmd.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Schema(description = "管理后台 - 应用 Response VO")
@Data
public class AppInfoResp4RpcVO implements Serializable {

    @Schema(description = "应用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "24728")
    private Long id;

    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String name;

    @Schema(description = "应用链接", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.iocoder.cn")
    private String url;

    @Schema(description = "安卓原生应用的包名", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "com.example.app")
    private String packageAndroid;

    @Schema(description = "鸿蒙应用的包名", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "com.example.hm")
    private String packageHm;

    @Schema(description = "鸿蒙应用时需要填写的activity name", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "EntryAbility")
    private String activity;

    @Schema(description = "应用ID", example = "xxxx")
    private String appId;

    @Schema(description = "跳转参数", example = "{name:'*',query:{key:value}}")
    private String params;


    @Schema(description = "应用图标", requiredMode = Schema.RequiredMode.REQUIRED)
    private String icon;

    @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;
    // 新增字段：是否启用
    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "true")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    @Schema(description = "应用类型", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "0")
    private Integer type;

    @Schema(description = "应用区域", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2")
    private Integer zone;

    @Schema(description = "前置应用ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "1")
    private Long prerequisite;
}
