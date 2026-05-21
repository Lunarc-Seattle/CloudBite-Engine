# AOP 切面执行流程：AutoFill 自动填充 — *AOP Aspect Execution Flow: AutoFill Common-Field Auto-Population*

> 配合 `010 - day3 重复字段 - Common Audit Fields.md` 一起看。010 讲"是什么、为什么"，本篇讲"AOP **具体在哪里截获**、**反射如何赋值**"。
>
> *Read alongside `010 - day3 重复字段 - Common Audit Fields.md`. File 010 covers the "what" and "why"; this file focuses on **where AOP actually intercepts the call** and **how reflection assigns the values**.*

---

## 1. 整体大图：一次 Mapper 调用的真实路径 — *The Big Picture: What Really Happens During a Mapper Call*

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       业务层（你写的代码）                                    │
│                                                                             │
│    EmployeeServiceImpl.save(employee)                                       │
│              ↓                                                              │
│    employeeMapper.insert(employee)         ← 看上去是直接调 Mapper           │
└─────────────────────────────────────────────────────────────────────────────┘
              ↓
              ↓ 但实际上 Spring 启动时已经把 Mapper 包了一层"代理"
              ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Spring AOP 代理对象                                    │
│                                                                             │
│   ① 检查 insert() 方法上有没有 @AutoFill 注解？                              │
│         有 → 触发匹配 @Pointcut 的切面                                      │
│                                                                             │
│   ② 执行 @Before("autoFillPointCut()") 通知                                  │
│         ↓                                                                   │
│      AutoFillAspect.autoFill(joinPoint)        ← 切面方法在这里运行！        │
│         ├── 拿到方法签名 → 读到 @AutoFill(value=INSERT)                      │
│         ├── 拿到方法参数 → Employee 对象                                     │
│         └── 反射调用 employee.setCreateTime() / setUpdateTime()             │
│                       setCreateUser() / setUpdateUser()                     │
│         ↓                                                                   │
│   ③ 此时 employee 对象的 4 个公共字段已经被填好                              │
└─────────────────────────────────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                    真正的 Mapper.insert() 执行                               │
│                                                                             │
│    MyBatis 拿这个已经填好字段的 employee 对象 → 拼 SQL → 执行 INSERT         │
│    数据库里看到的就是带 createTime / createUser 的完整记录                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

**核心 takeaway**：

- Service 调 Mapper 的那一刻，并**不是直接**进入 Mapper 接口的方法
- 而是先进入 **Spring 包的代理对象**
- 代理对象**根据切点表达式判断要不要执行通知**，要的话先跑切面方法
- 跑完后**才真正执行 Mapper 方法**

***Key takeaways:***

- *When the Service calls the Mapper, the call **does not** go directly into the Mapper interface.*
- *Instead, it first enters the **Spring-generated proxy object** that wraps the Mapper.*
- *The proxy **decides whether to invoke an advice** based on the pointcut expression — if so, it runs the aspect method first.*
- *Only after that does the **real Mapper method** actually execute.*

---

## 2. AOP 在哪里截获的？ — *Where Exactly Does AOP Intercept the Call?*

### 关键代码（`AutoFillAspect.java` 第 29、34 行） — *Key code (`AutoFillAspect.java`, lines 29 and 34)*

```java
@Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
public void autoFillPointCut() {}

@Before("autoFillPointCut()")
public void autoFill(JoinPoint joinPoint) { ... }
```

### 切点表达式逐部分拆解 — *Breaking Down the Pointcut Expression Piece by Piece*

```text
execution( *  com.sky.mapper.*  .  *  (..) )
              ↑                      ↑   ↑
          ① 返回类型任意         ② 方法名任意   ③ 参数列表任意
              
              com.sky.mapper.*    ← ④ com.sky.mapper 包下所有类

       &&
       
@annotation( com.sky.annotation.AutoFill )
                                    ↑
                          ⑤ 方法上必须贴了这个注解
```

翻译过来：

> "拦截 `com.sky.mapper` 包下所有类的、**贴了 `@AutoFill` 注解**的方法，无论返回什么、参数是什么。"

*In plain English:*

> *"Intercept every method in every class under `com.sky.mapper` **that carries the `@AutoFill` annotation**, regardless of return type or parameter list."*

### Spring 怎么实现这个"截获" — *How Spring Actually Performs the Interception*

```text
应用启动时：
    Spring 扫描所有 @Aspect 类
        ↓
    Spring 扫描所有 Mapper 接口
        ↓
    对每个 Mapper 方法，检查是否匹配某个 @Pointcut
        ↓
    匹配的方法 → 给 Mapper 生成一个"动态代理对象"
        ↓
    把代理对象（而不是原 Mapper）注入到 Service 里
        ↓
运行时：
    Service 调 mapper.insert(employee)
        ↓
    实际上是调 代理.insert(employee)
        ↓
    代理：先看 @Before 通知，再调真正的 Mapper 方法
```

