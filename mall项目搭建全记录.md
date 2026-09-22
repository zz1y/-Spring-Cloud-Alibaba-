# mall 微服务电商项目 —— 从零搭建全记录

> 本文档记录整个项目**每一步怎么做、为什么这么做、面试怎么讲**。
> 适合：复习、面试前梳理、给面试官讲项目。

---

## 0. 项目是什么？为什么要微服务？

### 0.1 项目一句话介绍

一个基于 **Spring Cloud Alibaba** 的微服务电商系统，包含商品、用户、订单、文件、网关五个服务，实现了从「下单 → 扣库存 → 支付」的完整业务闭环，中间用到了 Nacos、Sentinel、Seata、Redis、MinIO、JWT 等主流中间件。

### 0.2 为什么拆成微服务（面试必问「单体 vs 微服务」）

| 维度 | 单体 | 微服务 |
|---|---|---|
| 部署 | 一个 jar 全打包 | 每个服务独立部署 |
| 扩展 | 只能整体扩 | 哪个服务压力大就扩哪个 |
| 故障隔离 | 一处崩，全挂 | 一个服务挂了不影响别的 |
| 技术栈 | 绑死一套 | 每个服务可自选 |

**大白话**：单体就像一个大超市，什么都放在一起；微服务像商场里的独立店铺，各管各的、独立开门营业。

**面试一句话**：「拆微服务是为了**独立部署、独立扩展、故障隔离**，代价是引入了分布式事务、服务调用、注册中心这些复杂度。」

---

## 1. 环境准备（中间件清单）

| 中间件 | 位置/端口 | 作用 | 启动命令 |
|---|---|---|---|
| JDK 17 | `D:\develop\jdk17` | 运行环境 | — |
| Maven | `D:\develop\Maven` | 构建/依赖管理 | — |
| MySQL 9 | 127.0.0.1:3306（root/123456） | 存数据 | 服务自启 |
| Redis | `D:\Redis`（6379） | 缓存/存 token | `redis-server` |
| Nacos | `D:\Nacos`（8848） | 注册中心 | `startup.cmd -m standalone` |
| Seata Server | `D:\apache-seata-2.5.0-incubating-bin`（8091） | 分布式事务协调者 | `seata-server.bat` |
| MinIO | `D:\MinIO\minio.exe`（9000/9001） | 对象存储 | `minio.exe server D:\minio-data --console-address ":9001"` |

**面试提醒**：Nacos 是「注册中心 + 配置中心」，我们主要用它做**服务注册发现**。记住 8848 端口。

---

## 2. 创建 Maven 多模块 + mall-common

### 怎么做

1. 建一个 Maven 父工程 `mall`（packaging 设为 `pom`）。
2. 父工程里统一管理 Spring Boot 3.5.0、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0 版本。
3. 新建第一个子模块 `mall-common`，写统一的返回体 `Result<T>`。

### 关键代码 —— Result 统一返回

```java
public class Result<T> {
    private Integer code;    // 200 成功，其他失败
    private String message;
    private T data;

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> error(String msg) { ... }
}
```

### 为什么

所有接口返回格式统一（`{"code":200,"message":"成功","data":...}`），前端不用每个接口去适配不同格式。

**面试一句话**：「用 Result 统一返回体，前后端约定好 code/message/data 结构，前端好处理、后端好排查。」

---

## 3. 商品服务 mall-product（CRUD + 分页）

### 怎么做

1. 新建 `mall-product` 模块（端口 8081）。
2. pom 加依赖：web + nacos-discovery + mybatis-plus + mysql + lombok。
3. 写 `Product` 实体类 → `ProductMapper`（继承 `BaseMapper`）→ `ProductService` → `ProductController`。
4. 分页：加 `MybatisPlusConfig`，注册 `PaginationInnerInterceptor` 分页插件。

### 关键点 —— MyBatis-Plus 是什么

MyBatis-Plus 是 MyBatis 的增强，**继承 `BaseMapper` 就不用写 SQL 了**：`getById`、`list`、`save`、`updateById`、`page` 全是现成的。

### 为什么用分页插件

