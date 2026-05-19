**公共字段自动填充**，简单来说，就是把数据库表里**每个表都有、且每次新增或修改时都需要重复赋值的字段**，交给程序自动去填，不再需要人工手动一条条写 `set` 代码。

***Common-field auto-fill**, in short, means letting the program automatically populate **the audit fields that exist on every database table and that must be repeatedly assigned on each insert or update**, so that developers no longer have to manually write the `set` calls one by one.*

![img_30.png](img_30.png)
![img_32.png](img_32.png)
---

### 1. 什么是“公共字段”？ — *What Are "Common Fields"?*

在企业级开发中，为了方便后期审计和追溯数据，基本上**每一张数据库表**（比如员工表、分类表、菜品表、套餐表）都会包含以下 4 个字段：

*In enterprise-grade development, to support later auditing and data traceability, **almost every database table** (e.g. employee, category, dish, setmeal) carries the following four fields:*

| 字段名 / Field | 含义 / Meaning | 操作类型 / Operation | 填充的值 / Value to fill |
| --- | --- | --- | --- |
| `create_time` | 创建时间 <br> *Creation time* | `INSERT` (新增时) <br> *`INSERT` (on creation)* | 当前系统时间 <br> *Current system time* |
| `create_user` | 创建人 ID <br> *ID of the creator* | `INSERT` (新增时) <br> *`INSERT` (on creation)* | 当前登录用户的 ID <br> *ID of the currently logged-in user* |
| `update_time` | 修改时间 <br> *Last-modified time* | `INSERT`、`UPDATE` (新增和修改时) <br> *`INSERT` and `UPDATE` (both)* | 当前系统时间 <br> *Current system time* |
| `update_user` | 修改人 ID <br> *ID of the modifier* | `INSERT`、`UPDATE` (新增和修改时) <br> *`INSERT` and `UPDATE` (both)* | 当前登录用户的 ID <br> *ID of the currently logged-in user* |

这就是所谓的**公共字段**。

*These are what we call **common fields**.*

---

### 2. 为什么要搞“自动填充”？（痛点） — *Why Do We Need "Auto-Fill"? (The Pain Point)*

在没有做自动填充之前（比如 Day02 阶段），你每写一个新增或修改的功能，在 Service 层的实现类（`ServiceImpl`）里都得手动敲上这几行代码：

*Before introducing auto-fill (e.g. during the Day02 stage), every time you wrote an insert or update feature, you had to manually type these lines in the Service implementation class (`ServiceImpl`):*

```java
// 新增员工时要手动写：
employee.setCreateTime(LocalDateTime.now());
employee.setUpdateTime(LocalDateTime.now());
employee.setCreateUser(BaseContext.getCurrentId());
employee.setUpdateUser(BaseContext.getCurrentId());

// 新增分类时又要手动写一遍：
category.setCreateTime(LocalDateTime.now());
category.setUpdateTime(LocalDateTime.now());
// ... 后面还有一堆重复的 set

```

**这样做有什么坏处？**

***What's wrong with this approach?***

1. **代码太冗余**：项目里有几十个增删改查，你就要复制粘贴几百行一模一样的 `set` 代码。
2. **极其容易漏写**：万一哪个新手程序员在写“新增菜品”时忘了 `setCreateTime`，数据库直接就会报错或者存入 `null` 值，产生 Bug。

1. ***Heavy code duplication:** the project has dozens of CRUD endpoints, which means hundreds of lines of identical `set` boilerplate copy-pasted everywhere.*
2. ***Extremely easy to forget:** if a junior developer forgets `setCreateTime` while writing "Add Dish", the database will throw an error or store a `null`, producing a bug.*

---

### 3. “自动填充”是怎么实现的？ — *How Is "Auto-Fill" Implemented?*

为了解决这个痛点，黑马的老师带大家使用了 **AOP（面向切面编程） + 反射机制**。

*To solve this pain point, the Heima instructor walks us through using **AOP (Aspect-Oriented Programming) + reflection**.*

它的核心思想是：**“横向切一刀，统一拦截，集中处理”**。

*The core idea is: **"slice horizontally, intercept uniformly, handle centrally."***

1. **统一拦截**：通过 AOP，让程序死死盯着所有的 `Mapper` 层方法。只要发现某个方法身上贴了特定的标签（比如自定义注解 `@AutoFill`），AOP 就会把这个方法**拦截**下来。
2. **自动塞值（反射）**：拦截下来后，AOP 会自动获取你传进去的实体类对象（比如 `Employee` 或 `Category`），然后利用 **Java 反射机制** 自动去调用它们的 `setCreateTime()`、`setUpdateUser()` 等方法，把时间和当前登录工号强行塞进去。
3. **放行**：塞完值后，再让代码继续往下走，去执行真正的 SQL 语句。

