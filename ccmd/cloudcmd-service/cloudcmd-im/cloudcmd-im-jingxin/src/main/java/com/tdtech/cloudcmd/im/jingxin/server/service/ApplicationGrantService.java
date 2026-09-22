package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrant;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrantVO;

/**
 * 系统应用授权信息服务（操作新表 linkx_open.tb_application_grant）
 */
public interface ApplicationGrantService {

    /**
     * 新建应用授权
     *
     * @param vo VO
     * @return 主键id
     */
    Long createApplicationGrant(ApplicationGrantVO vo);

    /**
     * 分页查询应用授权
     *
     * @param vo VO
     * @return 分页结果
     */
    Page<ApplicationGrant> pageApplicationGrant(ApplicationGrantVO vo);

    /**
     * 应用授权详情
     *
     * @param id 主键id
     * @return 应用授权
     */
    ApplicationGrant getApplicationGrant(Long id);

    /**
     * 更新应用授权
     *
     * @param vo VO
     */
    void updateApplicationGrant(ApplicationGrantVO vo);

    /**
     * 删除应用授权
     *
     * @param id 主键id
     */
    void deleteApplicationGrant(Long id);
}