简单来说，**Password（密码）** 是你的身份证明，而 **JWT（JSON Web Token）** 是你登录成功后拿到的“通行证”。

*In short, the **password** is your proof of identity, while the **JWT (JSON Web Token)** is the "pass" you receive after logging in successfully.*

在您观看的 [苍穹外卖项目](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) 中，这两者处于认证流程的不同阶段。以下是它们的详细对比：

*In the [Sky Take-out project](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) you're watching, the two sit at different stages of the authentication flow. Here is a detailed comparison:*

### 1. 核心定义与用途 — *Core Definitions and Uses*

| 特性 / Aspect | Password (密码) <br> *Password* | JWT (令牌) <br> *JWT (Token)* |
| --- | --- | --- |
| **本质 / Essence** | 用户身份的**原始凭证**。 <br> *The **original credential** of the user's identity.* | 服务器签发的**临时身份证明**。 <br> *A **temporary proof of identity** issued by the server.* |
| **主要用途 / Main use** | 证明“我是谁”（身份认证）。 <br> *To prove "who I am" (authentication).* | 证明“我有权限访问这些资源”（授权）。 <br> *To prove "I'm allowed to access these resources" (authorization).* |
| **存储位置 / Storage location** | **数据库**（通常是加密后的哈希值）。 <br> ***Database** (usually as an encrypted hash).* | **客户端**（浏览器 LocalStorage 或 Cookie）。 <br> ***Client side** (browser LocalStorage or cookies).* |
| **时效性 / Lifetime** | 长期有效，直到用户修改。 <br> *Long-lived, valid until the user changes it.* | 短期有效，过期需重新获取。 <br> *Short-lived; a new one must be obtained when it expires.* |

---

### 2. 工作流程（以苍穹外卖为例） — *Workflow (Using Sky Take-out as an Example)*

在 [Day01-08 后端环境搭建](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) 的登录功能中，它们是协作关系：

*In the login functionality from [Day01-08 Back-End Environment Setup](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9), the two work together:*

1. **认证阶段 (Password)**：
* 前端发送用户名和 **Password** 到后端。
* 后端比对数据库中的密码。如果匹配成功，说明身份合法。


2. **颁发阶段 (JWT)**：
* 身份确认后，后端使用 `JwtUtil.createJWT()` 生成一个 **JWT** 字符串。
* 这个 JWT 包含了用户 ID 等非敏感信息，并由服务器私钥签名。


3. **鉴权阶段 (JWT)**：
* 前端后续请求（如查询菜品、修改订单）不再发送密码。
* 前端只在请求头中携带 **JWT**。后端拦截器校验 JWT 的合法性，校验通过即放行。

***Key points:***

1. ***Authentication stage (password):***
* *The front end sends the username and **password** to the back end.*
* *The back end compares them against the database. A match means the identity is legitimate.*


2. ***Issuance stage (JWT):***
* *Once the identity is confirmed, the back end calls `JwtUtil.createJWT()` to generate a **JWT** string.*
* *That JWT contains non-sensitive info such as the user ID, signed with the server's private key.*


3. ***Authorization stage (JWT):***
* *Subsequent front-end requests (e.g., listing dishes, updating orders) no longer send the password.*
* *The front end only carries the **JWT** in the request header. The back-end interceptor validates the JWT — once it passes, the request goes through.*



---

### 3. 为什么不直接一直传密码？ — *Why Not Just Keep Sending the Password Every Time?*

* **安全性**：密码是敏感信息，频繁在网络上传输会增加被拦截的风险。JWT 即使被窃取，也可以通过设置较短的过期时间来降低损失。
* **性能**：校验密码通常需要查询数据库，而校验 JWT 只需要在内存中通过算法验证签名（如视频中提到的解密过程），速度极快。
* **无状态性**：在分布式或集群环境下，服务器不需要存储 Session，只要有密钥，任何一台服务器都能解析 JWT 确认用户身份。

***Key points:***

