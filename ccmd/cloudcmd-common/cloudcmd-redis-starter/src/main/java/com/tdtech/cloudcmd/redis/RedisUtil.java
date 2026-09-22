package com.tdtech.cloudcmd.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * @author mWX556161
 * @date 2020/6/2 17:38
 */
@AllArgsConstructor
public class RedisUtil {
    private static final byte[] EMPTY = new byte[0];
    private static final byte[][] EMPTY_ARR = new byte[][] {};
    private static final RedisSerializer<String> STRING_REDIS_SERIALIZER = RedisSerializer.string();

    private final ByteArrayRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    private <T> T deserialize(byte[] data, Class<T> type) {
        if (data == null || data.length == 0) {
            return null;
        } else if (type.equals(String.class)) {
            return (T)STRING_REDIS_SERIALIZER.deserialize(data);
        } else {
            return objectMapper.readValue(data, type);
        }
    }

    @SneakyThrows
    private <T> T deserialize(byte[] data, TypeReference<T> type) {
        if (data == null || data.length == 0) {
            return null;
        } else {
            return objectMapper.readValue(data, type);
        }
    }

    private <T> Map<String, T> deserialize(Map<String, byte[]> data, Class<T> type) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> map = new HashMap<>();
        data.forEach((k, v) -> map.put(k, deserialize(v, type)));
        return map;
    }

    private <T> Map<String, T> deserialize(Map<String, byte[]> data, TypeReference<T> type) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, T> map = new HashMap<>();
        data.forEach((k, v) -> map.put(k, deserialize(v, type)));
        return map;
    }

    private <T> Set<T> deserialize(Set<byte[]> data, Class<T> type) {
        if (data == null || data.isEmpty()) {
            return Collections.emptySet();
        }
        Set<T> map = new HashSet<>();
        data.forEach(v -> map.add(deserialize(v, type)));
        return map;
    }

    private <T> List<T> deserialize(List<byte[]> data, Class<T> type) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> map = new LinkedList<>();
        data.forEach(v -> map.add(deserialize(v, type)));
        return map;
    }

    @SneakyThrows({JsonProcessingException.class})
    private <T> byte[] serialize(T t) {
        if (t == null) {
            return EMPTY;
        } else if (ClassUtils.isAssignable(String.class, t.getClass())) {
            return STRING_REDIS_SERIALIZER.serialize((String)t);
        } else if (ClassUtils.isAssignable(Number.class, t.getClass())) {
            return STRING_REDIS_SERIALIZER.serialize(t.toString());
        } else {
            return objectMapper.writeValueAsBytes(t);
        }
    }

    private <T> byte[][] serialize(Collection<T> t) {
        if (t == null) {
            return EMPTY_ARR;
        } else {
            return t.stream().map(this::serialize).toArray(byte[][]::new);
        }
    }

    private <T, V> byte[][] serialize(T[] t) {
        if (t == null || t.length == 0) {
            return EMPTY_ARR;
        } else {
            ArrayList<byte[]> collect = Arrays.stream(t).collect(ArrayList::new, (a, b) -> {
                if (b.getClass().isArray()) {
                    for (V v : (V[])b) {
                        a.add(serialize(v));
                    }
                } else {
                    a.add(serialize(b));
                }
            }, ArrayList::addAll);
            return collect.toArray(new byte[0][]);
        }
    }

    private <T> Map<String, byte[]> serialize(Map<String, T> t) {
        if (t == null || t.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, byte[]> map = new HashMap<>();
        t.forEach((k, v) -> map.put(k, serialize(v)));
        return map;
    }

    /**************************************************** key 相关操作 *************************************************/

    /**
     * 实现命令 : KEYS pattern 查找所有符合 pattern 模式的 key ? 匹配单个字符 * 匹配0到多个字符 [a-c] 匹配a和c [ac] 匹配a到c [^a] 匹配除了a以外的字符
     *
     * @see org.springframework.data.redis.core.RedisOperations#keys(java.lang.Object)
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 实现命令 : SCAN pattern 使用游标迭代查找所有符合 pattern 模式的 key
     * 不会阻塞 Redis，适合生产环境大数据量场景使用
     *
     * @param pattern 匹配模式，如 "prefix:*"
     * @return 匹配的 key 集合
     */
    public Set<String> scan(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build())) {
                while (cursor.hasNext()) {
                    keys.add(STRING_REDIS_SERIALIZER.deserialize(cursor.next()));
                }
            }
            return null;
        });
        return keys;
    }

    /**
     * 实现命令 : DEL key1 [key2 ...] 删除一个或多个key
     *
     * @see org.springframework.data.redis.core.RedisOperations#delete(Collection)
     */
    public Long del(String... keys) {
        return redisTemplate.delete(Arrays.asList(keys));
    }

    /**
     * 实现命令 : UNLINK key1 [key2 ...] 删除一个或多个key
     *
     * @see org.springframework.data.redis.core.RedisOperations#unlink(Collection)
     */
    public Long unlink(String... keys) {
        return redisTemplate.unlink(Arrays.asList(keys));
    }

    /**
     * 实现命令 : EXISTS key1 [key2 ...] 查看 key 是否存在，返回存在 key 的个数
     *
     * @see org.springframework.data.redis.core.RedisOperations#countExistingKeys(Collection)
     */
    public Long exists(String... keys) {
        return redisTemplate.countExistingKeys(Arrays.asList(keys));
    }

    /**
     * 实现命令 : TYPE key 查看 key 的 value 的类型
     *
     * @see org.springframework.data.redis.core.RedisOperations#type(Object)
     */
    public DataType type(String key) {
        return redisTemplate.type(key);
    }

    /**
     * 实现命令 : PERSIST key 取消 key 的超时时间，持久化 key
     *
     * @see org.springframework.data.redis.core.RedisOperations#persist(Object)
     */
    public boolean persist(String key) {
        Boolean result = redisTemplate.persist(key);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : TTL key 返回给定 key 的剩余生存时间,key不存在返回 null 单位: 秒
     *
     * @see org.springframework.data.redis.core.RedisOperations#getExpire(Object)
     */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 实现命令 : PTTL key 返回给定 key 的剩余生存时间,key不存在返回 null
     *
     * @see org.springframework.data.redis.core.RedisOperations#getExpire(Object, TimeUnit)
     */
    public Long pTtl(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * 实现命令 : EXPIRE key 设置key 的生存时间
     *
     * @see org.springframework.data.redis.core.RedisOperations#expire(Object, long, TimeUnit)
     */
    public boolean expire(String key, long ttl, TimeUnit timeUnit) {
        Boolean result = redisTemplate.expire(key, ttl, timeUnit);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : EXPIRE key 设置key 的生存时间
     *
     * @see org.springframework.data.redis.core.RedisOperations#expire(Object, long, TimeUnit)
     */
    public boolean expire(String key, Duration duration) {
        Boolean result = redisTemplate.expire(key, duration);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : EXPIREAT key Unix时间戳(自1970年1月1日以来的秒数) 设置key 的过期时间
     *
     * @see org.springframework.data.redis.core.RedisOperations#expireAt(Object, Date)
     */
    public boolean expireAt(String key, Date date) {
        Boolean result = redisTemplate.expireAt(key, date);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : RENAME key newkey 重命名key，如果newKey已经存在，则newKey的原值被覆盖
     *
     * @see org.springframework.data.redis.core.RedisOperations#rename(Object, Object)
     */
    public void rename(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 实现命令 : RENAMENX key newkey 安全重命名key，newKey不存在时才重命名
     *
     * @see org.springframework.data.redis.core.RedisOperations#renameIfAbsent(Object, Object)
     */
    public boolean renameNx(String oldKey, String newKey) {
        Boolean result = redisTemplate.renameIfAbsent(oldKey, newKey);
        if (null == result) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令：DEL key1 [key2 ...] 删除任意多个 key
     *
     * @see org.springframework.data.redis.core.RedisOperations#delete(Collection)
     */
    public Long del(Collection<String> keys) {
        Set<String> keySet = new HashSet<>(keys);
        return redisTemplate.delete(keySet);
    }

    /**
     * 实现命令：UNLINK key1 [key2 ...] 删除任意多个 key
     *
     * @see org.springframework.data.redis.core.RedisOperations#unlink(Collection)
     */
    public Long unlink(Collection<String> keys) {
        Set<String> keySet = new HashSet<>(keys);
        return redisTemplate.unlink(keySet);
    }

    /**
     * 实现命令：EXISTS key1 [key2 ...] key去重后，查看 key 是否存在，返回存在 key 的个数
     *
     * @see org.springframework.data.redis.core.RedisOperations#countExistingKeys(Collection)
     */
    public Long exists(Collection<String> keys) {
        Set<String> keySet = new HashSet<>(keys);
        return redisTemplate.countExistingKeys(keySet);
    }

    /**************************************************** key 相关操作 *************************************************/

    /************************************************* String 相关操作 *************************************************/

    /**
     * 实现命令 : SET key value 添加一个持久化的 String 类型的键值对
     *
     * @see org.springframework.data.redis.core.ValueOperations#set(Object, Object)
     */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, serialize(value));
    }

    /**
     * 实现命令 : SET key value EX 秒、 setex key value 秒 添加一个 String 类型的键值对，并设置生存时间
     *
     * @see org.springframework.data.redis.core.ValueOperations#set(Object, Object, long, TimeUnit)
     */
    public <T> void set(String key, T value, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, serialize(value), ttl, timeUnit);
    }

    /**
     * 实现命令 : SET key value EX 秒、 setex key value 秒 添加一个 String 类型的键值对，并设置生存时间
     *
     * @see org.springframework.data.redis.core.ValueOperations#set(Object, Object, long, TimeUnit)
     */
    public <T> void set(String key, T value, Duration duration) {
        redisTemplate.opsForValue().set(key, serialize(value), duration);
    }

    /**
     * 实现命令 : SET XX 存在时设值
     *
     * @see org.springframework.data.redis.core.ValueOperations#setIfPresent(Object, Object, long, TimeUnit)
     */
    public <T> boolean setXx(String key, T value, Long ttl, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfPresent(key, serialize(value), ttl, timeUnit);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : SET XX 存在时设值
     *
     * @see org.springframework.data.redis.core.ValueOperations#setIfPresent(Object, Object)
     */
    public <T> boolean setXx(String key, T value) {
        Boolean result = redisTemplate.opsForValue().setIfPresent(key, serialize(value));
        return result == null ? false : result;
    }

    /**
     * 实现命令 : SET NX 不存在时设值
     *
     * @see org.springframework.data.redis.core.ValueOperations#setIfAbsent(Object, Object, long, TimeUnit)
     */
    public <T> boolean setNx(String key, T value, Long ttl, TimeUnit timeUnit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, serialize(value), ttl, timeUnit);
        return result == null ? false : result;
    }

    /**
     * 实现命令 : SET NX 不存在时设值
     *
     * @see org.springframework.data.redis.core.ValueOperations#setIfAbsent(Object, Object)
     */
    public <T> boolean setNx(String key, T value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, serialize(value));
        return result == null ? false : result;
    }

    /**
     * 实现命令 : MSET key1 value1 [key2 value2...] 安全批量添加键值对,只要有一个 key 已存在，所有的键值对都不会插入
     *
     * @see org.springframework.data.redis.core.ValueOperations#multiSet(Map)
     */
    public <T> void mSet(Map<String, T> keyValueMap) {
        if (keyValueMap == null) {
            return;
        }
        redisTemplate.opsForValue().multiSet(serialize(keyValueMap));
    }

    /**
     * 实现命令 : MSETNX key1 value1 [key2 value2...] 批量添加键值对
     *
     * @see org.springframework.data.redis.core.ValueOperations#multiSetIfAbsent(Map)
     */
    public <T> void mSetNx(Map<String, T> keyValueMap) {
        if (keyValueMap == null) {
            return;
        }
        redisTemplate.opsForValue().multiSetIfAbsent(serialize(keyValueMap));
    }

    /**
     * 实现命令 : SETRANGE key 下标 str 覆盖 原始 value 的一部分，从指定的下标开始覆盖， 覆盖的长度为指定的字符串的长度。
     *
     * @see org.springframework.data.redis.core.ValueOperations#set(Object, Object, long)
     */
    public <T> void setRange(String key, T val, int offset) {
        redisTemplate.opsForValue().set(key, serialize(val), offset);
    }

    /**
     * 实现命令 : APPEND key value 在原始 value 末尾追加字符串
     *
     * @see org.springframework.data.redis.core.ValueOperations#append(Object, String)
     */
    public void append(String key, String str) {
        redisTemplate.opsForValue().append(key, str);
    }

    /**
     * 实现命令 : GETSET key value 设置 key 的 value 并返回旧 value
     *
     * @see org.springframework.data.redis.core.ValueOperations#getAndSet(Object, Object)
     */
    public <T> T getSet(String key, T value) {
        byte[] andSet = redisTemplate.opsForValue().getAndSet(key, serialize(value));
        return deserialize(andSet, (Class<T>)value.getClass());
    }

    /**
     * 实现命令 : GETSET key value 设置 key 的 value 并返回旧 value
     *
     * @see org.springframework.data.redis.core.ValueOperations#getAndSet(Object, Object)
     */
    public <T, V> V getSet(String key, T value, Class<V> oldType) {
        byte[] andSet = redisTemplate.opsForValue().getAndSet(key, serialize(value));
        return deserialize(andSet, oldType);
    }

    /**
     * 实现命令 : GET key 获取一个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#get(Object)
     */
    public <T> T get(String key, Class<T> type) {
        byte[] bytes = redisTemplate.opsForValue().get(key);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : GET key 获取一个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#get(Object)
     */
    public <T> T get(String key, TypeReference<T> type) {
        byte[] bytes = redisTemplate.opsForValue().get(key);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : GET key 获取一个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#get(Object)
     * @deprecated 需要显式指定反序列化类型 {@link #get(String, Class)}
     */
    @Deprecated
    public String get(String key) {
        byte[] bytes = redisTemplate.opsForValue().get(key);
        return deserialize(bytes, String.class);
    }

    /**
     * 实现命令 : GET key 获取一个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#get(Object)
     * @deprecated 需要显式指定反序列化类型 {@link #get(String, Class)}
     */
    @Deprecated
    public String getString(String key) {
        byte[] bytes = redisTemplate.opsForValue().get(key);
        return deserialize(bytes, String.class);
    }

    public Long increaseAndGet(String key, Long step) {
        return redisTemplate.opsForValue().increment(key, step);
    }

    /**
     * 实现命令 : MGET key1 [key2...] 获取多个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#multiGet(Collection)
     */
    public <T> List<T> mGet(Class<T> type, String... keys) {
        List<byte[]> bytes = redisTemplate.opsForValue().multiGet(Arrays.asList(keys));
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : GETRANGE key 开始下标 结束下标 获取指定key的value的子串，下标从0开始，包括开始下标，也包括结束下标。
     *
     * @see org.springframework.data.redis.core.ValueOperations#get(Object, long, long)
     */
    public String getRange(String key, int start, int end) {
        return redisTemplate.opsForValue().get(key, start, end);
    }

    /**
     * 实现命令 : STRLEN key 获取 key 对应 value 的字符串长度
     *
     * @see org.springframework.data.redis.core.ValueOperations#size(Object)
     */
    public Long strLen(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    /**
     * 实现命令 : INCR key 给 value 加 1,value 必须是整数
     *
     * @see org.springframework.data.redis.core.ValueOperations#increment(Object)
     */
    public Long inCr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 实现命令 : inCrBy key 整数 给 value 加 上一个整数,value 必须是整数
     *
     * @see org.springframework.data.redis.core.ValueOperations#increment(Object, long)
     */
    public Long inCrBy(String key, long number) {
        return redisTemplate.opsForValue().increment(key, number);
    }

    /**
     * 实现命令 : INCRBYFLOAT key 数 给 value 加上一个小数,value 必须是数
     *
     * @see org.springframework.data.redis.core.ValueOperations#increment(Object, double)
     */
    public Double inCrBy(String key, double number) {
        return redisTemplate.opsForValue().increment(key, number);
    }

    /**
     * 实现命令 : DECR key 给 value 减去 1,value 必须是整数
     *
     * @see org.springframework.data.redis.core.ValueOperations#decrement(Object)
     */
    public Long deCr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 实现命令 : DECRBY key 整数 给 value 减去一个整数,value 必须是整数
     *
     * @see org.springframework.data.redis.core.ValueOperations#decrement(Object, long)
     */
    public Long deCcrBy(String key, Long number) {
        return redisTemplate.opsForValue().decrement(key, number);
    }

    /**
     * 实现命令 : MGET key1 [key2...] key去重后，获取多个key的value
     *
     * @see org.springframework.data.redis.core.ValueOperations#multiGet(Collection)
     */
    public <T> List<T> mGet(Collection<String> keys, Class<T> type) {
        Set<String> keySet = new HashSet<>(keys);
        List<byte[]> bytes = redisTemplate.opsForValue().multiGet(keySet);
        return deserialize(bytes, type);
    }

    /************************************************* String 相关操作 *************************************************/

    /************************************************* Hash 相关操作 ***************************************************/

    /**
     * 实现命令 : HSET key field value 添加 hash 类型的键值对，如果字段已经存在，则将其覆盖。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hSet(byte[], byte[], byte[])
     */
    public <T> void hSet(String key, String field, T value) {
        redisTemplate.<String, byte[]>opsForHash().put(key, field, serialize(value));
    }

    /**
     * 实现命令 : HSET key field1 value1 [field2 value2 ...] 添加 hash 类型的键值对，如果字段已经存在，则将其覆盖。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hMSet(byte[], Map)
     */
    public <T> void hMSet(String key, Map<String, T> map) {
        redisTemplate.<String, byte[]>opsForHash().putAll(key, serialize(map));
    }

    /**
     * 实现命令 : HSET key field1 value1 [field2 value2 ...] 添加 hash 类型的键值对，如果字段已经存在，则将其覆盖。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hMSet(byte[], Map)
     */
    public <T> void hMSet(String key, Map<String, T> map, Duration ttl) {
        redisTemplate.<String, byte[]>opsForHash().putAll(key, serialize(map));
        redisTemplate.expire(key, ttl);
    }

    /**
     * 实现命令 : HSETNX key field value 添加 hash 类型的键值对，如果字段不存在，才添加
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hSetNX(byte[], byte[], byte[])
     */
    public <T> boolean hSetNx(String key, String field, T value) {
        Boolean result = redisTemplate.<String, byte[]>opsForHash().putIfAbsent(key, field, serialize(value));
        if (result == null) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令 : HGET key field 返回 field 对应的值
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hGet(byte[], byte[])
     */
    public <T> T hGet(String key, String field, Class<T> type) {
        return deserialize(redisTemplate.<String, byte[]>opsForHash().get(key, field), type);
    }

    /**
     * 实现命令 : HMGET key field1 [field2 ...] 返回 多个 field 对应的值
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hMGet(byte[], byte[]...)
     */
    public <T> List<T> hMGet(Class<T> type, String key, String... fields) {
        List<byte[]> bytes = redisTemplate.<String, byte[]>opsForHash().multiGet(key, Arrays.asList(fields));
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : HGETALL key 返回所以的键值对
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hGetAll(byte[])
     */
    public <T> Map<String, T> hGetAll(String key, Class<T> type) {
        return deserialize(redisTemplate.<String, byte[]>opsForHash().entries(key), type);
    }

    /**
     * 实现命令 : HGETALL key 返回所以的键值对
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hGetAll(byte[])
     */
    public <T> Map<String, T> hGetAll(String key, TypeReference<T> type) {
        return deserialize(redisTemplate.<String, byte[]>opsForHash().entries(key), type);
    }

    /**
     * 实现命令 : HKEYS key 获取所有的 field
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hKeys(byte[])
     */
    public Set<String> hKeys(String key) {
        return redisTemplate.<String, byte[]>opsForHash().keys(key);
    }

    /**
     * 实现命令 : HVALS key 获取所有的 value
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hVals(byte[])
     */
    public <T> List<T> hValue(String key, Class<T> type) {
        List<byte[]> values = redisTemplate.<String, byte[]>opsForHash().values(key);
        return deserialize(values, type);
    }

    /**
     * 实现命令 : HDEL key field [field ...] 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hDel(byte[], byte[]...)
     */
    public Long hDel(String key, String... fields) {
        return redisTemplate.<String, byte[]>opsForHash().delete(key, fields);
    }

    /**
     * 实现命令 : HEXISTS key field 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hExists(byte[], byte[])
     */
    public boolean hExists(String key, String field) {
        Boolean result = redisTemplate.<String, byte[]>opsForHash().hasKey(key, field);
        if (result == null) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令 : HLEN key 获取 hash 中字段:值对的数量
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hLen(byte[])
     */
    public Long hLen(String key) {
        return redisTemplate.<String, byte[]>opsForHash().size(key);
    }

    /**
     * 实现命令 : HSTRLEN key field 获取字段对应值的长度
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hStrLen(byte[], byte[])
     */
    public Long hStrLen(String key, String field) {
        return redisTemplate.<String, byte[]>opsForHash().lengthOfValue(key, field);
    }

    /**
     * 实现命令 : HINCRBY key field 整数 给字段的值加上一个整数
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hIncrBy(byte[], byte[], long)
     */
    public Long hInCrBy(String key, String field, long number) {
        return redisTemplate.<String, byte[]>opsForHash().increment(key, field, number);
    }

    /**
     * 实现命令 : HINCRBYFLOAT key field 浮点数 给字段的值加上一个浮点数
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hIncrBy(byte[], byte[], double)
     */
    public Double hInCrByFloat(String key, String field, double number) {
        return redisTemplate.<String, byte[]>opsForHash().increment(key, field, number);
    }

    /**
     * 实现命令 : HMGET key field1 [field2 ...] 返回 多个 field 对应的值
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hMGet(byte[], byte[]...)
     */
    public <T> List<T> hMGet(String key, Collection<String> fields, Class<T> type) {
        List<byte[]> bytes = redisTemplate.<String, byte[]>opsForHash().multiGet(key, fields);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : HDEL key field [field ...] 删除哈希表 key 中的一个或多个指定域，不存在的域将被忽略。
     *
     * @see org.springframework.data.redis.connection.RedisHashCommands#hDel(byte[], byte[]...)
     */
    public Long hDel(String key, Collection<String> fields) {
        return redisTemplate.<String, byte[]>opsForHash().delete(key, fields.toArray(new String[0]));
    }

    /************************************************* Hash 相关操作 ***************************************************/

    /************************************************* List 相关操作 ***************************************************/

    /**
     * 实现命令 : LPUSH key 元素1 [元素2 ...] 在最左端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lPush(byte[], byte[]...)
     */
    public <T> Long lPush(String key, T... values) {
        List<byte[]> collect = Arrays.stream(values).map(this::serialize).collect(Collectors.toList());
        return redisTemplate.opsForList().leftPushAll(key, collect);
    }

    /**
     * 实现命令 : RPUSH key 元素1 [元素2 ...] 在最右端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#rPush(byte[], byte[]...)
     */
    public <T> Long rPush(String key, T... values) {
        List<byte[]> collect = Arrays.stream(values).map(this::serialize).collect(Collectors.toList());
        return redisTemplate.opsForList().rightPushAll(key, collect);
    }

    /**
     * 实现命令 : LPOP key 弹出最左端的元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lPop(byte[])
     */
    public <T> List<T> lPop(String key, long count, Class<T> type) {
        List<byte[]> bytes = redisTemplate.opsForList().leftPop(key, count);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : RPOP key 弹出最右端的元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#rPop(byte[])
     */
    public <T> T rPop(String key, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().rightPop(key);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : BLPOP key (阻塞式)弹出最左端的元素，如果 key 中没有元素，将一直等待直到有元素或超时为止
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#bLPop(int, byte[]...)
     */
    public <T> T bLPop(String key, long timeout, TimeUnit timeUnit, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().leftPop(key, timeout, timeUnit);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : BRPOP key (阻塞式)弹出最右端的元素，将一直等待直到有元素或超时为止
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#bRPop(int, byte[]...)
     */
    public <T> T bRPop(String key, long timeout, TimeUnit timeUnit, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().rightPop(key, timeout, TimeUnit.SECONDS);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : LINDEX key index 返回指定下标处的元素，下标从0开始
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lIndex(byte[], long)
     */
    public <T> T lIndex(String key, int index, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().index(key, index);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : LINSERT key BEFORE|AFTER 目标元素 value 在目标元素前或后插入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lInsert(byte[], RedisListCommands.Position,
     *      byte[], byte[])
     */
    public <T> Long lInsert(String key, T pivot, T value) {
        return redisTemplate.opsForList().rightPush(key, serialize(pivot), serialize(value));
    }

    /**
     * 实现命令 : LRANGE key 开始下标 结束下标 获取指定范围的元素,下标从0开始，包括开始下标，也包括结束下标(待验证)
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lRange(byte[], long, long)
     */
    public <T> List<T> lRange(String key, int start, int end, Class<T> type) {
        List<byte[]> range = redisTemplate.opsForList().range(key, start, end);
        return deserialize(range, type);
    }

    /**
     * 实现命令 : LLEN key 获取 list 的长度
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lLen(byte[])
     */
    public Long lLen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 实现命令 : LREM key count 元素 删除 count 个指定元素,
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lRem(byte[], long, byte[])
     */
    public <T> Long lRem(String key, int count, T value) {
        return redisTemplate.opsForList().remove(key, count, serialize(value));
    }

    /**
     * 实现命令 : LSET key index 新值 更新指定下标的值,下标从 0 开始，支持负下标，-1表示最右端的元素(未验证)
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lSet(byte[], long, byte[])
     */
    public <T> void lSet(String key, int index, T value) {
        redisTemplate.opsForList().set(key, index, serialize(value));
    }

    /**
     * 实现命令 : LTRIM key 开始下标 结束下标 裁剪 list。[01234] 的 `LTRIM key 1 -2 的结果为 [123]
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lTrim(byte[], long, long)
     */
    public void lTrim(String key, int start, int end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 实现命令 : RPOPLPUSH 源list 目标list 将 源list 的最右端元素弹出，推入到 目标list 的最左端，
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#rPopLPush(byte[], byte[])
     */
    public <T> T rPopLPush(String sourceKey, String targetKey, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, targetKey);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : BRPOPLPUSH 源list 目标list timeout (阻塞式)将 源list 的最右端元素弹出，推入到 目标list 的最左端，如果 源list 没有元素，将一直等待直到有元素或超时为止
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#bRPopLPush(int, byte[], byte[])
     */
    public <T> T bRPopLPush(String sourceKey, String targetKey, long timeout, TimeUnit timeUnit, Class<T> type) {
        byte[] bytes = redisTemplate.opsForList().rightPopAndLeftPush(sourceKey, targetKey, timeout, timeUnit);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : LPUSH key 元素1 [元素2 ...] 在最左端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lPush(byte[], byte[]...)
     */
    public <T> Long lPush(String key, Collection<T> values) {
        return redisTemplate.opsForList().leftPushAll(key, serialize(values));
    }

    /**
     * 实现命令 : LPUSHX key 元素 key 存在时在，最左端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#lPushX(byte[], byte[])
     */
    public <T> Long lPushX(String key, T value) {
        return redisTemplate.opsForList().leftPushIfPresent(key, serialize(value));
    }

    /**
     * 实现命令 : RPUSH key 元素1 [元素2 ...] 在最右端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#rPush(byte[], byte[]...)
     */
    public <T> Long rPush(String key, Collection<T> values) {
        return redisTemplate.opsForList().rightPushAll(key, serialize(values));
    }

    /**
     * 实现命令 : RPUSHX key 元素 key 存在时，在最右端推入元素
     *
     * @see org.springframework.data.redis.connection.RedisListCommands#rPushX(byte[], byte[])
     */
    public <T> Long rPushX(String key, T value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, serialize(value));
    }

    /************************************************* List 相关操作 ***************************************************/

    /************************************************** SET 相关操作 ***************************************************/

    /**
     * 实现命令 : SADD key member1 [member2 ...] 添加成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sAdd(byte[], byte[]...)
     */
    public <T> Long sAdd(String key, T... values) {
        return redisTemplate.opsForSet().add(key, serialize(values));
    }

    /**
     * 实现命令 : SREM key member1 [member2 ...] 删除指定的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sRem(byte[], byte[]...)
     */
    public <T> Long sRem(String key, T... values) {
        return redisTemplate.opsForSet().remove(key, serialize(values));
    }

    /**
     * 实现命令 : SCARD key 获取set中的成员数量
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sCard(byte[])
     */
    public Long sCard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 实现命令 : SISMEMBER key member 查看成员是否存在
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sIsMember(byte[], byte[])
     */
    public <T> boolean sIsMember(String key, T values) {
        Boolean result = redisTemplate.opsForSet().isMember(key, serialize(values));
        if (null == result) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令 : SMEMBERS key 获取所有的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sMembers(byte[])
     */
    public <T> Set<T> sMembers(String key, Class<T> cls) {
        Set<byte[]> members = redisTemplate.opsForSet().members(key);
        return deserialize(members, cls);
    }

    /**
     * 实现命令 : SMOVE 源key 目标key member 移动成员到另一个集合
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sMove(byte[], byte[], byte[])
     */
    public <T> boolean sMove(String sourceKey, String targetKey, T value) {
        Boolean result = redisTemplate.opsForSet().move(sourceKey, serialize(value), targetKey);
        if (null == result) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令 : SDIFF key [otherKey ...] 求 key 的差集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sDiff(byte[]...)
     */
    public <T> Set<T> sDiff(Class<T> type, String key, String... otherKeys) {
        Set<byte[]> difference = redisTemplate.opsForSet().difference(key, Arrays.asList(otherKeys));
        return deserialize(difference, type);
    }

    /**
     * 实现命令 : SDIFFSTORE 目标key key [otherKey ...] 存储 key 的差集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sDiffStore(byte[], byte[]...)
     */
    public Long sDiffStore(String targetKey, String key, String... otherKeys) {
        return redisTemplate.opsForSet().differenceAndStore(key, Arrays.asList(otherKeys), targetKey);
    }

    /**
     * 实现命令 : SINTER key [otherKey ...] 求 key 的交集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sInter(byte[]...)
     */
    public <T> Set<T> sInter(Class<T> type, String key, String... otherKeys) {
        Set<byte[]> intersect = redisTemplate.opsForSet().intersect(key, Arrays.asList(otherKeys));
        return deserialize(intersect, type);
    }

    /**
     * 实现命令 : SINTERSTORE 目标key key [otherKey ...] 存储 key 的交集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sInterStore(byte[], byte[]...)
     */
    public Long sInterStore(String targetKey, String key, String... otherKeys) {
        return redisTemplate.opsForSet().intersectAndStore(key, Arrays.asList(otherKeys), targetKey);
    }

    /**
     * 实现命令 : SUNION key [otherKey ...] 求 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnion(byte[]...)
     */
    public <T> Set<T> sUnion(Class<T> type, Collection<String> keys) {
        Set<byte[]> union = redisTemplate.opsForSet().union(keys);
        return deserialize(union, type);
    }

    /**
     * 实现命令 : SUNION key [otherKey ...] 求 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnion(byte[]...)
     */
    public <T> Set<T> sUnion(Class<T> type, String... otherKeys) {
        Set<byte[]> union = redisTemplate.opsForSet().union(Arrays.asList(otherKeys));
        return deserialize(union, type);
    }

    /**
     * 实现命令 : SUNION key [otherKey ...] 求 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnion(byte[]...)
     */
    public <T> Set<T> sUnion(Class<T> type, String key, String... otherKeys) {
        Set<byte[]> union = redisTemplate.opsForSet().union(key, Arrays.asList(otherKeys));
        return deserialize(union, type);
    }

    /**
     * 实现命令 : SUNIONSTORE 目标key key [otherKey ...] 存储 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnionStore(byte[], byte[]...)
     */
    public Long sUnionStore(String targetKey, String... otherKeys) {
        List<String> otherKeyList = Stream.of(otherKeys).collect(Collectors.toList());
        return redisTemplate.opsForSet().unionAndStore(otherKeyList, targetKey);
    }

    /**
     * 实现命令 : SPOP key [count] 随机删除(弹出)指定个数的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sPop(byte[])
     */
    public <T> T sPop(String key, Class<T> type) {
        byte[] pop = redisTemplate.opsForSet().pop(key);
        return deserialize(pop, type);
    }

    /**
     * 实现命令 : SPOP key [count] 随机删除(弹出)指定个数的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sPop(byte[], long)
     */
    public <T> List<T> sPop(String key, int count, Class<T> type) {
        List<byte[]> pop = redisTemplate.opsForSet().pop(key, count);
        return deserialize(pop, type);
    }

    /**
     * 实现命令 : SRANDMEMBER key [count] 随机返回指定个数的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sRandMember(byte[])
     */
    public <T> T sRandMember(String key, Class<T> type) {
        byte[] bytes = redisTemplate.opsForSet().randomMember(key);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : SRANDMEMBER key [count] 随机返回指定个数的成员 如果 count 为正数，随机返回 count 个不同成员 如果 count 为负数，随机选择 1 个成员，返回 count 个
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sRandMember(byte[], long)
     */
    public <T> List<T> sRandMember(String key, int count, Class<T> type) {
        List<byte[]> bytes = redisTemplate.opsForSet().randomMembers(key, count);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : SREM key member1 [member2 ...] 删除指定的成员
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sRem(byte[], byte[]...)
     */
    public <T> Long sRem(String key, Collection<T> values) {
        return redisTemplate.opsForSet().remove(key, serialize(values));
    }

    /**
     * 实现命令 : SDIFF key [otherKey ...] 求 key 的差集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sDiff(byte[]...)
     */
    public <T> Set<T> sDiff(String key, List<String> otherKeys, Class<T> type) {
        Set<byte[]> difference = redisTemplate.opsForSet().difference(key, otherKeys);
        return deserialize(difference, type);
    }

    /**
     * 实现命令 : SDIFFSTORE 目标key key [otherKey ...] 存储 key 的差集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sDiffStore(byte[], byte[]...)
     */
    public Long sDiffStore(String targetKey, String key, List<String> otherKeys) {
        return redisTemplate.opsForSet().differenceAndStore(key, otherKeys, targetKey);
    }

    /**
     * 实现命令 : SINTER key [otherKey ...] 求 key 的交集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sInter(byte[]...)
     */
    public <T> Set<T> sInter(String key, List<String> otherKeys, Class<T> type) {
        Set<byte[]> intersect = redisTemplate.opsForSet().intersect(key, otherKeys);
        return deserialize(intersect, type);
    }

    /**
     * 实现命令 : SINTERSTORE 目标key key [otherKey ...] 存储 key 的交集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sInterStore(byte[], byte[]...)
     */
    public Long sInterStore(String targetKey, String key, List<String> otherKeys) {
        return redisTemplate.opsForSet().intersectAndStore(key, otherKeys, targetKey);
    }

    /**
     * 实现命令 : SUNION key [otherKey ...] 求 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnion(byte[]...)
     */
    public <T> Set<T> sUnion(String key, List<String> otherKeys, Class<T> type) {
        Set<byte[]> union = redisTemplate.opsForSet().union(key, otherKeys);
        return deserialize(union, type);
    }

    /**
     * 实现命令 : SUNIONSTORE 目标key key [otherKey ...] 存储 key 的并集
     *
     * @see org.springframework.data.redis.connection.RedisSetCommands#sUnionStore(byte[], byte[]...)
     */
    public Long sUnionStore(String targetKey, String key, List<String> otherKeys) {
        return redisTemplate.opsForSet().unionAndStore(key, otherKeys, targetKey);
    }

    /************************************************** SET 相关操作 ***************************************************/

    /*********************************************** Sorted SET 相关操作 ***********************************************/

    /**
     * 实现命令 : ZADD key score member 添加一个 成员/分数 对
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zAdd(byte[], double, byte[])
     */
    public <T> boolean zAdd(String key, double score, T value) {
        Boolean result = redisTemplate.opsForZSet().add(key, serialize(value), score);
        if (result == null) {
            return false;
        }
        return result;
    }

    /**
     * 实现命令 : ZREM key member [member ...] 删除成员
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRem(byte[], byte[]...)
     */
    public <T> Long zRem(String key, T... values) {
        Object[] objects = Arrays.stream(values).map(this::serialize).toArray();
        return redisTemplate.opsForZSet().remove(key, objects);
    }

    /**
     * 实现命令 : ZREMRANGEBYRANK key start stop 删除 start下标 和 end下标间的所有成员 下标从0开始，支持负下标，-1表示最右端成员，包括开始下标也包括结束下标 (未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRange(byte[], long, long)
     */
    public Long zRemRange(String key, int start, int end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 实现命令 : ZREMRANGEBYSCORE key start stop 删除分数段内的所有成员 包括min也包括max (未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRemRangeByScore(byte[], double, double)
     */
    public Long zRemRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
     * 实现命令 : ZSCORE key member 获取成员的分数
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zScore(byte[], byte[])
     */
    public <T> Double zScore(String key, T value) {
        return redisTemplate.opsForZSet().score(key, serialize(value));
    }

    /**
     * 实现命令 : ZINCRBY key 带符号的双精度浮点数 member 增减成员的分数
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zIncrBy(byte[], double, byte[])
     */
    public <T> Double zInCrBy(String key, T value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, serialize(value), delta);
    }

    /**
     * 实现命令 : ZCARD key 获取集合中成员的个数
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zCard(byte[])
     */
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 实现命令 : ZCOUNT key min max 获取某个分数范围内的成员个数，包括min也包括max (未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zCount(byte[], double, double)
     */
    public Long zCount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 实现命令 : ZRANK key member 按分数从小到大获取成员在有序集合中的排名
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRank(byte[], byte[])
     */
    public <T> Long zRank(String key, T value) {
        return redisTemplate.opsForZSet().rank(key, serialize(value));
    }

    /**
     * 实现命令 : ZREVRANK key member 按分数从大到小获取成员在有序集合中的排名
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRank(byte[], byte[])
     */
    public <T> Long zRevRank(String key, T value) {
        return redisTemplate.opsForZSet().reverseRank(key, serialize(value));
    }

    /**
     * 实现命令 : ZRANGE key start end 获取 start下标到 end下标之间到成员，并按分数从小到大返回 下标从0开始，支持负下标，-1表示最后一个成员，包括开始下标，也包括结束下标(未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRange(byte[], long, long)
     */
    public <T> Set<T> zRange(String key, long start, long end, Class<T> type) {
        Set<byte[]> range = redisTemplate.opsForZSet().range(key, start, end);
        return deserialize(range, type);
    }

    /**
     * 实现命令 : ZREVRANGE key start end 获取 start下标到 e?nd下标之间到成员，并按分数从小到大返回 下标从0开始，支持负下标，-1表示最后一个成员，包括开始下标，也包括结束下标(未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRange(byte[], long, long)
     */
    public <T> Set<T> zRevRange(String key, long start, long end, Class<T> type) {
        Set<byte[]> bytes = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : ZRANGEBYSCORE key min max 获取分数范围内的成员并按从小到大返回 (未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], double, double)
     */
    public <T> Set<T> zRangeByScore(String key, double min, double max, Class<T> type) {
        Set<byte[]> bytes = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : ZRANGEBYSCORE key min max LIMIT offset count 分页获取分数范围内的成员并按从小到大返回 包括min也包括max(未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRangeByScore(byte[], double, double, long,
     *      long)
     */
    public <T> Set<T> zRangeByScore(String key, double min, double max, int offset, int count, Class<T> type) {
        Set<byte[]> bytes = redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : ZREVRANGEBYSCORE key min max 获取分数范围内的成员并按从大到小返回 (未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], double, double)
     */
    public <T> Set<T> zRevRangeByScore(String key, double min, double max, Class<T> type) {
        Set<byte[]> bytes = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : ZREVRANGEBYSCORE key min max LIMIT offset count 分页获取分数范围内的成员并按从大到小返回 包括min也包括max(未验证)
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRevRangeByScore(byte[], double, double, long,
     *      long)
     */
    public <T> Set<T> zRevRangeByScore(String key, double min, double max, int offset, int count, Class<T> type) {
        Set<byte[]> bytes = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
        return deserialize(bytes, type);
    }

    /**
     * 实现命令 : ZREM key member [member ...] 删除成员
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zRem(byte[], byte[]...)
     */
    public <T> Long zRem(String key, Collection<T> values) {
        return redisTemplate.opsForZSet().remove(key, serialize(values));
    }

    /**
     * 实现命令 : ZUNIONSTORE destination numkeys key1 [key2 ...] 计算指定key集合与otherKey集合的并集，并保存到targetKey集合，aggregat 默认为 SUM
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], byte[]...)
     */
    public Long zUnionStore(String targetKey, String key, String otherKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, targetKey);
    }

    /**
     * 实现命令 : ZUNIONSTORE destination numkeys key1 [key2 ...] 计算指定key集合与otherKey集合的并集，并保存到targetKey集合，aggregat 默认为 SUM
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], byte[]...)
     */
    public Long zUnionStore(String targetKey, String key, Collection<String> otherKeys) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, targetKey);
    }

    /**
     * 实现命令 : ZUNIONSTORE destination numkeys key1 [key2 ...][AGGREGATE SUM | MIN | MAX]
     * 计算指定key集合与otherKey集合的并集，并保存到targetKey集合
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zUnionStore(byte[], RedisZSetCommands.Aggregate,
     *      int[], byte[]...)
     */
    public Long zUnionStore(String targetKey, String key, Collection<String> otherKeys,
                            RedisZSetCommands.Aggregate aggregat) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, targetKey, aggregat);
    }

    /**
     * 实现命令 : ZINTERSTORE destination numkeys key1 [key2 ...] 计算指定key集合与otherKey集合的交集，并保存到targetKey集合，aggregat 默认为 SUM
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], byte[]...)
     */
    public Long zInterStore(String targetKey, String key, String otherKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, targetKey);
    }

    /**
     * 实现命令 : ZINTERSTORE destination numkeys key1 [key2 ...] 计算指定key集合与otherKey集合的交集，并保存到targetKey集合，aggregat 默认为 SUM
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], byte[]...)
     */
    public Long zInterStore(String targetKey, String key, Collection<String> otherKeys) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, targetKey);
    }

    /**
     * 实现命令 : ZINTERSTORE destination numkeys key1 [key2 ...][AGGREGATE SUM | MIN | MAX]
     * 计算指定key集合与otherKey集合的交集，并保存到targetKey集合
     *
     * @see org.springframework.data.redis.connection.RedisZSetCommands#zInterStore(byte[], RedisZSetCommands.Aggregate,
     *      RedisZSetCommands.Weights, byte[]...)
     */
    public Long zInterStore(String targetKey, String key, Collection<String> otherKeys,
                            RedisZSetCommands.Aggregate aggregat) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, targetKey, aggregat);
    }

    /*********************************************** Sorted SET 相关操作 ***********************************************/

    @SuppressWarnings("unchecked")
    public void batchSet(Map<String, ?> payloadMap) {
        if (payloadMap == null || payloadMap.isEmpty()) {
            return;
        }
        RedisSerializer<String> keySerializer = (RedisSerializer<String>)redisTemplate.getKeySerializer();
        redisTemplate.executePipelined(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<String, ?> entry : payloadMap.entrySet()) {
                    if (entry.getValue() == null) {
                        connection.del(keySerializer.serialize(entry.getKey()));
                    } else {
                        connection.set(keySerializer.serialize(entry.getKey()), serialize(entry.getValue()));
                    }
                }
                // 管道里返回没用
                return null;
            }
        });
    }

    public void sAddAll(String key ,Collection<?> set) {
        if (set == null || set.isEmpty()) {
            return;
        }
        RedisSerializer<String> keySerializer = (RedisSerializer<String>)redisTemplate.getKeySerializer();
        var keyS = keySerializer.serialize(key);
        redisTemplate.executePipelined(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Object payload : set) {
                    connection.sAdd(keyS, serialize(payload));
                }
                // 管道里返回没用
                return null;
            }
        });
    }

    public void sRemMultiKeys(Map<String, ?> keyMemberMap) {
        if (keyMemberMap == null || keyMemberMap.isEmpty()) {
            return;
        }
        RedisSerializer<String> keySerializer = (RedisSerializer<String>)redisTemplate.getKeySerializer();
        redisTemplate.executePipelined(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<String, ?> entry : keyMemberMap.entrySet()) {
                    connection.sRem(Objects.requireNonNull(keySerializer.serialize(entry.getKey())), serialize(entry.getValue()));
                }
                return null;
            }
        });
    }

    public void sAddMultiKeys(Map<String, ?> keyMemberMap) {
        if (keyMemberMap == null || keyMemberMap.isEmpty()) {
            return;
        }
        RedisSerializer<String> keySerializer = (RedisSerializer<String>)redisTemplate.getKeySerializer();
        redisTemplate.executePipelined(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Map.Entry<String, ?> entry : keyMemberMap.entrySet()) {
                    connection.sAdd(Objects.requireNonNull(keySerializer.serialize(entry.getKey())), serialize(entry.getValue()));
                }
                return null;
            }
        });
    }
}
