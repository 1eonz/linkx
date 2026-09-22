package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-huawei
 * @description: UmpExecutorRsp
 * @author: yj
 * @date: 2024-06-12
 **/

@Data
public class UmpExecutorRsp implements Serializable {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 编号（工号、车牌号等）
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    private Long organizationId;

    private String organizationName;

    /**
     * 执行者分类ID
     */
    private Long type;

    private String position;

    private String sex;

    /**
     * 状态  0-正常 1-离职/报废/丢失
     */
    private Integer status;

    private Integer state;

    private Double lon;

    private Double lat;

    private Date gmtCreated;

}
