package com.tdtech.cloudcmd.base.entity.dto;

import java.util.Date;
import java.util.List;

import com.tdtech.cloudcmd.base.entity.DictionaryItem;
import com.tdtech.cloudcmd.base.entity.DictionaryType;

import lombok.Data;

/**
 * @author mWX556161
 * @date 2020/6/16 9:17
 */
@Data
public class DictionaryTypeDto {

    /**
     * 编号，主键ID。数据初始化。顺序加1
     */
    private Long id;

    /**
     * 类型编码。根据编码获取字典项
     */
    private String code;

    /**
     * 类型名称
     */
    private String name;

    private Date gmtCreated;

    private Date gmtModified;

    /**
     * 与其相关的所有字典配置值
     */
    private List<DictionaryItem> dictionaryItemList;

    public void build(DictionaryType dictionaryType) {
        this.id = dictionaryType.getId();
        this.code = dictionaryType.getCode();
        this.name = dictionaryType.getName();
        this.gmtCreated = dictionaryType.getGmtCreated();
        this.gmtModified = dictionaryType.getGmtModified();
    }

}
