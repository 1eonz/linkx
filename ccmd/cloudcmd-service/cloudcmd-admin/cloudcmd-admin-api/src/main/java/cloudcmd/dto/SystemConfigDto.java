package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 系统配置DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDto implements Serializable {
    /**
     * 系统配置名称
     */
    private String key;

    /**
     * 系统配置内容
     */
    private String value;
}
