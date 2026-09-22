package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPostLog;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface CollaborationPostLogService extends IService<CollaborationPostLog> {
    Page<CollaborationPostLog> page(int pageNum, int pageSize, String postName, Long orgId, String orgName, String relatedUserNames, String startTime, String endTime);

    void exportToExcel(String fileName, String postName, Long orgId, String orgName, String relatedUserNames, String startTime, String endTime, HttpServletResponse response) throws IOException;

    void saveLog(CollaborationPost collaborationPost, Integer operationType, String content);

    void saveLog(CollaborationPost collaborationPost, UserInfo user, Integer operationType, String content);
}