***Step by step:***

1. ***Uniform interception:** Using AOP, the program watches every `Mapper`-layer method. As soon as it sees a method carrying a particular tag (e.g. our custom `@AutoFill` annotation), AOP **intercepts** the call.*
2. ***Auto-injection via reflection:** After interception, AOP grabs the entity argument you passed in (e.g. `Employee` or `Category`), then uses **Java reflection** to call methods like `setCreateTime()` and `setUpdateUser()`, force-feeding in the current time and the current user's ID.*
3. ***Release:** Once the values are stuffed in, the call continues so that the real SQL gets executed.*

### 最终效果 — *The End Result*

做完这个功能后，以后你再写任何的 Service 业务代码，**那 4 行枯燥的 `set` 代码一行都不用写了**。

*After implementing this feature, whenever you write any Service business logic in the future, **you no longer have to write a single one of those four boring `set` lines**.*

你只需要在对应的 Mapper 接口方法上轻松贴一个注解：

*You only need to put a single annotation on the corresponding Mapper interface method:*

```java
@AutoFill(value = OperationType.INSERT) // 告诉AOP：这是新增操作，帮我自动填4个字段
void insert(Category category);

```

业务代码瞬间变得无比干净、优雅，这就是公共字段自动填充的魅力。

*Business code instantly becomes incredibly clean and elegant — that's the magic of common-field auto-fill.*

简单来说，**AOP 是一种“编程思想”**，而**切面（Aspect）是这种思想在代码里的“具体实现”**。

*In short, **AOP is a "programming paradigm"**, while **an Aspect is the "concrete implementation" of that paradigm in code**.*

我们可以用一句话理清它们的关系：**AOP 是我们要去解决问题的核心思想，而切面就是我们为了实现这个思想而写出来的一个具体的 Java 类。**

*One sentence sums up the relationship: **AOP is the core idea for solving the problem; an Aspect is the concrete Java class we write to realize that idea.***

为了让你彻底搞懂，我们把这两个概念放在一起拆解：

*To make this fully clear, let's unpack the two concepts side by side:*

---

## 1. 什么是 AOP？（大局观） — *What Is AOP? (The Big Picture)*

**AOP（Aspect-Oriented Programming，面向切面编程）** 是一种设计模式和编程思想。

***AOP (Aspect-Oriented Programming)** is both a design pattern and a programming paradigm.*

* **它的目标：** 把那些与核心业务无关、却在多个地方重复出现的“大锅饭代码”（如：公共字段填充、日志记录、权限校验、事务管理），从普通的业务方法中**抽离出来，集中管理**。
* **它的手法（横向切一刀）：** 传统的 Java 开发（OOP）是纵向的（Service 调用 Mapper，Mapper 操作数据库）。AOP 则是在这个调用链条中**横向拦截**，在方法执行前或执行后自动插播一段逻辑，从而实现不修改原有代码，就能动态添加新功能的效果。

***Key points:***

* ***Its goal:** to extract the "cross-cutting" code that has nothing to do with core business but appears repeatedly in many places (e.g. common-field filling, logging, permission checks, transaction management) **out of regular business methods and manage it centrally**.*
* ***Its technique ("slice horizontally"):** Traditional Java development (OOP) is vertical (Service calls Mapper, Mapper hits the DB). AOP **horizontally intercepts** this call chain, inserting a snippet of logic before or after a method runs — so we can dynamically add new capabilities without modifying existing code.*

---

## 2. 什么是切面（Aspect）？（具体代码） — *What Is an Aspect? (The Concrete Code)*

**切面（Aspect）** 是 AOP 思想中的核心单位。在 Spring Boot 代码里，它其实就是**一个普通的 Java 类，只不过这个类上面加上了 `@Aspect` 注解**。

***An Aspect** is the core unit in AOP. In Spring Boot code, it is simply **a regular Java class that has been decorated with the `@Aspect` annotation**.*

一个完整的**切面**，内部必须包含两样东西：

*A complete **Aspect** must contain two things internally:*

