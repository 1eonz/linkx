package com.tdtech.cloudcmd.admin.resource.rpc;

import cloudcmd.service.rpc.AppInfoRpcService;
import cloudcmd.vo.AppInfoResp4RpcVO;
import cloudcmd.vo.UserCommonAppResp4RpcVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tdtech.cloudcmd.admin.resource.entity.AppInfo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.AppInfoDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.UserCommonAppRespVO;
import com.tdtech.cloudcmd.admin.resource.mapper.AppInfoMapper;
import com.tdtech.cloudcmd.admin.resource.service.IAppInfoService;
import com.tdtech.cloudcmd.admin.resource.service.UserCommonAppService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.ListUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@DubboService
public class AppInfoRpcServiceImpl implements AppInfoRpcService {

    @Autowired
    private AppInfoMapper appInfoMapper;

    @Autowired
    private IAppInfoService appInfoService;

    @Autowired
    private UserCommonAppService userCommonAppService;

    @Override
    public List<AppInfoResp4RpcVO> getAppInfoByIds(List<Long> appIds,Integer scope) {
        if (ListUtils.isBlankList(appIds)) {
            return new ArrayList<>();
        }
        List<AppInfoDO> apps = appInfoService.selectList(scope);
        List<AppInfoDO> infoDOS = apps.stream().filter(it -> Objects.nonNull(it.getId()) && appIds.contains(it.getId()))
                .sorted(Comparator.comparing(AppInfoDO::getSort)).collect(Collectors.toList());
        return BeanCopyUtils.copyList(infoDOS, AppInfoResp4RpcVO::new);
    }

    @Override
    public List<AppInfoResp4RpcVO> getAllAppInfoByIds(Integer scope) {
        List<AppInfoDO> infoDOS = appInfoService.selectList(scope);
        return BeanCopyUtils.copyList(infoDOS, AppInfoResp4RpcVO::new);
    }

    @Override
    public List<UserCommonAppResp4RpcVo> getMy(String userId, Integer terminalType,Integer scope) {
        List<UserCommonAppRespVO> vos = userCommonAppService.getMy(userId, terminalType ,scope);
        return BeanCopyUtils.copyList(vos, UserCommonAppResp4RpcVo::new, (source, target) -> {
            // 手动复制 app 对象，因为两个类的 AppInfoRespVO 类型不同
            if (source.getApp() != null) {
                AppInfoResp4RpcVO targetApp = new AppInfoResp4RpcVO();
                BeanCopyUtils.copyBean(source.getApp(), targetApp);
                target.setApp(targetApp);
            }
        });
    }

    public Long getH5AppCount() {
        QueryWrapper<AppInfo> wrapper = new QueryWrapper<>();
        // status=0 表示上架状态
        wrapper.eq(AppInfo.STATUS, 0);
        // is_deleted=0 表示未删除
        wrapper.eq(AppInfo.IS_DELETED, 0);
        return appInfoMapper.selectCount(wrapper);
    }
}
