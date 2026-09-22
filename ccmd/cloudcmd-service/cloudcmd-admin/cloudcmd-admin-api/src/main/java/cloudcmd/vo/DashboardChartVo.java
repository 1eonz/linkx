package cloudcmd.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new
 * @description: DashboardChartVo
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartVo implements Serializable {

    private Long id;
    private Long dashboardId;
    private Long chartId;
    private String chartUrl;
    private String area;
    private Date gmtCreated;

}
