package com.tdtech.cloudcmd.base.api.param;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/23 10:19
 */
@Data
public class TypeDto implements Serializable {

    private String code;

    private String name;

    private List<ItemDto> itemList;
}
