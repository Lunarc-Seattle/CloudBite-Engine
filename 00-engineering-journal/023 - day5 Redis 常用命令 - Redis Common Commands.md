配合 `022 - day5 Redis 数据类型` 一起看。022 讲"每种 Value 类型是什么、装什么数据"，本篇是**操作这些 Value 的实操命令清单** —— 每个分组都给"命令 + 一句话解释 + 实际例子"。

*Read alongside `022 - day5 Redis Data Types`. While 022 explains "what each Value type is and what it stores", this file is a **hands-on cheat sheet of the commands used to operate on those Values** — each group lists "command + one-line meaning + a real example".*

---

## 0. 通用 / Key 管理命令 — *Generic / Key-Management Commands*

跟具体 Value 类型无关，操作 key 本身的命令。

*These commands operate on the key itself, independent of the Value type.*

| 命令 / Command | 作用 / Purpose | 例子 / Example |
| --- | --- | --- |
| `KEYS pattern` | 按通配符列出 key（**生产慎用，会阻塞**）/ *list keys by glob pattern (**dangerous in prod — blocks the server**)* | `KEYS dish_*` → 所有以 `dish_` 开头的 key |
| `SCAN cursor MATCH pattern` | 渐进式遍历 key，不阻塞 / *iterate keys incrementally, non-blocking* | `SCAN 0 MATCH dish_* COUNT 100` |
| `EXISTS key` | key 是否存在（返回 0/1）/ *does the key exist? (0/1)* | `EXISTS SHOP_STATUS` → `1` |
| `DEL key [key ...]` | 删除一个或多个 key / *delete one or more keys* | `DEL dish_1 dish_2` |
| `TYPE key` | 查看 value 是什么类型 / *what type is the value?* | `TYPE cart:1001` → `hash` |
| `RENAME old new` | 重命名 key / *rename a key* | `RENAME dish_old dish_new` |
| `EXPIRE key seconds` | 设置过期时间（秒）/ *set TTL in seconds* | `EXPIRE token:emp:42 1800` |
| `TTL key` | 查看剩余过期时间 / *check remaining TTL* | `TTL token:emp:42` → `1799` |
| `PERSIST key` | 移除过期时间，让 key 永久存活 / *remove TTL, key never expires* | `PERSIST SHOP_STATUS` |
| `RANDOMKEY` | 随机返回一个 key / *return a random key* | — |
| `DBSIZE` | 当前 DB 里有多少 key / *how many keys in the current DB* | `DBSIZE` → `1234` |

### 实用例子 — *Practical Example*

```redis
# 批量清掉某个分类的菜品缓存（写操作触发缓存失效时常用）
KEYS dish_*               # 看看有哪些
DEL dish_1 dish_2 dish_3  # 一次删多个
```

> ⚠️ 生产环境 **千万不要用 `KEYS *`** —— Redis 是单线程的，几百万 key 全扫一遍会卡住几秒钟。改用 `SCAN`。
>
> ⚠️ ***Never run `KEYS *` in production** — Redis is single-threaded, and scanning millions of keys at once will block the server for seconds. Use `SCAN` instead.*

---

## 1. String 类型命令 — *String-Type Commands*

### 1.1 最基础的四个命令 — *The Four Most Basic Commands*

不管是面试还是日常开发，String 类型几乎都从这四个命令切入：

*Whether in interviews or daily development, these four commands are the entry point to working with String values:*

| 命令 / Command | 作用 / Purpose | 例子 / Example | 频率 / Frequency |
| --- | --- | --- | --- |
| `SET key value` | 设值（如果已存在直接覆盖）/ *set value (overwrites if exists)* | `SET shop_status 1` | ⭐⭐⭐⭐⭐ 天天用 / *daily* |
| `GET key` | 取值 / *get value* | `GET shop_status` → `"1"` | ⭐⭐⭐⭐⭐ 天天用 / *daily* |
| `SETEX key seconds value` | 设值 + 过期时间（**单位：秒**）/ *set value with TTL (seconds)* | `SETEX token:emp:42 1800 "eyJhbGc..."` | ⭐⭐ 用得少 / *seldom* |
| `SETNX key value` | 仅当 key 不存在时才设值 / *set only if key does not exist* | `SETNX lock:order:1 "thread-A"` | ⭐⭐ 用得少 / *seldom* |

