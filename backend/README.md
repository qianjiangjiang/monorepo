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

> M0 为保证空骨架无需本地数据库即可启动，入口类暂时排除了 `DataSourceAutoConfiguration`。后续接入真实 Mapper / 数据库时移除该排除并启用本地配置。

参见 `../docs/architecture.md`、`../docs/api-contract.md`、`../docs/db-schema.sql`。
