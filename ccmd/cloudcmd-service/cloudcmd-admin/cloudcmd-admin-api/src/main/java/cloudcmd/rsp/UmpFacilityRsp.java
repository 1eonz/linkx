package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-huawei
 * @description: UmpFacilityRsp
 * @author: yj
 * @date: 2024-06-12
 **/

@Data
public class UmpFacilityRsp implements Serializable {

    private Long id;

    /**
     * 编号
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 设施种类（数据字典定义） 0-未知 1-摄像头 2-治安岗亭 3-消防栓
     */
    private Integer category;

    private Long type;

    /**
     * 所在辖区
     */
    private Long districtId;

    /**
     * 所属组织ID(可以不属于任何组织，但在某一个辖区范围内)
     */
    private Long organizationId;

    private String organizationName;

    /**
     * 所在地址
     */
    private String address;

    /**
     * 备注
     */
    private String remark;

    /**
     * 通讯账号isdn号
     */
    private String isdn;

    /**
     * 状态  0-正常 1-报废 2-丢失，可扩展
     */
    private Integer status;

    private Integer source;

    private Integer state;

    private Double lon;

    private Double lat;

    /**
     * 创建时间
     */
    private Date gmtCreated;


}
