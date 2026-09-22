package cloudcmd.rsp;

import com.tdtech.cloudcmd.util.ListUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @program: back-new
 * @description: ChartPageRsp
 * @author: yj
 * @date: 2024-05-17
 **/

@Data
public class ChartPageRsp<T> implements Serializable {

    private Integer current;
    private Integer size;
    private Integer total = 0;
    private List<T> records = ListUtils.EMPTY_LIST;

}
