package com.tdtech.cloudcmd.im.jingxin.api;

import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.AiRecordQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public interface UserAvatarRpcApi {

    Map<String, String> getUserAvatar(List<String> fileIds);
}