### 1.2 为什么 `SETEX` 和 `SETNX` 用得少？— *Why Are `SETEX` and `SETNX` Seldom Used Anymore?*

**Redis 2.6.12 之后，`SET` 命令支持了所有选项参数**，一条命令就能干 `SETEX` 和 `SETNX` 的活，所以新代码一般不再单独写后两条。

***Since Redis 2.6.12, the `SET` command supports all option flags**, so a single `SET` can do what `SETEX` and `SETNX` used to do — modern code rarely writes the latter two separately.*

```redis
# 旧写法 / Legacy
SETEX token:emp:42 1800 "eyJhbGc..."            # 设值 + 过期 30 分钟
SETNX lock:order:1 "thread-A"                    # 仅当不存在才设
EXPIRE lock:order:1 10                           # 然后再加过期（⚠️ 两步！不原子）

# 新写法（推荐）/ Modern (recommended)
SET token:emp:42 "eyJhbGc..." EX 1800
SET lock:order:1 "thread-A" NX EX 10             # 一条命令，原子操作
```

**关键风险**：`SETNX` 单独用 + 紧跟 `EXPIRE` **不是原子操作**。如果 `SETNX` 成功后服务器崩了，`EXPIRE` 没执行，这个 key 永远不会过期 —— 分布式锁会**永远锁死**。所以做锁**必须**用 `SET key value NX EX seconds` 一条命令。

***Critical risk:** Calling `SETNX` followed by `EXPIRE` as two separate commands is **not atomic**. If the server crashes between them, the `EXPIRE` never runs and the key never expires — a distributed lock built this way **gets stuck forever**. Always use a single `SET key value NX EX seconds` for locks.*

**一条 `SET` 通吃的完整语法** — *The full all-in-one `SET` syntax:*

```redis
SET key value [EX seconds | PX milliseconds] [NX | XX]
```

- 加 `EX seconds` → 等于 `SETEX`（秒级过期）/ *equivalent to `SETEX` (TTL in seconds)*
- 加 `PX milliseconds` → 毫秒级过期 / *TTL in milliseconds*
- 加 `NX` → 等于 `SETNX`（仅当不存在）/ *equivalent to `SETNX` (only if absent)*
- 加 `XX` → 仅当已存在才设 / *only if already present*
- 全都不加 → 普通 `SET`（覆盖式设值）/ *plain `SET` (overwriting)*

### 1.3 其他常用 String 命令 — *Other Frequently Used String Commands*

下面这些虽然不是最基础的四件套，但在缓存、计数器、限流场景里**比 `SETEX/SETNX` 用得更多**：

*These aren't part of the "basic four", but in caching, counters, and rate-limiting scenarios they're used **more often than `SETEX/SETNX`**:*