1. **切入点（Pointcut）：** 决定“在哪里切入”。也就是精准定义哪些类、哪些方法需要被我们拦截。
2. **通知（Advice）：** 决定“切入后干什么”**。也就是拦截到方法后，具体执行什么代码（比如是贴上时间，还是记录日志），以及**什么时候干（在方法执行前 `@Before`，还是执行后 `@After`）。

***Two essential parts:***

1. ***Pointcut — decides "where to slice in":** it precisely defines which classes and methods we want to intercept.*
2. ***Advice — decides "what to do after slicing in":** it specifies the code to execute once we intercept the method (e.g. stamp the time, write a log), and also **when** to do it (before the method via `@Before`, after the method via `@After`, etc.).*

---

## 3. 结合《苍穹外卖》的实际例子 — *A Real Example from the "Sky Takeaway" Project*

在项目里，为了实现“公共字段自动填充”，你马上就会写一个切面。它们之间的对应关系长这样：

*In the project, to implement "common-field auto-fill", you'll soon be writing an Aspect. The mapping between the concepts looks like this:*

* **你想用 AOP 思想解决问题：** 你不想在每个 Service 里手动写 `setCreateTime`、`setUpdateTime`，你想把这个重复的动作抽离出来。
* **你写出来的“切面类”：** 就是接下来的 `AutoFillAspect.java`。

***Mapping:***

* ***The AOP-style problem you want to solve:** you don't want to manually write `setCreateTime` and `setUpdateTime` in every Service — you want to extract this repeated action.*
* ***The Aspect class you'll write:** the upcoming `AutoFillAspect.java`.*

这个切面类拆开看就是：

*Broken down, this Aspect class looks like:*

```java
@Aspect // 1. 告诉Spring：我是一个【切面】
@Component
public class AutoFillAspect {

    // 2. 【切入点】：精准打击！只要Mapper层的方法上贴了 @AutoFill 注解，就拦截它
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    // 3. 【通知】：干活！在被拦截的方法【执行前】(@Before)，利用反射自动把时间和用户ID填进去
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        // 具体的自动填充逻辑...
    }
}

```

### 总结 — *Summary*

* **AOP** 是总称，是一种**面向切面**的编程**方法论**。
* **切面（Aspect）** 是具体的**兵器**，由“**切入点**（在哪切）” + “**通知**（干什么）”组合而成。你通过编写切面，最终实现了 AOP 的横向拦截拦截功能。

***Key points:***

* ***AOP** is the umbrella term — an **aspect-oriented** programming **methodology**.*
* ***An Aspect** is the concrete **weapon**, made up of "**Pointcut** (where to cut)" + "**Advice** (what to do)". By writing aspects, you ultimately realize AOP's horizontal-interception capability.*

---

# Q&A: 自定义注解的语法 — *Custom Annotation Syntax*

## Q: `OperationType value();` 这一行如何理解？这是 Java 定义属性的方式吗？ — *Q: How Should I Understand the Line `OperationType value();`? Is This How Java Defines a Class Property?*

> **Q (EN):** How should I understand the line `OperationType value();`? Is this how Java defines a class property?

### A: 不是普通类的属性写法，这是 Java 注解（`@interface`）**特有的语法** —— 长得像方法，但实际上是注解的属性。 — *A: This Is Not Regular Class Property Syntax — It Is **Syntax Unique to Java Annotations (`@interface`)**: It Looks Like a Method but Actually Declares an Annotation Attribute.*

> **A (EN):** No — this is **not** how you declare a property in a regular Java class. It is a syntax **unique to annotation types (`@interface`)**: it *looks* like an abstract method, but it actually declares an **attribute** (element) of the annotation.

### `AutoFill.java` 源码 — *`AutoFill.java` Source Code*

```java
package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理
 * Custom annotation: marks a method that needs auto-fill processing
 * for common audit fields (createTime / updateTime / createUser / updateUser).
 */
@Target(ElementType.METHOD)          // 只能贴在方法上 / Can only be placed on methods
@Retention(RetentionPolicy.RUNTIME)  // 运行时反射可读 / Available at runtime via reflection
public @interface AutoFill {
    // 因为只有 INSERT 和 UPDATE 需要填充公共字段
    // Because only INSERT and UPDATE need common-field auto-fill
    OperationType value();
}
```

### 同一行代码的"三种身份" — *Three Faces of the Same Line*

`OperationType value();` 这一行，**根据使用场景，扮演三种不同角色**：

> This single line plays **three different roles** depending on the context:

