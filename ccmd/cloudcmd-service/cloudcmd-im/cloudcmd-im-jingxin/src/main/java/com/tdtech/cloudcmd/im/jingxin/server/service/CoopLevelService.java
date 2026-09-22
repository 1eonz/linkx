package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevel;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelReq;

import java.util.List;

public interface CoopLevelService {

    /**
     * 创建协同级节点
     *
     * @param coopLevelReq 协同级节点信息
     * @return 是否创建成功
     */
    boolean createCoopLevelNode(CoopLevelReq coopLevelReq);

    boolean updateCoopLevelNode(String levelId, CoopLevelReq coopLevelReq);

    boolean deleteCoopLevelNode(String levelId);

    List<CoopLevel> childrenLevelNode(String levelId);
}
