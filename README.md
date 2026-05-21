# CloudBite Engine

<p align="center">
  <b>A Cloud-Native Food Delivery & Restaurant Operations Platform</b>
</p>

<p align="center">
  <b>智能外卖订单与餐饮运营平台</b>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Architecture-Layered%20Backend-black" />
  <img src="https://img.shields.io/badge/API-RESTful-blueviolet" />
  <img src="https://img.shields.io/badge/Auth-JWT-orange" />
  <img src="https://img.shields.io/badge/Cache-Redis-critical" />
  <img src="https://img.shields.io/badge/Realtime-WebSocket-informational" />
  <img src="https://img.shields.io/badge/Storage-OSS-yellowgreen" />
  <img src="https://img.shields.io/badge/Database-MySQL-blue" />
  <img src="https://img.shields.io/badge/Deployment-Docker-2496ED" />
</p>

---

## ✨ Overview

**CloudBite Engine** is a full-stack food delivery and restaurant operations platform designed for both restaurant administrators and end users.

The system supports menu management, dish and set meal configuration, shopping cart, order placement, order tracking, real-time order notifications, employee management, and business data analytics.

Unlike a simple CRUD application, CloudBite Engine focuses on practical backend engineering patterns, including authentication, caching, real-time communication, file storage, scheduled tasks, transaction management, and layered RESTful API design.

---

## 项目简介

**CloudBite Engine** 是一个面向餐厅管理端和用户端的外卖点餐与餐饮运营平台。

系统支持员工管理、分类管理、菜品管理、套餐管理、购物车、用户下单、订单追踪、订单实时提醒和营业数据统计等功能。

该项目不仅是普通的 CRUD 系统，而是围绕真实后端开发场景进行设计，重点体现登录认证、Redis 缓存、WebSocket 实时通信、文件上传、定时任务、事务管理和 RESTful API 分层设计等工程能力。

---

## 🚀 Core Features

| Module | English | 中文 |
|---|---|---|
| Authentication | JWT-based login and session management | 基于 JWT 的登录认证与会话管理 |
| Employee Management | Admin-side employee CRUD and status control | 管理端员工增删改查与状态管理 |
| Menu Management | Dish, category, and set meal management | 菜品、分类、套餐管理 |
| Shopping Cart | User-side cart operations | 用户端购物车功能 |
| Order System | Order placement, payment simulation, status tracking | 下单、支付模拟、订单状态追踪 |
| Realtime Notification | WebSocket-based order reminders | 基于 WebSocket 的订单实时提醒 |
| File Upload | Image upload for dishes and set meals | 菜品与套餐图片上传 |
| Data Analytics | Business overview and sales statistics | 营业数据统计与销售分析 |
| Scheduled Tasks | Automatic order status update | 定时任务自动更新订单状态 |

---

## 🧠 Engineering Highlights

```text
CloudBite Engine is designed to demonstrate backend engineering capability,
not just feature completion.
````

* Designed layered backend architecture with Controller, Service, Mapper, DTO, VO, and Entity separation.
* Implemented JWT-based authentication for secure admin and user-side access.
* Used Redis to cache high-frequency dish and category data and reduce database pressure.
* Built WebSocket-based real-time order notification for restaurant administrators.
* Designed RESTful APIs for admin workflows and user-side ordering flows.
* Integrated object storage for dish and set meal image uploads.
* Applied scheduled tasks to automatically handle timeout orders and delivery status updates.
* Used database transactions to ensure order creation consistency across order, order detail, and shopping cart tables.

---

## 技术亮点

```text
CloudBite Engine 不是单纯完成业务功能，而是用于展示后端工程能力。
```

* 采用 Controller、Service、Mapper、DTO、VO、Entity 分层架构，提高代码可维护性。
* 实现基于 JWT 的登录认证机制，支持管理端和用户端访问控制。
* 使用 Redis 缓存高频访问的菜品和分类数据，降低数据库压力。
* 使用 WebSocket 实现管理端订单实时提醒。
* 设计 RESTful API，覆盖管理端业务流和用户端下单流程。
* 集成对象存储服务，实现菜品和套餐图片上传。
* 使用定时任务自动处理超时订单和订单状态流转。
* 使用数据库事务保证下单过程中订单、订单明细、购物车等数据的一致性。

---

## 🏗️ System Architecture

```text
                    ┌──────────────────────┐
                    │   Admin Web Portal    │
                    │ 管理端后台系统         │
                    └───────────┬──────────┘
                                │
                    ┌───────────▼──────────┐
                    │      REST APIs        │
                    │   Spring Boot Layer   │
                    └───────────┬──────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐     ┌────────▼────────┐     ┌────────▼────────┐