| 命令 / Command | 作用 / Purpose | 例子 / Example | 频率 / Frequency |
| --- | --- | --- | --- |
| `SET key value EX seconds` | 设值 + 过期（**取代 `SETEX`**）/ *set value with TTL (replaces `SETEX`)* | `SET token:emp:42 "..." EX 1800` | ⭐⭐⭐⭐ 常用 / *common* |
| `SET key value NX EX seconds` | 原子"占位 + 过期"（**分布式锁标准写法**）/ *atomic set-if-absent with TTL (**standard lock pattern**)* | `SET lock:order:1 "A" NX EX 10` | ⭐⭐⭐⭐ 常用 / *common* |
| `INCR key` | 原子 +1（要求 value 是数字）/ *atomic +1 (numeric value required)* | `INCR pv:dish:1` | ⭐⭐⭐⭐ 常用 / *common* |
| `DECR key` | 原子 -1 / *atomic -1* | `DECR stock:dish:1` | ⭐⭐⭐ 较常用 / *fairly common* |
| `INCRBY key n` | 原子加 n（n 可负）/ *atomic +n (n may be negative)* | `INCRBY balance:1001 -50` | ⭐⭐⭐ 较常用 / *fairly common* |
| `MSET k1 v1 k2 v2 ...` | 批量设置多个 key / *bulk set* | `MSET a 1 b 2 c 3` | ⭐⭐ 偶尔 / *occasional* |
| `MGET k1 k2 ...` | 批量取值 / *bulk get* | `MGET a b c` → `["1","2","3"]` | ⭐⭐ 偶尔 / *occasional* |
| `STRLEN key` | 字符串长度 / *string length* | `STRLEN dish_1` → `2048` | ⭐ 很少 / *rare* |
| `APPEND key value` | 在 value 末尾追加 / *append to value* | `APPEND log:today "new\n"` | ⭐ 很少 / *rare* |
| `GETSET key new` | 设新值并返回旧值 / *set new, return old* | `GETSET counter 0` | ⭐ 很少 / *rare* |

### 经典组合 — *Classic Combos*

```redis
# 1. 60 秒限流：用户 60 秒内只能发 1 次验证码
INCR sms_count:13800138000
EXPIRE sms_count:13800138000 60
# 取出来 > 1 就拒绝
GET sms_count:13800138000

# 2. 分布式锁（简化版，生产用 Redisson）
SET lock:order:202605270001 "thread-A" NX EX 10
# → "OK" 表示拿到锁；nil 表示别人抢到了

# 3. 计数器原子加（避免读-改-写竞争）
INCR pv:dish:1                    # 浏览量 +1
INCRBY balance:1001 -50           # 扣 50 块余额
```

---

## 2. List 类型命令 — *List-Type Commands*

| 命令 / Command | 作用 / Purpose                                                | 例子 / Example |
| --- |-------------------------------------------------------------| --- |
| `LPUSH key v1 v2 ...` | 从左边塞 / *push from the left*                                 | `LPUSH queue "task1"` |
| `RPUSH key v1 v2 ...` | 从右边塞 / *push from the right*                                | `RPUSH queue "task2"` |
| `LPOP key` | 从左边弹出 / *pop from the left*                                 | `LPOP queue` → `"task1"` |
| `RPOP key` | 从右边弹出 / *pop from the right*                                | `RPOP queue` |
| `BLPOP key timeout` | 阻塞式左弹（**消费者用**）/ *blocking left pop (**consumer pattern**)* | `BLPOP order_queue 30` |
| `BRPOP key timeout` | 阻塞式右弹 / *blocking right pop*                                | — |
| `LRANGE key start stop` | 按索引区间查（`-1` 表示末尾）/ *fetch by index range (`-1` = last)*     | `LRANGE queue 0 -1`（全部）|
| `LLEN key` | 列表长度 / *list length*                                        | `LLEN queue` → `42` |
| `LINDEX key i` | 按索引取单个 / *get by index*                                     | `LINDEX queue 0` |
| `LREM key count value` | 删除指定值（按 count 控制数量和方向）/ *remove by value*                   | `LREM queue 1 "task1"` |
| `LTRIM key start stop` | 截断只保留区间内 / *trim to a range*                                | `LTRIM recent 0 99`（只留前 100）|

### 经典组合 —— 消息队列模式 — *Classic Combo: Message Queue*

```redis
# 生产者：把订单号塞进队列
RPUSH order_queue "order:202605270001"
RPUSH order_queue "order:202605270002"

# 消费者：阻塞式取，没消息就等 30 秒
BLPOP order_queue 30
# → ["order_queue", "order:202605270001"]

# 维护"最新 N 条"：插入 + 截断
LPUSH recent_comments "刚发的评论"
LTRIM recent_comments 0 99            # 只保留最新 100 条
```

