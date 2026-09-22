package com.tdtech.cloudcmd.im.jingxin.api.entity.im;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserGetResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ImUserVo> results;

    private List<UserFailVo> failures;
}
