package com.tdtech.cloudcmd.bean;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mWX556161
 * @date 2020/6/9 13:53
 */

@Data
@Deprecated
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /**
     * 数据总数
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long total;

    /**
     * 当前页数
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long current;

    /**
     * 总页数
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pages;

    /**
     * 当前数量
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long size;

    /**
     * 数据
     */
    private List<T> records;

    public static <T> PageResult<T> fromIPage(IPage<?> page, List<T> records) {
        var r = new PageResult<T>();
        r.total = page.getTotal();
        r.current = page.getCurrent();
        r.pages = page.getPages();
        r.size = page.getSize();
        r.records = records;
        return r;
    }
}
