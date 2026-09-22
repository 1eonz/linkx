package cloudcmd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcpConfigDto implements java.io.Serializable {
    private Long id;

    private String ip;

    private Integer port;

    private String username;

    private String password;

    private String wssUrl;

    private String departmentId;

    private String departmentName;

    private String cameraLevelId;

    private String cameraLevelName;

    private Integer protocol;

    private Integer environment;

    private Integer status;

    private String remark;
}