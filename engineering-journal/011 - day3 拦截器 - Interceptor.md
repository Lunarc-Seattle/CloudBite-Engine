# JWT 的 Interceptor 如何运转 — *How the JWT Interceptor Works*

## 1. 核心问题 — *The Core Question*

`JwtTokenAdminInterceptor` 跟 AOP 看起来很像：都是"在某个动作前后插入逻辑"。但实现机制和工作层级**完全不同**。

*`JwtTokenAdminInterceptor` looks a lot like AOP — both "insert logic before or after some action". But the underlying mechanisms and the layer at which they operate are **completely different**.*

重点要回答的疑问：**拦截器放行后，Spring 怎么知道执行什么？**

*The key question to answer: **after the interceptor lets the request pass, how does Spring know what to execute next?***

## 2. 核心答案 — *The Core Answer*

Spring 在调你 `preHandle` 之前，**就已经知道这个请求要去哪个 Controller 方法了**。
拦截器只是被"插队"在中间问一句"能不能放行"，**它不参与决定执行谁**。

*Before Spring ever calls your `preHandle`, **it already knows which Controller method this request is heading to**. The interceptor is merely "cut in line" in the middle to ask "may this request pass?" — **it does not participate in deciding who gets executed**.*

## 3. 请求处理的完整流程 — *The Full Request-Processing Flow*

```text
浏览器请求 GET /admin/employee/page
        ↓
Tomcat（Servlet 容器）收到 HTTP 请求
        ↓
Tomcat 查 web.xml / Spring Boot 自动配置：
   "/admin/employee/page 应该交给哪个 Servlet？"
        ↓
匹配到 DispatcherServlet（注册的 URL = /）
        ↓
┌─────────── DispatcherServlet 内部 ───────────┐
│                                              │
│  ① HandlerMapping                            │
│     ─ 根据 URL 找到目标方法：                  │
│       EmployeeController#page()              │
│     ─ 同时找到所有匹配该路径的拦截器            │
│        ↓                                     │
│  ② 组装一个 HandlerExecutionChain：           │
│     {                                        │
│       handler:      EmployeeController#page  │
│       interceptors: [JwtTokenAdminInterceptor]│
│     }                                        │
│        ↓                                     │
│  ③ 依次调用每个拦截器的 preHandle()           │
│     └─ JwtTokenAdminInterceptor.preHandle()  │
│         ─ 校验 JWT                           │
│         ─ return true   ← 放行（false 终止） │
│        ↓                                     │
│  ④ 通过反射调用 handler，进入 Controller 方法 │
│        ↓                                     │
│  ⑤ 倒序调用 postHandle()                     │
│        ↓                                     │
│  ⑥ 视图渲染 / 返回 JSON                       │
│        ↓                                     │
│  ⑦ 倒序调用 afterCompletion()                │
│     ← 即使出异常也会调，适合清理资源           │
│                                              │
└──────────────────────────────────────────────┘
        ↓
Tomcat 把响应返回给浏览器
```

**关键点**：步骤 ① 已经把"要执行哪个方法"确定了，并打包在 `HandlerExecutionChain` 里。
拦截器只是这条链上的"哨兵"，它返回 `true` 之后，`DispatcherServlet` 接着拿出 `handler` 直接反射调用就行了——根本不需要拦截器告诉它执行什么。

***Key takeaway:** Step ① already decides "which method to execute" and packs it inside the `HandlerExecutionChain`. The interceptor is merely a "sentry" on this chain — once it returns `true`, `DispatcherServlet` simply takes out the `handler` and invokes it via reflection. It does not need the interceptor to tell it what to run.*

### DispatcherServlet 的作用 — *The Role of `DispatcherServlet`*

`DispatcherServlet` 是 **Spring MVC 的"前端控制器"（Front Controller）**。它本质上是一个 **Servlet**，在 Tomcat（Servlet 容器）里注册了 URL 模式 `/`，意思是：

*`DispatcherServlet` is **Spring MVC's "Front Controller"**. It is essentially a **Servlet** registered in Tomcat (the servlet container) under the URL pattern `/`, which means:*

> "所有进来的 HTTP 请求，先全部交给我处理。"

> *"All incoming HTTP requests must come through me first."*

它就像个项目经理，自己不写代码，但**所有事都得通过它调度**——上图框里 ①~⑦ 全部都是它内部干的活：

*It behaves like a project manager — it doesn't write code itself, but **everything must be scheduled through it**. Steps ① through ⑦ in the diagram above are all things it does internally:*

