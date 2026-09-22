package cloudcmd.rsp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CdrMsgRsp implements Serializable {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderNo;
    private Long receiverId;
    private String receiverName;
    private String receiverNo;
    private Integer messageType;
    private Date callTime;
    private Integer result;
    private Integer isRead;
    private Date gmtCreated;
}
