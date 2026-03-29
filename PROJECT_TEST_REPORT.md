# PROJECT_TEST_REPORT

## 测试时间

- 日期：2026-03-29
- 时区：Europe/London

## 测试环境

- 操作系统：Windows
- Shell：PowerShell
- JDK：17.0.12
- Maven：项目自带 `mvnw`
- Docker CLI：当前机器未安装或不在 PATH，`docker` 命令不可用
- 宿主机数据库：存在本地 `MySQL` 服务，占用 `3306`
- Qwen 环境变量：当前 shell 中未提供 `DASHSCOPE_API_KEY` / `QWEN_BASE_URL` / `QWEN_MODEL`

## 测试范围

- 本地构建与测试
- 本地默认 profile (`local`) 启动与接口验证
- `prod` profile 启动与接口/profile 行为验证
- 鉴权 / 审计 / actuator / 异常路径验证
- Docker 配置与环境可执行性排查
- Qwen 环境变量缺失时的实际行为验证

## 执行过的关键命令

```powershell
java -version
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd -DskipTests package
.\mvnw.cmd test "-Dtest=MockIntentClientTest,ProdProfileStaticResourceIntegrationTest,ApiKeyAuthIntegrationTest,AiTicketHandleIntegrationTest"

cmd /c java -jar target\ai-ticket-service-0.0.1-SNAPSHOT-exec.jar
curl.exe -i http://127.0.0.1:8080/swagger-ui.html
curl.exe -i http://127.0.0.1:8080/h2-console
curl.exe -i http://127.0.0.1:8080/tickets/1
curl.exe -i http://127.0.0.1:8080/actuator/metrics
curl.exe -i http://127.0.0.1:8080/actuator/metrics -H "X-API-Key: local-dev-key"

cmd /c set SPRING_PROFILES_ACTIVE=prod && ... && java -jar target\ai-ticket-service-0.0.1-SNAPSHOT-exec.jar
curl.exe -i http://127.0.0.1:8081/swagger-ui.html
curl.exe -i http://127.0.0.1:8081/h2-console
curl.exe -i http://127.0.0.1:8081/actuator/metrics
curl.exe -i http://127.0.0.1:8081/actuator/metrics -H "X-API-Key: prod-key"

docker --version
docker compose config
netstat -ano | Select-String ':3306'
Get-Process -Id 7004
Get-Service | Where-Object { $_.Name -like '*mysql*' -or $_.DisplayName -like '*MySQL*' }
```

## 通过项

- `.\mvnw.cmd test` 全量通过，当前共 `29` 个测试，`0` 失败。
- `.\mvnw.cmd -DskipTests package` 通过，产物成功生成。
- 默认 `local` profile 可正常启动。
- `local` 下 Swagger 可访问，返回 `302` 到 Swagger UI 资源。
- `local` 下 H2 Console 可访问，返回 `302` 到 `/h2-console/`。
- `local` 下 `/tickets/**` 未带鉴权头返回 `401`。
- `local` 下 Ticket 基本流程通过：
  - 创建工单 `200`
  - 查询工单 `200`
  - 关闭工单 `200`
- `local` 下非法工单创建请求返回 `400`，响应体与状态码一致。
- `local` 下 AI 接口通过 UTF-8 请求文件验证成功：
  - `CREATE_TICKET`
  - `QUERY_TICKET`
- `local` 下 `/actuator/health` 返回 `200`。
- `local` 下 `/actuator/metrics` 未鉴权 `401`，带鉴权 `200`。
- `prod` profile 在 H2 覆盖数据源条件下可正常启动。
- `prod` 下 Swagger / H2 Console 已关闭，并且访问返回已收敛为 `404`，不再是 `500`。
- `prod` 下 `/actuator/health` 返回 `200`。
- `prod` 下 `/actuator/metrics` 未鉴权 `401`，带鉴权 `200`。
- `prod` 下 `/tickets/1` 未鉴权 `401`，带鉴权后按业务返回 `400`（工单不存在）。
- `prod` 下 AI 主流程在 `APP_AI_PROVIDER=mock` 时可用：
  - 创建工单 `CREATE_TICKET`
  - 查询工单 `QUERY_TICKET`
- 审计日志存在，包含：
  - `method`
  - `path`
  - `status`
  - `durationMs`
  - `caller`
- Qwen 环境变量缺失时，应用侧返回通用 `500`，不会把 secret 直接回显给客户端。

## 失败项 / 阻塞项

- `.\mvnw.cmd clean package` 在当前机器上失败过一次，原因是旧的 `target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar` 被外部 Java 进程占用，无法删除。
- Docker 相关实际执行全部被当前环境阻塞：
  - `docker` 命令不存在
  - `docker compose config` 无法执行
  - `docker compose up -d` / `ps` / `logs` / healthcheck 无法在当前机器做实机验证