| 它要做的事 / Job | 委托给谁 / Delegated to |
| --- | --- |
| URL → Controller 方法的映射 <br> *Mapping URL → Controller method* | `HandlerMapping` |
| 调用 Controller 前后的钩子 <br> *Hooks before/after the Controller call* | `HandlerInterceptor`（你写的拦截器！） <br> *`HandlerInterceptor` (the interceptors you write!)* |
| 真正调用 Controller 方法 <br> *Actually invoking the Controller method* | 反射 + `HandlerAdapter` <br> *Reflection + `HandlerAdapter`* |
| 把返回值变成 HTTP 响应 <br> *Turning the return value into an HTTP response* | `HttpMessageConverter`（JSON 序列化） / `ViewResolver`（HTML 模板） <br> *`HttpMessageConverter` (JSON) / `ViewResolver` (HTML templates)* |
| 异常处理 <br> *Exception handling* | `HandlerExceptionResolver` |

**类比理解**：

***An analogy:***

- **Tomcat** = 大楼前台保安，看到客人就让进
- **DispatcherServlet** = 接待大堂的总管，决定客人去哪个部门
- **Controller** = 各部门的工作人员，干具体业务
- **Interceptor** = 大堂里检查证件的安保，总管路过时让他先查一下

- ***Tomcat** — the front-desk security guard who lets every guest in.*
- ***DispatcherServlet** — the lobby manager who decides which department each guest is sent to.*
- ***Controller** — the staff in each department, doing the actual work.*
- ***Interceptor** — the lobby security who checks IDs; when the manager walks by, they pause to verify each visitor.*

所以"为什么访问 `/admin/...` 会被你的 `JwtTokenAdminInterceptor` 拦到"？因为：

*So why does a request to `/admin/...` get caught by your `JwtTokenAdminInterceptor`? Because:*

1. 请求到 Tomcat
2. Tomcat 转给 `DispatcherServlet`（它是唯一注册了 `/` 的 Servlet）
3. `DispatcherServlet` 查 `WebMvcConfiguration` 里登记的拦截器列表，发现 `/admin/**` 匹配了 `JwtTokenAdminInterceptor`
4. 在调 Controller 之前调它的 `preHandle`

1. *The request arrives at Tomcat.*
2. *Tomcat forwards it to `DispatcherServlet` (the only Servlet registered at `/`).*
3. *`DispatcherServlet` looks up the interceptor list registered in `WebMvcConfiguration` and finds that `/admin/**` matches `JwtTokenAdminInterceptor`.*
4. *Before invoking the Controller, it calls that interceptor's `preHandle`.*

## 4. `Object handler` 参数就是关键 — *The `Object handler` Parameter Is the Key*

```text
public boolean preHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler) throws Exception {
    if (!(handler instanceof HandlerMethod)) {
        return true;
    }
    ...
}
```

这个 `handler` 不是空的占位符，它里面**就装着要调用的 Controller 方法和它所在的 Bean**：

*This `handler` is not an empty placeholder — it **already carries the Controller method to be invoked and the Bean it lives on**:*

```text
HandlerMethod hm = (HandlerMethod) handler;
hm.getBeanType();   // EmployeeController.class
hm.getMethod();     // page() 这个 Method 对象
```

代码里 `if (!(handler instanceof HandlerMethod))` —— 就是判断当前拦截到的是不是 Controller 方法（动态方法）。如果是静态资源（图片、JS），`handler` 不是 `HandlerMethod`，直接放行。

*The `if (!(handler instanceof HandlerMethod))` check decides whether the intercepted resource is a Controller method (a "dynamic" method). For static resources (images, JS), `handler` is not a `HandlerMethod`, so we let it through immediately.*

## 5. 拦截器的实现：`JwtTokenAdminInterceptor` — *The Interceptor Implementation: `JwtTokenAdminInterceptor`*

实现 `HandlerInterceptor` 接口：

*Implements the `HandlerInterceptor` interface:*

```java
package com.sky.interceptor;

/**
 * jwt 令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 判断当前拦截到的是 Controller 的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        // 2、校验令牌
        try {
            log.info("jwt 校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            log.info("当前员工 id：{}", empId);
            BaseContext.setCurrentId(empId);  // 使用 ThreadLocal
            // 3、通过，放行
            return true;
        } catch (Exception ex) {
            // 4、不通过，响应 401 状态码
            response.setStatus(401);
            return false;
        }
    }
}
```

## 6. 注册拦截器：`WebMvcConfiguration` — *Registering the Interceptor: `WebMvcConfiguration`*

光实现接口还不够，**Spring 不知道你这个拦截器要拦哪些路径**，得在配置类里注册：

*Just implementing the interface isn't enough — **Spring still doesn't know which paths your interceptor should intercept**, so you must register it in a configuration class:*

