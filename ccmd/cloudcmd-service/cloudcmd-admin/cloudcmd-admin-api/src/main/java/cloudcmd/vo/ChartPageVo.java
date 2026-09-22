package cloudcmd.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @program: back-new
 * @description: ChartPageVo
 * @author: yj
 * @date: 2024-05-16
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartPageVo implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String chartName;
    private Long orgId;
    private List<Long> organizationIds;
    private String name;
}
