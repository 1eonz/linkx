package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-huawei
 * @description: UmpEquipmentRsp
 * @author: yj
 * @date: 2024-06-12
 **/

@Data
public class UmpEquipmentRsp implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 装备编号。属性模式时，为IMEI。岗位模式时，为ISDN。
     */
    private String code;

    /**
     * 资源名称
     */
    private String name;

    /**
     * 装备种类(数据字典定义) 0-未知 1-设备 2-坐席 3-车辆（驾驶工具） 4-摩托车
     */
    private Integer category;

    /**
     * 装备分类ID。 0-未知

     */
    private Long type;

    /**
     * 组织ID。坐席时，为所在组织ID。
     */
    private Long organizationId;

    /**
     * 组织名称
     */
    private String organizationName;
    /**
     * 装备能力
     */
    private String capability;

    /**
     * 备注描述
     */
    private String remark;

    /**
     * 状态  0-正常 1-离职/报废/丢失
     */
    private Integer status;

    private Integer state;

    private Double lon;

    private Double lat;

    /**
     * 创建时间
     */
    private Date gmtCreated;

}
