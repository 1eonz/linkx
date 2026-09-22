package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @program: back-new
 * @description: DashboardCreateDto
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardCreateDto implements Serializable {

    private String dashboardName;
    private Integer status;
    private Integer type;
    private Long orgId;
    private String remark;
    private String segment;
}
