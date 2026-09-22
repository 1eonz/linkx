package com.tdtech.cloudcmd.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {

    private CollectionUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> Map<Integer, List<T>> group(Collection<T> col, Integer groupSize) {
        Objects.requireNonNull(col, "collection cant be null");
        Objects.requireNonNull(groupSize, "group size cant be null");
        int groupCount = col.size() / groupSize + (col.size() % groupSize == 0 ? 0 : 1);
        AtomicInteger ai = new AtomicInteger(0);
        return col.stream().collect(Collectors.groupingBy(a -> ai.getAndIncrement() % groupCount));
    }

    /**
     * 集合取交集
     */
    @SafeVarargs
    public static <T> List<T> join(Collection<T>... col) {
        if (col.length == 0) {
            throw new RuntimeException("nope");
        }
        Stream<T> stream = col[0].stream();
        for (int i = 1; i < col.length; i++) {
            stream = stream.filter(col[i]::contains);
        }
        return stream.collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T> List<T> joinIgnoreEmpty(Collection<T>... col) {
        var iterator = Arrays.stream(col).filter(a -> a != null && !a.isEmpty()).iterator();
        if (!iterator.hasNext()) {
            return Collections.emptyList();
        }
        Stream<T> stream = iterator.next().stream();
        while (iterator.hasNext()) {
            var next = iterator.next();
            stream = stream.filter(next::contains);
        }
        return stream.collect(Collectors.toList());
    }

    public static <T> Set<T> joinSets(List<Set<T>> sets) {
        if (isNotEmpty(sets)) {
            // 使用第一个集合作为交集的初始值
            Set<T> intersection = new HashSet<>(sets.get(0));
            // 从第二个集合开始，逐一与交集取交集
            for (int i = 1; i < sets.size(); i++) {
                Set<T> currentSet = sets.get(i);
                intersection.retainAll(currentSet); // 保留交集
                // 如果交集为空，则无需继续计算，因为后续集合的交集也一定是空
                if (intersection.isEmpty()) {
                    break;
                }
            }
            return intersection;
        }
       return null;
    }

    public static <T> String join(Collection<T> col, String sep) {
        return join(col, sep, null, null);
    }

    public static <T> String join(Collection<T> col, String sep, String pre, String suf) {
        return col.stream().map(Objects::toString)
            .collect(Collectors.joining(sep, pre == null ? "" : pre, suf == null ? "" : suf));
    }

    public static boolean isEmpty(Collection<?> col) {
        return col == null || col.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> col) {
        return col == null || col.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> col) {
        return col != null && !col.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> col) {
        return col != null && !col.isEmpty();
    }

    public static <T> boolean isSame(List<? extends T> c1, List<? extends T> c2, Comparator<T> comparator) {
        if (c1 == null && c2 == null) {
            return true;
        }
        if (c1 == null || c2 == null) {
            return false;
        }
        if (c1.size() != c2.size()) {
            return false;
        }
        c1.sort(comparator);
        c2.sort(comparator);
        for (int i = 0; i < c1.size(); i++) {
            if (!Objects.equals(c1.get(i), c2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static <T, M> boolean contains(List<T> list, Function<T, M> function, M member) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (var ts : list) {
            if (Objects.equals(function.apply(ts), member)) {
                return true;
            }
        }
        return false;
    }

    public static <T, M> T find(List<T> list, Function<T, M> function, M member) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (var ts : list) {
            if (Objects.equals(function.apply(ts), member)) {
                return ts;
            }
        }
        return null;
    }
}
