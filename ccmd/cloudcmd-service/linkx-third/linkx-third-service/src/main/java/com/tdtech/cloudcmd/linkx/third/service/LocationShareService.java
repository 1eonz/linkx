package com.tdtech.cloudcmd.linkx.third.service;

import com.tdtech.cloudcmd.linkx.third.vo.LocationShareCreateVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareExitVo;
import com.tdtech.cloudcmd.linkx.third.vo.LocationShareJoinVo;

public interface LocationShareService {

    Long createLocationShare(LocationShareCreateVo vo);

    LocationShareDetailVo getMyLocationShare(Long shareId);

    void joinLocationShare(Long shareId, LocationShareJoinVo vo);

    void exitLocationShare(Long shareId, LocationShareExitVo vo);

}
