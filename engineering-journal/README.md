# Engineering Journal — *苍穹外卖项目开发日志*

我在开发 **Sky Takeaway**（苍穹外卖）全栈项目时记录的工程日志。每篇笔记都是**中英双语**——读者可以根据偏好选择任一语言。

*My engineering journal while building the **Sky Takeaway** (苍穹外卖) full-stack project. Every note is **bilingual (中文 + English)** — feel free to read in whichever language you prefer.*

---

## 📚 内容索引 — *Table of Contents*

### 🏗️ 架构与通用概念 — *Architecture & Core Concepts*

| # | 中文 | English |
| --- | --- | --- |
| 001 | [项目结构](001%20-%20%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84%20-%20Project%20Structure.md) | Project Structure |
| 002 | [封装（DTO / Entity / VO）](002%20-%20%E5%B0%81%E8%A3%85%20-%20Encapsulation.md) | Encapsulation (DTO / Entity / VO) |
| 003 | [JWT 鉴权](003%20-%20JWT%20%E9%89%B4%E6%9D%83%20-%20JWT%20Authentication.md) | JWT Authentication |
| 004 | [API 工具（Swagger / Postman / Apifox / Knife4j）](004%20-%20API%20%E5%B7%A5%E5%85%B7%20-%20API%20Tools%20%28Swagger%2C%20Postman%2C%20Apifox%2C%20Knife4j%29.md) | API Tools |
| 005 | [需求分析和设计](005%20-%20%E9%9C%80%E6%B1%82%E5%88%86%E6%9E%90%E5%92%8C%E8%AE%BE%E8%AE%A1%20-%20Requirements%20Analysis%20%26%20Design.md) | Requirements Analysis & Design |
| 013 | [Nginx 反向代理](013%20-%20nginx%20%E5%8F%8D%E5%90%91%E4%BB%A3%E7%90%86%20-%20Nginx%20Reverse%20Proxy.md) | Nginx Reverse Proxy |

### 📆 Day 2 — 员工管理 — *Employee Management*

| # | 中文 | English |
| --- | --- | --- |
| 006 | [异常处理](006%20-%20day2%20%E5%BC%82%E5%B8%B8%E5%A4%84%E7%90%86%20-%20Error%20Handler.md) | Error Handler |
| 007 | [ThreadLocal](007%20-%20day2%20ThreadLocal%20-%20ThreadLocal.md) | ThreadLocal |
| 008 | [分页查询](008%20-%20day2%20%E5%88%86%E9%A1%B5%E6%9F%A5%E8%AF%A2%20-%20Pagination%20Query.md) | Pagination Query |
| 009 | [编辑员工](009%20-%20day2%20%E7%BC%96%E8%BE%91%E5%91%98%E5%B7%A5%20-%20Edit%20Employee.md) | Edit Employee |

### 📆 Day 3 — 分类、菜品（含 AOP、拦截器、Spring 分层）— *Categories, Dishes (with AOP, Interceptors, Spring Layering)*

| # | 中文 | English |
| --- | --- | --- |
| 010 | [公共字段自动填充](010%20-%20day3%20%E9%87%8D%E5%A4%8D%E5%AD%97%E6%AE%B5%20-%20Common%20Audit%20Fields.md) | Common Audit Fields |
| 011 | [JWT Interceptor 工作原理](011%20-%20day3%20%E6%8B%A6%E6%88%AA%E5%99%A8%20-%20Interceptor.md) | JWT Interceptor |
| 012 | [AOP 切面执行流程](012%20-%20day3%20AOP%20%E5%88%87%E9%9D%A2%E6%89%A7%E8%A1%8C%E6%B5%81%E7%A8%8B%20-%20AOP%20Aspect%20Execution%20Flow.md) | AOP Aspect Execution Flow |
| 014 | [Spring 分层架构调用链](014%20-%20day3%20Spring%20%E5%88%86%E5%B1%82%E6%9E%B6%E6%9E%84%E8%B0%83%E7%94%A8%E9%93%BE%20-%20Spring%20Layered%20Architecture%20Call%20Chain.md) | Spring Layered Architecture Call Chain |
| 015 | [文件上传（OSS / S3）](015%20-%20day3%20%E4%B8%8A%E4%BC%A0%E6%96%87%E4%BB%B6%20-%20File%20Upload.md) | File Upload (OSS / S3) |
| 016 | [菜品分页查询](016%20-%20day3%20%E5%88%86%E9%A1%B5%E6%9F%A5%E8%AF%A2%E8%8F%9C%E5%93%81%20-%20Dish%20Pagination%20Query.md) | Dish Pagination Query |
| 017 | [新增菜品需求分析与设计](017%20-%20day3%20%E6%96%B0%E5%A2%9E%E8%8F%9C%E5%93%81%E9%9C%80%E6%B1%82%E5%88%86%E6%9E%90%E4%B8%8E%E8%AE%BE%E8%AE%A1%20-%20Add%20Dish%20Requirements%20%26%20Design.md) | Add Dish Requirements & Design |
| 018 | [新增菜品截图速览](018%20-%20day3%20%E6%96%B0%E5%A2%9E%E8%8F%9C%E5%93%81%E6%88%AA%E5%9B%BE%E9%80%9F%E8%A7%88%20-%20Add%20Dish%20Quick%20Visual%20Reference.md) | Add Dish Quick Visual Reference |

---

## 🛠️ 技术栈 — *Tech Stack*

- **后端 / Backend**：Spring Boot · MyBatis · MySQL · Redis · Spring AOP
- **鉴权 / Auth**：JWT + 自定义拦截器（HandlerInterceptor）
- **文件存储 / Storage**：阿里云 OSS（可扩展为 AWS S3）
- **前端 / Frontend**：Vue + Element UI（管理端）/ uni-app（用户端）
- **接口文档 / API Doc**：Knife4j（Swagger 增强版）
- **反向代理 / Reverse Proxy**：Nginx

---

## ✍️ 我为什么写这些笔记 — *Why I Keep This Journal*

不只是抄写课程内容，而是**记录我自己思考、Challenge、求证、踩坑的过程**。每篇笔记都尝试回答：

*This is not a transcript of course videos — it documents **my own thinking, my own questions, the verifications I ran, and the pitfalls I hit**. Each note tries to answer:*

1. **它是什么？** —— What is this concept? *(definition)*
2. **它为什么存在？** —— Why does it exist? *(motivation)*
3. **它具体如何工作？** —— How does it actually work? *(mechanism / call chain)*
4. **容易踩什么坑？** —— What are the common gotchas? *(pitfalls & Q&A)*

希望对未来读到这些笔记的人——包括未来的我自己——有帮助。

*Hopefully this helps whoever reads it next — including future me.*

---

## 📖 怎么阅读这些笔记 — *How to Read These Notes*

- **从头到尾按顺序读**：跟着我学习 Sky Takeaway 的时间线走 / *Read sequentially to follow my Sky Takeaway learning timeline.*
- **挑感兴趣的主题读**：每篇笔记都是相对独立的 / *Or just jump to any topic — each note is self-contained.*
- **VS Code 用户**：用 Markdown Preview（`Cmd+K V`）渲染最佳 / *VS Code users: open with Markdown Preview (`Cmd+K V`) for best rendering.*

---

*Last updated: 2026-05*