│ Authentication │     │ Business Logic  │     │ Realtime Notify │
│ JWT / RBAC     │     │ Order / Menu    │     │ WebSocket       │
└───────┬────────┘     └────────┬────────┘     └────────┬────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
                    ┌───────────▼──────────┐
                    │     Data Layer        │
                    │ MySQL / Redis / OSS   │
                    └───────────┬──────────┘
                                │
                    ┌───────────▼──────────┐
                    │  WeChat Mini Program  │
                    │ 用户端微信小程序       │
                    └──────────────────────┘
```

---

## 🛠️ Tech Stack

| Layer             | Technologies                          |
| ----------------- | ------------------------------------- |
| Backend           | Spring Boot, Spring MVC, MyBatis      |
| Database          | MySQL                                 |
| Cache             | Redis                                 |
| Authentication    | JWT                                   |
| Realtime          | WebSocket                             |
| API Documentation | Swagger / Knife4j                     |
| File Storage      | Object Storage Service                |
| Client Side       | Admin Web Portal, WeChat Mini Program |
| Deployment        | Docker                                |

---

## 📦 Project Structure

```text
cloudbite-engine
├── cloudbite-common
│   ├── constant
│   ├── context
│   ├── exception
│   ├── json
│   ├── properties
│   └── result
│
├── cloudbite-pojo
│   ├── entity
│   ├── dto
│   └── vo
│
├── cloudbite-server
│   ├── controller
│   │   ├── admin
│   │   └── user
│   ├── service
│   ├── mapper
│   ├── config
│   ├── interceptor
│   ├── task
│   └── websocket
│
└── README.md
```

---

## 🔥 Feature Showcase

### Admin Portal

```text
Employee Management  →  Category Management  →  Dish Management
        ↓                       ↓                      ↓
Role Control          Set Meal Management      Order Processing
        ↓                       ↓                      ↓
Business Analytics    Realtime Notification    Data Dashboard
```

### User Ordering Flow

```text
Browse Dishes
     ↓
Add to Cart
     ↓
Submit Order
     ↓
Payment Simulation
     ↓
Order Tracking
     ↓
Realtime Status Update
```

---

## ⚙️ Backend Design

### Request Lifecycle

```text
Client Request
     ↓
JWT Interceptor
     ↓
Controller
     ↓
Service
     ↓
Mapper
     ↓
MySQL / Redis
     ↓
Response VO
```

### Order Creation Transaction

```text
Create Order
     ↓
Insert Order Record
     ↓
Insert Order Details
     ↓
Clear Shopping Cart
     ↓
Update Order Status
```

This flow is wrapped in a database transaction to ensure data consistency.

---

## 🌐 API Design

Example API groups:

```text
/admin/employee
/admin/category
/admin/dish
/admin/setmeal
/admin/order
/admin/report

/user/shop
/user/dish
/user/cart
/user/order
/user/addressBook
```

The APIs follow RESTful design principles and separate admin-side workflows from user-side workflows.

---

## 📊 Business Analytics

CloudBite Engine supports operational data analysis, including:

* Daily revenue
* Order volume
* Valid order rate
* Top-selling dishes
* User growth
* Sales trend analysis

中文支持的数据统计包括：

* 每日营业额
* 订单数量
* 有效订单率
* 热销菜品
* 用户增长
* 销售趋势分析

---

## 🧩 What I Learned

Through this project, I practiced:

* Designing layered Spring Boot applications.
* Building secure authentication with JWT.
* Using Redis for backend performance optimization.
* Handling real-time communication with WebSocket.
* Managing complex order workflows with database transactions.
* Writing maintainable RESTful APIs.
* Structuring backend code for real-world business systems.

通过这个项目，我重点练习了：

* Spring Boot 分层架构设计。
* 基于 JWT 的登录认证。
* 使用 Redis 优化高频数据访问。
* 使用 WebSocket 实现实时订单通知。
* 使用数据库事务处理复杂下单流程。
* 设计清晰、可维护的 RESTful API。
* 按照真实业务系统组织后端代码结构。

---

## 📌 Future Improvements

* Add Docker Compose for one-command local deployment.
* Add unit tests and integration tests with JUnit and Testcontainers.
* Add Kafka-based asynchronous order notification.
* Add recommendation system for personalized dish ranking.
* Add CI/CD pipeline with GitHub Actions.
* Add role-based permission management with finer-grained access control.
* Add monitoring dashboard using Prometheus and Grafana.

---

## 中文后续优化方向

* 使用 Docker Compose 实现一键本地部署。
* 使用 JUnit 和 Testcontainers 增加单元测试与集成测试。
* 引入 Kafka 实现异步订单通知。
* 增加个性化菜品推荐系统。
* 使用 GitHub Actions 搭建 CI/CD 流水线。
* 增加更细粒度的 RBAC 权限管理。
* 使用 Prometheus 和 Grafana 增加系统监控面板。

---

## 📄 License

This project is for learning, backend engineering practice, and portfolio demonstration.

本项目用于学习、后端工程实践和作品集展示。

````
