package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-huawei
 * @description: UmpOrganizationRsp
 * @author: yj
 * @date: 2024-06-12
 **/

@Data
public class UmpOrganizationRsp implements Serializable {

    /**
     * 组织id
     */
    private Long id;

    /**
     * 组织编号
     */
    private String code;
    /**
     * 名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long type;

    /**
     * 上级组织机构ID
     */
    private Long parentId;

    /**
     * 组织机构简称
     */
    private String shortName;

    /**
     * 组织所在层级
     */
    private Integer level;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-正常,1-禁用
     */
    private Integer status;


    /**
     * 行政区划ID
     */
    private Long districtId;

    private String keyUnit;

    /**
     * 创建时间
     */
    private Date gmtCreated;

}
