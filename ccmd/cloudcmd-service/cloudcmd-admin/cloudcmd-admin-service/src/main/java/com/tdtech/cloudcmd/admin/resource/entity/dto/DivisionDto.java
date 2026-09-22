package com.tdtech.cloudcmd.admin.resource.entity.dto;

import java.io.Serializable;

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
public class DivisionDto implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 行政划分文件名称
     */
    private String name;

    /**
     * 行政划分文件地址
     */
    @NotBlank(message = "Illegal address")
    private String address;
}
