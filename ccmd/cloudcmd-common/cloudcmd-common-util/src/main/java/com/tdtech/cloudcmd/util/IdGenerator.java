package com.tdtech.cloudcmd.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zWX446107
 * @description 生成15位序列号 生成规则如下：53位ID (0+33(秒)+4(机器ID)+15(重复累加))，适用于javascript snowflake缩减版，去掉了datacenterId.由64整型改为53位整型
 * @create 2019-10-06 13:43
 */
@Slf4j
public class IdGenerator {
    /**
     * 开始时间截 (1970-01-01)
     */
    private static final long TWEPOCH = 0L;

    /**
     * 机器id所占的位数，占4位
     */
    private static final long WORKERID_BITS = 4L;
    /**
     * 支持的最大机器id，结果是15
     */
    private static final long MAX_WORKERID = ~(-1L << WORKERID_BITS);

    /**
     * 生成序列占的位数
     **/
    private static final long SEQUENCE_BITS = 15L;
    /**
     * 机器ID偏左移15位
     **/
    private static final long WORKERID_SHIFT = SEQUENCE_BITS;

    /**
     * 生成序列的掩码，这里为最大是32767 (1111111111111=32767)
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 时间截向左移19位(4+15)
     **/
    private static final long TIMESTAMP_LEFTSHIFT = WORKERID_BITS + SEQUENCE_BITS;
    /**
     * 上次生成ID的时间截
     */
    private static long lastTimestamp = -1L;
    /**
     * 机器id，范围是1到15
     */
    private final long workerId;
    /**
     * 秒内序列(0~32767)
     */
    private long sequence = 0L;

    public IdGenerator(long workerId) {
        if (workerId > MAX_WORKERID || workerId < 0) {
            throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0");
        }
        this.workerId = workerId;
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     *
     * @return SnowflakeId
     */
    public long nextId() {
        synchronized (this) {
            long timestamp = timeGen();

            // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
            if (timestamp < lastTimestamp) {
                try {
                    throw new IllegalArgumentException("Clock moved backwards.  Refusing to generate id for "
                        + (lastTimestamp - timestamp) + " milliseconds");
                } catch (Exception e) {
                    log.error("Error!", e);
                }
            }

            // 如果是同一时间生成的，则进行秒内序列
            if (lastTimestamp == timestamp) {
                // 当前毫秒内，则+1
                sequence = (sequence + 1) & SEQUENCE_MASK;
                // 秒内序列溢出
                if (sequence == 0) {
                    // 当前毫秒内计数满了，则等待下一秒
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0;
            }

            // 上次生成ID的时间截
            lastTimestamp = timestamp;

            // 移位并通过或运算拼到一起组成53 位的ID
            long nextId = ((timestamp - TWEPOCH) << TIMESTAMP_LEFTSHIFT) | (workerId << WORKERID_SHIFT) | sequence;
            return nextId;
        }
    }

    /**
     * 阻塞到下一个秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    private long tilNextMillis(final long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以秒为单位的当前时间
     *
     * @return 当前时间(秒)
     */
    private long timeGen() {
        return System.currentTimeMillis() / 1000L;
    }
}
