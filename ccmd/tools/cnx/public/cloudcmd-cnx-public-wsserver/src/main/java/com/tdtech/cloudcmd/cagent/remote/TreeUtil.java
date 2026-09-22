package com.tdtech.cloudcmd.cagent.remote;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class TreeUtil {
    public static <E> List<TreeNode<E>> buildTree(Collection<E> source, BiPredicate<E, E> isChild) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<TreeNode<E>> result = source.stream().map(TreeNode::new).collect(LinkedList::new, List::add, List::addAll);
        var roots = new LinkedList<TreeNode<E>>();
        BP:
        for (var c : result) {
            for (var p : result) {
                if (isChild.test(c.data, p.data)) {
                    // 有爹 把自己挂到爹下头
                    p.children.add(c);
                    continue BP;
                }
            }
            // 没爹孤儿 自己当爹
            roots.add(c);
        }
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

    @Getter
    @Setter
    @ToString
    public static class TreeNode<E> {
        private E data;
        private List<TreeNode<E>> children = new LinkedList<>();

        public TreeNode(E data) {
            this.data = data;
        }
    }
}
