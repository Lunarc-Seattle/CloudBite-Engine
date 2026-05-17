# JWT 的 Interceptor 如何运转

## 1. 核心问题

`JwtTokenAdminInterceptor` 跟 AOP 看起来很像：都是"在某个动作前后插入逻辑"。但实现机制和工作层级**完全不同**。

重点要回答的疑问：**拦截器放行后，Spring 怎么知道执行什么？**

## 2. 核心答案

Spring 在调你 `preHandle` 之前，**就已经知道这个请求要去哪个 Controller 方法了**。
拦截器只是被"插队"在中间问一句"能不能放行"，**它不参与决定执行谁**。

## 3. 请求处理的完整流程

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

### DispatcherServlet 的作用

`DispatcherServlet` 是 **Spring MVC 的"前端控制器"（Front Controller）**。它本质上是一个 **Servlet**，在 Tomcat（Servlet 容器）里注册了 URL 模式 `/`，意思是：

> "所有进来的 HTTP 请求，先全部交给我处理。"

它就像个项目经理，自己不写代码，但**所有事都得通过它调度**——上图框里 ①~⑦ 全部都是它内部干的活：

| 它要做的事 | 委托给谁 |
| --- | --- |
| URL → Controller 方法的映射 | `HandlerMapping` |
| 调用 Controller 前后的钩子 | `HandlerInterceptor`（你写的拦截器！） |
| 真正调用 Controller 方法 | 反射 + `HandlerAdapter` |
| 把返回值变成 HTTP 响应 | `HttpMessageConverter`（JSON 序列化） / `ViewResolver`（HTML 模板） |
| 异常处理 | `HandlerExceptionResolver` |

**类比理解**：

- **Tomcat** = 大楼前台保安，看到客人就让进
- **DispatcherServlet** = 接待大堂的总管，决定客人去哪个部门
- **Controller** = 各部门的工作人员，干具体业务
- **Interceptor** = 大堂里检查证件的安保，总管路过时让他先查一下

所以"为什么访问 `/admin/...` 会被你的 `JwtTokenAdminInterceptor` 拦到"？因为：

1. 请求到 Tomcat
2. Tomcat 转给 `DispatcherServlet`（它是唯一注册了 `/` 的 Servlet）
3. `DispatcherServlet` 查 `WebMvcConfiguration` 里登记的拦截器列表，发现 `/admin/**` 匹配了 `JwtTokenAdminInterceptor`
4. 在调 Controller 之前调它的 `preHandle`

## 4. `Object handler` 参数就是关键

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

```text
HandlerMethod hm = (HandlerMethod) handler;
hm.getBeanType();   // EmployeeController.class
hm.getMethod();     // page() 这个 Method 对象
```

代码里 `if (!(handler instanceof HandlerMethod))` —— 就是判断当前拦截到的是不是 Controller 方法（动态方法）。如果是静态资源（图片、JS），`handler` 不是 `HandlerMethod`，直接放行。

## 5. 拦截器的实现：`JwtTokenAdminInterceptor`

实现 `HandlerInterceptor` 接口：

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

## 6. 注册拦截器：`WebMvcConfiguration`

光实现接口还不够，**Spring 不知道你这个拦截器要拦哪些路径**，得在配置类里注册：

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

## 7. Interceptor vs AOP 对比

| 维度 | **Interceptor（拦截器）** | **AOP** |
| --- | --- | --- |
| 工作层级 | HTTP 请求层 | 方法调用层 |
| 能拦截 | 只能拦 Controller（基于 URL 匹配） | 任意 Spring Bean 的方法 |
| 触发者 | `DispatcherServlet` 主动调用 | 动态代理（JDK Proxy 或 CGLIB） |
| 拿到的上下文 | `HttpServletRequest / Response` | `ProceedingJoinPoint`（方法签名、参数、可决定是否真的调用 `proceed()`） |
| 配置方式 | `WebMvcConfig.addInterceptors()` | `@Aspect` + `@Pointcut` |
| 典型场景 | 登录校验、权限、日志 | 公共字段填充、事务、缓存 |

## 8. 一个常见的认知陷阱

很多人觉得拦截器是"代理"了 Controller —— **不是**。
Controller 还是原来那个 Controller，没有被代理。拦截器是 `DispatcherServlet` 这个调度员主动在调用 Controller 前后多加了几次"问候"调用，跟 Controller 本身解耦。

