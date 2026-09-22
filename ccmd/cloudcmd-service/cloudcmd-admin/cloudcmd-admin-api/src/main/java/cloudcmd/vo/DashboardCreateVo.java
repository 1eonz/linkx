package cloudcmd.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @program: back-new
 * @description: DashboardCreateVo
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardCreateVo implements Serializable {

    private String dashboardName;
    private Integer status;
    private Integer type;
    private Long orgId;
    private String remark;

}
