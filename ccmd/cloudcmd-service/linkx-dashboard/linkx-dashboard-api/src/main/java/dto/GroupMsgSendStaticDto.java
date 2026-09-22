package dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMsgSendStaticDto implements Serializable {

    /**
     * 消息ID
     */
    private Long msgId;

    /**
     * SESSIONID
     */
    private Long sessionId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 部门id
     */
    private Long departmentId;

    /**
     * 消息时间
     */
    private Date msgTime;

    /**
     * 消息类型
     */
    private Integer category;

    /**
     * 子消息类型
     */
    private Integer msgType;

    /**
     * 创建时间
     */
    private Date gmtCreated;
}