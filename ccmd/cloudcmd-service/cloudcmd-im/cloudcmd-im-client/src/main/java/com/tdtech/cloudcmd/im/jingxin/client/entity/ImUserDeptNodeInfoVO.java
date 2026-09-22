package com.tdtech.cloudcmd.im.jingxin.client.entity;

import lombok.Data;

import java.util.List;

@Data
public class ImUserDeptNodeInfoVO {
    private List<ImUserDeptInfoVO> departments;
    private List<ImUserNodeInfoVO> nodes;
}
