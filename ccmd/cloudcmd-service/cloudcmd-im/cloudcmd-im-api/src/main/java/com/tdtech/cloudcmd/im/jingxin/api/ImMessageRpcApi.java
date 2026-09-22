package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.im.IMMsgRspVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.MMSMsgVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendImMessageCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.SendNotificationCO;

public interface ImMessageRpcApi {

    IMMsgRspVo sendImMessage(SendImMessageCO co);

    IMMsgRspVo sendNotification(SendNotificationCO co);

    /**
     * 根据 fileId（文件存储记录的 fileMd5）构建彩信消息体
     * <p>
     * 查询文件存储记录，读取物理文件并上传至警信换取 fileKey，再填充 MMSMsgVo 必填字段。
     *
     * @param fileId 文件存储记录的 fileMd5（openapi 上传接口返回值）
     * @return 彩信消息体
     */
    MMSMsgVo buildMmsMsgVo(String fileId);
}