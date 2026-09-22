package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Key Value 的键值对，用于搜索查询参数
 *
 * @author author
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchPair<K, V> implements Serializable {
    /**
     * 搜索键
     */
    private K key;

    /**
     * 搜索值
     */
    private V value;
}
