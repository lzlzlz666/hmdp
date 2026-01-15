package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    /**
     * 开始时间戳
     * 设置为 2026-01-01 00:00:00 UTC 的秒数
     * 计算方法：LocalDateTime.of(2026, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.UTC)
     */
    private static final long BEGIN_TIMESTAMP = 1767225600L;

    /**
     * 序列号的位数
     * 根据截图底部的图解，序列号占 32 位
     */
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号
        // 2.1. 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2. 自增长
        // Redis 的 INCR 命令：如果 key 不存在会自动创建并置为 0，然后加 1
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接并返回
        // 将时间戳左移 32 位，空出低 32 位给序列号，然后通过或运算(|)把序列号填进去
        return timestamp << COUNT_BITS | count;
    }
}