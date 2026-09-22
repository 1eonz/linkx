package com.tdtech.cloudcmd.im.openapi.controller.entity;

import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.Category;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ToString
public class AiRecordCountByCategory implements Serializable {

    private Long id;

    private String name;

    private Long count;

}
