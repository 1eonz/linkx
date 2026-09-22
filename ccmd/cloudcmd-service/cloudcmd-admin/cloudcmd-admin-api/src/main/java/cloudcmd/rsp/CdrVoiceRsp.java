package cloudcmd.rsp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-v1
 * @description: CdrVoiceRsp
 * @author: yj
 * @date: 2024-06-04
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CdrVoiceRsp implements Serializable {

    private Long id;
    private Long callerId;
    private String callerName;
    private String callerNo;
    private Long calleeId;
    private String calleeName;
    private String calleeNo;
    private Date callTime;
    private Date pickupTime;
    private Long talkDuration;
    private Date hangupTime;
    private Integer result;
    private Date gmtCreated;

}
