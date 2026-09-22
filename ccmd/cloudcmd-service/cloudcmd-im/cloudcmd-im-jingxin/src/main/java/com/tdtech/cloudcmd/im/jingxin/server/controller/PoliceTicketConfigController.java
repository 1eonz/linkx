package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketClientEnableCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketClientQO;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketConfigService;
import com.tdtech.cloudcmd.im.jingxin.server.service.client.ScriptClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 警单对接配置控制器
 */
@Slf4j
@Tag(name = "警单对接配置")
@RestController
@RequestMapping("/collaboration/v1/poltclients")
@RequiredArgsConstructor
@Validated
public class PoliceTicketConfigController {

    private final PoliceTicketConfigService policeTicketConfigService;
    private final ScriptClient scriptClient;
    private final ReportUtil reportUtil;

    /**
     * 创建警单对接配置
     *
     * @param policeTicketClient 警单对接配置对象
     * @return 创建结果
     */
    @Operation(summary = "创建警单对接配置", description = "添加新的警单对接配置")
    @PostMapping
    public R<PoliceTicketClient> createPoliceTicketClient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "警单对接配置对象",
                    required = true) @Valid @RequestBody PoliceTicketClient policeTicketClient) {
        boolean result = policeTicketConfigService.createPoliceTicket(policeTicketClient);
        if (result) {
            return R.success(policeTicketClient);
        }
        return R.failure("创建失败");
    }

    /**
     * 更新警单对接配置
     *
     * @param policeTicketClient 警单对接配置对象
     * @return 更新结果
     */
    @Operation(summary = "更新警单对接配置", description = "更新现有的警单对接配置")
    @PutMapping
    public R<Boolean> updatePoliceTicketClient(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "警单对接配置对象",
                    required = true) @Valid @RequestBody PoliceTicketClient policeTicketClient) {
        try {
            boolean result = policeTicketConfigService.updatePoliceTicket(policeTicketClient);
            reportUtil.saveOperationLog(OperationTypeEnum.POLICE_TICKET_CONFIG_UPDATE,
                    "修改了\"" + policeTicketClient.getName() + "\"警单系统信息",
                    MSIPConstant.OPERATION_SUCCESS);
            return R.success(result);
        } catch (Exception e) {
            log.error("更新警单对接配置失败", e);
            reportUtil.saveOperationLog(OperationTypeEnum.POLICE_TICKET_CONFIG_UPDATE,
                    "修改了\"" + Objects.toString(policeTicketClient.getName(), "") + "\"警单系统信息");
            return R.failure(e.getMessage());
        }
    }

    /**
     * 删除警单对接配置
     *
     * @param id 警单对接配置ID
     * @return 删除结果
     */
    @Operation(summary = "删除警单对接配置", description = "根据ID删除警单对接配置")
    @DeleteMapping("/{id}")
    public R<Boolean>
    deletePoliceTicketClient(
        @Parameter(description = "警单对接配置ID", required = true) @PathVariable Long id) {
        PoliceTicketClient old = policeTicketConfigService.getPoliceTicketById(id);
        if (Objects.isNull(old)) {
            reportUtil.saveOperationLog(OperationTypeEnum.POLICE_TICKET_CONFIG_DELETE,
                    "删除警单系统信息[id=" + id + "]不存在");
            return R.failure("该警单对接配置不存在");
        }
        boolean result = policeTicketConfigService.deletePoliceTicket(old);
        return R.success(result);
    }

    /**
     * 获取警单对接配置详情
     *
     * @param id 警单对接配置ID
     * @return 警单对接配置对象
     */
    @Operation(summary = "获取警单对接配置详情", description = "根据ID查询警单对接配置详情")
    @GetMapping("/{id}")
    public R<PoliceTicketClient>
    getPoliceTicketClientById(
            @Parameter(description = "警单对接配置ID", required = true) @PathVariable Long id) {
        PoliceTicketClient policeTicketClient = policeTicketConfigService.getPoliceTicketById(id);
        return R.success(policeTicketClient);
    }

    /**
     * 查询所有警单对接配置
     *
     * @return 警单对接配置列表
     */
    @Operation(summary = "查询所有警单对接配置", description = "获取所有警单对接配置列表")
    @GetMapping("/all")
    public R<List<PoliceTicketClient>> listAllPoliceTicketClients() {
        List<PoliceTicketClient> policeTicketClients = policeTicketConfigService.listAllPoliceTickets();
        return R.success(policeTicketClients);
    }

    /**
     * 分页查询警单对接配置
     *
     * @param qo      查询条件对象
     * @param current 当前页码
     * @param size    每页记录数
     * @return 分页结果
     */
    @Operation(summary = "分页查询警单对接配置", description = "根据条件分页查询警单对接配置")
    @GetMapping("/page")
    public R<Page<PoliceTicketClient>> pagePoliceTicketClients(PoliceTicketClientQO qo,
                                                               @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Long current,
                                                               @Parameter(description = "每页记录数", example = "10") @RequestParam(defaultValue = "10") Long size) {
        Page<PoliceTicketClient> page = policeTicketConfigService.pagePoliceTicketClients(qo,
                current, size);
        return R.success(page);
    }

    @PostMapping("/try")
    @Operation(summary = "脚本试运行", description = "脚本固定两个参数：Params：json string形式的入参，Result：任意形式的出参，脚本从Params中提取参数，然后给Result赋值就行了")
    public R<Object> tryScript(@RequestBody TryBody body) {
        return R.success(scriptClient.tryScript(body.script, body.payload));
    }

    @PutMapping("/enable")
    public R<Void> enable(@RequestBody PoliceTicketClientEnableCO policeTicketClientEnableCO) {
        policeTicketConfigService.enable(policeTicketClientEnableCO.getId(),
                policeTicketClientEnableCO.getStatus());
        return R.success();
    }


    @Data
    public static class TryBody {

        @NotNull
        private String script;
        @NotNull
        private String payload;
    }
}