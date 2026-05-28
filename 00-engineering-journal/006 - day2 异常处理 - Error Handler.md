# 全局异常处理 — *Global Exception Handling*

> Spring Boot 项目里所有 Controller 抛出的异常，**统一在一个地方接住、转换成友好的 `Result` 返回给前端**，而不是让用户看到一坨堆栈。靠的是 `@RestControllerAdvice` + `@ExceptionHandler`。
>
> *In a Spring Boot project, **every exception thrown out of any Controller is caught in one central place and turned into a friendly `Result` for the frontend** — instead of dumping a stack trace at the user. This is achieved with `@RestControllerAdvice` + `@ExceptionHandler`.*

---

## 1. 新增员工里"用户名已存在"的 duplicate handler — *Duplicate-Entry Handler When Adding an Employee Whose Username Already Exists*

### 场景 — *Scenario*

`employee.username` 字段在数据库里有唯一索引 `idx_username`。当新增员工时如果填了一个已经存在的用户名，MySQL 会抛 `SQLIntegrityConstraintViolationException`，消息形如：

*The `employee.username` column has a unique index `idx_username`. If you try to add an employee with a username that already exists, MySQL throws a `SQLIntegrityConstraintViolationException` with a message like:*

```
Duplicate entry 'ik' for key 'employee.idx_username'
```

直接把这个异常往上抛，前端拿到的会是 500 + 一段堆栈，体验很差。所以我们要在全局异常处理器里**拦截这个异常 → 解析出冲突的用户名 → 返回友好提示**。

*If we let this exception bubble up, the frontend gets HTTP 500 with a stack trace — terrible UX. So we intercept the exception globally, **parse out the conflicting username, and return a friendly message**.*

### 代码 — *Code*

```java
@ExceptionHandler
public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
    // 例如: Duplicate entry 'ik' for key 'employee.idx_username'
    String message = ex.getMessage();
    if (message.contains("Duplicate entry")) {
        String[] split = message.split(" ");
        String username = split[2];                 // 'ik'
        String msg = username + MessageConstant.ALREADY_EXISTS;
        return Result.error(msg);
    } else {
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
```

> ⚠️ 原始代码里 `message.split("")`（空字符串）会把字符串拆成单字符数组，是个 bug。应该用 `split(" ")` 按空格拆。
>
> ⚠️ *The original snippet used `message.split("")` (empty string), which splits into single characters — a bug. It should be `split(" ")` (split on space).*

---

## 2. 全局异常处理器的标准写法 — *The Standard Shape of a Global Exception Handler*

```java
@RestControllerAdvice   // 所有 Controller 抛出的异常都先到这里 / catches exceptions from every Controller
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务自定义异常 / Business-layer custom exceptions
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex) {
        log.error("业务异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 数据库唯一约束冲突 / Unique-constraint violation from MySQL
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String username = split[2];
            return Result.error(username + MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
```

---

## 3. 关键注解拆解 — *Annotation Breakdown*

| 注解 / Annotation | 作用 / Purpose |
| --- | --- |
| `@RestControllerAdvice` | 等于 `@ControllerAdvice + @ResponseBody`：拦截所有 Controller 抛出的异常，并把返回值序列化成 JSON / *equivalent to `@ControllerAdvice + @ResponseBody`: catches every Controller exception and serializes the return as JSON.* |
| `@ExceptionHandler` | 贴在方法上，参数声明的异常类型决定**这个方法处理哪种异常** / *placed on a method; the parameter's exception type decides **which exception this method handles**.* |
| 多个 `@ExceptionHandler` | Spring 按"最具体的异常类型"匹配，没匹配上的最后兜底到处理 `Exception` 的那个方法 / *Spring matches the **most specific** exception type; anything unmatched falls through to a handler for `Exception`.* |

---

## 4. 为什么不在 Controller 里写 try-catch？ — *Why Not Just Wrap Every Controller in `try-catch`?*

| 写法 / Approach | 问题 / Problem |
| --- | --- |
| 每个 Controller 自己 try-catch | 几十个接口要复制几十遍同样的代码 —— 维护噩梦 / *Dozens of endpoints would each duplicate the same try-catch — a maintenance nightmare.* |
| Service 抛异常 + 全局处理器 (✅) | 业务代码只关心"成功路径"，异常统一兜底，代码干净 / *Business code only cares about the happy path; exceptions are caught centrally — much cleaner.* |

**结合 ThreadLocal 的全链路**：拦截器 → Controller → Service → Mapper，任何一层抛异常都会被全局处理器接住，然后 `BaseContext.removeCurrentId()` 在 `afterCompletion` 里释放线程变量，保证下个请求干净。

***Full lifecycle with ThreadLocal:** Interceptor → Controller → Service → Mapper — an exception thrown at any layer is caught by the global handler, and `BaseContext.removeCurrentId()` runs in `afterCompletion` to release the thread-local so the next request starts clean.*
