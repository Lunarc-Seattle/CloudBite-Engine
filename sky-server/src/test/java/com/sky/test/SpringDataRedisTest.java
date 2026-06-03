package com.sky.test;

import com.sky.SkyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = SkyApplication.class)
public class SpringDataRedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    // ─── 1. String / opsForValue ─────────────────────────────────────────
    @Test
    public void testString() {
        System.out.println(redisTemplate);
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // SET key value
        valueOperations.set("shop_status", "1");
        // GET key
        Object status = valueOperations.get("shop_status");
        System.out.println("shop_status = " + status);

        // SET key value EX seconds  (相当于 SETEX)
        valueOperations.set("token:emp:42", "eyJhbGc...", 30, TimeUnit.MINUTES);

        // SET key value NX EX seconds (分布式锁标准写法)
        Boolean got = valueOperations.setIfAbsent("lock:order:1", "thread-A", 10, TimeUnit.SECONDS);
        System.out.println("lock acquired? " + got);

        // INCR
        valueOperations.increment("pv:dish:1");
        valueOperations.increment("pv:dish:1");
        System.out.println("pv:dish:1 = " + valueOperations.get("pv:dish:1"));
    }

    // ─── 2. List / opsForList ────────────────────────────────────────────
    @Test
    public void testList() {
        ListOperations listOps = redisTemplate.opsForList();

        // RPUSH —— 从右边塞（消息队列模式）
        listOps.rightPush("order_queue", "order:202605270001");
        listOps.rightPush("order_queue", "order:202605270002");

        // LPOP —— 从左边弹（FIFO）
        Object first = listOps.leftPop("order_queue");
        System.out.println("popped: " + first);

        // LRANGE 0 -1 —— 全部
        System.out.println("queue: " + listOps.range("order_queue", 0, -1));

        // LLEN
        System.out.println("size: " + listOps.size("order_queue"));
    }

    // ─── 3. Hash / opsForHash ────────────────────────────────────────────
    @Test
    public void testHash() {
        HashOperations hashOps = redisTemplate.opsForHash();

        // HSET —— 购物车经典用法
        hashOps.put("cart:1001", "dish:1", 2);
        hashOps.put("cart:1001", "dish:5", 1);
        hashOps.put("cart:1001", "dish:9", 3);

        // HGET
        Object count = hashOps.get("cart:1001", "dish:1");
        System.out.println("dish:1 count = " + count);

        // HINCRBY —— 用户点了 + 号
        hashOps.increment("cart:1001", "dish:1", 1);

        // HGETALL —— 结算前一次性取整个购物车
        Map cart = hashOps.entries("cart:1001");
        System.out.println("cart:1001 = " + cart);

        // HDEL
        hashOps.delete("cart:1001", "dish:5");
    }

    // ─── 4. Set / opsForSet ──────────────────────────────────────────────
    @Test
    public void testSet() {
        SetOperations setOps = redisTemplate.opsForSet();

        // SADD —— 给菜品打标签
        setOps.add("tags:dish:1", "辣", "川菜", "招牌");
        setOps.add("tags:dish:5", "辣", "粤菜");

        // SMEMBERS
        Set members = setOps.members("tags:dish:1");
        System.out.println("tags:dish:1 = " + members);

        // SISMEMBER
        Boolean spicy = setOps.isMember("tags:dish:1", "辣");
        System.out.println("dish:1 is 辣? " + spicy);

        // SINTER —— 同时是"辣"和某分类的菜品
        Set intersect = setOps.intersect("tags:dish:1", "tags:dish:5");
        System.out.println("dish:1 ∩ dish:5 = " + intersect);

        // SCARD
        System.out.println("tag count of dish:1 = " + setOps.size("tags:dish:1"));
    }

    // ─── 5. ZSet / opsForZSet ────────────────────────────────────────────
    @Test
    public void testZSet() {
        ZSetOperations zsetOps = redisTemplate.opsForZSet();

        // ZINCRBY —— 每卖一份菜品销量 +1
        zsetOps.incrementScore("hot_dishes", "dish:1", 1);
        zsetOps.incrementScore("hot_dishes", "dish:5", 3);
        zsetOps.incrementScore("hot_dishes", "dish:1", 1);  // dish:1 = 2

        // ZSCORE
        Double score = zsetOps.score("hot_dishes", "dish:1");
        System.out.println("dish:1 score = " + score);

        // ZREVRANGE 0 9 —— Top 10
        Set top = zsetOps.reverseRangeWithScores("hot_dishes", 0, 9);
        System.out.println("top dishes = " + top);

        // ZREVRANK —— 当前排名（0 起）
        Long rank = zsetOps.reverseRank("hot_dishes", "dish:1");
        System.out.println("dish:1 rank = " + rank);
    }

    // ─── 6. 通用 / Key 管理 ──────────────────────────────────────────────
    @Test
    public void testKeyOps() {
        // EXISTS
        Boolean exists = redisTemplate.hasKey("shop_status");
        System.out.println("shop_status exists? " + exists);

        // EXPIRE
        redisTemplate.expire("shop_status", 60, TimeUnit.SECONDS);

        // TTL
        Long ttl = redisTemplate.getExpire("shop_status", TimeUnit.SECONDS);
        System.out.println("shop_status TTL = " + ttl + "s");

        // TYPE
        System.out.println("type = " + redisTemplate.type("cart:1001"));

        // DEL
        redisTemplate.delete("shop_status");
    }
}