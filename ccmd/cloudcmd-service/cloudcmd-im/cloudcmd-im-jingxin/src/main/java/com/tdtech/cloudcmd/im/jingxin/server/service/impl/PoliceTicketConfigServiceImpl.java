package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.polTicket.PoliceTicketClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketClientQO;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketConfigService;
import com.tdtech.cloudcmd.im.jingxin.server.service.client.ScriptClient;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.PoliceTicketClientMapper;

import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PoliceTicketConfigServiceImpl extends
    ServiceImpl<PoliceTicketClientMapper, PoliceTicketClient> implements PoliceTicketConfigService {

    private final IdWorker idWorker;
    private final ScriptClient scriptClient;

    /**
     * 创建警单
     *
     * @param policeTicketClient 警单对象
     * @return 是否创建成功
     */
    @Override
    @LogReport(type = OperationTypeEnum.POLICE_TICKET_CONFIG_INSERT)
    public boolean createPoliceTicket(@LogReportParam PoliceTicketClient policeTicketClient) {
        var count = this.count(Wrappers.lambdaQuery(PoliceTicketClient.class)
                .eq(PoliceTicketClient::getSystemCode, policeTicketClient.getSystemCode()));
        if(count!=0){
            throw new BusinessException("系统编码重复");
        }

        var user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        policeTicketClient.setId(idWorker.nextId());
        policeTicketClient.setCreator(user.getUserName());
        policeTicketClient.setCreatorId(user.getUserId());
        policeTicketClient.setGmtCreated(new Date());
        var b = scriptClient.checkScript(policeTicketClient.getScript());
        if (b == null || !b) {
            throw new BusinessException("脚本验证失败");
        }
        var save = this.save(policeTicketClient);
        scriptClient.loadScript(policeTicketClient.getId());
        return save;
    }

    /**
     * 更新警单（根据ID）
     *
     * @param policeTicketClient 警单对象
     * @return 是否更新成功
     */
    @Override
    public boolean updatePoliceTicket(PoliceTicketClient policeTicketClient) {
        var count = this.count(Wrappers.lambdaQuery(PoliceTicketClient.class)
                .eq(PoliceTicketClient::getSystemCode, policeTicketClient.getSystemCode())
                .ne(PoliceTicketClient::getId,policeTicketClient.getId()));
        if(count!=0){
            throw new BusinessException("系统编码重复");
        }
        var b = scriptClient.checkScript(policeTicketClient.getScript());
        if (b == null || !b) {
            throw new BusinessException("脚本验证失败");
        }
        var update = this.updateById(policeTicketClient);
        scriptClient.loadScript(policeTicketClient.getId());
        return update;
    }


    @Override
    public void enable(Long id, Integer status) {
        this.update(
            Wrappers.lambdaUpdate(PoliceTicketClient.class).eq(PoliceTicketClient::getId, id)
                .set(PoliceTicketClient::getStatus, status));
        scriptClient.loadScript(id);
    }

    /**
     * 删除警单（根据ID）
     *
     * @param id 警单ID
     * @return 是否删除成功
     */
    @Override
    public boolean deletePoliceTicket(Long id) {
        var b = this.removeById(id);
        scriptClient.loadScript(id);
        return b;
    }

    @Override
    @LogReport(type = OperationTypeEnum.POLICE_TICKET_CONFIG_DELETE)
    public boolean deletePoliceTicket(@LogReportParam PoliceTicketClient policeTicketClient) {
        return deletePoliceTicket(policeTicketClient.getId());
    }

    /**
     * 获取警单详情（根据ID）
     *
     * @param id 警单ID
     * @return 警单对象
     */
    @Override
    public PoliceTicketClient getPoliceTicketById(Long id) {
        return this.getById(id);
    }

    /**
     * 查询所有警单
     *
     * @return 警单列表
     */
    @Override
    public List<PoliceTicketClient> listAllPoliceTickets() {
        return this.list();
    }

    /**
     * 分页查询警单客户端配置（支持模糊查询）
     *
     * @param qo      查询条件对象
     * @param current 当前页码
     * @param size    每页记录数
     * @return 分页结果
     */
    @Override
    public Page<PoliceTicketClient> pagePoliceTicketClients(PoliceTicketClientQO qo, Long current,
        Long size) {
        Page<PoliceTicketClient> page = new Page<>(current, size);
        var wrapper = Wrappers.lambdaQuery(PoliceTicketClient.class)
            .orderByDesc(PoliceTicketClient::getGmtCreated);

        // 字符串类型字段的模糊查询
        if (qo != null) {
            wrapper = wrapper
                // 字符串类型字段的模糊查询（使用isBlank判空）
                .like(qo.getName() != null && !qo.getName().isBlank(), PoliceTicketClient::getName,
                    qo.getName()).like(qo.getSystemName() != null && !qo.getSystemName().isBlank(),
                    PoliceTicketClient::getSystemName, qo.getSystemName())
                .like(qo.getSystemCode() != null && !qo.getSystemCode().isBlank(),
                    PoliceTicketClient::getSystemCode, qo.getSystemCode())
                .like(qo.getIp() != null && !qo.getIp().isBlank(), PoliceTicketClient::getIp,
                    qo.getIp())
                .like(qo.getPath() != null && !qo.getPath().isBlank(), PoliceTicketClient::getPath,
                    qo.getPath()).like(qo.getScript() != null && !qo.getScript().isBlank(),
                    PoliceTicketClient::getScript, qo.getScript())

                // 其他精确查询
                .eq(qo.getSchema() != null && !qo.getSchema().isBlank(),
                    PoliceTicketClient::getSchema, qo.getSchema())
                .eq(qo.getPort() != null, PoliceTicketClient::getPort, qo.getPort())
                .eq(qo.getCreator() != null, PoliceTicketClient::getCreator, qo.getCreator())
                .eq(qo.getId() != null, PoliceTicketClient::getId, qo.getId())
                .eq(qo.getStatus() != null, PoliceTicketClient::getStatus, qo.getStatus())

                // 时间范围查询
                .ge(qo.getGmtCreatedStart() != null, PoliceTicketClient::getGmtCreated,
                    qo.getGmtCreatedStart())
                .le(qo.getGmtCreatedEnd() != null, PoliceTicketClient::getGmtCreated,
                    qo.getGmtCreatedEnd());
        }

        // 按创建时间排序
        wrapper.orderByDesc(PoliceTicketClient::getGmtCreated);
        return this.page(page, wrapper);
    }
}