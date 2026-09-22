package com.tdtech.cloudcmd.im.jingxin.server.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author lsc
 * @date 2025/8/11
 **/
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_collaboration_client")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollaborationClient {
    /**
     * id
     */
    private Long id;
    /**
     * 应用id
     */
    private String clientId;
    /**
     * 应用密钥
     */
    private String clientSecret;
    /**
     * 应用名称
     */
    private String clientName;
    /**
     * 应用类型
     */
    private String clientType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;
    /**
     * token有效期
     */
    private Integer tokenTime;
    /**
     * 刷新token有效期
     */
    private Integer refreshTokenTime;

}
