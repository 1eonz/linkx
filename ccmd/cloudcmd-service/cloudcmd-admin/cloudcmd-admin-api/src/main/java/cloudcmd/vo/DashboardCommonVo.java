package cloudcmd.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: back-new
 * @description: DashboardCommonVo
 * @author: yj
 * @date: 2024-05-27
 **/

@Data
public class DashboardCommonVo implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private String chartName;
    private String dashboardName;
    private String orgName;

    private String name;
    private String shortName;
    private Integer level;
    private Integer status;
    private Long typeId;
    private Integer priority;
    private String rescueStation;
    private String ignitingSubstance;
    private String dispatchName;
    private String dispatchedName;
    private Integer isSelf;

    private Integer type;
    private String code;

    private String callTalkingName;
    private String callTalkingNo;
    private Integer validCall;
    private Long eventId;

    /**
     * 主叫人名称
     */
    private String callerName;
    /**
     * 主叫人号码
     */
    private String callerNo;
    /**
     * 被叫人名称
     */
    private String calleeName;
    /**
     * 被叫人号码
     */
    private String calleeNo;

    private Integer result;

    /**
     * 发送人名称
     */
    private String senderName;
    /**
     * 发送人号码
     */
    private String senderNo;
    /**
     * 收件人名称
     */
    private String receiverName;
    /**
     * 收件人号码
     */
    private String receiverNo;

}