---

## 3. Hash 类型命令 — *Hash-Type Commands*

| 命令 / Command | 作用 / Purpose | 例子 / Example |
| --- | --- | --- |
| `HSET key field value` | 设字段（新增或覆盖）/ *set field* | `HSET cart:1001 dish:1 2` |
| `HGET key field` | 取字段 / *get field* | `HGET cart:1001 dish:1` → `"2"` |
| `HMSET key f1 v1 f2 v2 ...` | 批量设字段（Redis 4.0+ 用 `HSET` 多参数代替）/ *bulk set fields* | `HMSET user:1 name "张三" age 28` |
| `HMGET key f1 f2 ...` | 批量取字段 / *bulk get fields* | `HMGET user:1 name age` |
| `HGETALL key` | 取整个 hash（`field, value` 交替返回）/ *get all field/value pairs* | `HGETALL cart:1001` |
| `HDEL key field [field ...]` | 删字段 / *delete fields* | `HDEL cart:1001 dish:5` |
| `HEXISTS key field` | 字段是否存在 / *does the field exist?* | `HEXISTS cart:1001 dish:1` → `1` |
| `HINCRBY key field n` | 字段值原子加 n / *atomic +n on a field* | `HINCRBY cart:1001 dish:1 1` |
| `HKEYS key` | 所有字段名 / *all field names* | `HKEYS cart:1001` → `["dish:1","dish:5"]` |
| `HVALS key` | 所有字段值 / *all field values* | `HVALS cart:1001` → `["2","1"]` |
| `HLEN key` | 字段总数 / *number of fields* | `HLEN cart:1001` → `3` |

### 经典组合 —— 购物车操作 — *Classic Combo: Shopping Cart*

```redis
# 用户加购：dish:1 加 2 份
HSET cart:1001 dish:1 2

# 用户点 + 号（数量原子 +1）
HINCRBY cart:1001 dish:1 1            # → "3"

# 用户点 - 号
HINCRBY cart:1001 dish:1 -1

# 删除一道菜
HDEL cart:1001 dish:1

# 结算前一次性拿出整个购物车
HGETALL cart:1001
# → {"dish:1": "2", "dish:5": "1", "dish:9": "3"}
```

---

## 4. Set 类型命令 — *Set-Type Commands*

| 命令 / Command | 作用 / Purpose | 例子 / Example |
| --- | --- | --- |
| `SADD key m1 m2 ...` | 加成员（自动去重）/ *add members (dedup)* | `SADD tags:dish:1 "辣" "川菜"` |
| `SREM key m1 m2 ...` | 删成员 / *remove members* | `SREM tags:dish:1 "辣"` |
| `SMEMBERS key` | 列所有成员 / *list all members* | `SMEMBERS tags:dish:1` |
| `SISMEMBER key m` | 成员是否在集合里 / *is member in the set?* | `SISMEMBER likes:dish:1 1001` → `1` |
| `SCARD key` | 集合大小 / *set size* | `SCARD likes:dish:1` → `5` |
| `SRANDMEMBER key [n]` | 随机取一个或 n 个成员 / *random pick* | `SRANDMEMBER tags:dish:1` |
| `SPOP key [n]` | 随机弹出（**抽奖常用**）/ *random pop (**common for raffles**)* | `SPOP lottery_pool 1` |
| `SINTER k1 k2 ...` | 交集 / *intersection* | `SINTER likes:dish:1 likes:dish:5` |
| `SUNION k1 k2 ...` | 并集 / *union* | `SUNION tag:辣 tag:川菜` |
| `SDIFF k1 k2 ...` | 差集（k1 减 k2）/ *difference (k1 minus k2)* | `SDIFF following:1001 following:1002` |

### 经典组合 — *Classic Combos*

