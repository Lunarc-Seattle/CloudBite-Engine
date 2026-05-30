**Spring Data Redis** 是 Spring 官方提供的 Redis 客户端封装库 —— 让你用类似操作 JPA 的方式操作 Redis，不用写一堆原始命令字符串，也不用自己管连接池。配合 022（数据类型）和 023（命令清单）一起看：022/023 讲"Redis 自己是什么"，本篇讲"Java 怎么调它"。

***Spring Data Redis** is Spring's official Redis client abstraction — it lets you operate on Redis with a JPA-like programming model, without hand-writing raw command strings or managing connection pools yourself. Read alongside 022 (data types) and 023 (commands): those two cover "what Redis itself is", and this file covers "how Java talks to it".*

---

## 1. 它在 Spring 全家桶里的定位 — *Where It Sits in the Spring Stack*

```text
你的代码 (Service / Controller)
       ↓ 调用 / calls
Spring Data Redis           ← 高层 API（RedisTemplate, @Cacheable 等）
       ↓ 委托给 / delegates to
底层驱动 (Lettuce / Jedis)   ← 真正跟 Redis 服务器通信的库
       ↓ TCP
Redis Server
```

*Your code → Spring Data Redis (high-level API like `RedisTemplate`, `@Cacheable`) → underlying driver (Lettuce or Jedis) → TCP → Redis server.*

---

## 2. 它解决什么问题 — *What Problem It Solves*

如果**不用 Spring Data Redis**，直接用 Jedis 长这样：

*Without Spring Data Redis, calling Jedis directly looks like:*

```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.auth("password");
jedis.set("shop_status", "1");
String status = jedis.get("shop_status");
jedis.close();   // 必须自己关连接，否则连接泄漏 / must close manually, else connection leaks
```

每个方法都要**手动开 / 关连接**、自己 try-catch、自己管连接池、自己写序列化。100 个 Service 方法 = 100 份样板代码。

*Every method has to **manually open and close connections**, wrap its own try-catch, manage its own pool, write its own serialization. 100 service methods = 100 copies of boilerplate.*

**用了 Spring Data Redis** 之后变成：

***With Spring Data Redis** it becomes:*

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void someMethod() {
    redisTemplate.opsForValue().set("shop_status", "1");
    String status = (String) redisTemplate.opsForValue().get("shop_status");
}
```

干净、无样板、连接池自动管理。

*Clean, zero boilerplate, connection pool managed automatically.*

---

## 3. 核心组件 — *Core Components*

| 组件 / Component | 作用 / Purpose |
| --- | --- |
| `RedisTemplate<K,V>` | 万能模板，K/V 都可以是任意类型（需要配序列化器）/ *generic template; K and V can be any type, but you must configure serializers* |
| `StringRedisTemplate` | RedisTemplate 的特化版，K 和 V 都强制是 String（**外卖项目最常用**）/ *specialization with K and V forced to `String` (**most common in the project**)* |
| `opsForValue()` | 操作 **String** 类型 / *operates on **String** values* |
| `opsForList()` | 操作 **List** 类型 / *operates on **List** values* |
| `opsForHash()` | 操作 **Hash** 类型 / *operates on **Hash** values* |
| `opsForSet()` | 操作 **Set** 类型 / *operates on **Set** values* |
| `opsForZSet()` | 操作 **ZSet** 类型 / *operates on **ZSet** values* |
| `RedisConnectionFactory` | 连接工厂，配置主机、端口、密码、连接池 / *connection factory: host, port, password, pool* |
| `RedisSerializer` | 控制 K/V 怎么从 Java 对象 → 字节 → Redis 存的格式 / *controls how K/V are converted from Java object → bytes → what's stored in Redis* |

---

## 4. 怎么用（外卖项目的真实步骤） — *How to Use It (The Real Steps in the Project)*

### Step 1：加依赖 — *Add the Dependency* (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Spring Boot 2.x 默认带 **Lettuce**（不是 Jedis）作为底层驱动。

*Spring Boot 2.x ships with **Lettuce** as the default driver (not Jedis).*

### Step 2：配置 Redis 连接 — *Configure the Redis Connection* (`application.yml`)

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: 123456
      database: 0
      lettuce:
        pool:
          max-active: 8       # 最大连接数 / max connections
          max-idle: 8         # 最大空闲连接数 / max idle connections
          min-idle: 0
          max-wait: -1ms      # 等待获取连接的最大时间 / max wait for a connection
```

