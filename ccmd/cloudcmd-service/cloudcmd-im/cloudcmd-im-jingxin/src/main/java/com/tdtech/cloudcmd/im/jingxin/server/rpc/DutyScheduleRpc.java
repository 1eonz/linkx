package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.DutyScheduleRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.duty.DutyScheduleUserVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排班服务 Dubbo RPC 接口实现
 * <p>
 * 提供 Dubbo 远程调用接口，供其他服务（如 auth 服务）调用
 */
@DubboService
public class DutyScheduleRpc implements DutyScheduleRpcApi {

    @Resource
    private IDutyScheduleService dutyScheduleService;

    @Override
    public List<DutyScheduleUserVO> listDutyScheduleUsers(List<Long> userIdList, LocalDateTime dutyStartDate,
                                                          LocalDateTime dutyEndDate, Long dutyType) {
        // 调用本地服务查询排班用户列表（包含头像处理逻辑）
        List<DutyScheduleVO> dutyScheduleUsers =
                dutyScheduleService.listDutyScheduleUsers(userIdList, dutyStartDate, dutyEndDate, dutyType);
        // 转换为 RPC 接口返回类型，并保留头像字段
        // 注意：BeanCopyUtils.copyBean 不会复制 avatar 字段（因为 DutyScheduleVO 和 DutyScheduleUserVO 字段不完全对应）
        // 因此需要手动设置 avatar 字段
        return dutyScheduleUsers.stream().map(vo -> {
            DutyScheduleUserVO userVO = BeanCopyUtils.copyBean(vo, DutyScheduleUserVO::new);
            // 【头像传递】将处理后的头像 URL 路径传递给调用方
            userVO.setAvatar(vo.getAvatar());
            return userVO;
        }).collect(Collectors.toList());
    }
}
