package com.tdtech.cloudcmd.admin.resource.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDiff {
    private String fieldName;           // 字段名
    private DiffType type;              // 变更类型：ADDED/REMOVED/MODIFIED/UNCHANGED
    private Object oldValue;            // 旧值（为null表示新增）
    private Object newValue;            // 新值（为null表示删除）

    public enum DiffType {
        ADDED,      // 新增
        REMOVED,    // 删除
        MODIFIED,   // 修改
        UNCHANGED   // 无变化（可选，用于完整树展示）
    }
}
