![img_9.png](img_9.png)
在《苍穹外卖》这个项目里，Nginx（简称 NG）就像是餐厅里的“超级领班”。如果没有它，后端的 Spring Boot（大厨）可能会直接被食客（前端请求）淹没。

*In the "Sky Takeaway" project, Nginx (NG for short) acts like a "super maître d'" of the restaurant. Without it, the backend Spring Boot (the head chef) would be drowned by the diners (incoming front-end requests).*

为了让你更生动地理解，我们把 Nginx 的好处拆解成四个形象的角色：

*To make the idea more vivid, we break Nginx's benefits into four illustrative roles:*

---

### 1. 它是“金牌翻译官”（反向代理） — *1. It's the "Gold-Standard Translator" (Reverse Proxy)*

**场景：** 这里的食客（前端）说的是“/api/”语，而大厨（后端）只听得懂“/admin/”语。

***Scenario:** the diners (front end) speak "/api/" while the chef (back end) only understands "/admin/".*

* **形象理解**：如果没有 Nginx，前端直接找后端，由于语言不通（路径不匹配）和安保限制（跨域问题），大厨根本不理你。
* **具体好处**：Nginx 站在门口，把前端的 `http://localhost/api/login` 翻译成后端能懂的 `http://localhost:8080/admin/login`。
* **核心价值**：**解决跨域**，让前后端虽然住在不同的“房间”（端口），但能顺畅交流。

***Key points:***

* ***Picture it this way:** without Nginx, the front end goes straight to the back end, but because the languages don't match (path mismatch) and security blocks them (cross-origin issues), the chef simply ignores them.*
* ***Concrete benefit:** Nginx stands at the door and translates the front end's `http://localhost/api/login` into something the back end understands — `http://localhost:8080/admin/login`.*
* ***Core value:** **solves CORS**, letting the front end and back end live in different "rooms" (ports) yet still communicate smoothly.*

### 2. 它是“排队调度员”--可以分发好几个服务器server（负载均衡）![img_11.png](img_11.png) — *2. It's the "Queue Dispatcher" — Distributes Load Across Multiple Servers (Load Balancing)![img_11.png](img_11.png)*

**场景：** 餐厅生意太火爆，一个大厨（服务器）炒菜冒烟了都忙不过来。

***Scenario:** business is booming, and a single chef (server) is so overloaded the stove is literally smoking.*

* **形象理解**：你开了三家分店（后端集群：服务器A、B、C）。Nginx 站在街口看哪家店人少，就带客人去哪家。
* **具体好处**：他在 [nginx.conf](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D10) 里配置 `upstream`。当几千个请求涌入时，他平均分配，不让任何一个大厨累趴下。
* **核心价值**：**高并发处理**，让系统像丝般顺滑，不会因为人多就挂掉。
* **负载均衡策略**：<BR>![img_12.png](img_12.png)

***Key points:***

* ***Picture it this way:** you've opened three branches (a backend cluster: servers A, B, C). Nginx stands at the corner, looks at which branch has the fewest customers, and ushers people there.*
* ***Concrete benefit:** it configures `upstream` blocks in [nginx.conf](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D10). When thousands of requests pour in, it spreads them evenly so no single chef collapses.*
* ***Core value:** **handles high concurrency** — the system stays silky smooth and doesn't fall over under load.*
* ***Load-balancing strategies:** <BR>![img_12.png](img_12.png)*

### 3. 它是“硬核保镖”（安全隔离） — *3. It's the "Hardcore Bodyguard" (Security Isolation)*

**场景：** 坏人想直接冲进厨房看大厨用什么调料、偷看保险柜。

***Scenario:** an attacker wants to barge straight into the kitchen to see what spices the chef uses or peek at the safe.*

* **形象理解**：大厨躲在隐蔽的后院（内网），外人根本看不见。所有人只能看到大门口的 Nginx。
* **具体好处**：Nginx 暴露在公网，而后端服务器藏在防火墙后面。即使黑客攻击，也只能攻击到 Nginx 这个“前台”，碰不到核心数据和代码。
* **核心价值**：**保护后端安全**，隐藏真实服务器 IP。

***Key points:***

* ***Picture it this way:** the chef hides in the secluded back yard (the internal network) where outsiders can't see in. All anyone can see is the Nginx at the front gate.*
* ***Concrete benefit:** Nginx is exposed on the public network while the back-end servers sit behind a firewall. Even if hackers attack, they can only hit Nginx — the "reception desk" — and never reach the core data or code.*
* ***Core value:** **protects the back end** and hides the real server IP.*

