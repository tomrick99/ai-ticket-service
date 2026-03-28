# AI Ticket Service

基于 Java 17 / Spring Boot 3.x 的轻量工单服务示例，提供：

- 传统 Ticket CRUD 接口
- AI 文本处理入口 `/ai/tickets/handle`
- 默认 H2 本地运行
- 可选 MySQL profile
- Swagger / OpenAPI 文档
- Docker / Docker Compose 启动方式

当前阶段目标是先把接口、持久化、AI 路径、文档和容器化基础打稳，不接入真实 API key，也不改现有接口契约。

## 技术栈

- Java 17
- Spring Boot 3.5.0
- Spring Web
- Spring Validation
- Spring Data JPA
- H2 Database
- MySQL Connector/J
- springdoc OpenAPI / Swagger UI
- Maven

## 运行前要求

### 1. JDK 17

项目以 **Java 17** 为准。

`pom.xml` 已显式使用：

- `<java.version>17</java.version>`
- `maven-compiler-plugin` 的 `<release>17</release>`

如果你在命令行里使用 Maven Wrapper，建议先确认 `JAVA_HOME` 也指向 JDK 17，因为 `mvnw.cmd` 会优先使用 `JAVA_HOME`。

Windows PowerShell 示例：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd -version
```

### 2. Maven Wrapper

项目自带 Maven Wrapper，无需单独安装 Maven：

```powershell
.\mvnw.cmd -version
```

## 本地运行

### 默认 H2 启动

默认 profile 使用 H2 内存库，适合本地开发和测试：

```powershell
.\mvnw.cmd spring-boot:run
```

启动后可访问：

- 应用：`http://localhost:8080`
- H2 Console：`http://localhost:8080/h2-console`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

默认 H2 配置位于：

- `src/main/resources/application.yaml`

其中保留了当前默认行为：

- H2 仍是默认数据源
- 现有测试默认仍走 H2
- 不需要额外数据库即可启动

### 打包运行

执行：

```powershell
.\mvnw.cmd -DskipTests package
```

当前可执行 jar 输出为：

```text
target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar
```

运行方式：

```powershell
java -jar target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar
```

之所以使用 `-exec.jar`，是为了避开 Windows 下 Spring Boot `repackage` 对主 jar 重命名时的锁文件问题。

## MySQL Profile

项目新增了：

- `src/main/resources/application-mysql.yaml`

启用方式：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=mysql --spring.datasource.url=jdbc:mysql://localhost:3306/ai_ticket_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8 --spring.datasource.username=root --spring.datasource.password=changeit"
```

也可以在 jar 方式下使用：

```powershell
java -jar target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar --spring.profiles.active=mysql --spring.datasource.url=jdbc:mysql://localhost:3306/ai_ticket_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8 --spring.datasource.username=root --spring.datasource.password=changeit
```

MySQL profile 特点：

- 默认不会生效，只有显式激活 `mysql` profile 才切换
- 数据源账号密码通过环境变量或启动参数注入
- `ddl-auto` 当前配置为 `update`
- 默认 H2 配置不受影响

`application-mysql.yaml` 中的数据源占位符：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Swagger / OpenAPI

已接入 springdoc，并补齐当前主要接口的基础注解。

文档入口：

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

当前重点展示的接口：

- `POST /tickets`
- `GET /tickets/{id}`
- `PUT /tickets/{id}/close`
- `POST /ai/tickets/handle`

说明：

- 没有修改现有 Controller 路径
- 没有修改请求字段名
- 没有修改响应结构

## Docker

### Dockerfile

项目已提供：

- `Dockerfile`
- `.dockerignore`

构建镜像：

```powershell
docker build -t ai-ticket-service .
```

默认 H2 运行：

```powershell
docker run --rm -p 8080:8080 ai-ticket-service
```

### Docker Compose

项目已提供 `docker-compose.yml`，包含：

- `app`
- `mysql`

启动：

```powershell
docker compose up --build
```

Compose 默认行为：

- `app` 使用 `mysql` profile
- `mysql` 使用可修改的示例账号密码
- `APP_QWEN_API_KEY` 通过环境变量注入，不写死在镜像或配置文件里

主要环境变量：

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `APP_QWEN_API_KEY`

## 环境变量

### 数据库

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### AI / Qwen

- `APP_QWEN_API_KEY`
- `APP_AI_PROVIDER`

当前默认仍然是 mock provider，适合本地和无网环境。

## 测试

运行全部测试：

```powershell
.\mvnw.cmd test
```

如果你的 `JAVA_HOME` 还没切到 17，建议先设置：

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

## 验证建议

### 1. 默认 H2 启动验证

```powershell
.\mvnw.cmd spring-boot:run
```

检查：

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

### 2. MySQL profile 验证

先准备 MySQL，再执行：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=mysql --spring.datasource.url=jdbc:mysql://localhost:3306/ai_ticket_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8 --spring.datasource.username=root --spring.datasource.password=changeit"
```

### 3. Docker Compose 验证

```powershell
docker compose up --build
```

## 当前已完成

- 默认 H2 本地运行
- Ticket CRUD + JPA
- AI 路径与测试
- Swagger / OpenAPI
- MySQL profile
- Docker / Docker Compose 基础支持
- Java 17 编译目标对齐
- Windows 下 `package` 产物锁问题规避

## 当前仍未完成

- 真实 Qwen / 其他 AI 提供方联调
- 生产级 MySQL 部署参数
- Docker / Compose 在当前机器上的实际运行验证
- 更完整的鉴权、审计、监控和生产配置