```redis
# 1. 共同好友（社交常见）
SINTER following:1001 following:1002
# → 两人都关注的用户列表

# 2. 点赞去重
SADD likes:dish:1 1001
SADD likes:dish:1 1001                # 重复加无效，集合还是 {1001}
SCARD likes:dish:1                    # → 1（真实点赞数）

# 3. 抽奖
SADD lottery_pool 1001 1002 1003 1004 1005
SPOP lottery_pool 1                   # 随机抽 1 个出来当中奖者
```

---

## 5. ZSet（Sorted Set）类型命令 — *ZSet-Type Commands*

| 命令 / Command | 作用 / Purpose | 例子 / Example |
| --- | --- | --- |
| `ZADD key score member` | 加成员并设分数 / *add member with score* | `ZADD hot_dishes 5 dish:1` |
| `ZINCRBY key n member` | 给成员分数加 n / *increment score* | `ZINCRBY hot_dishes 1 dish:1` |
| `ZSCORE key member` | 查成员的分数 / *get a member's score* | `ZSCORE hot_dishes dish:1` → `"6"` |
| `ZRANK key member` | 升序排名（0 起）/ *ascending rank (0-based)* | `ZRANK hot_dishes dish:1` |
| `ZREVRANK key member` | 降序排名（**排行榜常用**）/ *descending rank (**typical for leaderboards**)* | `ZREVRANK hot_dishes dish:1` → `0` |
| `ZRANGE key start stop [WITHSCORES]` | 升序取区间 / *ascending range* | `ZRANGE hot_dishes 0 9` |
| `ZREVRANGE key start stop [WITHSCORES]` | 降序取区间（**Top N**）/ *descending range (**Top N**)* | `ZREVRANGE hot_dishes 0 9 WITHSCORES` |
| `ZRANGEBYSCORE key min max` | 按分数区间取 / *by score range* | `ZRANGEBYSCORE hot_dishes 5 10` |
| `ZCARD key` | 成员总数 / *member count* | `ZCARD hot_dishes` → `100` |
| `ZCOUNT key min max` | 分数区间内的成员数 / *count in score range* | `ZCOUNT hot_dishes 5 10` |
| `ZREM key m1 m2 ...` | 删成员 / *remove members* | `ZREM hot_dishes dish:1` |

### 经典组合 — *Classic Combos*

```redis
# 1. 销量榜：下单时给对应菜品 +1
ZINCRBY hot_dishes 1 dish:1
ZINCRBY hot_dishes 1 dish:5

# Top 10 销量
ZREVRANGE hot_dishes 0 9 WITHSCORES
# → ["dish:5", "12", "dish:1", "8", ...]

# 查菜品 1 当前排名
ZREVRANK hot_dishes dish:1            # → 1（说明第二名，0 起）

# 2. 延时队列：score = 到期时间戳
ZADD delayed_tasks 1779407354 "cancel_order:202605270001"
# 后台定时任务：取出当前已到期的
ZRANGEBYSCORE delayed_tasks 0 1779407354
```

---

## 6. 连接 / 服务器相关 — *Connection & Server Commands*

| 命令 / Command | 作用 / Purpose | 例子 / Example |
| --- | --- | --- |
| `PING` | 测试连通性 / *test connectivity* | `PING` → `PONG` |
| `AUTH password` | 输入密码 / *authenticate* | `AUTH 123456` |
| `SELECT n` | 切换 DB（0-15）/ *switch DB index* | `SELECT 0` |
| `FLUSHDB` | 清空当前 DB（**只在开发用**）/ *clear current DB (**dev only**)* | — |
| `FLUSHALL` | 清空所有 DB（**别在生产用**）/ *clear all DBs (**never in prod**)* | — |
| `INFO [section]` | 服务器统计信息 / *server stats* | `INFO memory` |
| `CONFIG GET pattern` | 看配置 / *read config* | `CONFIG GET maxmemory` |
| `CLIENT LIST` | 列出所有客户端连接 / *list client connections* | — |
| `MONITOR` | 实时打印所有命令（**调试用**）/ *real-time command stream (**debugging only**)* | — |
| `SHUTDOWN` | 关闭 Redis 服务 / *shut down the server* | — |