### Step 3：写一个 Config 类让数据**人类可读** — *Write a Config Class So Data Is **Human-Readable***

不写这个的话，默认用 JDK 序列化，Redis 里看到的是一堆 `\xac\xed\x00\x05...` 二进制，调试时根本看不懂。

*Without this step, Spring uses JDK serialization by default — you'll see binary garbage like `\xac\xed\x00\x05...` in Redis, completely unreadable during debugging.*

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 用 String 序列化器 → Redis 里看到的就是 "token:emp:42"
        // Key serializer = String → keys appear in Redis as "token:emp:42"
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 用 JSON 序列化器 → Redis 里看到的就是 {"id":1,"name":"宫保鸡丁"}
        // Value serializer = JSON → values appear in Redis as {"id":1,"name":"宫保鸡丁"}
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }
}
```

### Step 4：在 Service 里注入用 — *Inject and Use It in a Service*

```java
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DishMapper dishMapper;

    public List<DishVO> listByCategoryId(Long categoryId) {
        String key = "dish_" + categoryId;

        // 1. 查 Redis / Check Redis first
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null && !list.isEmpty()) {
            return list;
        }

        // 2. miss 后查 MySQL / On cache miss, fall back to MySQL
        list = dishMapper.listByCategoryId(categoryId);

        // 3. 回写 Redis / Populate the cache
        redisTemplate.opsForValue().set(key, list);

        return list;
    }
}
```

---

## 5. `opsForX()` 设计模式 —— "分门别类的工具箱" — *The `opsForX()` Pattern: A Toolbox per Type*

为什么不直接 `redisTemplate.set(...)` 而是 `redisTemplate.opsForValue().set(...)` 多绕一层？

*Why isn't it just `redisTemplate.set(...)` instead of going through `redisTemplate.opsForValue().set(...)`?*

因为 Redis 五种类型的命令是**完全不同的语义集**：

*Because Redis's five data types are **semantically disjoint command sets**:*

| 类型 / Type | 操作入口 / Ops Entry | 调用示例 / Example Calls |
| --- | --- | --- |
| String | `opsForValue()` | `.set(key, val)` / `.get(key)` / `.increment(key)` |
| List | `opsForList()` | `.leftPush(...)` / `.rightPop(...)` / `.range(...)` |
| Hash | `opsForHash()` | `.put(key, field, val)` / `.entries(key)` |
| Set | `opsForSet()` | `.add(...)` / `.members(...)` / `.intersect(...)` |
| ZSet | `opsForZSet()` | `.add(key, member, score)` / `.reverseRange(...)` |

把方法按类型分到不同的"门店"里，避免单个 `redisTemplate` 上挂几百个名字相似的方法，IDE 自动补全也清爽。

*Each "shop" handles one type — avoids piling hundreds of similarly named methods onto `redisTemplate`, and IDE auto-completion stays uncluttered.*

> **记忆口诀**：跟 023 笔记里 Redis 命令的首字母规律完全对应。L 开头 → `opsForList`，H 开头 → `opsForHash`，S 开头 → `opsForSet`，Z 开头 → `opsForZSet`，无前缀 → `opsForValue`。
>
> ***Memory aid:** this mirrors the first-letter rule from file 023. `L` → `opsForList`, `H` → `opsForHash`, `S` → `opsForSet`, `Z` → `opsForZSet`, no prefix → `opsForValue`.*

---

## 6. 声明式缓存：`@Cacheable` / `@CacheEvict` — *Declarative Caching*

Spring 在 Data Redis 之上再叠了一层 **Spring Cache 抽象**，让你**不写一行 `RedisTemplate` 代码**就能给方法加缓存：

*Spring layers a **Spring Cache abstraction** on top of Data Redis, letting you cache method results **without writing any `RedisTemplate` code**:*

```java
// 启动类加这个开启缓存抽象 / Enable caching on the main class
@EnableCaching
@SpringBootApplication
public class SkyApplication { ... }
```

```java
@Service
public class DishService {