所以**"AOP 在哪里截获"的答案**是：**在 Spring 为 Mapper 生成的代理对象里**。这层代理是 Spring 启动时悄悄包上的，业务代码完全感知不到。

*So the answer to "where does AOP intercept" is: **inside the dynamic proxy object that Spring generates for the Mapper**. Spring wraps this proxy around the Mapper silently at startup — the business code never sees it.*

---

## 3. 反射是怎么做的？ — *How Does the Reflection Part Work?*

切面方法 `autoFill(JoinPoint joinPoint)` 拿到 `joinPoint` 之后，靠反射完成 4 步操作。

*Once the aspect method `autoFill(JoinPoint joinPoint)` receives the `joinPoint`, it performs four reflection-based steps.*

### 完整源码（`AutoFillAspect.java`） — *Full Source Code (`AutoFillAspect.java`)*

```java
@Before("autoFillPointCut()")
public void autoFill(JoinPoint joinPoint) {
    log.info("开始进行公共字段自动填充...");

    // ─── 步骤 1：从 JoinPoint 拿到方法签名 ───
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    
    // ─── 步骤 2：从方法签名拿到 @AutoFill 注解，读出 value() ───
    AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
    OperationType operationType = autoFill.value();  // INSERT 或 UPDATE

    // ─── 步骤 3：从 JoinPoint 拿到方法实参（实体对象） ───
    Object[] args = joinPoint.getArgs();
    if (args == null || args.length == 0) {
        return;
    }
    Object entity = args[0];  // 用 Object 接，因为不知道是 Employee / Category / Dish...

    // 准备赋值
    LocalDateTime now = LocalDateTime.now();
    Long currentId = BaseContext.getCurrentId();

    // ─── 步骤 4：根据操作类型，反射调用 setter ───
    if (operationType == OperationType.INSERT) {
        try {
            Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
            Method setCreateUser = entity.getClass().getDeclaredMethod("setCreateUser", Long.class);
            Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

            setCreateTime.invoke(entity, now);
            setCreateUser.invoke(entity, currentId);
            setUpdateTime.invoke(entity, now);
            setUpdateUser.invoke(entity, currentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } else if (operationType == OperationType.UPDATE) {
        try {
            Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

            setUpdateTime.invoke(entity, now);
            setUpdateUser.invoke(entity, currentId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 反射 4 步流程图 — *The Four Reflection Steps, Visualized*

```text
         joinPoint （Spring 传进来的拦截点对象）
                ↓
┌─────────────────────────────────────────────────────────┐
│ 步骤 1: joinPoint.getSignature()                        │
│         ↓                                               │
│   MethodSignature（被拦截方法的"身份证"）                  │
│         ↓ .getMethod()                                  │
│   java.lang.reflect.Method（反射的 Method 对象）         │
└─────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────┐
│ 步骤 2: method.getAnnotation(AutoFill.class)            │
│         ↓                                               │
│   @AutoFill 注解实例                                     │
│         ↓ .value()                                      │
│   OperationType.INSERT 或 OperationType.UPDATE          │
└─────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────┐
│ 步骤 3: joinPoint.getArgs()                             │
│         ↓                                               │
│   Object[] args  ← 调用 Mapper 时传入的所有参数           │
│         ↓ args[0]                                       │
│   Object entity  ← 实体对象（实际可能是 Employee 等）     │
└─────────────────────────────────────────────────────────┘
                ↓
┌─────────────────────────────────────────────────────────┐
│ 步骤 4: 反射调用 setter                                  │
│                                                         │
│   entity.getClass()                                     │
│         ↓                                               │
│   .getDeclaredMethod("setCreateTime", LocalDateTime.class)│
│         ↓                                               │
│   Method setCreateTime  ← 拿到 setter 的 Method 对象      │
│         ↓                                               │
│   .invoke(entity, now)  ← 在 entity 上调用，传参 now      │
│         ↓                                               │
│   等价于：entity.setCreateTime(now);                     │
└─────────────────────────────────────────────────────────┘
```

### 为什么必须用反射，不能直接 `entity.setCreateTime(...)`？ — *Why Must Reflection Be Used? Why Not Call `entity.setCreateTime(...)` Directly?*

因为切面要拦截**所有 Mapper**（`EmployeeMapper`、`CategoryMapper`、`DishMapper`...），编译时**不知道** entity 是哪个具体类型，只能用 `Object` 接住。`Object` 没有 `setCreateTime` 方法，所以必须用反射。

*Because the aspect needs to intercept **all Mappers** (`EmployeeMapper`, `CategoryMapper`, `DishMapper`, ...). At compile time the aspect **has no idea** what concrete type `entity` is — it can only catch it as an `Object`. `Object` has no `setCreateTime` method, so reflection is the only way to invoke it dynamically.*

```java
Object entity = args[0];

// ❌ 不能这样写（Object 没有这个方法）
entity.setCreateTime(now);

// ✅ 反射调用（运行时按 entity 的实际类查找方法）
entity.getClass()
      .getDeclaredMethod("setCreateTime", LocalDateTime.class)
      .invoke(entity, now);
