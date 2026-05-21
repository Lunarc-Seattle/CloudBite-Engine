# Spring 分层架构调用链 — *Spring Layered Architecture Call Chain*

> 以"新增菜品 + 口味"（`saveWithFlavor`）为例，把一次 HTTP 请求从前端到数据库的完整路径走一遍。
>
> *Using the "add dish + flavors" (`saveWithFlavor`) feature as an example, this note walks through the full path of a single HTTP request — from the front end all the way to the database.*

---

## 1. 完整调用链一图 — *The Full Call Chain in One Diagram*

```text
┌─────────────────────────────────────────────────────────────┐
│ ① 前端发请求                                                   │
│   POST /admin/dish  + JSON body                              │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ② Controller 层（DishController.java）                        │
│                                                              │
│   @RestController                                            │
│   public class DishController {                              │
│       @Autowired                                             │
│       private DishService dishService;  ← 注入 Service 接口   │
│                                                              │
│       @PostMapping                                           │
│       public Result saveWithFlavor(@RequestBody DishDTO d) { │
│           dishService.saveWithFlavor(d);  ← 调 Service 方法  │
│           return Result.success();                           │
│       }                                                      │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ③ Service 接口（DishService.java）—— 定义"能做什么"             │
│                                                              │
│   public interface DishService {                             │
│       void saveWithFlavor(DishDTO dishDTO);  ← 只有签名       │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ④ ServiceImpl 实现类（DishServicempl.java）—— 写"怎么做"       │
│                                                              │
│   @Service                                                   │
│   public class DishServicempl implements DishService {       │
│                                                              │
│       @Autowired                                             │
│       private DishMapper dishMapper;  ← 注入 Mapper 接口       │
│                                                              │
│       @Transactional                                         │
│       public void saveWithFlavor(DishDTO dishDTO) {          │
│           // 业务逻辑：拷贝属性、调 Mapper 等                    │
│           dishMapper.insert(dish);                           │
│       }                                                      │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ⑤ Mapper 接口（DishMapper.java）—— 定义"能操作哪些数据库"        │
│                                                              │
│   @Mapper                                                    │
│   public interface DishMapper {                              │
│       void insert(Dish dish);  ← 只有签名                     │
│   }                                                          │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ⑥ Mapper XML（DishMapper.xml）—— 真正的 SQL                   │
│                                                              │
│   <insert id="insert" useGeneratedKeys="true" keyProperty="id">│
│       INSERT INTO dish (name, category_id, price, ...)       │
│       VALUES (#{name}, #{categoryId}, #{price}, ...)         │
│   </insert>                                                  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ ⑦ MySQL 数据库—— 真正执行 SQL                                  │
│   dish 表新增一行                                             │
└─────────────────────────────────────────────────────────────┘
```

## 2. 关键注解逐个讲 — *Key Annotations Explained One by One*

| 注解 / Annotation | 贴在哪 / Where | 作用 / Role |
| --- | --- | --- |
| `@RestController` | Controller 类 <br> *Controller class* | 告诉 Spring "我是一个 HTTP 接口处理类，返回值自动转 JSON" <br> *Tells Spring "I'm an HTTP endpoint class; return values are auto-serialized to JSON."* |
| `@Service` | ServiceImpl 类 <br> *ServiceImpl class* | 告诉 Spring "我是业务逻辑类，请帮我创建实例放进 IoC 容器" <br> *Tells Spring "I'm a business-logic class; please instantiate me and put me in the IoC container."* |
| `@Mapper` | Mapper 接口 <br> *Mapper interface* | 告诉 MyBatis "请帮我生成实现类（动态代理），用 XML 里的 SQL" <br> *Tells MyBatis "Please generate an implementation (dynamic proxy) for me, using the SQL in the XML."* |
| `@Autowired` | 字段 <br> *Field* | 告诉 Spring "从 IoC 容器里找一个实例注入到我这" <br> *Tells Spring "Find a matching instance from the IoC container and inject it here."* |

## 3. 为什么要分这么多层 — *Why So Many Layers?*

你可能会想："直接在 Controller 里写 SQL 不就行了？"——确实可以，但分层有这些好处：

*You might wonder: "Why can't I just write SQL directly in the Controller?" — Technically you could, but layering brings real benefits:*

| 层 / Layer | 关心什么 / Cares about | 不关心什么 / Doesn't care about |
| --- | --- | --- |
| Controller | HTTP 协议（URL、参数、返回 JSON） <br> *HTTP protocol (URLs, parameters, JSON responses)* | 业务逻辑、数据库 <br> *Business logic, database* |
| Service | 业务规则、事务、跨表操作 <br> *Business rules, transactions, multi-table operations* | HTTP、SQL 语法 <br> *HTTP, SQL syntax* |
| Mapper | 数据库操作（CRUD） <br> *Database operations (CRUD)* | 业务逻辑、HTTP <br> *Business logic, HTTP* |

**好处**：

***Key benefits:***

1. **换前端协议方便**：以后要支持 GraphQL，只改 Controller，Service 不动
2. **换数据库方便**：MySQL 换 PostgreSQL，只改 Mapper XML 里的 SQL
3. **业务逻辑复用**：同一个 `saveWithFlavor` 方法，可以被 Web Controller 调，也可以被定时任务调，也可以被 RPC 调
4. **测试方便**：可以单独 mock 掉 Mapper 测试 Service，不用真连数据库

