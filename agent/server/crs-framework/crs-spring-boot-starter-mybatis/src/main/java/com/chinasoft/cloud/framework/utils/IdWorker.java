package com.chinasoft.cloud.framework.utils;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * 抄的ID生成器
 *
 * 开源项目：https://gitee.com/yu120/sequence
 */
@Slf4j
public class IdWorker {

    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动）
     */
    private static final long TWEPOCH = 1288834974657L;
    private static final long WORKER_ID_BITS = 3L;// 机器标识位数
    private static final long DATACENTER_ID_BITS = 8L;
    private static final long max_worker_id = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_BITS = 11L;// 毫秒内自增位
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;// 时间戳左移动位
    private static final long MAX_TIMESTAMP = ~(-1L << (Long.bitCount(Long.MAX_VALUE) - TIMESTAMP_LEFT_SHIFT));
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long datacenterId;
    private final Object lockObject = new Object();// 并发控制
    private volatile long sequence = 0L;
    private volatile long lastTimestamp = -1L;// 上次生产 ID 时间戳

    public IdWorker(String podIp) throws UnknownHostException {
        this.datacenterId = getDatacenterId(podIp, MAX_DATACENTER_ID);
        this.workerId = getMaxWorkerId(datacenterId, max_worker_id);
        log.warn("new id worker with datacenterId:{} workerId:{}", datacenterId, workerId);
    }

    public IdWorker() {
        this.datacenterId = getDatacenterId(MAX_DATACENTER_ID);
        this.workerId = getMaxWorkerId(datacenterId, max_worker_id);
        log.warn("new id worker with datacenterId:{} workerId:{}", datacenterId, workerId);
    }

    /**
     * 有参构造器
     *
     * @param workerId 工作机器 ID
     * @param datacenterId 序列号
     */
    public IdWorker(long workerId, long datacenterId) {
        if (workerId > max_worker_id || workerId < 0) {
            throw new RuntimeException(
                String.format("worker Id can't be greater than %d or less than 0", max_worker_id));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new RuntimeException(
                String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.warn("new id worker with datacenterId:{} workerId:{}", datacenterId, workerId);
    }

    /**
     * 获取 maxWorkerId
     */
    private static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (name != null && !name.isBlank()) {
            /*
             * GET jvmPid
             */
            mpid.append(name.split("@")[0]);
        }
        /*
         * MAC + PID 的 hashcode 获取16个低位
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * 数据标识id部分
     */
    private static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long)mac[mac.length - 1])
                        | (0x0000FF00 & (((long)mac[mac.length - 2]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            log.warn(" getDatacenterId: ", e);
        }
        return id;
    }

    /**
     * 数据标识id部分
     */
    private static long getDatacenterId(String podIP, long maxDatacenterId) throws UnknownHostException {
        var byName = Inet4Address.getByName(podIP);
        var address = byName.getAddress();
        return (long)(address[address.length - 1] & maxDatacenterId);
    }

    public static void main(String[] args) throws UnknownHostException, ExecutionException, InterruptedException {
        var idWorker = new IdWorker("10.42.0.222");
        var threadPoolExecutor = new ThreadPoolExecutor(100, 100, 30L, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000), new ThreadPoolExecutor.AbortPolicy());
        var countDownLatch = new CountDownLatch(10000);
        var se = new HashSet<>();
        int f = 0, s = 0;
        var l = new LinkedList<CompletableFuture<Long>>();
        for (int i = 0; i < 10000; i++) {
            var cf = CompletableFuture.supplyAsync(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                var id = idWorker.nextId();
                return id;
            });
            l.add(cf);
            countDownLatch.countDown();
        }
        for (CompletableFuture<Long> c : l) {
            if (se.add(c.get())) {
                s++;
            } else {
                f++;
            }
        }
        System.out.println(s + ":" + f);
        threadPoolExecutor.shutdownNow();
    }

    /**
     * 获取下一个 ID
     *
     * @return 下一个 ID
     */
    public long nextId() {
        synchronized (lockObject) {
            long timestamp = timeGen();
            // 闰秒
            if (timestamp < lastTimestamp) {
                long offset = lastTimestamp - timestamp;
                if (offset <= 5) {
                    try {
                        wait(offset << 1);
                        timestamp = timeGen();
                        if (timestamp < lastTimestamp) {
                            throw new RuntimeException(String
                                .format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new RuntimeException(
                        String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                }
            }

            if (lastTimestamp == timestamp) {
                // 相同毫秒内，序列号自增
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 同一毫秒的序列数已经达到最大
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                // 不同毫秒内，序列号置为 1 - 3 随机数
                sequence = ThreadLocalRandom.current().nextLong(1, 3);
            }
            log.trace("lti:{} ti:{} seq:{}", lastTimestamp, timestamp, sequence);
            lastTimestamp = timestamp;
            var timeMark = (timestamp - TWEPOCH) & MAX_TIMESTAMP;

            // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
            return (timeMark << TIMESTAMP_LEFT_SHIFT) | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT) | sequence;
        }
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

}
