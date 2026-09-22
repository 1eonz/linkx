package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @program: back-new
 * @description: DashboardUpdateDto
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUpdateDto implements Serializable {

    private Long id;
    private String dashboardName;
    private Integer status;
    private Long orgId;
    private Integer type;
    private String remark;
    private Integer updateFlag; // 0-更新dashboard 1-更新dashboard下的chart
    private List<DashboardChartListDto> dashboardChartList;
    private String segment;
}
