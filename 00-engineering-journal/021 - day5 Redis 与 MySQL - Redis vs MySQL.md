**Redis 与 MySQL** 都是后端项目里几乎绕不开的"数据存储"组件，但它们的定位完全不同：**MySQL 是一本"账本"，必须把所有业务数据老老实实存到磁盘上、要能查得出来、不能丢**；而 **Redis 是一张"便签"，写得飞快、随手贴随手撕，丢了也不心疼**（因为账本里还有底）。在《苍穹外卖》项目里，二者是**配合**使用的，而不是替代关系。

***Redis and MySQL** are two storage components you almost always end up using together in a backend project, but they serve very different roles: **MySQL is the "ledger" — every piece of business data must be reliably persisted to disk, queryable, and never lost**; **Redis is the "sticky note" — extremely fast to write, easy to throw away, and losing it is not the end of the world** (because the ledger is still the source of truth). In the "Sky Takeaway" project the two are used **in combination**, not as substitutes.*

---

## 1. 核心区别一览 — *  Core Differences at a Glance*

| 维度 / Aspect | MySQL | Redis |
| --- | --- | --- |
| 数据库类型 / Type | 关系型数据库（RDBMS）/ *Relational DB* | 内存型 Key-Value 数据库 / *In-memory KV store* |
| 存储位置 / Storage | 磁盘持久化 / *Disk (persistent)* | 内存为主，可选持久化（RDB / AOF）/ *Memory-first, optional persistence* |
| 数据结构 / Data model | 二维表（行 + 列 + Schema）/ *Tables with rows, columns, schema* | String / Hash / List / Set / ZSet / Stream |
| 查询能力 / Query power | SQL，支持多表 JOIN、复杂条件 / *SQL, JOINs, complex predicates* | 按 key 取值为主，范围查询有限 / *Key-based access, limited range queries* |
| 事务 / Transactions | ACID 强一致 / *ACID, strong consistency* | 弱事务（MULTI/EXEC，不支持回滚）/ *Weak (MULTI/EXEC, no rollback)* |
| 性能 / Throughput | 千级 QPS（带索引）/ *~1k QPS with indexes* | 十万级 QPS / *~100k QPS* |
| 数据规模 / Scale | TB 级，存得多 / *TB-scale, cheap to store a lot* | 受内存限制，存得少但快 / *Memory-bound: less data, but fast* |
| 定位 / Role | 业务数据的**真相源（Source of Truth）** / *Source of truth for business data* | **加速器 + 临时状态容器** / *Accelerator + transient state holder* |

---

## 2. 在外卖项目里"谁存什么" — *Who Stores What in the Sky Takeaway Project*

### 用 MySQL 存的（业务核心，必须持久 + 要 JOIN + 要报表） — *Stored in MySQL (core business data: must be persistent, joinable, reportable)*

* `dish`、`dish_flavor`、`category`、`setmeal`、`setmeal_dish` —— 菜品、口味、分类、套餐
* `orders`、`order_detail` —— 订单与订单明细（绝对不能丢）
* `employee`、`user`、`address_book` —— 员工、用户、地址簿
* 任何要做"按日/按周/按分类"统计的报表数据

***Stored in MySQL:***

* *`dish`, `dish_flavor`, `category`, `setmeal`, `setmeal_dish` — dishes, flavors, categories, setmeals*
* *`orders`, `order_detail` — orders and order line items (must never be lost)*
* *`employee`, `user`, `address_book` — employees, customers, address book*
* *Any data that needs to power daily / weekly / by-category aggregate reports*

### 用 Redis 存的（高频读 / 临时状态 / 全局开关 / 限流） — *Stored in Redis (hot reads, transient state, global switches, rate limiting)*

| 场景 / Scenario | Redis 用法 / Redis usage | 为什么不放 MySQL / Why not MySQL |
| --- | --- | --- |
| 用户端菜品列表缓存 / *Customer-side dish list cache* | `String`：`dish_${categoryId}` → JSON | 客户端打开 App 第一屏就要看到菜单，QPS 极高 / *Highest-QPS read path in the app* |
| 店铺营业状态 / *Shop open/close flag* | `String`：`SHOP_STATUS` = `1`/`0` | 全局一个值，所有客户端都要读，没必要建一张表 / *Single global value, no need for a table* |
| 登录 Token / *Login token / session* | `String`：`token:${userId}` + TTL 30 分钟 | 频繁读、要自动过期、丢了重新登录就行 / *Frequent reads, needs TTL, OK to lose* |
| 短信验证码 / *SMS verification code* | `String`：`sms:${phone}` + TTL 60 秒 | 临时数据，60 秒后自动消失，最适合 TTL / *Temporary, auto-expires — perfect for TTL* |
| 验证码限流 / *Rate limit on SMS sending* | `INCR` + TTL | 用 SQL 做计数会被打爆 / *SQL counter would be hammered* |
| 购物车 / *Shopping cart* | `Hash`：`cart:${userId}` → {dishId: count} | 加减项极频繁，下单瞬间一次性写回订单表 / *Frequent updates, finalized into orders on checkout* |
| 秒杀/限量套餐 / *Flash sale, limited setmeal* | `SETNX` 分布式锁 + `DECR` 库存 | MySQL 行锁扛不住高并发 / *MySQL row locks won't scale under high concurrency* |
| 热销榜 / *Bestseller leaderboard* | `ZSet`：`ZINCRBY hot_dishes 1 dishId` | ZSet 天然按分数排序，省去 ORDER BY / *ZSet keeps scores sorted natively* |

---

## 3. 配合套路：Cache-Aside Pattern（旁路缓存） — *The Cache-Aside Pattern*

