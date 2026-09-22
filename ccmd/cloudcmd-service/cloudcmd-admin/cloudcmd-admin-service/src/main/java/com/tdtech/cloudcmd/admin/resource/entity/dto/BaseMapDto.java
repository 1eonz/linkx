package com.tdtech.cloudcmd.admin.resource.entity.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@Builder(toBuilder = true)
public class BaseMapDto implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 序号
     */
    private Long number;

    /**
     * 底图文件名称
     */
    @NotBlank(message = "Illegal name")
    private String name;

    /**
     * 底图文件大小
     */
    private String size;

    /**
     * 创建时间
     */
    private Date created;

    /**
     * 瓦片地址
     */
    private String tiles;

    /**
     * 底图数据元数据
     */
//    private MetaData metaData;
}
