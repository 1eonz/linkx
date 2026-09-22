package com.tdtech.cloudcmd.admin.resource.service;

import com.tdtech.cloudcmd.admin.resource.entity.IcpConfig;
import com.tdtech.cloudcmd.admin.resource.entity.vo.IcpConfigVO;

public interface IIcpConfigService {

    void updateIcpConfig(IcpConfigVO icpConfigVO);

    IcpConfig getIcpConfig();
}