```java
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    /**
     * 注册自定义拦截器
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")                   // 所有 /admin/** 都拦
                .excludePathPatterns("/admin/employee/login");  // 但登录接口排除
    }
}
```

`DispatcherServlet` 启动时会读这个配置，把拦截器登记到对应的 URL 路径上。之后每次匹配路径的请求进来，第一步组装 `HandlerExecutionChain` 时就会自动把它塞进去。

*At startup, `DispatcherServlet` reads this configuration and registers the interceptor against the corresponding URL paths. From then on, whenever a request matching those paths comes in, the interceptor is automatically slotted into the `HandlerExecutionChain` during step ①.*

## 7. Interceptor vs AOP 对比 — *Interceptor vs AOP: Side-by-Side Comparison*

| 维度 / Dimension | **Interceptor（拦截器）** / **Interceptor** | **AOP** |
| --- | --- | --- |
| 工作层级 / Layer of operation | HTTP 请求层 <br> *HTTP request layer* | 方法调用层 <br> *Method-invocation layer* |
| 能拦截 / What it can intercept | 只能拦 Controller（基于 URL 匹配） <br> *Only Controllers (URL-pattern matching)* | 任意 Spring Bean 的方法 <br> *Any method on any Spring Bean* |
| 触发者 / Triggered by | `DispatcherServlet` 主动调用 <br> *Invoked directly by `DispatcherServlet`* | 动态代理（JDK Proxy 或 CGLIB） <br> *Dynamic proxies (JDK Proxy or CGLIB)* |
| 拿到的上下文 / Context available | `HttpServletRequest / Response` | `ProceedingJoinPoint`（方法签名、参数、可决定是否真的调用 `proceed()`） <br> *`ProceedingJoinPoint` (method signature, arguments, ability to decide whether to actually call `proceed()`)* |
| 配置方式 / How it's configured | `WebMvcConfig.addInterceptors()` | `@Aspect` + `@Pointcut` |
| 典型场景 / Typical use cases | 登录校验、权限、日志 <br> *Login checks, authorization, logging* | 公共字段填充、事务、缓存 <br> *Common-field filling, transactions, caching* |

## 8. 一个常见的认知陷阱 — *A Common Misconception*

很多人觉得拦截器是"代理"了 Controller —— **不是**。
Controller 还是原来那个 Controller，没有被代理。拦截器是 `DispatcherServlet` 这个调度员主动在调用 Controller 前后多加了几次"问候"调用，跟 Controller 本身解耦。

*Many people think interceptors "proxy" the Controller — **they don't**. The Controller remains the original, un-proxied Controller. The interceptor is simply an extra "courtesy call" added by `DispatcherServlet` (the scheduler) before and after the Controller invocation; it is decoupled from the Controller itself.*

而 AOP 不一样：AOP 会真的把目标对象包一层代理，你拿到的 "Service" 其实是个代理对象，调方法时代理对象决定要不要、什么时候调真的方法。

*AOP is different: it really does wrap the target object in a proxy. The "Service" you receive is actually a proxy object, which decides whether and when to invoke the real method.*

---

**简而言之**：拦截器的"放行后执行谁"，是 Spring MVC 在更早一步（`HandlerMapping`）就决定好的。拦截器只负责"准不准过"，不负责"过了之后去哪"。

***In short:** the "who gets executed after the interceptor passes" question is decided one step earlier by Spring MVC (in `HandlerMapping`). The interceptor is only responsible for "may you pass?"; it is not responsible for "where to go after you pass".*

---

## 9. AutoFill 自动填充流程（AOP） — *The AutoFill Auto-Fill Flow (AOP)*

```text
Controller 调用 Service
        ↓
Service 调用 Mapper 的方法（方法上贴了 @AutoFill）
        ↓
AOP 切面拦截：发现方法有 @AutoFill 注解
        ↓
通过反射读出 value()，判断是 INSERT 还是 UPDATE
        ↓
拿到方法参数（如 Employee 对象），用反射调用 setCreateTime / setUpdateTime / ...
        ↓
Mapper 真正执行 SQL，字段已被填好
```

---

# Q&A 补充 — *Supplementary Q&A*

## Q1：一个项目里只有一个 Servlet 吗？`DispatcherServlet` 是唯一的吗？ — *Q1: Is There Only One Servlet per Project? Is `DispatcherServlet` the Only One?*

**A：不是。Tomcat 里可以跑很多个 Servlet，`DispatcherServlet` 只是 Spring MVC 自己注册的那一个。**

***A:** No. Tomcat can run many Servlets at once; `DispatcherServlet` is just the one that Spring MVC itself registers.*

