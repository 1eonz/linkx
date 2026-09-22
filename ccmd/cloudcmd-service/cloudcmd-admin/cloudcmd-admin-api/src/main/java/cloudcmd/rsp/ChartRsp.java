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
 * @description: ChartRsp
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartRsp implements Serializable {

    private Long id;
    private String chartName;
    private String chartUrl;
    private Integer builtIn;
    private Integer isExport;
    private String dataModel;
    private Long countOrgId;
    private Integer countTimeType;
    private Date startTime;
    private Date endTime;
    private Integer num;
    private Integer unit;
    private Long orgId;
    private String remark;
    private Integer headStyle;
    private Integer type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;

    public String getChartName() {
        return I18nUtil.get(chartName);
    }

    public String getRemark() {
        return I18nUtil.get(remark);
    }

}