```

---

## 4. Q&A 速查 — *Quick-Reference Q&A*

### Q1: AOP 是在编译时还是运行时拦截？ — *Q1: Does AOP Intercept at Compile Time or at Runtime?*

**A**: Spring AOP 是**运行时**拦截，通过动态代理（JDK Proxy / CGLIB）。区别于 AspectJ 的"编译时织入"。

***A:** Spring AOP intercepts at **runtime**, via dynamic proxies (JDK Proxy or CGLIB). This is different from AspectJ, which performs "compile-time weaving".*

### Q2: `joinPoint.getArgs()` 一定返回实体对象吗？ — *Q2: Does `joinPoint.getArgs()` Always Return an Entity Object?*

**A**: 不一定。它返回的是**被拦截方法的所有参数**。Mapper 方法第一个参数**通常**是实体对象，但如果方法签名是 `void deleteById(Long id)`，那 `args[0]` 就是 `Long`。所以代码里要做空数组判断 + 类型检查（实际项目通常假设第一个参数是实体）。

***A:** No. `getArgs()` returns **all the parameters** of the intercepted method. The first parameter of a Mapper method is **usually** an entity, but if the signature is `void deleteById(Long id)`, then `args[0]` will be a `Long`. That's why the code includes a null/empty-array check — and in practice projects often assume "the first arg is the entity".*

### Q3: 为什么用 `getDeclaredMethod` 而不是 `getMethod`？ — *Q3: Why Use `getDeclaredMethod` Instead of `getMethod`?*

**A**:

- `getMethod` —— 只能找到 `public` 方法，**包括从父类继承的**
- `getDeclaredMethod` —— 能找到**本类声明的所有方法**（包括 private），**但不包括继承的**

Lombok 生成的 setter 是 public 的，但写在实体类自己里（不是继承的），两个都能找到。习惯上用 `getDeclaredMethod` 表示"我就在这个类里找"。

***A:***

- *`getMethod` — finds only `public` methods, **including inherited ones**.*
- *`getDeclaredMethod` — finds **every method declared on this class** (including `private` ones), but **excludes inherited methods**.*

*Lombok-generated setters are public and live directly on the entity class (not inherited), so either method works. `getDeclaredMethod` is preferred here as a convention — it signals "I'm looking specifically on this class".*

### Q4: 类型必须传 `Long.class` 不能传 `long.class`？ — *Q4: Must I Pass `Long.class` and Not `long.class`?*

**A**: 对，必须类型完全匹配。

- `Long.class` —— 包装类（对象引用，能为 `null`）
- `long.class` —— 基本类型（不能为 `null`）

实体字段几乎都用包装类 `Long`，反射要传 `Long.class`。混用会抛 `NoSuchMethodException`。

***A:** Yes — the types must match exactly.*

- *`Long.class` — the wrapper class (an object reference, nullable).*
- *`long.class` — the primitive type (non-nullable).*

*Entity fields almost always use the wrapper type `Long`, so reflection must pass `Long.class`. Mixing them up throws `NoSuchMethodException`.*

### Q5: `@Pointcut` 那个空方法 `autoFillPointCut() {}` 为什么是空的？ — *Q5: Why is the `@Pointcut` Method `autoFillPointCut() {}` Empty?*

**A**: 它**只是个挂载点 / 名字标签**，不会被实际调用。`@Before("autoFillPointCut()")` 通过引用这个方法名来复用切点表达式。好处是：如果有多个 `@Before` / `@After` 都用同一个切点，不用重复写一长串表达式。

***A:** It is only a **named anchor / label** — it is never actually invoked. `@Before("autoFillPointCut()")` references this method name to reuse the pointcut expression. The benefit: if multiple `@Before` / `@After` advices share the same pointcut, you don't have to repeat the long expression in each.*

---

## 5. 关键源代码索引 — *Index of Key Source Files*

| 文件 / File | 作用 / Purpose |
| --- | --- |
| `sky-server/src/main/java/com/sky/annotation/AutoFill.java` | 自定义注解，标记需要自动填充的 Mapper 方法 <br> *Custom annotation that marks Mapper methods needing auto-fill.* |
| `sky-server/src/main/java/com/sky/enumeration/OperationType.java` | 枚举 INSERT / UPDATE <br> *Enum with `INSERT` and `UPDATE` values.* |
| `sky-server/src/main/java/com/sky/aspect/AutoFillAspect.java` | 切面类，包含切点 + @Before 通知 + 反射逻辑 <br> *The aspect class — pointcut + `@Before` advice + reflection logic.* |
| `sky-server/src/main/java/com/sky/mapper/EmployeeMapper.java` | 在 `insert` / `update` 方法上贴 `@AutoFill` 触发切面 <br> *Applies `@AutoFill` to `insert` / `update` methods to trigger the aspect.* |
| `sky-server/src/main/java/com/sky/context/BaseContext.java` | ThreadLocal 存当前登录用户 ID，供切面读取 <br> *`ThreadLocal` storing the current user's ID, read by the aspect.* |