MyBatis-Plus 3.5.9 起，分页插件被拆到单独的 jar（`mybatis-plus-jsqlparser`），**必须额外引入**，否则 `page` 方法不生效。

**面试一句话**：「用 MyBatis-Plus 的 `BaseMapper` 免写 CRUD SQL，分页用 `PaginationInnerInterceptor` 插件，内部是拼 `LIMIT` 语句。」

---

## 4. Redis 缓存

### 怎么做

1. pom 加 `spring-boot-starter-data-redis`。
2. 写 `RedisConfig`，设缓存的 TTL（过期时间）。
3. 商品查询方法上加 `@Cacheable`，商品更新/删除上加 `@CacheEvict`。

### 为什么用缓存 + 三大问题（面试高频）

**为什么**：数据库抗不住高并发读，把热点数据放 Redis（内存），读的速度快几百倍。

**三大问题**：

| 问题 | 是什么 | 怎么解决 |
|---|---|---|
| 缓存穿透 | 查一个**不存在**的数据，每次都打到 DB | 布隆过滤器 / 缓存空值 |
| 缓存击穿 | 一个**热点 key 过期**瞬间，大量请求打 DB | 热点数据不过期 / 加互斥锁 |
| 缓存雪崩 | **大量 key 同时过期**，DB 被打崩 | TTL 加随机值 / 多级缓存 |

**面试一句话**：「用 Redis 缓存热点商品数据，注意处理穿透（空值/布隆过滤器）、击穿（互斥锁/不过期）、雪崩（TTL 加随机）三个问题。」

---

## 5. 用户服务 mall-user（注册登录 + JWT）

### 怎么做

1. 新建 `mall-user`（端口 8082）。
2. **注册**：密码用 **BCrypt** 加密后存库（明文存密码是死罪）。
3. **登录**：校验密码 → 生成 **JWT** token → token 存 Redis。
4. **拦截器** `LoginInterceptor`：请求带 token，拦截器校验合法性，不合法返回 401。

### 关键点 —— 为什么密码要 BCrypt

BCrypt 是**单向加密 + 加盐**：同样的密码每次加密结果都不同，只能「加密后比对」，不能「解密」，数据库泄露也拿不到明文。

### 关键点 —— JWT 是什么

JWT（Json Web Token）是一种**无状态**的登录凭证：把用户信息签名成一个 token 字符串，客户端带着它，服务端解析就能知道「你是谁」，不用在服务端存 session。

### 关键点 —— 为什么 token 还存 Redis

JWT 本身无状态，但要实现「**踢人下线**」就必须让服务端能主动失效 token，所以把 token 存 Redis，删掉就失效。

**面试一句话**：「登录用 JWT 做无状态认证，token 存 Redis 方便主动失效（踢人），密码用 BCrypt 加盐加密，拦截器统一校验 token。」

---

## 6. 网关 mall-gateway

### 怎么做

1. 新建 `mall-gateway`（端口 8080）。
2. pom **只加** `spring-cloud-starter-gateway` + nacos-discovery，**不能加** spring-boot-starter-web。
3. 配置路由：`/product/**` 转到 `mall-product`，`/user/**` 转到 `mall-user`。

### 关键点 —— 为什么不能加 web 依赖

Gateway 是 **WebFlux**（响应式）技术栈，web 依赖是 **MVC**（Servlet）技术栈，两者冲突，同时引会启动报错。

