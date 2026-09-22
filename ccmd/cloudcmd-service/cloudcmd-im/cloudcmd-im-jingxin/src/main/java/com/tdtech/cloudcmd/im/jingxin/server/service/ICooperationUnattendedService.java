
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattended;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CooperationUnattendedVO;

public interface ICooperationUnattendedService extends IService<CooperationUnattended> {

    Page<CooperationUnattendedVO> findPage(int pageNum, int pageSize, String keywords, String deptCode, String startTime, String endTime);

    boolean saveUnattendedRecord(Long postId);
}