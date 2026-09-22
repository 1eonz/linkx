package com.tdtech.cloudcmd.im.jingxin.server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoopLevel {

    private Long id;

    private String name;

    private Long parentId;

    private Date gmtCreated;

    private Boolean hasChildren;
}
