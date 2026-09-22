package cloudcmd.vo;

import cloudcmd.rsp.DashboardChartListRsp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @program: back-new
 * @description: DashboardUpdateVo
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdateVo implements Serializable {

    private Long id;
    private String dashboardName;
    private Integer status;
    private Long orgId;
    private Integer type;
    private String remark;
    private Integer updateFlag = 0; // 默认 0：0-更新dashboard 1-更新dashboard下的chart
    private List<DashboardChartListRsp> dashboardChartList;

}