### 4. 它是“闪电外送员”（动静分离） — *4. It's the "Lightning Delivery Guy" (Static/Dynamic Separation)*

**场景：** 有人只想买瓶矿泉水（静态图片/HTML），大厨还要停下手里的炒锅去拿，太浪费时间。

***Scenario:** a customer just wants to buy a bottle of mineral water (a static image or HTML page) — making the chef stop stir-frying to fetch it would be a waste of time.*

* **形象理解**：Nginx 柜台里直接摆好了矿泉水（静态资源）。
* **具体好处**：对于图片、CSS、JS 等不需要大厨（Java代码）加工的东西，Nginx 自己直接秒发给用户，根本不经过后端。
* **核心价值**：**极速响应**，解放后端资源，让大厨专心处理复杂的炒菜逻辑（业务逻辑）。

***Key points:***

* ***Picture it this way:** Nginx already has the mineral water (static assets) ready on its counter.*
* ***Concrete benefit:** for images, CSS, JS, and other things that don't need the chef (Java code) to prepare, Nginx hands them straight to the user without ever touching the back end.*
* ***Core value:** **lightning-fast response**, freeing up back-end resources so the chef can focus on complex stir-fry logic (business logic).*

---

### 📝 总结笔记：Nginx 的四大神功 — *Summary Notes: Nginx's Four Superpowers*

| 角色 / Role | 术语 / Term | 一句话概括 / One-Line Summary |
| --- | --- | --- |
| **翻译官** <br> ***Translator*** | **反向代理** <br> ***Reverse Proxy*** | 统一入口，转化路径，解决跨域。 <br> *Single entry point, path rewriting, solves CORS.* |
| **调度员** <br> ***Dispatcher*** | **负载均衡** <br> ***Load Balancing*** | 多个后端排排坐，请求分发不塞车。 <br> *Multiple back ends sit in a row; requests are distributed without traffic jams.* |
| **保镖** <br> ***Bodyguard*** | **安全屏蔽** <br> ***Security Shielding*** | 后端藏得深，黑客找不着。 <br> *Back end is hidden deep — hackers can't find it.* |
| **外送员** <br> ***Delivery Guy*** | **动静分离** <br> ***Static/Dynamic Separation*** | 静态网页跑得快，后端减负不等待。 <br> *Static pages fly; the back end is offloaded and never makes you wait.* |

---

**学习建议：**
你在看 [Day01-10 视频](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D10) 配置 `proxy_pass` 的时候，可以试着故意把 Nginx 关掉，你会发现前端页面能打开（静态资源还在），但登录按钮点了一点反应都没有（动态转发断了）。这能帮你更深刻地理解 Nginx 的“翻译”作用。

***Study tip:** while watching the [Day01-10 video](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D10) and configuring `proxy_pass`, try shutting Nginx down on purpose. You'll find the front-end page still opens (static assets are still there) but the login button does absolutely nothing (the dynamic proxy is broken). This experiment really cements Nginx's "translator" role.*

你现在能分清，为什么图片加载很快，而登录有时需要转圈圈了吗？（一个是 Nginx 直接给的，一个是 Nginx 转交给后端算的）

*Can you now tell why images load instantly while login sometimes spins for a while? (One is served directly by Nginx; the other is forwarded by Nginx to the back end for processing.)*

# 在《苍穹外卖》项目里，Nginx 就像是一个“精明的超级前台”。它不仅负责接待（接收请求），还会根据你的需求帮你“转接”到正确的部门，甚至顺手帮你改个名字。 — *In the "Sky Takeaway" Project, Nginx Acts Like a "Shrewd Super Receptionist". It Not Only Receives Requests, but Also Forwards You to the Correct Department — and Even Renames You Along the Way.*

以下是关于 Nginx 动态请求转化的生动解析：

*Here's a vivid breakdown of how Nginx transforms dynamic requests:*

---

### 1. Nginx 是如何“变魔术”转化 HTTP 的？ — *1. How Does Nginx "Magically" Transform HTTP Requests?*

当你在浏览器（前端）点击登录，请求会先到 Nginx，然后发生了一场悄无声息的“身份伪装”： -- 这个需要`nginx.conf`里配置

