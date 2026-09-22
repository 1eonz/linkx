package com.tdtech.cloudcmd.mysql.entity;

import com.tdtech.cloudcmd.util.BeanCopyUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页数据封装")
public class CcmdPage<T> implements Serializable {

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页大小")
    private Long pageSize;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "记录列表")
    private List<T> records;

    public <E> CcmdPage<E> mult(List<E> newRecords) {
        return new CcmdPage<>(pageNum, pageSize, total, newRecords);
    }

    public <E> CcmdPage<E> mult(Supplier<E> supplier) {
        return new CcmdPage<>(pageNum, pageSize, total,
            BeanCopyUtils.copyList(records, supplier));
    }

    public static <E> CcmdPage<E> empty(Long pageNum, Long pageSize) {
        return new CcmdPage<>(pageNum, pageSize, 0L, null);
    }
    public static <E> CcmdPage<E> empty(CcmdPageParam pageParam) {
        return empty(pageParam.getPageNum(), pageParam.getPageSize());
    }

    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }
}
