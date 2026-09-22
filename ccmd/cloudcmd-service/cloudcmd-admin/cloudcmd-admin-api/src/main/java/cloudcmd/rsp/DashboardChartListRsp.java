package cloudcmd.rsp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new
 * @description: DashboardChartListRsp
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardChartListRsp implements Serializable {

    private Long id;
    private Long dashboardId;
    private Long chartId;
    private String chartUrl;
    private Integer builtIn;
    private Integer isExport;
    private String chartName;
    private Integer headStyle;
    private Integer type;
    private String area;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;

    public String getChartName() {
        return I18nUtil.get(chartName);
    }

}
