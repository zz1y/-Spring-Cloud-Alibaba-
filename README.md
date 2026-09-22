# mall 微服务电商系统

基于 **Spring Cloud Alibaba** 的微服务电商项目，采用前后端分离 + 微服务架构，实现商品、用户、订单、文件、网关五大独立服务，打通「注册登录 → 商品浏览 → 下单 → 扣库存 → 支付」的完整电商业务链路。

## 技术栈

- **核心框架**：Spring Boot 3.5、Spring Cloud 2025、Spring Cloud Alibaba 2025
- **注册中心**：Nacos
- **网关**：Spring Cloud Gateway
- **服务调用**：OpenFeign + LoadBalancer
- **服务治理**：Sentinel（限流 / 熔断 / 降级）
- **分布式事务**：Seata（AT 模式）
- **持久层**：MyBatis-Plus + MySQL
- **缓存**：Redis
- **认证**：JWT + BCrypt
- **对象存储**：MinIO
- **支付**：微信支付（模拟）

## 系统架构

| 服务 | 端口 | 功能 | 技术亮点 |
|---|---|---|---|
| mall-gateway | 8080 | 网关统一入口 | 路由转发 + 负载均衡 |
| mall-product | 8081 | 商品 CRUD / 搜索分页 | Redis 缓存 + Sentinel 限流 |
| mall-user | 8082 | 注册 / 登录 | BCrypt + JWT + 拦截器鉴权 |
| mall-order | 8083 | 下单 / 支付 | Seata 分布式事务 + OpenFeign |
| mall-file | 8084 | 图片上传 | MinIO 对象存储 |
| mall-common | — | 公共模块 | Result 统一返回 |

## 核心亮点

- **分布式事务**：Seata AT 模式实现「下单 + 扣库存」跨服务一致性，异常时订单和库存一起回滚
- **服务治理**：Nacos 注册发现 + Gateway 统一网关 + OpenFeign 声明式调用 + Sentinel 限流降级
- **高并发缓存**：Redis 缓存商品热点数据，处理缓存穿透 / 击穿 / 雪崩
- **安全认证**：BCrypt 密码加密 + JWT 无状态登录 + 拦截器统一鉴权
- **对象存储**：MinIO 存商品图片，桶公开读 + UUID 防重名
- **支付闭环**：微信支付（模拟）跑通「待支付 → 支付 → 回调 → 已支付」，处理回调幂等

## 环境要求

| 中间件 | 版本 / 端口 | 说明 |
|---|---|---|
| JDK | 17 | |
| Maven | 3.9+ | |
| MySQL | 9（3306，root/123456） | 库名 `mall` |
| Redis | 6379 | |
| Nacos | 8848 | `startup.cmd -m standalone` 单机启动 |
| Seata Server | 2.5.0（8091） | registry 注册到 Nacos |
| MinIO | 9000 / 9001 | `minio.exe server D:\minio-data --console-address ":9001"` |

## 快速开始

**1. 启动中间件**（依次启动 Nacos、MySQL、Redis、Seata Server、MinIO）

**2. 初始化数据库**：创建 `mall` 库，建表 `product`、`t_user`、`t_order`、`undo_log`（undo_log 供 Seata AT 模式回滚使用）

**3. 按顺序启动服务**：mall-product → mall-user → mall-order → mall-file → mall-gateway

**4. 测试核心链路**：

```
下单（触发扣库存，count=3 触发分布式事务回滚）：
POST http://localhost:8083/order/create?productId=1&count=1

支付（待支付 → 已支付）：
POST http://localhost:8083/order/pay?orderId=1
POST http://localhost:8083/order/pay/callback?orderId=1

图片上传：
curl -X POST -F "file=@图片路径" http://localhost:8084/file/upload
```

## 项目结构

```
mall
├── mall-common      # 公共模块（Result 统一返回）
├── mall-product     # 商品服务（8081）
├── mall-user        # 用户服务（8082）
├── mall-order       # 订单服务（8083）
├── mall-file        # 文件服务（8084）
├── mall-gateway     # 网关（8080）
└── pom.xml          # 父工程（版本管理）
```

## 说明

- 微信支付为**模拟实现**：真实支付需商户号（营业执照），本项目跑通完整流程，逻辑与真实一致
- 项目为学习 / 求职用途，配置中密码为本地默认值，正式环境应改用环境变量或配置中心
