# backend

Spring Boot 3.x 后端服务骨架，包名 `com.dream`。当前 M0 只提供可启动工程和探活接口，业务接口在后续里程碑实现。

## 技术栈

- JDK 17+
- Maven
- Spring Boot Web / WebFlux(WebClient) / Validation / Actuator
- MyBatis-Plus + MySQL Connector
- Redis
- OkHttp
- JJWT
- Caffeine
- Lombok

## 本地启动

```bash
cd backend
mvn spring-boot:run
```

启动后访问：

```bash
curl http://localhost:8080/api/health
```

返回统一响应体：

```json
{"code":0,"message":"ok","data":{"status":"UP","service":"dream-backend"}}
```

## 本地配置

敏感配置不要提交到仓库。需要连接本地 MySQL / Redis / AI Key 时：

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

按需修改 `application-local.yml`，该文件已被根 `.gitignore` 忽略。

M1 已接入 MySQL / Redis / JWT。启动前需要按 `../docs/db-schema.sql` 创建库表，或先执行：

```bash
mysql -u dream -p dream < src/main/resources/db/schema.sql
```

本地无微信小程序密钥时，可在 `application-local.yml` 设置 `dream.wx.mock-enabled: true`，再用 mock code 联调：

```bash
curl -X POST http://localhost:8080/api/auth/wxLogin \
  -H 'Content-Type: application/json' \
  -d '{"code":"mock:local-user"}'
```

受保护接口需要携带 JWT：

```bash
curl http://localhost:8080/api/user/me \
  -H 'Authorization: Bearer <token>'
```

参见 `../docs/architecture.md`、`../docs/api-contract.md`、`../docs/db-schema.sql`。