- Qwen 真实外部 API 当前无法重新做 live 验证，因为本次测试环境中没有提供：
  - `DASHSCOPE_API_KEY`
  - `QWEN_BASE_URL`
  - `QWEN_MODEL`

## 已修复问题

### 1. prod 下 Swagger / H2 访问返回 500

- 现象：
  - `prod` 下访问 `/swagger-ui.html`、`/h2-console` 返回 `500`
- 根因：
  - 资源不存在时抛出 `NoResourceFoundException`
  - `GlobalExceptionHandler` 之前统一把所有 `Exception` 都转成了 `500`
- 修复：
  - 在 `GlobalExceptionHandler` 中新增 `NoResourceFoundException` 的 `404` 处理
- 修复后验证：
  - `/swagger-ui.html` -> `404`
  - `/h2-console` -> `404`
  - 响应体为 `{"success":false,"message":"Not Found","data":null}`

### 2. MockIntentClient 对正常中文关键词识别不稳定

- 现象：
  - 本地 AI 验收中，正常中文请求未稳定识别 `创建/查询/关闭`
- 根因：
  - 旧 mock 关键词里混入了历史编码异常字符串
  - 对正常中文输入兼容不足
- 修复：
  - 重写 `MockIntentClient` 的关键词匹配
  - 同时兼容正常中文、历史乱码关键词和英文关键字
  - 采用 Unicode escape 规避源文件编码问题
- 修复后验证：
  - 新增 `MockIntentClientTest`
  - `local` 实际启动后，使用 UTF-8 JSON 文件请求，AI 创建 / 查询均返回正确 intent

### 3. Docker Compose 对宿主机 MySQL 端口冲突不友好

- 现象：
  - 当前宿主机 `3306` 已被本地 `MySQL` 占用
  - 原始 compose 把宿主机 `3306` 写死为 `3306:3306`
- 根因：
  - Compose 配置缺少可配置宿主机端口
- 修复：
  - `docker-compose.yml` 改为可配置端口：
    - `APP_HOST_PORT`
    - `MYSQL_HOST_PORT`
  - `.env.example` 默认将 `MYSQL_HOST_PORT` 设为 `3307`
- 修复后验证：
  - 静态检查确认 compose 已使用变量端口
  - 但当前环境无 Docker CLI，无法完成 `compose up` 实机验证

## 未修复问题

### 1. Docker 无法实机验证

- 复现步骤：
  - 执行 `docker --version`
  - 执行 `docker compose config`
- 结果：
  - 当前环境提示 `docker` 命令不存在
- 根因：
  - 当前测试机未安装 Docker，或 Docker 不在 PATH
- 影响：
  - 无法在当前机器完成 `compose config / up / ps / logs / health` 的真实验收
- 建议：
  - 在具备 Docker Desktop / Docker Engine 的机器上复测
  - 优先使用已修复后的 `MYSQL_HOST_PORT=3307`

### 2. Qwen 真实 live 链路本轮无法复测

- 复现步骤：
  - 检查当前 shell 环境变量
- 结果：
  - `DASHSCOPE_API_KEY=False`
  - `QWEN_BASE_URL=` 空
  - `QWEN_MODEL=` 空
- 根因：
  - 当前环境未注入真实 Qwen 配置
- 影响：
  - 无法在本轮做真实外部 AI 服务回归
- 建议：
  - 在注入真实环境变量后，执行最终联调脚本中的 Qwen 验收步骤

### 3. `clean package` 受外部文件锁影响

- 复现步骤：
  - 执行 `.\mvnw.cmd clean package`
- 结果：
  - 清理 `target/ai-ticket-service-0.0.1-SNAPSHOT-exec.jar` 失败
- 根因：
  - 外部 Java 进程持有 jar 文件句柄
- 影响：
  - Windows 下 `clean` 可能偶发失败
- 建议：
  - 打包前确保没有旧 jar 在运行
  - 如需进一步治理，可补充启动/停止脚本，避免遗留 Java 进程占用产物

## 每个问题的复现步骤 / 根因分析 / 修复建议

### A. prod Swagger/H2 返回 500

1. 使用 `prod` profile 启动应用。
2. 访问：
   - `/swagger-ui.html`
   - `/h2-console`
3. 旧行为：返回 `500`
4. 根因：`NoResourceFoundException` 被全局 `Exception` handler 吞成 `500`
5. 修复建议：对资源未找到单独返回 `404`
6. 当前状态：已修复并验证

### B. Docker 端口冲突

1. 检查宿主机 `3306`：
   - `netstat -ano | Select-String ':3306'`
2. 当前结果：
   - `mysqld` 正在监听 `3306`
