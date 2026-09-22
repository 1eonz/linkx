package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @program: back-new-huawei
 * @description: DateParam
 * @author: yj
 * @date: 2024-08-19
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadParam implements Serializable {

    private Date startTime;
    private Date endTime;
    private String filedFilter;
    private Long orgId;


    // 设置CDR相关表的主叫No和被叫No
    private List<String> accountNos;

}
