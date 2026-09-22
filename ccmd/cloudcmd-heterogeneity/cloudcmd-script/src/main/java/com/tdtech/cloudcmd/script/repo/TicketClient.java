package com.tdtech.cloudcmd.script.repo;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Serdeable
@MappedEntity("tb_police_ticket_client")
public record TicketClient(@Id Long id, // ID
                           @NotNull String name, // 配置名称
                           @NotNull String systemName, // 所属系统
                           @NotNull String systemCode, // 系统编码
                           @NotNull String script,//
                           @NotNull Integer status//状态，0启用 1停用
) {

}