### 路由配置长这样

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-route
          uri: lb://mall-product      # lb = loadbalance，走负载均衡
          predicates:
            - Path=/product/**
```

**面试一句话**：「网关做统一入口 + 路由转发 + 负载均衡，`lb://` 表示通过注册中心负载均衡到具体服务。Gateway 基于 WebFlux，所以不能和 web 依赖共存。」

---

## 7. OpenFeign 服务调用

### 怎么做

1. pom 加三个依赖：`openfeign` + `nacos-discovery` + `loadbalancer`。
2. 启动类加 `@EnableFeignClients`。
3. 写接口，`@FeignClient(name = "mall-product")`，方法上写 `@PostMapping("/product/deductStock")`。

### 关键点 —— 原理（面试必问）

OpenFeign 是**声明式 HTTP 客户端**：你写一个接口 + 注解，它**动态代理**帮你生成实现类，本质就是发 HTTP 请求。

```java
@FeignClient(name = "mall-product")   // 服务名（去 Nacos 查地址）
public interface ProductClient {
    @PostMapping("/product/deductStock")
    Result<String> deductStock(@RequestParam("productId") Long productId,
                               @RequestParam("count") Integer count);
}
```

### 为什么还要 loadbalancer 依赖

Feign 从注册中心拿到的是**一个服务名的多个实例地址**，具体发到哪台要**负载均衡**，所以必须配 loadbalancer，否则报 `No Feign Client for loadBalancing defined`。

**面试一句话**：「OpenFeign 声明式调用，`@FeignClient(name=服务名)` 从 Nacos 拿到地址后由 loadbalancer 负载均衡，底层是动态代理发 HTTP。」

---

## 8. Sentinel 限流

### 怎么做

1. pom 加 `spring-cloud-starter-alibaba-sentinel`。
2. 方法上加 `@SentinelResource(value = "资源名", blockHandler = "降级方法")`。
3. 写 `FlowRule`（QPS 规则），在启动时加载规则。

### 关键点 —— 三个概念（面试高频）

| 概念 | 一句话 |
|---|---|
| 限流 | 超过 QPS 阈值就拒绝（保护系统不被冲垮） |
| 熔断 | 下游一直出错就「断开」，快速失败，不再调它 |
| 降级 | 出问题时返回一个兜底结果（blockHandler 里的方法） |

### blockHandler 方法签名（有讲究）

返回值要和原方法**一致**，且要多一个 `BlockException` 参数。

**面试一句话**：「Sentinel 做限流熔断降级，我项目里给商品列表加 QPS=1 的流控规则，超了就走 blockHandler 返回降级结果。」

---

## 9. Seata 分布式事务（重点）

### 9.1 为什么需要它

下单跨两个服务、两个库（订单库 + 商品库），本地 `@Transactional` 管不到别的库，需要分布式事务保证「订单和库存要么都成功、要么都失败」。

### 9.2 三个角色

| 角色 | 是什么 | 在我项目里 |
|---|---|---|
| TC（协调者） | 独立部署的 Seata Server | `seata-server.bat` |
| TM（发起方） | 加 `@GlobalTransactional` 的 | 订单服务 |
| RM（参与方） | 被调用的分支事务 | 商品服务 |

### 9.3 AT 模式原理

- **一阶段**：执行业务 SQL 时，自动记录 **undo_log**（改之前「前镜像」+ 改之后「后镜像」）。
- **二阶段**：成功 → 异步删 undo_log；失败 → 按 undo_log 的**反向 SQL** 补偿回滚。
- **XID**（全局事务 ID）通过 Feign 拦截器在服务间自动透传。
- **数据源**被 Seata 自动代理，所以记 undo_log 对业务代码零侵入。

### 9.4 核心代码

```java
@GlobalTransactional   // 发起全局事务
public String createOrder(Long productId, Integer count) {
    this.save(order);                              // 写订单
    productClient.deductStock(productId, count);   // Feign 调商品扣库存
    if (count == 3) {
        throw new RuntimeException("模拟失败");      // 触发全局回滚
    }
    return "下单成功";
}
```

### 9.5 踩过的坑（面试可以主动讲，显经验）

1. **XID 自动透传**：`SeataFeignClientAutoConfiguration` 已自动配置 Feign 拦截器，**不要手动加**，否则 bean 冲突。
2. **数据源自动代理**：`SeataAutoDataSourceProxyCreator` 按 AT 模式自动代理，不用自己配。
3. **加依赖必须 Reload Maven**：否则 classpath 没有 seata jar，服务**静默**不初始化 Seata（日志里一条 Seata 都没有），排查了半天。

**面试一句话**：「用 Seata 的 AT 模式做下单扣库存，`@GlobalTransactional` 标记全局事务，靠 undo_log 自动补偿回滚。我特意模拟了「扣完库存后抛异常」的场景，验证了跨服务回滚。」

---

## 10. MinIO 文件服务

### 怎么做

1. 下载 `minio.exe`，启动（9000=API，9001=控制台）。
2. 新建 `mall-file`（端口 8084），pom 加 `io.minio:minio`。
3. 写 `MinioConfig`：创建 `MinioClient` bean（读配置里的地址账号密码）。
4. 写上传接口：`putObject` 上传 → 返回 URL。

### 关键点

| 点 | 说明 |
|---|---|
| 桶（bucket） | MinIO 的顶级文件夹，文件必须放进桶里 |
| UUID 重命名 | 防止文件名重名互相覆盖 |
| 公开读 | 桶默认**私有**，浏览器访问会 403，要 `setBucketPolicy` 设成公开读 |
| 9000 vs 9001 | 9000 是程序用的 API，9001 是人看的控制台 |

### 上传核心代码

```java
minioClient.putObject(PutObjectArgs.builder()
        .bucket(bucket)
        .object(fileName)          // UUID 重命名后的名字
        .stream(file.getInputStream(), file.getSize(), -1)
        .contentType(file.getContentType())
        .build());
```

### 为什么文件不存本地磁盘

微服务多实例：图片存在 A 机器，下次请求路由到 B 机器就找不到。所以要放**公共对象存储**，所有实例都能访问。

**面试一句话**：「用 MinIO 做对象存储存商品图片，上传返回 URL。桶默认私有，图片桶设公开读让浏览器直接访问；敏感文件保持私有或用预签名 URL 临时授权。」

---

## 11. 微信支付（模拟）

### 真实微信支付流程（6 步）

1. 下单 → 订单状态「待支付」
2. 后端调微信「统一下单」
3. 生成支付二维码（code_url）
4. 用户扫码付钱
5. 微信**异步**发「支付回调」通知
6. 后端**验签** → 订单改成「已支付」

### 为什么是模拟

真实微信支付需要**商户号**（要营业执照，学生没有），所以模拟跑通流程，逻辑和真实一致。

### 三个面试考点

| 考点 | 代码体现 | 大白话 |
|---|---|---|
| 状态机 | `if (status != 0) 返回异常` | 只有待支付能支付 |
| 幂等 | `if (status == 1) 返回忽略` | 重复回调不重复处理 |
| 验签 | 注释「真实要验签」 | 防伪造「已付款」回调 |

### 订单状态流转

```
0 待支付 ──支付──▶ 1 已支付
```

**面试一句话**：「支付做了「下单→待支付→统一下单→回调→已支付」的完整闭环，处理了回调幂等（重复回调忽略）和状态机（只有待支付能支付），真实场景回调要验微信签名防伪造。」

---

## 12. 面试速查表（30 秒版）

| 技术 | 一句话话术 |
|---|---|
| 微服务 | 独立部署、独立扩展、故障隔离，代价是分布式事务和调用复杂度 |
| Nacos | 注册中心，服务启动注册、调用时拉取地址 |
| Gateway | 统一入口 + 路由转发，WebFlux 技术栈 |
| OpenFeign | 声明式 HTTP 调用，`@FeignClient` + loadbalancer 负载均衡 |
| Sentinel | 限流（QPS）、熔断、降级 |
| Seata | AT 模式分布式事务，undo_log 自动补偿，`@GlobalTransactional` |
| Redis | 缓存（穿透/击穿/雪崩）+ 存 token |
| JWT | 无状态登录凭证，配合 Redis 实现踢人 |
| BCrypt | 密码加盐单向加密 |
| MinIO | 对象存储，兼容 S3，存图片 |
| 微信支付 | 统一下单 → 二维码 → 异步回调 → 验签改状态，注意幂等 |
| MyBatis-Plus | BaseMapper 免写 CRUD，分页用插件 |

---

## 附：本项目的完整技术栈

```
Spring Boot 3.5.0
Spring Cloud 2025.0.0
Spring Cloud Alibaba 2025.0.0.0
  ├── Nacos（注册中心）
  ├── Sentinel（限流熔断）
  └── Seata（分布式事务）
OpenFeign（服务调用）
Spring Cloud Gateway（网关）
MyBatis-Plus（ORM）
Redis（缓存）
MySQL（数据库）
JWT + BCrypt（认证）
MinIO（对象存储）
微信支付（模拟）
```
