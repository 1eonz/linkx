package cloudcmd.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new
 * @description: ChartCreateVo
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartCreateVo implements Serializable {

    private String chartName;
    private String chartUrl;
    private Long orgId;
    private String remark;
    private Integer headStyle;
    private Integer type;
    private Integer isExport;
    private String dataModel;
    private Long countOrgId;
    private Integer countTimeType;
    private Date startTime;
    private Date endTime;
    private Integer num;
    private Integer unit;

}
