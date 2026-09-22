package cloudcmd.rsp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @program: back-new
 * @description: DashboardChartRsp
 * @author: yj
 * @date: 2024-05-17
 **/

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardChartRsp extends DashboardRsp implements Serializable {

    private List<DashboardChartListRsp> dashboardChartList;

}