而 AOP 不一样：AOP 会真的把目标对象包一层代理，你拿到的 "Service" 其实是个代理对象，调方法时代理对象决定要不要、什么时候调真的方法。

---

**简而言之**：拦截器的"放行后执行谁"，是 Spring MVC 在更早一步（`HandlerMapping`）就决定好的。拦截器只负责"准不准过"，不负责"过了之后去哪"。

---

## 9. AutoFill 自动填充流程（AOP）

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

# Q&A 补充

## Q1：一个项目里只有一个 Servlet 吗？`DispatcherServlet` 是唯一的吗？

**A：不是。Tomcat 里可以跑很多个 Servlet，`DispatcherServlet` 只是 Spring MVC 自己注册的那一个。**

### Servlet 是什么

**Servlet 就是"能处理 HTTP 请求的一个 Java 类"**。一个 Tomcat 里能跑很多个 Servlet，每个负责自己的 URL 路径。

不用 Spring 的经典写法：

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

| URL | 交给哪个 Servlet |
| --- | --- |
| `/hello` | `HelloServlet` |
| `/api/user` | `UserServlet` |
| `/api/order` | `OrderServlet` |

### Spring Boot 项目里其实也有多个 Servlet

只是大部分都是框架默认注册的，你看不到：

| Servlet | URL 模式 | 干什么 |
| --- | --- | --- |
| **`DispatcherServlet`** | `/` | Spring MVC 的，处理所有 Controller 请求 |
| `DefaultServlet` | （兜底） | Tomcat 内置，处理静态文件（图片、CSS） |
| `JspServlet` | `*.jsp` | Tomcat 内置，渲染 JSP 页面（如果用） |

### Spring MVC 为什么只注册一个 `DispatcherServlet`

如果按经典写法，**每个 Controller 都得是一个 Servlet**，写起来超啰嗦。Spring MVC 的设计是：

> "我就注册**一个** Servlet（`DispatcherServlet`），让它捕获所有请求（URL 模式 = `/`），然后**在它内部**用 `HandlerMapping` 把请求分发到不同的 Controller 方法。"

这叫 **Front Controller 模式**（前端控制器模式）。好处：
- 不用为每个接口写 Servlet
- 拦截器、参数解析、JSON 序列化这些公共逻辑都集中在一处
- 你只需专心写 Controller 方法

### "匹配到 `DispatcherServlet`" 是什么意思

Tomcat 内部维护一张表：

```text
URL 模式      →  Servlet
─────────────────────────────
/             →  DispatcherServlet   ← Spring 注册的
*.jsp         →  JspServlet
（默认）       →  DefaultServlet
```

当请求 `/admin/employee/page` 进来：
1. Tomcat 查表
2. `/admin/employee/page` 匹配 `/`（最宽松的兜底模式）
3. 找到 `DispatcherServlet`，把请求交给它
4. **然后 `DispatcherServlet` 才开始干那些 ①~⑦ 的事**

---

## Q2：`DispatcherServlet` 内部真的包括 ①~⑦ 吗？我记得它只是其中一步？

**A：两种说法都对，看你从哪个视角切——但事实层面，`DispatcherServlet.doDispatch()` 这个方法确实从头到尾贯穿了 ①~⑦。**

### 事实层面（源码视角）

Spring 源码里 `DispatcherServlet.doDispatch()` 就是处理请求的核心方法，简化后是这样：

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

### 教学资料里另一种画法（组件协作视角）

很多教材（包括黑马苍穹外卖）画的是"组件协作图"，长这样：

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

### 两种视角对比

| 视角 | 怎么看 `DispatcherServlet` | 一句话 |
| --- | --- | --- |
| **调用栈视角**（源码） | 它是导演，①~⑦ 全在它的方法栈里 | 所有步骤都"在它内部" |
| **组件协作视角**（教学） | 它是其中一个组件，会调用别的组件 | 它"参与"几步，但不"包含"全部 |

两种说法都没错，差别在抽象层级：
- **源码角度**：①~⑦ 都在它内部
- **组件角度**：它委托给 `HandlerMapping` / `HandlerAdapter` / `ViewResolver`

记的时候记**组件协作版**就够了——跟课程资料一致，又能看到每个组件的分工。但要知道：**最终所有步骤都是被 `DispatcherServlet.doDispatch()` 这个方法串起来的**。