* ***Security:** the password is sensitive, and transmitting it over the network frequently increases the risk of interception. Even if a JWT is stolen, the damage can be limited by setting a short expiration.*
* ***Performance:** verifying a password usually requires a database query, while verifying a JWT only needs an in-memory signature check (the decryption process mentioned in the video) — which is extremely fast.*
* ***Statelessness:** in a distributed or clustered environment, the server doesn't need to store a session. As long as it has the key, any server can parse the JWT and confirm the user's identity.*

---

### 4. 关键区别总结 — *Summary of Key Differences*

* **密码**是进入大门的钥匙，一生（通常）只在登录时用一次。
* **JWT**是进门后领取的胸卡，只要在有效期内，带着它可以在大楼里（各个接口）自由出入。

***Key points:***

* ***The password** is the key that opens the front door — typically used only once during login.*
* ***The JWT** is the badge you receive after entering. As long as it hasn't expired, you can move freely through the building (across endpoints) while wearing it.*

**温馨提示**：在您调试代码时，如果发现前端报 `401 Unauthorized` 错误，通常不是密码错了，而是 JWT 已经过期，或者后端拦截器在校验 JWT 时抛出了异常。

***Friendly reminder:** while debugging, if the front end reports a `401 Unauthorized` error, it's usually not the password that's wrong — more likely the JWT has expired, or the back-end interceptor threw an exception while validating it.*

您在断点调试 `EmployeeController.login` 方法时，是否已经看到了 `JwtUtil` 生成那个长字符串的过程？

*When breakpoint-debugging the `EmployeeController.login` method, have you already seen `JwtUtil` generate that long string?*

# 将 JWT 放入 HTTP 请求头的 authorization 字段中。 那岂不是在http link里会泄露 — *Placing the JWT in the HTTP `authorization` Header — Wouldn't That Leak It Over the HTTP Link?*

这是一个非常敏锐的问题！很多初学者在学习 [苍穹外卖项目](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) 时都会有类似的担心。

*That's a very sharp question! Many beginners studying the [Sky Take-out project](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) have similar worries.*

简单直接的回答是：**如果你使用普通的 HTTP 协议，确实会泄露；但如果你使用 HTTPS 协议，它是安全的。**

*The simple, direct answer is: **with plain HTTP, yes, it would be leaked; but with HTTPS, it's safe.***

我们可以从以下几个维度来彻底理解这个问题：

*We can fully unpack this from several angles:*

---

### 1. HTTP 链路泄露的风险 — *The Risk of Leaks Over an HTTP Link*

正如你所担心的，在**纯 HTTP**（未加密）环境下，JWT 确实存在巨大的安全隐患：

*Just as you fear, under **plain HTTP** (unencrypted), the JWT really does pose major security risks:*

* **明文传输**：HTTP 请求头（Header）是明文传输的。如果有人在同一个局域网内使用“抓包工具”（如 Wireshark），他们可以轻易截获你的 `Authorization` 字段。
* **中间人攻击**：链路上的路由器、代理服务器都能看到你的 JWT。

***Key points:***

* ***Plaintext transmission:** HTTP headers are sent in plaintext. If someone on the same LAN uses a packet sniffer (such as Wireshark), they can easily capture your `Authorization` header.*
* ***Man-in-the-middle attacks:** routers and proxy servers along the path can see your JWT.*

### 2. 为什么 HTTPS 能解决问题？ — *Why Does HTTPS Solve the Problem?*

在实际的企业级生产环境（包括《苍穹外卖》上线时）中，必须使用 **HTTPS**。

*In real enterprise-grade production environments (including when Sky Take-out goes live), you must use **HTTPS**.*

* **加密传输**：HTTPS 在 HTTP 之上加了一层 SSL/TLS 加密层。
* **保护 Header**：很多人误以为 HTTPS 只加密请求体（Body），其实 **HTTPS 会加密整个 HTTP 数据包，包括请求行、请求头（Header）和请求体**。
* **结果**：即使黑客在链路上拦截了数据包，他看到的也只是一堆乱码，无法解出你的 `Authorization` 字段中的 JWT。

