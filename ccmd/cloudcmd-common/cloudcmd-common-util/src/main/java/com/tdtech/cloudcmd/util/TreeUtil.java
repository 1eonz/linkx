package com.tdtech.cloudcmd.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class TreeUtil {

    private static class ChainNode<T> {
        private ChainNode<T> next;
        private ChainNode<T> prev;
        private T payload;
    }

    private static class Chain<T> {
        private ChainNode<T> head;

        private Chain(Collection<T> collection) {
            if (collection == null || collection.isEmpty()) {
                head = null;
                return;
            }
            var iterator = collection.iterator();
            var root = new ChainNode<T>();
            root.payload = iterator.next();
            var current = root;
            while (iterator.hasNext()) {
                var p = iterator.next();
                var t = new ChainNode<T>();
                t.payload = p;
                t.prev = current;
                current.next = t;
                current = t;
            }
            head = root;
        }

        public void delete(ChainNode<T> node) {
            if (head == node) {
                head = node.next;
            }
            if (node.prev != null) {
                node.prev.next = node.next;
            }
            if (node.next != null) {
                node.next.prev = node.prev;
            }
        }
    }

    public static <T> List<T> buildTree(Collection<T> source, Predicate<T> isRoot, BiPredicate<T, T> childPredicate,
                                        BiConsumer<T, T> pushChildConsumer) {
        var chain = new Chain<>(source);
        var current = chain.head;
        List<T> result = new LinkedList<>();
        while (current != null) {
            if (isRoot.test(current.payload)) {
                result.add(current.payload);
                chain.delete(current);
                buildTree(chain, current, childPredicate, pushChildConsumer);
            }
            current = current.next;
        }
        return result;
    }

    private static <T> void buildTree(Chain<T> chain, ChainNode<T> root, BiPredicate<T, T> childPredicate,
                                      BiConsumer<T, T> pushChildConsumer) {
        var current = chain.head;
        while (current != null) {
            if (childPredicate.test(root.payload, current.payload)) {
                chain.delete(current);
                pushChildConsumer.accept(root.payload, current.payload);
                buildTree(chain, current, childPredicate, pushChildConsumer);
            }
            current = current.next;
        }
    }

    @Getter
    @Setter
    @ToString
    public static class TreeNode<E> {
        @JsonUnwrapped
        private E data;
        private List<TreeNode<E>> children = new LinkedList<>();

        public TreeNode(E data) {
            this.data = data;
        }
    }

    /**
     * 优化的树构建方法，使用Map提高查找效率，时间复杂度从O(n²)降低到O(n)
     *
     * @param source 数据源
     * @param idGetter 获取节点ID的函数
     * @param parentIdGetter 获取父节点ID的函数
     * @return 树结构
     */
    public static <E> List<TreeNode<E>> buildTreeOptimized(Collection<E> source,
                                                           Function<E, String> idGetter,
                                                           Function<E, String> parentIdGetter) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用Map存储所有节点，提高查找效率
        Map<String, TreeNode<E>> nodeMap = new HashMap<>();
        List<TreeNode<E>> roots = new LinkedList<>();
        // 用于存储ID为空的节点
        List<TreeNode<E>> noIdNodes = new LinkedList<>();

        // 先创建所有节点
        for (E item : source) {
            TreeNode<E> node = new TreeNode<>(item);
            String id = idGetter.apply(item);
            if (id != null && !id.isEmpty()) {
                nodeMap.put(id, node);
            } else {
                // ID为空的节点单独存储
                noIdNodes.add(node);
            }
        }

        // 建立父子关系
        for (E item : source) {
            String id = idGetter.apply(item);
            String parentId = parentIdGetter.apply(item);

            TreeNode<E> node;
            if (id != null && !id.isEmpty()) {
                node = nodeMap.get(id);
                if (node == null) continue;
            } else {
                if (!noIdNodes.isEmpty()) {
                    node = noIdNodes.remove(0);
                } else {
                    continue;
                }
            }

            if (parentId == null || parentId.isEmpty() || "0".equals(parentId)) {
                roots.add(node);
            } else {
                TreeNode<E> parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        // 将所有ID为空的节点也添加到根节点中
        roots.addAll(noIdNodes);
        return roots;
    }

    /**
     * 遍历树
     */
    public static <E> void depthFirstTraverse(List<TreeNode<E>> tree, Consumer<TreeNode<E>> callback) {
        for (var eTreeNode : tree) {
            callback.accept(eTreeNode);
            depthFirstTraverse(eTreeNode.children, callback);
        }
    }

    /**
     * 遍历树
     */
    public static <E> void depthFirstTraverse(List<TreeNode<E>> tree, TreeNode<E> root,
                                              BiConsumer<TreeNode<E>, TreeNode<E>> callback) {
        for (var eTreeNode : tree) {
            callback.accept(root, eTreeNode);
            depthFirstTraverse(eTreeNode.children, eTreeNode, callback);
        }
    }

    public static <E> List<E> unwrap(List<TreeNode<E>> tree) {
        if (tree == null || tree.isEmpty()) {
            return Collections.emptyList();
        }
        return tree.stream().map(a -> a.data).collect(Collectors.toList());
    }
}
