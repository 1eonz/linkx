package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @program: back-new-huawei
 * @description: CommonDownloadDto
 * @author: yj
 * @date: 2024-08-19
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonDownloadDto implements Serializable {

    // cdr
    private String id;
    private String callerId;
    private String callerName;
    private String callerNo;
    private String calleeId;
    private String calleeName;
    private String calleeNo;
    private Date callTime;
    private Date pickupTime;
    private String talkDuration;
    private Date hangupTime;
    private Integer result;
    private String sharpness;
    private Date gmtCreated;
    private String senderId;
    private String senderName;
    private String senderNo;
    private String receiverId;
    private String receiverName;
    private String receiverNo;
    private Integer messageType;
    private Integer isRead;

    // ump
    private String organizationId;
    private String organizationName;
    private String name;
    private String parentId;
    private String code;
    private Integer category;
    private String type;
    private String capability;
    private String remark;
    private Integer status;
    private Integer state;
    private double lon;
    private double lat;
    private String title;
    private String typeId;
    private Integer typeLevel;
    private Integer priority;
    private Date occurTime;
    private String address;
    private String shortName;
    private Integer level;
    private String icon;
    private String position;
    private String sex;
    private String districtId;
    private String isdn;
    private Integer source;
    private String keyUnit;
    private String planName;
    private Integer planCategory;
    private Integer planType;
    private String planCode;
    private String eventType;
    private String authorizeOrgnization;
    private String issueOrgnization;
    private Date issuedDate;
    private Date revisionDate;
    private Integer scope;
    private Integer planVersion;

    public static String CALL_TIME = "call_time";
    public static String PICKUP_TIME = "pickup_time";
    public static String GMT_CREATED = "gmt_created";

}