    // 调用前先查缓存，命中直接返回；没命中执行方法 + 把结果存进 Redis
    // Check cache first; on hit return immediately, on miss run the method and store the result
    @Cacheable(cacheNames = "dishCache", key = "#categoryId")
    public List<DishVO> listByCategoryId(Long categoryId) {
        return dishMapper.listByCategoryId(categoryId);
    }

    // 方法执行后自动删 Redis 里对应的 key
    // After the method runs, evict the matching key in Redis
    @CacheEvict(cacheNames = "dishCache", key = "#dishDTO.categoryId")
    public void update(DishDTO dishDTO) {
        dishMapper.update(dishDTO);
    }

    // 删整个 cache（管理端改一道菜可能影响多个分类，干脆全清）
    // Wipe the entire cache (a single admin edit may affect multiple categories)
    @CacheEvict(cacheNames = "dishCache", allEntries = true)
    public void batchDelete(List<Long> ids) {
        dishMapper.deleteBatch(ids);
    }
}
```

代码里**完全看不到 Redis 字样**了 —— 业务代码只关心"我从数据库查菜品"，缓存逻辑被 Spring 自动织进去。这是 Spring 风格的精髓：**约定 + 注解 = 不写样板**。

*The word "Redis" disappears entirely from the business code — the service only cares about "fetch dishes from the DB", and caching is woven in automatically by Spring. This is the spirit of the Spring style: **convention + annotation = no boilerplate**.*

---

## 7. Lettuce vs Jedis（底层驱动） — *Lettuce vs Jedis (Underlying Driver)*

|  | Jedis | Lettuce |
| --- | --- | --- |
| 出生年代 / Era | 老牌（2009 起）/ *veteran (since 2009)* | 新（2014 起）/ *modern (since 2014)* |
| 线程安全 / Thread safety | ❌ 一个 Jedis 实例**不能多线程共享**，必须用连接池 / *a single Jedis instance is **not thread-safe**, must use a pool* | ✅ 一个连接可以多线程共享 / *one connection can be shared across threads* |
| 网络模型 / Network model | 同步阻塞 / *synchronous, blocking* | Netty + 非阻塞 NIO / *Netty + non-blocking NIO* |
| Spring Boot 2.x 默认 / *Spring Boot 2.x default* | ❌ | ✅ |
| 适合场景 / Best for | 小项目、同步调用 / *small projects, synchronous calls* | 高并发、Reactive / *high concurrency, reactive use cases* |

外卖项目里 Spring Boot 2.x **默认就是 Lettuce**，除非你在 `pom.xml` 里显式排掉再加 Jedis。99% 情况不用管这一层。

*The Sky Takeaway project runs on Spring Boot 2.x and therefore uses **Lettuce by default** — unless you explicitly exclude it from `pom.xml` and pull in Jedis. 99% of the time you can ignore this layer entirely.*

---

## 8. 一句话总结 — *Key Takeaway*

> **Spring Data Redis = Spring 风格的 Redis 客户端**：用 `RedisTemplate` 写 Java 代码代替手写 Redis 命令，连接池自动管，序列化可配置，再叠一层 `@Cacheable` 之类的注解就可以**完全脱离 Redis 字样**写缓存代码。
>
> 外卖项目里你只需要做三件事：① pom 加依赖 → ② `application.yml` 写 Redis 连接信息 → ③ Service 里 `@Autowired RedisTemplate`，用 `opsForValue() / opsForHash()`。剩下的它替你扛。

> ***Spring Data Redis = Redis client, the Spring way:** use `RedisTemplate` to write Java code instead of hand-writing Redis commands, get connection-pool management for free, configure serializers as needed, and layer `@Cacheable`-style annotations on top to **eliminate the word "Redis" from your business code**.*
>
> *In this project, you only do three things: ① add the dependency in `pom.xml` → ② declare Redis connection info in `application.yml` → ③ `@Autowired RedisTemplate` in your service and call `opsForValue()` / `opsForHash()`. The framework handles the rest.*