*When you click "Login" in the browser, the request first arrives at Nginx, where a silent "identity disguise" takes place — and this requires configuration in `nginx.conf`.*

* **路径整容**：Nginx 发现你带着 `/api/` 的工牌，它会按照你的配置，把 `/api/` 撕掉，换成后端真正认识的 `/admin/`。
* **全量搬运**：除了把 URL 路径改了，Nginx 会把 HTTP 的“灵魂”——也就是 **Method**（POST）、**Body**（用户名密码）和 **Header**（JWT 令牌）完整打包，发给后端 8080 端口。
* **最终结果**：浏览器发的 `http://localhost/api/employee/login` $\rightarrow$ 变成了发给后端的 `http://localhost:8080/admin/employee/login`。

***Key points:***

* ***Path makeover:** Nginx notices that you're wearing the `/api/` badge and, per your configuration, peels off `/api/` and swaps it for the `/admin/` the back end actually recognizes.*
* ***Full payload forwarding:** beyond rewriting the URL, Nginx packages the "soul" of the HTTP request — the **Method** (POST), **Body** (username/password), and **Headers** (JWT token) — and forwards it intact to the back end on port 8080.*
* ***End result:** the browser sends `http://localhost/api/employee/login` $\rightarrow$ which becomes `http://localhost:8080/admin/employee/login` on the back end.*

![img_10.png](img_10.png)
---

### 2. 如何配置 `nginx.conf`？ — *2. How to Configure `nginx.conf`*

在 [Day01-10 视频](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11) 中，核心配置就在 `server` 块里。你可以把它看作一套“分流规则”：

*In the [Day01-10 video](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11), the core configuration sits inside the `server` block. You can think of it as a set of "routing rules":*

```nginx
server {
    listen       80;          # Nginx 监听的窗口（前端访问的端口）
    server_name  localhost;   # 监听的名字

    # 规则1：如果是普通的请求，直接去拿静态网页
    location / {
        root   html/sky;      # 静态资源存放的目录
        index  index.html;
    }

    # 规则2：重点！如果是动态 API 请求，开始转化并转发
    location /api/ {
        # proxy_pass 就是“转发目的地”
        # 结尾的 /admin/ 就是把前面的 /api/ 替换掉的关键！
        proxy_pass   http://localhost:8080/admin/; 
    }
}

```

---

### 3. 只有 `/api/` 开头的会被转化吗？ — *3. Will Only Requests Starting With `/api/` Be Transformed?*

**是的，但这是你“规定”的。**
Nginx 非常听话，它只会按照你写的 `location` 块来办事：

***Yes — but only because you said so.** Nginx is very obedient: it only acts based on the `location` blocks you write.*

* 如果请求是 `/api/employee/login`，它匹配到了 `location /api/`，于是触发转发。
* 如果请求是 `/test/hello`，它匹配不到 `/api/`，就会去匹配默认的 `/`，如果静态文件夹里没这个文件，就会报 **404**。

***Examples:***

* *If the request is `/api/employee/login`, it matches `location /api/` and triggers proxying.*
* *If the request is `/test/hello`, it doesn't match `/api/`, so it falls through to the default `/`; if the static folder doesn't contain that file, you get a **404**.*

---

### 4. 总结：Login 如何通过 Nginx 加上 Admin？ — *4. Summary: How Does Login Get the `Admin` Prefix Added by Nginx?*

这个过程在 Nginx 术语里叫 **路径重写 (Path Rewriting)**。你可以这样生动理解：

*This process is known in Nginx terminology as **Path Rewriting**. Picture it this way:*

1. **用户输入**：`/api/login`（这是前端的“外语”）。
2. **Nginx 翻译**：它看到配置里写着 `location /api/` 对应 `proxy_pass .../admin/`。
3. **替换动作**：它像用橡皮擦一样，擦掉 URL 里的 `/api/`，在原位补上 `/admin/`。
4. **递交给后端**：后端 Spring Boot 收到请求时，看到的已经是带有 `/admin/login` 的请求了。

***Step by step:***

