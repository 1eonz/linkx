package com.tdtech.cloudcmd.mysql.entity;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.validation.annotation.Validated;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ToString
@Validated
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分页参数")
public class CcmdPageParam implements Serializable {

    @NotNull
    @Schema(description = "当前页码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long pageNum = 1L;

    @NotNull
    @Schema(description = "每页大小", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long pageSize = 10L;

}
