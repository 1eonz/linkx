package com.tdtech.cloudcmd.icp.proxy.client.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CameraLevelResp {
    private String highLevelNumber;//上级编号
    private String level;//层级
    private String levelNumber;//层级编号
    private String nodeName;//层级名称
    private String displayPriority;//层级在上级层级中的显示顺序,-1 为无效值,数字越小，在界面上列表中的显示顺序越前

}
