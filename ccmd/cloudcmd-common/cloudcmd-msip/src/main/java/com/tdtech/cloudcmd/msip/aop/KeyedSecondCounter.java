package com.tdtech.cloudcmd.msip.aop;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class KeyedSecondCounter {
    // 当前秒的所有计数器
    private ConcurrentHashMap<String, AtomicInteger> currMap = new ConcurrentHashMap<>();
    // 上一秒的 map（轮转后快照）
    private ConcurrentHashMap<String, AtomicInteger> prevMap = new ConcurrentHashMap<>();

    /** 对某个 key 原子 +1 */
    public void incr(String key) {
        currMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /** 轮转 1 秒：返回上一秒各 key 的计数快照，然后清空当前秒 */
    public Map<String, Integer> rotate() {
        // 交换引用
        ConcurrentHashMap<String, AtomicInteger> tmp = prevMap;
        prevMap = currMap;
        currMap = tmp;

        // 快速快照
        Map<String, Integer> snapshot = new HashMap<>(prevMap.size());
        prevMap.forEach((k, v) -> snapshot.put(k, v.get()));
        prevMap.clear();          // 清零，留给下一次轮转
        return snapshot;          // 调用方可以判断哪个 key > 500
    }
}
