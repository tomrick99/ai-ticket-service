# AI Ticket Service

一个基于 Spring Boot 的轻量级工单服务示例项目，提供传统 REST 接口与一个简单的 AI 文本处理入口。项目使用内存仓储保存工单，适合用于接口演示、课程实验、功能原型和二次开发练习。

## 项目特性

- 基础工单能力：创建工单、查询工单、关闭工单
- AI 入口：根据自然语言识别意图并执行对应工单操作
- 统一返回结构：所有接口返回统一的 `ApiResponse`
- 参数校验：基于 `jakarta validation`
- 统一异常处理：对校验异常、业务异常进行统一封装
- 零数据库依赖：默认使用内存仓储，启动简单

## 技术栈

- Java 17
- Spring Boot 3.5.0
- Spring Web
- Spring Validation
- Spring Boot Actuator
- Maven

## 项目结构

```text
ai-ticket-service
├─ src/main/java/com/example/aiticketservice
│  ├─ client        # AI 意图识别客户端与 mock 实现
│  ├─ config        # 全局异常处理
│  ├─ controller    # REST 接口层
│  ├─ dto           # 请求/响应对象
│  ├─ entity        # 实体对象
│  ├─ repository    # 仓储接口与内存实现
│  ├─ service       # 业务接口
│  └─ service/impl  # 业务实现
├─ src/main/resources
│  └─ application.yaml
├─ src/test
└─ pom.xml
```

## 核心设计

### 1. 工单服务

项目通过 `TicketService` 提供以下能力：

- 创建工单
- 根据 ID 查询工单
- 根据 ID 关闭工单

工单状态目前包含：

- `OPEN`
- `CLOSED`

### 2. AI 文本处理

`/ai/tickets/handle` 接口接收一段文本，通过 `MockIntentClient` 做关键词意图识别：

- 包含“创建”或“新建” -> `CREATE_TICKET`
- 包含“查询”或“查看” -> `QUERY_TICKET`
- 包含“关闭”或“结束” -> `CLOSE_TICKET`
- 其他情况 -> `UNKNOWN`

当前实现是 mock 版本，便于本地演示。后续可以将 `IntentClient` 替换为真实大模型或第三方 NLP 服务。

### 3. 数据存储

项目当前使用 `InMemoryTicketRepository` 保存数据：

- 数据保存在进程内存中
- 服务重启后数据会丢失
- 适合 demo，不适合生产环境

## 运行环境

- JDK 17+
- Maven 3.9+，或直接使用项目自带的 Maven Wrapper

## 启动方式

### 方式一：使用 Maven Wrapper

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
./mvnw spring-boot:run
```

### 方式二：先打包再运行

```powershell
.\mvnw.cmd clean package
java -jar target/ai-ticket-service-0.0.1-SNAPSHOT.jar
```

### 默认配置

配置文件位于 `src/main/resources/application.yaml`：

```yaml
spring:
  application:
    name: ai-ticket-service

server:
  port: 8080
```

启动后默认访问地址：

```text
http://localhost:8080
```

## 接口说明

### 1. 创建工单

- 方法：`POST`
- 路径：`/tickets`

请求示例：

```json
{
  "title": "登录失败",
  "description": "用户反馈账号无法登录系统"
}
```

返回示例：

```json
{
  "success": true,
  "message": "工单创建成功",
  "data": {
    "id": 1,
    "title": "登录失败",
    "description": "用户反馈账号无法登录系统",
    "status": "OPEN",
    "createTime": "2026-03-27T10:00:00"
  }
}
```

### 2. 查询工单

- 方法：`GET`
- 路径：`/tickets/{id}`

示例：

```text
GET /tickets/1
```

### 3. 关闭工单

- 方法：`PUT`
- 路径：`/tickets/{id}/close`

示例：

```text
PUT /tickets/1/close
```

### 4. AI 处理工单文本

- 方法：`POST`
- 路径：`/ai/tickets/handle`

请求示例 1：创建工单

```json
{
  "text": "请帮我创建一个工单，内容是支付页面打不开"
}
```

请求示例 2：查询工单

```json
{
  "text": "请查询工单 1"
}
```

请求示例 3：关闭工单

```json
{
  "text": "关闭工单 1"
}
```

返回示例：

```json
{
  "success": true,
  "message": "AI处理完成",
  "data": {
    "intent": "QUERY_TICKET",
    "result": {
      "id": 1,
      "title": "AI创建工单",
      "description": "请帮我创建一个工单，内容是支付页面打不开",
      "status": "OPEN",
      "createTime": "2026-03-27T10:05:00"
    }
  }
}
```

## 统一返回格式

所有接口使用统一结构：

```json
{
  "success": true,
  "message": "消息说明",
  "data": {}
}
```

失败时示例：

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

## 常见错误场景

- 请求字段为空：返回 400
- 工单 ID 不存在：返回 400
- AI 查询或关闭时未从文本中提取到数字 ID：返回 400
- 未识别到明确意图：返回成功响应，但 `intent` 为 `UNKNOWN`

## 快速测试

### 使用 curl 创建工单

```bash
curl -X POST "http://localhost:8080/tickets" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"测试工单\",\"description\":\"这是一个测试\"}"
```

### 使用 curl 查询工单

```bash
curl "http://localhost:8080/tickets/1"
```

### 使用 curl 关闭工单

```bash
curl -X PUT "http://localhost:8080/tickets/1/close"
```

### 使用 curl 调用 AI 接口

```bash
curl -X POST "http://localhost:8080/ai/tickets/handle" \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"查询工单 1\"}"
```

## 后续可扩展方向

- 接入 MySQL / PostgreSQL，替换内存仓储
- 增加工单列表、分页、按状态筛选
- 引入真实 AI/NLP 服务替代 `MockIntentClient`
- 增加接口测试与集成测试
- 接入 Swagger / OpenAPI 文档
- 增加鉴权与权限控制

## 说明

当前项目更偏向演示和练习用途。如果后续需要用于实际业务，建议至少补齐以下能力：

- 持久化存储
- 日志与监控
- 接口文档
- 单元测试与集成测试
- 并发与异常场景处理