3. 根因：
   - Compose 原配置写死 `3306:3306`
4. 修复建议：
   - 用 `MYSQL_HOST_PORT` 做宿主机端口参数化
5. 当前状态：已修复配置，但未完成 Docker 实机验证

### C. Qwen 环境变量缺失

1. `prod + qwen + fallback=none` 启动
2. 不提供 `DASHSCOPE_API_KEY`
3. 调用 `/ai/tickets/handle`
4. 当前结果：
   - 应用返回 `500`
   - 响应体是通用错误文案
   - 日志明确提示缺少 `app.qwen.api-key / DASHSCOPE_API_KEY`
5. 评估：
   - 行为合理，未泄露 secret

## 对 prod Swagger/H2 返回 500 的专项说明

- 本轮已完成专项修复。
- 修复前：
  - 访问已关闭的 Swagger/H2 资源时，会走到全局异常处理并被包装成 `500`
- 修复后：
  - `/swagger-ui.html` 返回 `404`
  - `/h2-console` 返回 `404`
  - 响应体为统一 JSON：

```json
{"success":false,"message":"Not Found","data":null}
```

- 回归验证：
  - 单元/集成测试：`ProdProfileStaticResourceIntegrationTest`
  - 实际启动验证：`prod` profile + H2 覆盖数据源，访问真实端口返回 `404`

## 对 Docker 问题的专项说明

- 这次没有“跳过 Docker”，而是做了完整可执行性排查。
- 排查结论分两层：

### 1. 当前测试环境问题

- `docker` 命令不存在
- 没有可用 Docker 服务
- 所以 `docker compose config / up / ps / logs` 无法在这台机器上真正执行

### 2. 项目配置层面的已知问题

- 宿主机本地 `MySQL` 已占用 `3306`
- 原 compose 固定映射 `3306:3306`，即使装了 Docker 也容易直接起不来
- 这个问题已修复为可配置端口：
  - `MYSQL_HOST_PORT`
  - `.env.example` 默认示例为 `3307`

## 修改过的文件

- `src/main/java/com/example/aiticketservice/config/GlobalExceptionHandler.java`
  - 修改目的：将 `NoResourceFoundException` 从 `500` 收敛为 `404`
- `src/main/java/com/example/aiticketservice/client/MockIntentClient.java`
  - 修改目的：修复中文关键词识别，兼容历史乱码关键词
- `src/test/java/com/example/aiticketservice/client/MockIntentClientTest.java`
  - 修改目的：增加正常中文关键词识别回归测试
- `docker-compose.yml`
  - 修改目的：将宿主机端口映射参数化，降低 Docker 端口冲突风险
- `.env.example`
  - 修改目的：给出默认 `APP_HOST_PORT` / `MYSQL_HOST_PORT` 示例
- `README.md`
  - 修改目的：补充 Docker 端口说明
- `src/test/java/com/example/aiticketservice/config/ProdProfileStaticResourceIntegrationTest.java`
  - 修改目的：锁定 prod 下 Swagger/H2 必须返回 `404`，避免回归到 `500`

## 修改后如何验证

- `GlobalExceptionHandler.java`
  - 执行 `ProdProfileStaticResourceIntegrationTest`
  - 实际访问 `/swagger-ui.html`、`/h2-console`，确认 `404`
- `MockIntentClient.java`
  - 执行 `MockIntentClientTest`
  - 本地启动后，用 UTF-8 JSON 文件请求 `/ai/tickets/handle`
- `docker-compose.yml` / `.env.example`
  - 查看 compose 文件是否使用 `${APP_HOST_PORT}` / `${MYSQL_HOST_PORT}`
  - 在具备 Docker 的机器上执行 `docker compose config`

## 最终结论

### 当前已达到的状态

- 代码层面：可编译、可测试、主流程可运行
- 本地默认 profile：通过
- `prod` profile 核心行为：通过
- 鉴权 / 审计 / actuator：通过
- `prod` 下 Swagger/H2 关闭且返回码已修复为 `404`
- AI 主流程在 mock/provider 缺失场景下行为合理

### 当前未完成的部分

- Docker 实机验收：被当前环境缺少 Docker CLI 阻塞
- Qwen 真实 live 回归：被当前环境缺少真实环境变量阻塞
- `clean package` 在 Windows 下仍可能受外部 jar 文件锁影响

### 是否达到可交付状态

- 结论：**基本达到可交付状态，但不建议在未补完 Docker 实机验证和真实 Qwen live 回归前直接作为最终发布结论。**
- 原因：
  - 代码和 profile 主流程已经跑通
  - 关键 prod 返回码问题已修复
  - Docker 和真实 Qwen 的剩余风险，主要来自当前测试环境阻塞，而不是本轮已验证到的代码主逻辑
