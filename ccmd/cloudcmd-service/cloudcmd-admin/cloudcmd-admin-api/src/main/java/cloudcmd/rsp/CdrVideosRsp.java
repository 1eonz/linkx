package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CdrVideosRsp implements Serializable {

    private Long id;
    private Long callerId;
    private String callerName;
    private String callerNo;
    private Long calleeId;
    private String calleeName;
    private String calleeNo;
    private Long talkDuration;
    private Date callTime;
    private Date pickupTime;
    private Date hangupTime;
    private Integer result;
    private String sharpness;
    private Date gmtCreated;
}