***Key points:***

* ***Encrypted transport:** HTTPS adds an SSL/TLS encryption layer on top of HTTP.*
* ***Header protection:** many people mistakenly believe HTTPS only encrypts the body — actually, **HTTPS encrypts the entire HTTP packet, including the request line, headers, and body**.*
* ***Result:** even if an attacker intercepts packets on the wire, all they see is gibberish — they cannot decode the JWT in your `Authorization` header.*

---

### 3. JWT 放在 Header vs 放在 URL — *JWT in the Header vs. in the URL*

你提到了“http link”，这里要区分两种做法：

*You mentioned the "http link" — here we need to distinguish between two approaches:*

1. **放在 Header（推荐）**：如 `Authorization: Bearer <token>`。这种方式不会被记录在服务器的访问日志（Access Log）或浏览器的历史记录中。
2. **放在 URL 参数中（极度危险）**：如 `http://api.com/user?token=xxx`。
* **风险**：即便有 HTTPS，URL 仍然会留在浏览器的历史记录、书签以及服务器的访问日志中。**这是绝对禁止的做法**。

***Key points:***

1. ***In the Header (recommended):** for example, `Authorization: Bearer <token>`. This is not recorded in the server's access log or the browser's history.*
2. ***In a URL parameter (extremely dangerous):** for example, `http://api.com/user?token=xxx`.*
* ***Risk:** even with HTTPS, the URL still ends up in the browser history, bookmarks, and server access logs. **This is absolutely forbidden.***



### 4. 苍穹外卖项目中的实际操作 — *What Actually Happens in the Sky Take-out Project*

在您看的 [Day01-08 视频](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) 中，前后端联调主要是在本地环境（localhost）。

*In the [Day01-08 video](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6/%3Fp%3D9) you're watching, front-end and back-end integration testing happens primarily on the local environment (`localhost`).*

* 在**本地开发阶段**，通常直接用 HTTP，因为此时数据只在你的电脑内部流动，不经过公网，所以相对安全。
* 在**后端拦截器**（`JwtTokenAdminInterceptor`）中，程序会从 `request.getHeader("token")` 中获取这个字符串进行校验。

***Key points:***

* *During **local development**, plain HTTP is typically fine because data flows only within your own machine and never touches the public internet — so it's relatively safe.*
* *In the **back-end interceptor** (`JwtTokenAdminInterceptor`), the code retrieves the string via `request.getHeader("token")` for validation.*

---

### 5. 总结与建议 — *Summary and Recommendations*

> **防窃听靠 HTTPS，防篡改靠 JWT 签名。**
>
> ***HTTPS prevents eavesdropping; the JWT signature prevents tampering.***

* **传输安全**：必须靠 **HTTPS** 协议来保证 Header 不在链路上泄露。
* **数据安全**：即使别人拿到了 JWT（比如你电脑被肉身攻击），由于 JWT 内部有 **Signature（签名）**，他也无法修改里面的用户 ID（一旦修改，签名失效）。

***Key points:***

* ***Transport security:** must rely on **HTTPS** to ensure the header doesn't leak on the wire.*
* ***Data security:** even if someone gets hold of the JWT (say, by physically attacking your machine), the JWT carries a **signature** internally, so they cannot tamper with the user ID inside it — any modification invalidates the signature.*

**一句话建议：**
永远不要在 JWT 的 **Payload**（负载部分）存放用户密码、手机号等隐私信息，因为 JWT 仅仅是 Base64 编码，相当于“透明”的。

***One-line advice:***
*Never store sensitive information such as passwords or phone numbers in the JWT **payload**, because the JWT is only Base64-encoded — effectively "transparent".*

既然提到了安全性，你是否注意到在 `JwtProperties` 配置文件里，那个 `admin-secret-key`（密钥）如果泄露了会发生什么？

*Speaking of security, have you noticed what would happen in the `JwtProperties` configuration file if the `admin-secret-key` (the secret key) were leaked?*