### Servlet 是什么 — *What Is a Servlet?*

**Servlet 就是"能处理 HTTP 请求的一个 Java 类"**。一个 Tomcat 里能跑很多个 Servlet，每个负责自己的 URL 路径。

***A Servlet is simply "a Java class capable of handling HTTP requests".** A single Tomcat instance can run many Servlets, each responsible for its own URL paths.*

不用 Spring 的经典写法：

*The classic non-Spring style:*

```text
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.getWriter().write("Hello!");
    }
}

@WebServlet("/api/user")
public class UserServlet extends HttpServlet { ... }

@WebServlet("/api/order")
public class OrderServlet extends HttpServlet { ... }
```

Tomcat 收到请求时按 URL 模式去找对应的 Servlet：

*When a request arrives, Tomcat finds the matching Servlet via the URL pattern:*

| URL | 交给哪个 Servlet / Routed to which Servlet |
| --- | --- |
| `/hello` | `HelloServlet` |
| `/api/user` | `UserServlet` |
| `/api/order` | `OrderServlet` |

### Spring Boot 项目里其实也有多个 Servlet — *A Spring Boot Project Actually Has Multiple Servlets Too*

只是大部分都是框架默认注册的，你看不到：

*Most of them are registered silently by the framework, so you don't notice them:*

| Servlet | URL 模式 / Pattern | 干什么 / What it does |
| --- | --- | --- |
| **`DispatcherServlet`** | `/` | Spring MVC 的，处理所有 Controller 请求 <br> *Spring MVC's Servlet — handles all Controller requests.* |
| `DefaultServlet` | （兜底） / *(fallback)* | Tomcat 内置，处理静态文件（图片、CSS） <br> *Built into Tomcat — serves static files (images, CSS).* |
| `JspServlet` | `*.jsp` | Tomcat 内置，渲染 JSP 页面（如果用） <br> *Built into Tomcat — renders JSP pages if used.* |

### Spring MVC 为什么只注册一个 `DispatcherServlet` — *Why Does Spring MVC Register Only One `DispatcherServlet`?*

如果按经典写法，**每个 Controller 都得是一个 Servlet**，写起来超啰嗦。Spring MVC 的设计是：

*If we used the classic style, **every Controller would have to be a Servlet**, which would be extremely tedious. Spring MVC's design is:*

> "我就注册**一个** Servlet（`DispatcherServlet`），让它捕获所有请求（URL 模式 = `/`），然后**在它内部**用 `HandlerMapping` 把请求分发到不同的 Controller 方法。"

> *"I'll register **one** Servlet (`DispatcherServlet`) that catches every request (URL pattern = `/`), and then **inside it** use `HandlerMapping` to dispatch the request to the appropriate Controller method."*

这叫 **Front Controller 模式**（前端控制器模式）。好处：

*This is known as the **Front Controller pattern**. Benefits:*

- 不用为每个接口写 Servlet
- 拦截器、参数解析、JSON 序列化这些公共逻辑都集中在一处
- 你只需专心写 Controller 方法

- *No need to write a Servlet per endpoint.*
- *Cross-cutting logic — interceptors, parameter resolution, JSON serialization — is centralized in one place.*
- *You only have to focus on writing Controller methods.*

### "匹配到 `DispatcherServlet`" 是什么意思 — *What Does "Matched to `DispatcherServlet`" Mean?*

Tomcat 内部维护一张表：

*Tomcat maintains an internal table:*

```text
URL 模式      →  Servlet
─────────────────────────────
/             →  DispatcherServlet   ← Spring 注册的
*.jsp         →  JspServlet
（默认）       →  DefaultServlet
```

当请求 `/admin/employee/page` 进来：

*When a request to `/admin/employee/page` arrives:*

1. Tomcat 查表
2. `/admin/employee/page` 匹配 `/`（最宽松的兜底模式）
3. 找到 `DispatcherServlet`，把请求交给它
4. **然后 `DispatcherServlet` 才开始干那些 ①~⑦ 的事**

1. *Tomcat looks up the table.*
2. *`/admin/employee/page` matches `/` (the broadest fallback pattern).*
3. *Tomcat finds `DispatcherServlet` and hands the request over.*
4. ***Only then does `DispatcherServlet` start performing steps ① through ⑦.***

---

## Q2：`DispatcherServlet` 内部真的包括 ①~⑦ 吗？我记得它只是其中一步？ — *Q2: Does `DispatcherServlet` Really Contain All of ①–⑦? I Thought It Was Just One of the Steps?*

**A：两种说法都对，看你从哪个视角切——但事实层面，`DispatcherServlet.doDispatch()` 这个方法确实从头到尾贯穿了 ①~⑦。**