1. ***User input:** `/api/login` (the front end's "foreign language").*
2. ***Nginx translates:** it sees that the config maps `location /api/` to `proxy_pass .../admin/`.*
3. ***Substitution:** like an eraser, it wipes `/api/` from the URL and fills `/admin/` in its place.*
4. ***Hand-off to the back end:** when Spring Boot receives the request, it already sees `/admin/login`.*

> **小笔记：**
> * **反向代理的好处**：前端不需要知道后端的真实 IP（安全），也不存在跨域报错（省心）。
> * **配置文件生效**：修改完 `nginx.conf` 别忘了运行 `nginx -s reload` 让这位“前台”更新规矩。
>
>

> ***Quick notes:***
> * ***Reverse-proxy benefits:** the front end doesn't need to know the back end's real IP (security), and there are no CORS errors (peace of mind).*
> * ***Reloading config:** after editing `nginx.conf`, don't forget `nginx -s reload` so the "receptionist" picks up the new rules.*

你在 [Day01-10 视频](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11) 里配置完之后，有没有试过在浏览器的“开发者工具（F12）”里，看看请求地址到底是带 `api` 还是带 `admin`？（提示：浏览器显示的永远是带 `api` 的，因为它只跟 Nginx 说话！）

*After completing the configuration in the [Day01-10 video](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11), have you tried opening the browser's DevTools (F12) and inspecting whether the request URL carries `api` or `admin`? (Hint: the browser always shows `api`, because it only ever talks to Nginx!)*


# 为什么叫做“反向”代理（Reverse Proxy）**，是相对于**“正向”代理（Forward Proxy）而言的。 — *Why Is It Called a "Reverse" Proxy? It's Named in Contrast to the "Forward" Proxy.*

我们可以通过一个生动的例子来理解这个“方向”到底是怎么反过来的。

*Let's use a vivid example to understand exactly how the "direction" gets reversed.*

---

### 1. 正向代理：代理的是“客户端” — *1. Forward Proxy: Proxies the Client*

**场景**：你想看外网，但你上不去，于是你找了一个**科学上网工具（代理服务器）**。

***Scenario:** you want to access an overseas website but you can't, so you use a **VPN/proxy tool**.*

* **谁发起的**：你（客户端）。
* **代理谁**：代理你。
* **目的是什么**：替你去访问你访问不到的服务器。
* **服务器知道你是谁吗？** 不知道。服务器只知道那个代理服务器来找它了，它不知道背后是你。
* **总结**：**正向代理隐藏了真实的客户端。**

***Key points:***

* ***Who initiates:** you (the client).*
* ***Who is being proxied:** you.*
* ***Purpose:** to access servers on your behalf that you can't reach directly.*
* ***Does the server know who you are?** No. The server only sees the proxy server knocking on its door; it has no idea you're behind it.*
* ***Bottom line:** **a forward proxy hides the real client.***

### 2. 反向代理：代理的是“服务端” — *2. Reverse Proxy: Proxies the Server*

**场景**：你访问《苍穹外卖》的网站 `http://localhost`。

***Scenario:** you visit the Sky Takeaway website at `http://localhost`.*

* **谁发起的**：你。
* **代理谁**：**Nginx 代理了后台那群“大厨”（Spring Boot 服务器）**。
* **目的是什么**：你以为你直接访问了服务器，其实你访问的是 Nginx。Nginx 收到你的请求后，回头看看哪个大厨有空，把活儿派给他。
* **你知道真实的服务端在哪吗？** 不知道。你只知道 Nginx 的地址，至于背后是一台服务器还是十台服务器，你完全不清楚。
* **总结**：**反向代理隐藏了真实的服务端。**

***Key points:***

* ***Who initiates:** you.*
* ***Who is being proxied:** **Nginx proxies the cluster of "chefs" (Spring Boot servers) behind it**.*
* ***Purpose:** you think you're hitting the server directly, but you're actually hitting Nginx. After receiving the request, Nginx turns around, finds an available chef, and assigns the work.*
* ***Do you know where the real server is?** No. You only know Nginx's address; whether there's one server behind it or ten, you have no idea.*
* ***Bottom line:** **a reverse proxy hides the real server.***

---

### 3. 生动对比：经纪人 vs 跑腿小哥 — *3. Vivid Comparison: Talent Agent vs Errand Runner*

* **正向代理（跑腿小哥）**：
  你（客户端）想买奶茶但不想露面，你雇了一个跑腿小哥（代理）去奶茶店。
* 奶茶店（服务端）只看到小哥，不知道是你买的。
* **代理的是你。**

***Forward proxy (errand runner):***
* *You (the client) want milk tea but don't want to show your face, so you hire an errand runner (the proxy) to go to the tea shop.*
* *The tea shop (server) only sees the runner; it has no idea you're the actual buyer.*
* ***The runner proxies you.***


* **反向代理（明星经纪人）**：
  你想找某个大明星（后端服务），你没法直接给明星打电话，你只能打给他的**经纪人（Nginx）**。
* 你（客户端）只跟经纪人说话，经纪人转身把要求告诉明星，再把明星的回复传给你。
* 你甚至不知道明星具体躲在哪个房间里。
* **代理的是明星（服务端）。**

***Reverse proxy (celebrity's agent):***
* *You want to reach a celebrity (the back-end service) but you can't call them directly — you can only call their **agent (Nginx)**.*
* *You (the client) only talk to the agent; the agent then conveys the request to the celebrity and relays the response back.*
* *You don't even know which room the celebrity is hiding in.*
* ***The agent proxies the celebrity (server).***



---

### 4. 为什么《苍穹外卖》里一定要叫它“反向代理”？ — *4. Why Must We Call It "Reverse Proxy" in the Sky Takeaway Project?*

在 [Day01-10 视频](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11) 中，Nginx 的作用就是典型的经纪人模式：

*In the [Day01-10 video](https://www.google.com/search?q=https://www.bilibili.com/video/BV1TP411v7v6%3Fp%3D11), Nginx's role is the textbook "agent" pattern:*

1. **统一入口**：前端不需要记住 8080 端口（admin）或 8081 端口（user），只需要统一找 Nginx（80 端口）。
2. **安全屏障**：由于你只和 Nginx 说话，你无法直接攻击藏在后面的数据库或 Java 程序。
3. **灵活变通**：就像你问的“如何加上 /admin”，这是经纪人（Nginx）在背地里帮你做的转换，你作为客户根本无感知。

***Key points:***

1. ***Unified entry point:** the front end doesn't need to remember port 8080 (admin) or port 8081 (user) — it only needs to talk to Nginx on port 80.*
2. ***Security barrier:** because you only speak to Nginx, you can't directly attack the database or the Java application hidden behind it.*
3. ***Flexibility:** as in your question about "how the /admin prefix gets added", this is a transformation the agent (Nginx) performs behind the scenes, completely transparent to you as the client.*

### 📝 深度笔记：方向总结 — *In-Depth Notes: Direction Summary*

| 维度 / Dimension | 正向代理 (Forward) / *Forward Proxy* | 反向代理 (Reverse) / *Reverse Proxy* |
| --- | --- | --- |
| **代理对象** / **Who is proxied** | 客户端 (Client) <br> *The client* | 服务端 (Server) <br> *The server* |
| **部署位置** / **Where it's deployed** | 靠近客户端（或在客户端电脑上） <br> *Close to the client (or on the client machine itself)* | 靠近服务端（在机房入口） <br> *Close to the server (at the data center entrance)* |
| **用户感知** / **User awareness** | 用户知道自己用了代理 <br> *The user knows they're using a proxy.* | 用户以为自己访问的就是最终目标 <br> *The user thinks they're hitting the real target.* |
| **典型案例** / **Typical examples** | 科学上网、公司内网统一出网 <br> *VPN tools, corporate outbound gateways.* | **Nginx**、负载均衡器 <br> ***Nginx**, load balancers.* |

**一句话总结：**
代理**客户端**出海的叫**正向**；代理**服务端**接客的叫**反向**。

***One-line summary:** the proxy that helps **clients reach out** is called **forward**; the proxy that helps **servers receive callers** is called **reverse**.*

现在你理解了，为什么在 `nginx.conf` 里的配置项叫做 `proxy_pass`（代理转发），因为它就是那个替后端服务器“接活”的经纪人。

*Now you understand why the directive in `nginx.conf` is called `proxy_pass` ("proxy and forward") — because it's the agent that "picks up the gig" on behalf of the back-end servers.*

你在看视频时，是不是发现 Nginx 不仅能转给一个服务器，还能转给一个 `upstream`（服务器组）？这其实就是利用“反向代理”实现的“负载均衡”，也就是经纪人手下有好几个艺人，谁闲着给谁派活。

*While watching the videos, did you notice that Nginx can forward not only to a single server but also to an `upstream` (a server group)? That's "load balancing" implemented through "reverse proxying" — like an agent who manages several performers and assigns work to whoever is currently free.*