这是外卖项目里**最常见、最实用**的 Redis + MySQL 配合模式，也是用户端查菜品列表时实际用的策略。

***This is the most common and most practical Redis + MySQL pattern in the project — and it's exactly what the customer-side dish list query uses.***

### 读流程 — *Read path*

```
1. 先查 Redis：cache.get("dish_" + categoryId)
2. 命中？→ 直接返回（90% 的请求走这条）
3. 未命中？→ 查 MySQL：dishMapper.list(categoryId)
4. 把结果写回 Redis：cache.set("dish_" + categoryId, json, ttl)
5. 返回数据
```

### 写流程（修改/删除菜品时） — *Write path (on dish create / update / delete)*

```
1. 先写 MySQL：dishMapper.update(dish)
2. 再删 Redis：cache.delete("dish_" + dish.getCategoryId())
   ↑ 注意：是【删除】而不是【更新】缓存
```

***Why delete instead of update?*** 因为在高并发下，"先更 DB → 再更 Cache" 的两步操作可能被其他线程交叉，导致 Cache 里写入旧值。直接 `delete`，让下次读请求自然触发上面读流程的第 3 步，从 MySQL 重新拉一份干净数据回来，最安全。

***In high-concurrency scenarios, "update DB then update cache" can interleave with other threads and leave a stale value in cache. Deleting the key instead lets the next read naturally re-trigger the read path above, pulling fresh data from MySQL — this is the safest option.***

### 代码长这样 — *What it looks like in code*

```java
// DishController（用户端） / Customer-side controller
@GetMapping("/list")
public Result<List<DishVO>> list(Long categoryId) {
    String key = "dish_" + categoryId;

    // 1. 查 Redis / 1. Try Redis first
    List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
    if (list != null && !list.isEmpty()) {
        return Result.success(list);          // 命中直接返回 / Hit — return
    }

    // 2. 未命中查 MySQL / 2. Miss — fall back to MySQL
    list = dishService.listWithFlavor(categoryId);

    // 3. 回写 Redis / 3. Populate the cache
    redisTemplate.opsForValue().set(key, list);

    return Result.success(list);
}

// DishController（管理端，写操作时清缓存） / Admin-side, evict cache on writes
@PostMapping
@CacheEvict(cacheNames = "dishCache", key = "'dish_' + #dishDTO.categoryId")
public Result save(@RequestBody DishDTO dishDTO) {
    dishService.saveWithFlavor(dishDTO);
    return Result.success();
}
```

> 这里用 Spring 的 `@CacheEvict` 注解比手写 `redisTemplate.delete()` 更优雅，等同效果，但代码更干净。
>
> *Using Spring's `@CacheEvict` annotation is cleaner than calling `redisTemplate.delete()` manually — same effect, much less boilerplate.*

---

## 4. 一些容易踩的坑 — *Common Pitfalls*

### 4.1 缓存穿透 — *Cache penetration*

* **症状 / Symptom**：恶意请求一个根本不存在的 `categoryId`，每次都 miss Redis，全部打到 MySQL。
* **解法 / Fix**：未命中 MySQL 时也写一个 `null` 占位（短 TTL），或者用布隆过滤器。

***Symptom:** A malicious request keeps asking for a `categoryId` that doesn't exist; every call misses Redis and hits MySQL.*
***Fix:** Cache a `null` placeholder (with a short TTL) for nonexistent keys, or use a Bloom filter.*

### 4.2 缓存雪崩 — *Cache avalanche*

* **症状 / Symptom**：大量 key 同时过期，所有请求瞬间打到 MySQL，DB 直接被压垮。
* **解法 / Fix**：给 TTL 加一个随机抖动（比如基础 30 分钟 + `random(0, 5)` 分钟）。

***Symptom:** Many keys expire at the same instant; all requests hit MySQL at once and the DB falls over.*
***Fix:** Add jitter to the TTL (e.g. base 30 minutes + a random 0–5 minute offset).*

### 4.3 缓存击穿 — *Cache breakdown*

* **症状 / Symptom**：某个**热点** key（比如"今日推荐套餐"）突然过期，瞬间几千请求同时去查 MySQL。
* **解法 / Fix**：用互斥锁（`SETNX`）让第一个线程查 DB 回填缓存，其他线程等一下再读缓存。

***Symptom:** A single hot key (e.g. "today's featured combo") expires; thousands of requests hit MySQL simultaneously.*
***Fix:** Use a mutex (`SETNX`) so only the first thread queries the DB and refills the cache; other threads wait and then re-read the cache.*

### 4.4 双写不一致 — *Dual-write inconsistency*

* **症状 / Symptom**：先更 DB 再更 Cache 的两步操作被并发交叉，导致 Cache 留下脏值。
* **解法 / Fix**：用上面第 3 节的"先更 DB → 删 Cache"策略；不要去"主动更新"缓存。

***Symptom:** Two-step "update DB then update cache" gets interleaved under concurrency, leaving stale data in cache.*
***Fix:** Use the "update DB then delete cache" strategy from section 3 — never proactively rewrite the cache.*

---

## 5. 一句话记住 — *Key Takeaway*

> **MySQL 是账本** —— 慢一点没关系，但绝对不能丢、要能查得出来；
> **Redis 是便签** —— 写得飞快，随手贴随手撕，丢了大不了再算一次。
> 生产环境里 99% 的项目都是**两个一起用**：MySQL 兜底真相，Redis 挡住绝大部分读请求。

> ***MySQL is the ledger** — being a little slow is fine, but it must never lose data and must be queryable;
> **Redis is the sticky note** — extremely fast to write, easy to throw away; losing it just means recomputing once.
> In real production systems 99% of projects use **both together**: MySQL holds the source of truth, Redis absorbs the overwhelming majority of read traffic.*