***A:** Both views are valid — it depends on the perspective you take. But factually, the method `DispatcherServlet.doDispatch()` does run end-to-end through ① to ⑦.*

### 事实层面（源码视角） — *The Factual Level (Source-Code View)*

Spring 源码里 `DispatcherServlet.doDispatch()` 就是处理请求的核心方法，简化后是这样：

*In the Spring source, `DispatcherServlet.doDispatch()` is the core method that handles a request. Simplified, it looks like:*

```text
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    // ① 查 HandlerMapping，找到目标 handler
    mappedHandler = getHandler(processedRequest);

    // ② 找 HandlerAdapter
    HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

    // ③ 调用拦截器的 preHandle()
    if (!mappedHandler.applyPreHandle(request, response)) return;

    // ④ 真正调用 Controller 方法
    mv = ha.handle(request, response, mappedHandler.getHandler());

    // ⑤ 调用拦截器的 postHandle()
    mappedHandler.applyPostHandle(request, response, mv);

    // ⑥ 渲染视图 / 序列化 JSON
    processDispatchResult(...);

    // ⑦ 在 finally 里调用 afterCompletion()
    triggerAfterCompletion(...);
}
```

**结论**：`doDispatch()` 从头到尾都在 `DispatcherServlet` 里跑。①~⑦ 的所有"决策点"都是它发起的——所以说"①~⑦ 是它内部的事"在源码层面是对的。

***Conclusion:** `doDispatch()` runs entirely inside `DispatcherServlet` from start to finish. Every "decision point" from ① to ⑦ is initiated by it — so saying "①–⑦ all happen inside it" is correct at the source-code level.*

### 教学资料里另一种画法（组件协作视角） — *Another Diagram Style from Teaching Materials (Component-Collaboration View)*

很多教材（包括黑马苍穹外卖）画的是"组件协作图"，长这样：

*Many textbooks (including the Heima "Sky Takeaway" course) draw a "component collaboration diagram" that looks like:*

```text
①请求 → [DispatcherServlet]
              ↓ ②问
        [HandlerMapping]
              ↓ ③返回 handler
        [DispatcherServlet]
              ↓ ④委托
        [HandlerAdapter]
              ↓ ⑤反射调用
        [Controller]
              ↓ ⑥返回 ModelAndView
        [HandlerAdapter]
              ↓
        [DispatcherServlet]
              ↓ ⑦委托
        [ViewResolver]
              ↓ ⑧返回 View
        [DispatcherServlet] → 响应
```

在这种图里，`DispatcherServlet` 看起来是"中转站"，**只是其中几步**，别的步骤由 `HandlerMapping`、`HandlerAdapter`、`Controller`、`ViewResolver` 完成。

*In this kind of diagram, `DispatcherServlet` looks like a "relay station" — **just a few of the steps** — while other steps are completed by `HandlerMapping`, `HandlerAdapter`, `Controller`, and `ViewResolver`.*

### 两种视角对比 — *Comparing the Two Perspectives*

| 视角 / Perspective | 怎么看 `DispatcherServlet` / How `DispatcherServlet` looks | 一句话 / One-liner |
| --- | --- | --- |
| **调用栈视角**（源码） / **Call-stack view (source code)** | 它是导演，①~⑦ 全在它的方法栈里 <br> *It's the director — ①–⑦ all sit inside its method stack.* | 所有步骤都"在它内部" <br> *Every step happens "inside it".* |
| **组件协作视角**（教学） / **Component-collaboration view (textbook)** | 它是其中一个组件，会调用别的组件 <br> *It's one of several components and calls the others.* | 它"参与"几步，但不"包含"全部 <br> *It "participates" in some steps but does not "contain" all of them.* |

两种说法都没错，差别在抽象层级：

*Both descriptions are correct — the difference is the level of abstraction:*

- **源码角度**：①~⑦ 都在它内部
- **组件角度**：它委托给 `HandlerMapping` / `HandlerAdapter` / `ViewResolver`

- ***From a source-code angle:** ① through ⑦ all live inside it.*
- ***From a component angle:** it delegates to `HandlerMapping` / `HandlerAdapter` / `ViewResolver`.*

记的时候记**组件协作版**就够了——跟课程资料一致，又能看到每个组件的分工。但要知道：**最终所有步骤都是被 `DispatcherServlet.doDispatch()` 这个方法串起来的**。

*For memorization the **component-collaboration version** is enough — it matches the course material and lets you see each component's role. But always remember: **every step is ultimately threaded together by the single method `DispatcherServlet.doDispatch()`**.*