---

## 7. 在 Spring Boot 里的对应调用 — *Equivalent Calls in Spring Boot*

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 通用 key 操作 / Generic key ops
redisTemplate.hasKey("SHOP_STATUS");                                   // EXISTS
redisTemplate.delete("dish_1");                                        // DEL
redisTemplate.expire("token:emp:42", 30, TimeUnit.MINUTES);            // EXPIRE
redisTemplate.getExpire("token:emp:42", TimeUnit.SECONDS);             // TTL

// String / 字符串
redisTemplate.opsForValue().set("shop_status", "1");                   // SET
redisTemplate.opsForValue().get("shop_status");                        // GET
redisTemplate.opsForValue().increment("pv:dish:1");                    // INCR
redisTemplate.opsForValue().setIfAbsent("lock:order:1", "A", 10, TimeUnit.SECONDS); // SET NX EX

// List / 列表
redisTemplate.opsForList().rightPush("queue", "task1");                // RPUSH
redisTemplate.opsForList().leftPop("queue");                           // LPOP
redisTemplate.opsForList().range("queue", 0, -1);                      // LRANGE

// Hash
redisTemplate.opsForHash().put("cart:1001", "dish:1", 2);              // HSET
redisTemplate.opsForHash().increment("cart:1001", "dish:1", 1);        // HINCRBY
redisTemplate.opsForHash().entries("cart:1001");                       // HGETALL

// Set
redisTemplate.opsForSet().add("tags:dish:1", "辣", "川菜");             // SADD
redisTemplate.opsForSet().isMember("likes:dish:1", 1001L);             // SISMEMBER
redisTemplate.opsForSet().intersect("likes:dish:1", "likes:dish:5");   // SINTER

// ZSet
redisTemplate.opsForZSet().incrementScore("hot_dishes", "dish:1", 1);  // ZINCRBY
redisTemplate.opsForZSet().reverseRangeWithScores("hot_dishes", 0, 9); // ZREVRANGE
redisTemplate.opsForZSet().reverseRank("hot_dishes", "dish:1");        // ZREVRANK
```

---

## 8. 命令记忆口诀 — *Mnemonic for Command Names*

```text
首字母 → 类型：
  无前缀  → String / 通用    （SET / GET / DEL / EXPIRE）
  L 开头  → List              （LPUSH / LPOP / LRANGE）
  H 开头  → Hash              （HSET / HGET / HGETALL）
  S 开头  → Set               （SADD / SMEMBERS / SINTER）
  Z 开头  → ZSet (Sorted Set) （ZADD / ZRANGE / ZINCRBY）
```

***First-letter → type mapping:***

- *no prefix → String / generic (`SET`, `GET`, `DEL`, `EXPIRE`)*
- *`L` → List (`LPUSH`, `LPOP`, `LRANGE`)*
- *`H` → Hash (`HSET`, `HGET`, `HGETALL`)*
- *`S` → Set (`SADD`, `SMEMBERS`, `SINTER`)*
- *`Z` → ZSet / Sorted Set (`ZADD`, `ZRANGE`, `ZINCRBY`)*

记住这个前缀规律，所有 200+ Redis 命令的归属一目了然。

*Once you internalize this prefix pattern, all 200+ Redis commands instantly fall into their correct buckets.*

---

## 9. 一句话总结 — *Key Takeaway*

> 看到命令的**首字母**就知道操作的是哪种 Value 类型；常用命令实际就**几十个**，背一遍 cheat sheet + 在外卖项目里跑几次就熟了。剩下的稀有命令 `redis.io/commands` 现查就行。

> *Look at the **first letter** of a command and you instantly know which Value type it operates on. The commonly used commands number in the **dozens, not hundreds** — go through this cheat sheet once and run them a few times in the project, and they'll stick. For rare commands, just look them up at `redis.io/commands`.*