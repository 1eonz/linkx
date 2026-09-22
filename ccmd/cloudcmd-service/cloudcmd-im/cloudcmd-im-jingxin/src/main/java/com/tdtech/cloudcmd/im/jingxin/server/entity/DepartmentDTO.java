package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.*;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DepartmentDTO {

    private String id;

    private String name;

    private String code;

    List<DepartmentDTO> children;
}
