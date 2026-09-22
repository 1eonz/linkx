package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 南向应用更新DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppCallableUpdateDto extends AppCallableCreateDto{
    /**
     * 主键id
     */
    private Long id;
}
