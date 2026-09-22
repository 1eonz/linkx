package cloudcmd.vo;

import lombok.Data;

/**
 * @program: back-new
 * @description: OpenapiCommonVo
 * @author: yj
 * @date: 2024-05-27
 **/

@Data
public class OpenapiCommonVo {

    private Integer pageNum;
    private Integer pageSize;

    private String chartName;
    private String orgName;

    private String name;
    private String shortName;
    private Integer level;

    private Integer type;
    private String code;

}
