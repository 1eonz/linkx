package com.tdtech.cloudcmd.admin.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdtech.cloudcmd.admin.resource.entity.PermissionDiff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionComparatorUtil {
    private static final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * 比较权限数据
     * @param oldJson 原数据JSON字符串
     * @param newJson 新数据JSON字符串
     * @return 差异列表（只包含有变化的部分）
     */
    public static List<PermissionDiff> compare(String oldJson, String newJson) throws JsonProcessingException {
        JsonNode oldNode = mapper.readTree(oldJson);
        JsonNode newNode = mapper.readTree(newJson);

        List<PermissionDiff> diffs = new ArrayList<>();

        // 1. 比较名称
        compareField(diffs, "name", oldNode.get("name"), newNode.get("name"));

        // 2. 比较数组字段（iccPrivJson等）
        compareArrayField(diffs, "iccPrivJson", oldNode.get("iccPrivJson"), newNode.get("iccPrivJson"));
        compareArrayField(diffs, "adminPrivJson", oldNode.get("adminPrivJson"), newNode.get("adminPrivJson"));
        compareArrayField(diffs, "cappPrivJson", oldNode.get("cappPrivJson"), newNode.get("cappPrivJson"));

        // 3. 比较部门树（最复杂）
//        compareOrgTree(diffs, oldNode.get("imOrgPrivJson"), newNode.get("imOrgPrivJson"));
        compareOrgTreeSimple(diffs, oldNode.get("imOrgPrivJson"), newNode.get("imOrgPrivJson"));

        return diffs.stream()
                .filter(d -> d.getType() != PermissionDiff.DiffType.UNCHANGED)
                .collect(Collectors.toList());
    }

    /**
     * 比较简单字段
     */
    private static void compareField(List<PermissionDiff> diffs, String fieldName,
                              JsonNode oldVal, JsonNode newVal) {
        String oldStr = oldVal != null ? oldVal.asText() : null;
        String newStr = newVal != null ? newVal.asText() : null;

        if (!Objects.equals(oldStr, newStr)) {
            PermissionDiff diff = new PermissionDiff();
            diff.setFieldName(fieldName);
            diff.setOldValue(oldStr);
            diff.setNewValue(newStr);
            diff.setType(oldVal == null ? PermissionDiff.DiffType.ADDED :
                    newVal == null ? PermissionDiff.DiffType.REMOVED :
                            PermissionDiff.DiffType.MODIFIED);
            diffs.add(diff);
        }
    }

    /**
     * 比较数组字段（ID列表）
     */
    private static void compareArrayField(List<PermissionDiff> diffs, String fieldName,
                                   JsonNode oldArr, JsonNode newArr) {
        Set<String> oldSet = arrayToSet(oldArr);
        Set<String> newSet = arrayToSet(newArr);

        // 新增的ID
        Set<String> added = new HashSet<>(newSet);
        added.removeAll(oldSet);

        // 删除的ID
        Set<String> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        if (!added.isEmpty() || !removed.isEmpty()) {
            PermissionDiff diff = new PermissionDiff();
            diff.setFieldName(fieldName);
            diff.setType(PermissionDiff.DiffType.MODIFIED);
            diff.setOldValue(removed.isEmpty() ? null : removed);
            diff.setNewValue(added.isEmpty() ? null : added);
            diffs.add(diff);
        }
    }

    // ========== 工具方法 ==========

    private static Set<String> arrayToSet(JsonNode arr) {
        Set<String> set = new HashSet<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(node -> set.add(node.asText()));
        }
        return set;
    }


    /**
     * 简化版部门树比对：只提取ID集合差异
     */
    private static void compareOrgTreeSimple(List<PermissionDiff> diffs, JsonNode oldTree, JsonNode newTree) {
        // 提取所有部门ID（递归遍历树）
        Set<String> oldIds = extractIdsFromTree(oldTree);
        Set<String> newIds = extractIdsFromTree(newTree);

        // 计算差异
        Set<String> addedIds = new HashSet<>(newIds);      // 新勾选的
        addedIds.removeAll(oldIds);

        Set<String> removedIds = new HashSet<>(oldIds);    // 取消勾选的
        removedIds.removeAll(newIds);

        if (!addedIds.isEmpty() || !removedIds.isEmpty()) {
            // 可选：转换为带名称的列表，便于前端展示
            List<Map<String, String>> added = addedIds.stream()
                    .map(id -> buildIdNameMap(id, newTree))
                    .collect(Collectors.toList());

            List<Map<String, String>> removed = removedIds.stream()
                    .map(id -> buildIdNameMap(id, oldTree))
                    .collect(Collectors.toList());

            diffs.add(PermissionDiff.builder()
                    .fieldName("imOrgPrivJson")
                    .type(PermissionDiff.DiffType.MODIFIED)
                    .oldValue(removed)    // 取消勾选的部门 [{id: "xxx", name: "xxx"}]
                    .newValue(added)      // 新勾选的部门 [{id: "xxx", name: "xxx"}]
                    .build());
        }
    }


    /**
     * 递归提取树中所有部门ID（扁平化）
     */
    private static Set<String> extractIdsFromTree(JsonNode tree) {
        Set<String> ids = new HashSet<>();
        if (tree == null || !tree.isArray()) return ids;

        for (JsonNode node : tree) {
            if (node.has("id")) {
                ids.add(node.get("id").asText());
                // 递归提取子部门
                ids.addAll(extractIdsFromTree(node.get("children")));
            }
        }
        return ids;
    }

    /**
     * 根据ID从树中查找部门名称
     */
    private static Map<String, String> buildIdNameMap(String id, JsonNode tree) {
        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        result.put("name", findNameById(tree, id));
        return result;
    }

    private static String findNameById(JsonNode tree, String targetId) {
        if (tree == null || !tree.isArray()) return "未知部门";

        for (JsonNode node : tree) {
            if (node.has("id") && targetId.equals(node.get("id").asText())) {
                return node.get("name").asText();
            }
            // 递归查找子节点
            String name = findNameById(node.get("children"), targetId);
            if (!"未知部门".equals(name)) return name;
        }
        return "未知部门";
    }
}