1. ***Easy to swap the front-end protocol:** switching to GraphQL later only requires changing the Controller; Service stays untouched.*
2. ***Easy to swap the database:** moving from MySQL to PostgreSQL only touches the SQL in the Mapper XML.*
3. ***Reusable business logic:** the same `saveWithFlavor` method can be called by a Web Controller, a scheduled task, or an RPC handler.*
4. ***Easier to test:** you can mock the Mapper and unit-test the Service without an actual database connection.*

## 4. 关于"实例化"——你不用手动 new — *About "Instantiation" — You Never Need to `new` Anything Yourself*

你不会看到代码里写 `new DishServicempl()`。**Spring 帮你自动 new 了**：

*You will never see `new DishServicempl()` in the codebase. **Spring instantiates everything for you automatically:***

```text
应用启动时，Spring 做的事：
        ↓
扫描所有 @Service / @Controller / @Mapper / @Component / @Repository 类
        ↓
对每个类：调它的无参构造函数 new 一个实例
        ↓
把实例存进 IoC 容器（本质是个 ConcurrentHashMap）
        ↓
扫到 @Autowired 字段时：从容器里拿对应类型的实例，塞进字段
```

所以你只要写好类 + 贴对注解，**Spring 启动时自动帮你 new + 装配**，运行时随时能用。

*So as long as you write the class and apply the right annotations, **Spring instantiates and wires everything at startup** — at runtime your bean is just there, ready to use.*

## 5. 接口 + 实现类，为什么搞这么麻烦？ — *Interface + Implementation: Why the Extra Complexity?*

你可能问："为什么有 `DishService` 接口又有 `DishServicempl` 实现？直接写一个类不行吗？"

*You may ask: "Why have both a `DishService` interface and a `DishServicempl` implementation? Wouldn't a single class be simpler?"*

**理由**：方便**多实现切换**和 **AOP**。

***Reason:** to enable **swappable implementations** and **AOP**.*

```java
public interface FileStorage { String upload(...); }

public class AliOssStorage implements FileStorage { ... }  // 阿里云实现
public class AwsS3Storage implements FileStorage { ... }   // AWS 实现
public class LocalStorage implements FileStorage { ... }   // 本地实现
```

代码里写 `@Autowired private FileStorage storage;`——**Spring 自己选注入哪个实现**（通过条件配置）。改云厂商不用动一行 Service 代码。

*Just write `@Autowired private FileStorage storage;` — **Spring will pick which implementation to inject** (driven by conditional config). Switching cloud vendors doesn't require changing a single line of Service code.*

**另外**：Spring AOP 给类做代理时，**接口比类更容易代理**（JDK 动态代理只能代理接口）。你看到的 `@Transactional` 之所以能生效，背后就是代理在管事务。

***Also:** when Spring AOP creates a proxy, **interfaces are easier to proxy than classes** (JDK dynamic proxies can only proxy interfaces). The reason `@Transactional` works at all is that there's a proxy underneath managing the transaction.*

## 6. 一句话总结 — *Summary in One Sentence*

> Controller 调 Service 接口 → Spring 自动找到 ServiceImpl 实现类 → ServiceImpl 调 Mapper 接口 → MyBatis 根据 XML 生成实现 → XML 里的 SQL 真正执行到数据库

> *Controller calls the Service interface → Spring auto-resolves it to the ServiceImpl class → ServiceImpl calls the Mapper interface → MyBatis generates the implementation from the XML → the SQL in the XML finally hits the database.*

## 7. 写代码时的顺序（自底向上） — *The Order to Write Code (Bottom-Up)*

通常是反过来的：

*Usually it's the reverse of the call chain:*

1. 设计数据库表 — *Design the database tables*
2. 写 Entity / DTO — *Write the Entity / DTO classes*
3. 写 Mapper 接口 + XML — *Write the Mapper interface + XML*
4. 写 Service 接口 + Impl — *Write the Service interface + Impl*
5. 写 Controller — *Write the Controller*

这样下层稳定了，上层调起来不会改来改去。

*This way, once the lower layers are stable, the upper layers can be built on top without constantly needing to rewrite them.*

---

## 8. 关键源代码索引 — *Index of Key Source Files*

| 文件 / File | 作用 / Purpose |
| --- | --- |
| `sky-server/src/main/java/com/sky/controller/admin/DishController.java` | Controller：接收 HTTP 请求 <br> *Controller: receives the HTTP request* |
| `sky-server/src/main/java/com/sky/service/DishService.java` | Service 接口：定义业务方法签名 <br> *Service interface: declares business-method signatures* |
| `sky-server/src/main/java/com/sky/service/impl/DishServicempl.java` | ServiceImpl：实现业务逻辑 + 事务 <br> *ServiceImpl: implements business logic + transactions* |
| `sky-server/src/main/java/com/sky/mapper/DishMapper.java` | Mapper 接口：定义数据库操作签名 <br> *Mapper interface: declares database-operation signatures* |
| `sky-server/src/main/resources/mapper/DishMapper.xml` | Mapper XML：写真正的 SQL <br> *Mapper XML: where the actual SQL lives* |
