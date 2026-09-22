package com.tdtech.cloudcmd.icp.proxy.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CameraResp {

    /**
     * 业务权限： 1：GIS上报。
     */
    @JsonProperty("IPCPrivilege")
    private String ipcPrivilege;
    /**
     * 别名
     */
    private String alias;
    /**
     * 应用类型。 0：未配置。 1：民警。 4：基地台。 102：处警警车。 103：应急指挥车。 104：消防车。 105：押运车。 106：公务警车。 107：警用摩托车。
     */
    private String apptype;
    /**
     * 用户类型。 0：调度台用户。 1：固定摄像头。 2：PSTN网关用户。 3：TETRA网关用户。 4：PLMN网关用户。 5：外部PTT用户。 6：网关融合用户。 7：公网APP用户。 8：终端登陆用户。 9：终端用户。
     * 10：网关代理用户。 11：视频记录仪。 12：有线视频终端用户。 255：未分类用户。
     */
    private String category;
    /**
     * 部门ID
     */
    private String departmentid;
    /**
     * 对接类型。 0：28181对接协议 1：其它。
     */
    private String ipctype;
    /**
     * 摄像头号码
     */
    private String isdn;
    /**
     * 节点名称
     */
    private String levelName;
    /**
     * 层级编号
     */
    private String levelNumber;
    /**
     * 摄像头已绑定的所有层级
     * <p>
     * 格式：层级编号1;层级编号2;层级编号3（分号分隔）
     * 例如：19967854063781048;19967854250012637;19967854369160912
     * <p>
     * 用途：用于权限过滤，判断用户是否有权限查看该摄像头
     * 只要用户权限中的任意层级出现在 levelNumberList 中，该摄像头就对该用户可见
     */
    private String levelNumberList;
    /**
     * 地址信息，以","进行分隔。
     * •第1位表示经度
     * •第2位表示纬度
     * •第3位表示高度
     * •第4位表示安装位置
     */
    private String location;
    /**
     * name
     */
    private String name;
    /**
     * 优先级（1~15）。
     */
    private String priority;
    /**
     * 形状。 0：未配置。 1：球形。 2：枪形。
     */
    private String shape;
    /**
     * 子用户类型
     * 0：调度台用户
     * 1：固定摄像头
     * 2：PSTN网关用户
     * 3：TETRA网关用户
     * 4：PLMN网关用户
     * 5：外部PTT用户
     * 6：网关融合用户
     * 8：终端登陆用户
     * 9：终端用户
     * 10：网关代理用户
     * 11：视频记录仪
     * 12：有线视频终端用户
     * 255：未分类用户
     */
    private String subusercategory;
    /**
     * 终端类型。 0：本地用户。 1：外部用户。
     */
    private String uetype;
    /**
     * VPN的ID。
     */
    private String vpnid;
    /**
     * 入VPN权限。 0-禁止。 1-允许。
     */
    private String vpnin;
    /**
     * 出VPN权限。 0-禁止。 1-允许。
     */
    private String vpnout;

}
