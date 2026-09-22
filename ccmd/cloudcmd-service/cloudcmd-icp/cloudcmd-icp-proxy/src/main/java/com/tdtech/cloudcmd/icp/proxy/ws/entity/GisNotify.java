package com.tdtech.cloudcmd.icp.proxy.ws.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "GIS位置通知信息")
public class GisNotify {

    @JsonProperty("isdn")
    @Schema(description = "ISDN号码", example = "123456789")
    private String isdn;
    /**
     * MCC+MNC。表示终端所在的主PLMN上报，例如MCC是460，MNC是01，上报就是46001，字符串类型，获取不到为NULL。
     */
    @JsonProperty("plmnid")
    @Schema(description = "MCC+MNC主PLMN号", example = "46001")
    private String plmnid;
    /**
     * 设备类型。
     */
    @JsonProperty("devicetype")
    @Schema(description = "设备类型", example = "mobile")
    private String devicetype;
    /**
     * 速度：表示终端运动速度，x.y浮点数类型，获取不到速度时为0.0，单位m/s。
     */
    @JsonProperty("speed")
    @Schema(description = "速度", example = "5.0")
    private String speed;
    /**
     * 水平朝向(方向\null)：表示水平角度方向，0~360度，如果获取不到就为空。
     */
    @JsonProperty("direction")
    @Schema(description = "水平朝向", example = "90.0")
    private String direction;
    /**
     * 地址信息，以","进行分隔。 •第1位表示经度 •第2位表示纬度 •第3位表示高度 •第4位表示方向 •第5位表示上报类型◾0表示周期上报 ◾1表示紧急事件开启 ◾2表示紧急事件结束 ◾101表示倒地告警触发
     * ◾102表示倒地告警清除 ◾103表示历史位置下发 ◾104表示登入上报 ◾105表示登出上报 ◾106表示关机上报 ◾3表示倒地告警触发 ◾4表示倒地告警清除 ◾5表示历史位置下发 •第6位表示搜星状态◾0表示信号正常
     * ◾1表示搜星失败 ◾101表示其它异常 ◾102表示GPS未注册 ◾103表示GPS上报权限关闭 ◾2表示其他异常 ◾3表示GPS未注册 ◾4表示GPS上报权限关闭 •第7位GPS上报时间点
     * (UTC的时间戳，1970年1月1日以来的毫秒数)
     */
    @JsonProperty("location")
    @Schema(description = "地址信息", example = "120.123,30.456,10.0,0,0,0,1609459200000")
    private String location;
    /**
     * GPS搜星个数
     */
    @JsonProperty("gpsstar")
    @Schema(description = "GPS搜星个数", example = "8")
    private String gpsstar;
    /**
     * 定位模式。 1、“GPS”，GPS定位。 2、“BD”，北斗定位。 3、“null”，搜星失败。
     */
    @JsonProperty("locatmode")
    @Schema(description = "定位模式", example = "GPS")
    private String locateMode;
    /**
     * UE所在小区的ECGI。 BitsOctets876543211MCC digit 2MCC digit 12MNC digit 3MCC digit 33MNC digit 2MNC digit
     * 14SpareECI5~7ECI (E-UTRAN Cell Identifier) 其中，ECI包含28个比特。字节4的比特4为最高位，字节7的比特1为最低位。
     * 定位业务UE上报的位置消息中包含该信息，ECGI的格式为String，此处是将上述7字节二进制数字转换成十六进制数在消息中传输。
     */
    @JsonProperty("cellid")
    @Schema(description = "小区ECGI", example = "001010000000001")
    private String cellid;
    /**
     * 终端名称
     */
    @JsonProperty("cellname")
    @Schema(description = "终端名称", example = "Cell001")
    private String cellname;

}
