Redis 是一个 **Key-Value（键值对）存储**，但它的 Value 不是只能存字符串这么简单 —— 它支持 **5 种最常用的 Value 类型**（String / List / Hash / Set / ZSet）外加几种特殊类型（Bitmap / HyperLogLog / Stream / Geo）。**Key 永远是字符串**，真正能"花样玩"的是 Value。理解每种 Value 类型适合什么场景，是写出好缓存代码的基础。

*Redis is a **key-value store**, but its Value is not limited to plain strings — it supports **5 commonly used Value types** (String / List / Hash / Set / ZSet) plus a few specialized types (Bitmap / HyperLogLog / Stream / Geo). **Keys are always strings**; the real richness lives in the Value side. Knowing which Value type fits which scenario is the foundation of writing good cache code.*

---

## 1. Key 是什么类型？ — *What Type Is a Key?*

**永远是 String（二进制安全字符串）**，没有别的选项。

***Always String (binary-safe string)** — there is no other option.*

- 最大长度：512 MB（实际不会用这么长）
- 命名约定：用冒号 `:` 分层，例如 `user:1001:cart`、`dish:category:1`、`token:emp:42`
- 区分大小写：`User:1` 和 `user:1` 是两个不同的 key

***Properties:***

- *Max length: 512 MB (you'll never get anywhere near this in practice).*
- *Naming convention: use colon `:` as a hierarchy separator, e.g. `user:1001:cart`, `dish:category:1`, `token:emp:42`.*
- *Case-sensitive: `User:1` and `user:1` are two distinct keys.*

### 举例（外卖项目里的实际 key 命名） — *Examples from the Sky Takeaway Project*

```text
dish_1                      ← 缓存 categoryId=1 的菜品列表
SHOP_STATUS                 ← 店铺营业状态全局开关
token:emp:42                ← 员工 42 的登录 token
sms:13800138000             ← 13800138000 的短信验证码
cart:1001                   ← 用户 1001 的购物车
hot_dishes                  ← 全局热销菜品排行榜
lock:order:202605270001     ← 订单号 202605270001 的分布式锁
```

***Real key names from the project:*** *`dish_1` (cached dish list for category 1), `SHOP_STATUS` (global shop open/close flag), `token:emp:42` (login token of employee 42), `sms:13800138000` (SMS code for that phone), `cart:1001` (cart for user 1001), `hot_dishes` (global bestseller leaderboard), `lock:order:202605270001` (distributed lock for that order).*

---

## 2. Value 的 5 种核心类型概览 — *The 5 Core Value Types at a Glance*

| 类型 / Type | 中文比喻 / Analogy                                                            | 典型用途 / Typical Use                                             | 关键命令 / Key Commands |
| --- |---------------------------------------------------------------------------|----------------------------------------------------------------| --- |
| **String** | 一个"字符串变量" / *single variable*                                             | 缓存 JSON、计数器、Token / *cached JSON, counters, tokens*            | `SET` / `GET` / `INCR` / `EXPIRE` |
| **List** | 按照插入的顺序排序，可以有重复的元素 可以从右从左都可以插入的"双端队列"类似java的linked list / *deque*         | 比如朋友圈点赞的先后，消息队列、最新评论列表 / *message queue, recent-comments feed* | `LPUSH` / `RPUSH` / `LPOP` / `LRANGE` |
| **Hash** | **用来存储对象，因为对象有属性和值** / Map" / *small object / map*                        | 购物车、用户属性 / *shopping cart, user attributes*                    | `HSET` / `HGET` / `HGETALL` / `HINCRBY` |
| **Set** | "无序 且 去重 "  类似java的hashset ，交集并集/ *unordered set, no duplicates*          | 标签、点赞用户去重（共同朋友的合集或交集） / *tags, unique liker sets*              | `SADD` / `SMEMBERS` / `SINTER` |
| **ZSet** | 集合中的每个元素关联一个分数score，根据分数生序排序，没重复元素（"带分数的排行榜"） / *sorted set, with scores* | 排行榜、延时队列 / *leaderboards, delayed queues*                      | `ZADD` / `ZRANGE` / `ZINCRBY` |

---

## 3. 逐个详解 + 外卖项目场景 — *Type-by-Type Walkthrough with Project Scenarios*

### 3.1 String —— 最常用的 Value 类型 — *The Most Commonly Used Value Type*

**特点**：存一个字符串，可以是数字（自动当数字处理）、JSON、甚至序列化的二进制对象。最大 512 MB。

***Properties:** stores a single string — can be a number (auto-handled as numeric), JSON, or even a serialized binary blob. Up to 512 MB.*

```redis
# 普通字符串
SET shop_status 1
GET shop_status                    # → "1"

# 带过期时间（30 分钟）
SET token:emp:42 "eyJhbGc..." EX 1800

# 计数器（原子自增）
INCR sms_count:13800138000
EXPIRE sms_count:13800138000 60    # 60 秒后过期，配合限流

# 缓存 JSON（菜品列表）
SET dish_1 "[{\"id\":1,\"name\":\"宫保鸡丁\"}, ...]"
```

**外卖项目用途** — ***Project usage:***

- **菜品列表缓存**：`dish_${categoryId}` → JSON 字符串

  ***Dish list cache:** `dish_${categoryId}` → JSON.*

- **店铺营业状态**：`SHOP_STATUS` = `"1"` 或 `"0"`

  ***Shop open/close flag:** `SHOP_STATUS = "1"` or `"0"`.*

- **登录 Token**：`token:emp:42` + 30 分钟 TTL

  ***Login token:** `token:emp:42` with a 30-minute TTL.*

- **短信验证码**：`sms:${phone}` + 60 秒 TTL

  ***SMS verification code:** `sms:${phone}` with a 60-second TTL.*

- **限流计数**：`INCR sms_count:${phone}`，超过阈值就拒绝

  ***Rate limiting:** `INCR sms_count:${phone}`, reject once it exceeds the threshold.*

---

### 3.2 List —— 有序、可重复的列表 — *Ordered, Duplicates-Allowed List*

**特点**：底层是双向链表，**从两头加 / 取都很快**。可以当队列（FIFO）也可以当栈（LIFO）。

***Properties:** internally a doubly-linked list, with **fast push/pop from both ends**. Can be used as a queue (FIFO) or stack (LIFO).*

```redis
# 从右边塞数据（当作消息队列）
RPUSH order_queue "order:202605270001"
RPUSH order_queue "order:202605270002"

# 从左边取（FIFO，先进先出）
LPOP order_queue                # → "order:202605270001"

# 看队列里所有元素（0 到 -1 = 全部）
LRANGE order_queue 0 -1

# 截断只保留最新 100 条
LTRIM recent_comments 0 99
```

**外卖项目用途** — ***Project usage:***

- **订单处理队列**：下单接口 `RPUSH order_queue`，后台 worker `BLPOP` 阻塞消费

  ***Order processing queue:** the order endpoint `RPUSH`es into `order_queue`; a background worker `BLPOP`s to consume.*

- **最近评论 Feed**：`LPUSH recent_comments` + `LTRIM 0 99` 保留最新 100 条

  ***Recent comments feed:** `LPUSH` new comments and `LTRIM 0 99` to keep the latest 100.*

- **历史搜索词**：用户搜索历史，按时间顺序展示

  ***Search history:** keep each user's recent search terms in time order.*

---

### 3.3 Hash —— 一个"小对象" — *A Small Object*

**特点**：Hash 内部是 `字段 → 值` 的映射，相当于一个**嵌套的小型 Map**。适合存"一个对象的多个属性"，**比把整个对象塞成 JSON 字符串更省内存、能单独改某个字段**。

***Properties:** a Hash is a `field → value` map — essentially a **nested mini-map**. Ideal for "multiple attributes of one object"; **more memory-efficient than serializing the whole object as JSON, and lets you update individual fields without rewriting everything**.*

```redis
# 存购物车：用户 1001 的购物车里每道菜的数量
HSET cart:1001 dish:1 2           # 菜品 1 加 2 份
HSET cart:1001 dish:5 1
HSET cart:1001 dish:9 3

# 取购物车里某一道菜的数量
HGET cart:1001 dish:1             # → "2"

# 取整个购物车
HGETALL cart:1001                 # → {dish:1: 2, dish:5: 1, dish:9: 3}

# 数量原子 +1（用户点了 dish:1 加号按钮）
HINCRBY cart:1001 dish:1 1        # → "3"

# 删掉某道菜
HDEL cart:1001 dish:5
```

**外卖项目用途** — ***Project usage:***

- **购物车**：`cart:${userId}` 每个 field 是一道菜，value 是数量 —— **最经典的 Hash 用法**

  ***Shopping cart:** `cart:${userId}` — each field is a dish, value is the quantity. **The textbook Hash example.***

- **用户属性缓存**：`user:${userId}` 字段有 `name`、`phone`、`level`，单独更新某个字段不影响其他

  ***Cached user attributes:** `user:${userId}` with fields `name`, `phone`, `level` — update one without touching the others.*

---

### 3.4 Set —— 无序、去重的集合 — *Unordered, Deduplicated Set*

**特点**：里面的元素**不重复、无序**。支持**交集、并集、差集**运算，这是数据库做不到的"高级查询"。

***Properties:** elements are **unique and unordered**. Supports **intersection / union / difference** operations — the kind of "advanced query" SQL can't do in one shot.*

```redis
# 给菜品 1 加标签
SADD tags:dish:1 "辣" "川菜" "招牌"

# 给菜品 5 加标签
SADD tags:dish:5 "辣" "粤菜"

# 查菜品 1 的所有标签
SMEMBERS tags:dish:1              # → ["辣", "川菜", "招牌"]

# 同时是"辣"和"招牌"的菜品有哪些？（交集）
SINTER tag_idx:辣 tag_idx:招牌

# 点赞过菜品 1 的用户集合（保证一人只能赞一次）
SADD likes:dish:1 1001 1002 1003
SCARD likes:dish:1                # → 3（点赞数）
SISMEMBER likes:dish:1 1001       # → 1（用户 1001 赞过）
```

**外卖项目用途** — ***Project usage:***

- **菜品标签 / 套餐标签**：`tags:dish:${id}` —— 多个标签不重复

  ***Dish / setmeal tags:** `tags:dish:${id}` — multiple unique tags.*

- **点赞 / 收藏去重**：`likes:dish:${id}` —— 同一用户只能赞一次

  ***Likes / favorites deduplication:** `likes:dish:${id}` — one like per user.*

- **互斥商品集合运算**：例如"同时被分类 A 和 B 包含的菜品"用 `SINTER`

  ***Set arithmetic on categories:** e.g. "dishes belonging to both category A and B" via `SINTER`.*

---

### 3.5 ZSet（Sorted Set）—— 带分数的有序集合 — *Sorted Set: Set with a Score per Element*

**特点**：每个元素都带一个 **分数（score）**，元素**按 score 自动排序**。这是 Redis 最强大的类型之一 —— **天然适合排行榜**。

***Properties:** every element carries a **score**, and elements are **automatically sorted by score**. One of Redis's most powerful types — **the natural fit for leaderboards**.*

```redis
# 每卖出一份菜，给菜品分数 +1
ZINCRBY hot_dishes 1 dish:1
ZINCRBY hot_dishes 1 dish:5
ZINCRBY hot_dishes 1 dish:1       # dish:1 现在分数是 2

# 看销量 Top 10（按分数从高到低）
ZREVRANGE hot_dishes 0 9 WITHSCORES

# 看菜品 1 当前排名（从 0 开始计数）
ZREVRANK hot_dishes dish:1        # → 0（说明它现在是第一名）

# 查分数在 5-10 之间的菜品（普通销量段）
ZRANGEBYSCORE hot_dishes 5 10
```

**外卖项目用途** — ***Project usage:***

- **热销菜品排行榜**：`hot_dishes` —— 每次下单 `ZINCRBY` 给对应菜品 +1

  ***Bestseller leaderboard:** `hot_dishes` — `ZINCRBY` by 1 for each dish on each order.*

- **延时队列**：score = 到期时间戳，`ZRANGEBYSCORE 0 now` 取出已到期的任务（比如自动取消未支付订单）

  ***Delayed queue:** score = expiration timestamp; `ZRANGEBYSCORE 0 now` to fetch tasks that are due (e.g. auto-cancel unpaid orders).*

- **会员积分榜**：`points_leaderboard`，按积分排会员

  ***Loyalty points leaderboard:** `points_leaderboard` ranked by member points.*

---

## 4. 不太常用但要知道的几种 — *Less Common but Worth Knowing*

| 类型 / Type | 一句话用途 / One-Line Use |
| --- | --- |
| **Bitmap**（位图）/ *Bitmap* | 用位（0/1）记录"某天某用户是否签到" —— 一年只需 365 bit ≈ 46 字节 / *Use bits to record "did user X check in on day Y" — only 365 bits per user per year ≈ 46 bytes.* |
| **HyperLogLog** | 估算"UV（独立访客数）"，**几乎不占内存**（12 KB 能估到几亿），误差 ~0.8% / *Estimate UV (unique visitor count) at near-zero memory cost — 12 KB estimates hundreds of millions with ~0.8% error.* |
| **Stream** | Redis 5.0 引入的**真正消息队列**（带消费组、ack 机制）/ *True message queue introduced in Redis 5.0 — with consumer groups and acknowledgements.* |
| **Geo** | 存经纬度，查"附近的店"（外卖**配送范围**就靠它）/ *Store lat/lng pairs, then query "shops nearby" — used for delivery-range lookups in food-delivery apps.* |

---

## 5. 在 Spring Boot 里怎么操作各类型 — *Operating on Each Type from Spring Boot*

Spring Data Redis 通过 `RedisTemplate`（或更常用的 `StringRedisTemplate`）暴露**每种类型对应的 ops 接口**。

*Spring Data Redis exposes a dedicated **ops interface for each Value type** via `RedisTemplate` (or more commonly `StringRedisTemplate`).*

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// String 类型 / String type
redisTemplate.opsForValue().set("shop_status", "1");
redisTemplate.opsForValue().set("token:emp:42", token, 30, TimeUnit.MINUTES);

// List 类型 / List type
redisTemplate.opsForList().rightPush("order_queue", "order:202605270001");
redisTemplate.opsForList().leftPop("order_queue");

// Hash 类型 / Hash type
redisTemplate.opsForHash().put("cart:1001", "dish:1", 2);
redisTemplate.opsForHash().increment("cart:1001", "dish:1", 1);

// Set 类型 / Set type
redisTemplate.opsForSet().add("likes:dish:1", 1001L, 1002L);
redisTemplate.opsForSet().intersect("tag_idx:辣", "tag_idx:招牌");

// ZSet 类型 / ZSet type
redisTemplate.opsForZSet().incrementScore("hot_dishes", "dish:1", 1);
redisTemplate.opsForZSet().reverseRangeWithScores("hot_dishes", 0, 9);
```

**记忆口诀**：`opsForX()` 里的 X 就是类型名 —— `Value`（String）、`List`、`Hash`、`Set`、`ZSet`。

***Memory aid:** the `X` in `opsForX()` is just the type name — `Value` (for String), `List`, `Hash`, `Set`, `ZSet`.*

---

## 6. 选型决策树 — *Decision Tree for Picking the Right Type*

```text
我要存什么？

├─ 单个值 / 一个对象的 JSON 序列化         → String
├─ 一串按顺序排列的东西（消息、最新N条）    → List
├─ 一个对象的多个独立字段（要单独改）       → Hash
├─ 一组不重复的元素（标签、去重 ID）        → Set
└─ 一组要按分数排序的元素（排行榜、延时队列） → ZSet
```

***What am I storing?***

- *A single value or the JSON of a single object → **String***
- *An ordered sequence (messages, "latest N items") → **List***
- *Multiple independent fields of one object (must update individually) → **Hash***
- *A unique-elements collection (tags, deduped IDs) → **Set***
- *A score-ranked collection (leaderboards, delayed queues) → **ZSet***

---

## 7. 一句话总结 — *Key Takeaway*

> **Key 永远是 String**；**Value 有 5 种主力**（String / List / Hash / Set / ZSet）+ 几种特殊类型。
> 选型的核心问题就一个：**"我要存的数据是什么形状的？"**
> 形状对了，Redis 才能跑出十万 QPS。

> ***Keys are always strings**; **Values have 5 workhorse types** (String / List / Hash / Set / ZSet) plus a few specialized ones.
> The core question when choosing a type is just one: **"What shape is my data?"**
> Pick the right shape, and Redis delivers its full 100k-QPS firepower.*