| 场景 / Context | 写法 / Form | 它的角色 / Role |
| --- | --- | --- |
| **定义注解时 / Declaring the annotation** | `OperationType value();` | 像个抽象方法 / Looks like an abstract method (no body, ends with `;`) |
| **使用注解时 / Using the annotation** | `@AutoFill(value = OperationType.INSERT)` | 像属性赋值 / Looks like attribute assignment (`name = value`) |
| **反射读取时 / Reading via reflection** | `annotation.value()` | 像调用方法 / Looks like a method call returning the stored value |

**同一个 `value`，三种姿态** —— 这就是注解语法的特殊性。
**One symbol, three forms** — that's the nature of annotation syntax.

### 跟普通类的属性对比 — *Comparison with Regular Class Fields*

**普通 Java 类** / **A regular Java class:**

```java
public class Employee {
    private String name;          // 字段 / field
    private OperationType type;   // 字段 / field
}
```

**注解类** / **An annotation type:**

```java
public @interface AutoFill {
    OperationType value();             // 注解元素 / annotation element
    String description() default "";   // 带默认值的元素 / element with default value
}
```

| 维度 / Aspect | 普通类属性 / Class field | 注解元素 / Annotation element |
| --- | --- | --- |
| 关键字 / Keyword | `class` | `@interface` |
| 声明语法 / Declaration | `private 类型 名;` | `类型 名();` |
| 是否带 `()` / Parens? | 否 / No | 是 / Yes (although it's not a method) |
| 默认值 / Default value | `private int x = 5;` | `int x() default 5;` |
| 赋值方式 / Assignment | `obj.setX(5)` | `@注解(x = 5)` / `@Anno(x = 5)` |
| 读取方式 / Reading | `obj.getX()` | `annotation.x()` (via reflection) |

### 为什么注解要用这种"假装是方法"的语法？ — *Why Does Java Annotation Use This "Pretending-to-Be-a-Method" Syntax?*

> ### Why does Java annotation use this "pretending-to-be-a-method" syntax?

`@interface` 底层实际上是一个**继承自 `java.lang.annotation.Annotation` 的接口**。Java 接口里成员只能是"抽象方法"，所以注解的属性**被设计成抽象方法的形式**——但编译器和 JVM 会特殊对待它，把它当属性用。

> Under the hood, `@interface` compiles into an **interface that extends `java.lang.annotation.Annotation`**. Since Java interfaces can only contain *abstract methods* as members, annotation attributes are syntactically expressed as abstract methods — but the compiler and JVM treat them specially as attributes.

可以这样理解：**"注解 = 一个特殊的接口，它的'抽象方法'其实是属性"。**

> You can think of it as: **"An annotation is a special interface whose 'abstract methods' are actually attributes."**

### 在 `AutoFill` 里具体怎么用 — *How It's Actually Used in `AutoFill`*

**1. 定义 / Define** (`AutoFill.java`):

```java
public @interface AutoFill {
    OperationType value();   // 一个名为 value 的属性 / an attribute named "value"
}
```

**2. 使用 / Use** (on a Mapper method):

```java
@AutoFill(value = OperationType.INSERT)   // 显式写出属性名 / explicit attribute name
void insert(Employee employee);

// 因为属性名是 value（特殊名字），可以省略：
// Because the attribute is named "value" (a special name), it can be omitted:
@AutoFill(OperationType.INSERT)           // 等价写法 / equivalent form
void insert(Employee employee);
```

**3. 反射读取 / Read via reflection** (in the AOP aspect):

```java
// 拿到方法上的 @AutoFill 注解 / Get the @AutoFill annotation from the method
AutoFill autoFill = method.getAnnotation(AutoFill.class);

// 调用 value() 拿到属性值（此时它真的"像方法"了）
// Call value() to get the attribute (now it really behaves like a method call)
OperationType type = autoFill.value();   // 返回 INSERT 或 UPDATE / returns INSERT or UPDATE

if (type == OperationType.INSERT) {
    // 填充 createTime / createUser / updateTime / updateUser
} else if (type == OperationType.UPDATE) {
    // 只填充 updateTime / updateUser
}
```

### 一句话记住 — *Key Takeaway*

> 在 `@interface` 里，`类型 名();` **不是定义方法**，而是**定义这个注解能带哪些"参数"**。它形式上像方法、用起来像属性、反射读取时又像方法 —— 所以语法长得"怪"是正常的。

> Inside an `@interface`, the syntax `Type name();` **does not define a method** — it defines what **parameters** the annotation can carry. It *looks* like a method, *behaves* like an attribute, and *reads* like a method via reflection. The unusual syntax is intentional, not a typo.
