package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.OpenApiCreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicket;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketStatisticsVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketService;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/collaboration/v1/policeticket")
@RequiredArgsConstructor
@Tag(name = "警单管理", description = "警单相关操作接口")
public class PoliceTicketController {
    private final PoliceTicketService policeTicketService;

    /**
     * 创建 PoliceTicket
     *
     * @param policeTicket 警单实体
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建警单", description = "创建一个新的警单记录")
    public R<Void> create(@RequestBody PoliceTicket policeTicket) {
        policeTicketService.create(policeTicket);
        return R.success();
    }

    /**
     * 根据 ID 查询 PoliceTicket
     *
     * @param id 警单ID
     * @return PoliceTicket 实体
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询警单", description = "根据主键ID查询警单详情")
    @Parameter(name = "id", description = "警单ID", required = true)
    public R<PoliceTicket> findById(@PathVariable Long id) {
        return R.success(policeTicketService.findById(id));
    }

    /**
     * 更新 PoliceTicket
     *
     * @param policeTicket 警单实体
     * @return 更新结果
     */
    @PutMapping
    @Operation(summary = "更新警单", description = "更新警单信息")
    public R<Void> update(@RequestBody PoliceTicket policeTicket) {
        policeTicketService.update(policeTicket);
        return R.success();
    }

    /**
     * 根据 ID 删除 PoliceTicket
     *
     * @param id 警单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除警单", description = "根据ID删除警单记录")
    @Parameter(name = "id", description = "警单ID", required = true)
    public R<Void> deleteById(@PathVariable Long id) {
        policeTicketService.deleteById(id);
        return R.success();
    }

    /**
     * 分页查询 PoliceTicket
     *
     * @param policeTicket 查询条件
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询警单", description = "根据条件分页查询警单列表")
    @Parameter(name = "current", description = "当前页码")
    @Parameter(name = "size", description = "每页条数")
    public R<Page<PoliceTicketVO>> findPage(@RequestParam(name = "current") Integer current, @RequestParam(name = "size") Integer size,
                                            PoliceTicketQO policeTicket) {
        Page<PoliceTicketVO> page = new Page<>(current, size);
        var r = policeTicketService.findPage(page, policeTicket);
        Optional.ofNullable(r.getRecords()).stream().flatMap(Collection::stream).forEach(record->{
            record.setOrigin(null);
        });
        return R.success(r);
    }

    @PostMapping("/groupbind/{groupId}")
    @Operation(summary = "警单关联，修改也是这个接口，每次传全量警单ID", description = "警单关联")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    public R<Void> bindGroup(@PathVariable("groupId") @NotNull Long groupId, @RequestBody @NotNull List<Long> ticketIds) {
        policeTicketService.bindGroup(groupId, ticketIds);
        return R.success();
    }

    @DeleteMapping("/groupbind/{groupId}/{ticketId}")
    @Operation(summary = "删除警单关联", description = "删除警单关联")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    public R<Void> deleteBinding(@NotNull @PathVariable("groupId") Long groupId,
                                 @NotNull @PathVariable("ticketId") Long ticketId) {
        policeTicketService.deleteBinding(groupId, ticketId);
        return R.success();
    }

    @GetMapping("/{ticketId}/groups")
    @Operation(summary = "警单查群ID，只有id", description = "警单查群ID")
    @Parameter(name = "ticketId", description = "警单关联", required = true)
    public R<List<Long>> getGroupByTicket(@PathVariable("ticketId") Long id) {
        return R.success(policeTicketService.groupIds(id));
    }

    @PostMapping("/create-group")
    @Operation(summary = "根据警单创建群组", description = "根据警单一键创建群组")
    public R<Long> createGroup(@RequestBody OpenApiCreateGroupCO createGroupCO) {
        var clientUserId = SecurityUtils.getUser().getUserId();
        return R.success(policeTicketService.createGroup(createGroupCO, String.valueOf(clientUserId)));
    }

    /**
     * 提供给sdk调用
     * @param orgIds
     * @return
     */
    @PostMapping("/statistics/count")
    @Operation(summary = "根据部门IDS数组查询关联统计数据", description = "根据部门IDS数组查询关联统计数据")
    @Parameter(name = "groupId", description = "分组ID", required = true)
    public R<List<PoliceTicketStatisticsVO>> statistics(@RequestBody @NotNull List<Long> orgIds) {
        return R.success(policeTicketService.statistics(orgIds));
    }

}
