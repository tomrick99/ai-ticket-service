# AI Ticket Service

基于 Java 17 / Spring Boot 3.x 的轻量工单服务示例，当前已具备：

- Ticket CRUD 接口
- AI 文本入口 `/ai/tickets/handle`
- Mock / Qwen 可切换 provider
- H2 本地开发与 MySQL 持久化
- Flyway 最小迁移
- Docker / Docker Compose 编排
- 基础 API Key 鉴权、审计日志与 Actuator

## 技术栈

- Java 17
- Spring Boot 3.5.0
- Spring Web / Validation / Data JPA / Actuator / WebFlux
- H2 / MySQL
- Flyway
- springdoc OpenAPI
- Maven Wrapper

## 快速开始

### 1. 本地默认启动

默认 profile 是 `local`，使用 H2 内存库：

```powershell
.\mvnw.cmd spring-boot:run
```

本地默认能力：

- H2 Console：`http://localhost:8080/h2-console`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI：`http://localhost:8080/v3/api-docs`
- Actuator Health：`http://localhost:8080/actuator/health`

### 2. 本地调用受保护接口

当前采用固定 API Key Header 鉴权。

默认请求头：

- 鉴权头：`X-API-Key`
- 调用方标识头：`X-Client-Id`

`local` profile 下默认示例 key：

```text
local-dev-key
```

创建工单示例：

```powershell
curl -X POST http://localhost:8080/tickets `
  -H "Content-Type: application/json" `
  -H "X-API-Key: local-dev-key" `
  -H "X-Client-Id: local-script" `
  -d "{\"title\":\"本地工单\",\"description\":\"用于本地验证\"}"
```

AI 路径示例：

```powershell
curl -X POST http://localhost:8080/ai/tickets/handle `
  -H "Content-Type: application/json" `
  -H "X-API-Key: local-dev-key" `
  -H "X-Client-Id: local-script" `
  -d "{\"text\":\"请帮我创建一个测试工单\"}"
```

## Profile 说明

### `local`

- 默认启用
- 数据库使用 H2
- H2 Console 开启
- Swagger / OpenAPI 开启
- Health 详情可见
- 默认 `APP_SECURITY_API_KEY=local-dev-key`

### `mysql`

- 只切换数据源到 MySQL
- 需要通过环境变量提供数据源账号密码
- Flyway 启动时自动执行迁移

### `prod`

- 自动包含 `mysql`
- H2 Console 关闭
- Swagger / springdoc 关闭
- 错误信息和日志暴露面更小
- Actuator 只暴露 `health`、`info`、`metrics`
- `APP_SECURITY_API_KEY` 需要显式提供，否则受保护接口会返回 `503`

## MySQL 启动

示例：

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/ai_ticket_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
$env:SPRING_DATASOURCE_USERNAME="ticket_user"
$env:SPRING_DATASOURCE_PASSWORD="changeit"
.\mvnw.cmd spring-boot:run
```

生产推荐：

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/ai_ticket_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
$env:SPRING_DATASOURCE_USERNAME="ticket_user"
$env:SPRING_DATASOURCE_PASSWORD="changeit"
$env:APP_SECURITY_API_KEY="replace-with-app-key"
$env:DASHSCOPE_API_KEY="replace-with-real-key"
$env:QWEN_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:QWEN_MODEL="qwen-plus"
java -jar target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar
```

## Qwen Provider

切换到真实 Qwen：

```powershell
$env:APP_AI_PROVIDER="qwen"
$env:APP_AI_FALLBACK="none"
$env:DASHSCOPE_API_KEY="replace-with-real-key"
$env:QWEN_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:QWEN_MODEL="qwen-plus"
```

保留本地 mock：

```powershell
$env:APP_AI_PROVIDER="mock"
```

## Actuator 与审计

当前保留的 Actuator 端点：

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

访问策略：

- `/actuator/health` 公开
- 其余 actuator 端点默认需要 `X-API-Key`
- `/tickets/**` 与 `/ai/tickets/**` 默认需要 `X-API-Key`

审计日志会记录：

- 请求时间
- 请求路径
- HTTP 方法
- 响应状态码
- 调用耗时
- 调用方标识（优先读取 `X-Client-Id`）
- 请求 ID

日志不会主动记录：

- 完整 API key
- 数据库密码
- 外部 provider 原始敏感片段

## Docker

### Dockerfile

项目包含：

- `Dockerfile`
- `.dockerignore`

镜像特性：

- 多阶段构建
- 默认 `SPRING_PROFILES_ACTIVE=prod`
- 启动后通过 `/actuator/health` 做容器健康检查
- Flyway 随应用启动自动执行

构建镜像：

```powershell
docker build -t ai-ticket-service:latest .
```

### Docker Compose

项目包含：

- `docker-compose.yml`
- `.env.example`

Compose 服务：

- `app`
- `mysql`

使用步骤：

```powershell
Copy-Item .env.example .env
docker compose build
docker compose up -d
```

如需本机覆盖端口或挂载调试配置，建议新增未提交的 `docker-compose.override.yml`。

## 关键环境变量

### 应用鉴权与审计

- `APP_SECURITY_ENABLED`
- `APP_SECURITY_API_KEY`
- `APP_SECURITY_HEADER_NAME`
- `APP_SECURITY_CLIENT_ID_HEADER`
- `APP_AUDIT_ENABLED`

### MySQL

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_DATABASE`

### Qwen

- `DASHSCOPE_API_KEY`
- `QWEN_BASE_URL`
- `QWEN_MODEL`
- `APP_AI_PROVIDER`
- `APP_AI_FALLBACK`

## 测试

运行全部测试：

```powershell
.\mvnw.cmd test
```

## 当前状态

已完成：

- H2 / MySQL 双配置
- Flyway 最小迁移
- Docker / Compose 基础编排
- Qwen provider 接入
- API Key 鉴权
- 审计日志
- Actuator health / metrics
- 生产 profile 收紧

待最后阶段统一执行：

- Docker / Compose 实机验证
- 生产环境联调验证
- 重启后持久化验证
- 鉴权、审计和日志脱敏实机检查
