package com.tdtech.cloudcmd.linkx.third.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Key Value 的键值对
 *
 * @author author
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyValue<K, V> implements Serializable {
    private K key;
    private V value;
}
