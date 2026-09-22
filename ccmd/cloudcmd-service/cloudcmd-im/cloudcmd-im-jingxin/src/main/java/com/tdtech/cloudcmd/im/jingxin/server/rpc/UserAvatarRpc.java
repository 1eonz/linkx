package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.DutyScheduleRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.UserAvatarRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.duty.DutyScheduleUserVO;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排班服务 Dubbo RPC 接口实现
 * <p>
 * 提供 Dubbo 远程调用接口，供其他服务（如 auth 服务）调用
 */
@DubboService
public class UserAvatarRpc implements UserAvatarRpcApi {



    @Resource
    private FileUtil fileUtil;

    @Resource
    private ImHttpClient imHttpClient;

    @Override
    public Map<String, String> getUserAvatar(List<String> fileIds) {
        Map<String, String> userIdAvatarMap = new HashMap<>();
        for (String fileId : fileIds) {
            userIdAvatarMap.put(fileId, fileUtil.getAvatarPathOrDownload(imHttpClient, fileId));
        }
        return userIdAvatarMap;
    }
}
