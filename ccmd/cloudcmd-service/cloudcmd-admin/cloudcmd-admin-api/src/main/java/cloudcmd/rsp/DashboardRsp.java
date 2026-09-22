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
 * @description: DashboardRsp
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardRsp implements Serializable {

    private Long id;
    private String dashboardName;
    private Integer status;
    private Integer type;
    private Integer builtIn;
    private Long orgId;
    private String orgName;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date gmtCreated;

    public String getDashboardName() {
        return I18nUtil.get(dashboardName);
    }

}
