package cloudcmd.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new
 * @description: DashboardChartListDto
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartListDto implements Serializable {

    private Long id;
    private Long dashboardId;
    private Long chartId;
    private String chartUrl;
    private String area;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;

}
