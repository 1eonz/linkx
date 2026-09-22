package cloudcmd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: back-new-huawei
 * @description: ResourceDto
 * @author: yj
 * @date: 2024-06-12
 **/

@Data
public class ResourceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 编号（工号、车牌号等）
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    private String orgId;

}
