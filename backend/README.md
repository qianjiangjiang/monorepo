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

### 密钥配置

启动时必须显式配置两个互不相同的密钥：

- `dream.jwt.secret` / `DREAM_JWT_SECRET`：JWT 签名密钥。轮换会让已签发 Token 失效。
- `dream.ai.encryption-key` / `DREAM_AI_ENCRYPTION_KEY`：AI 渠道 API Key 的 AES 主密钥。轮换前需要先重加密已有 `ai_provider_config.api_key`，否则旧密文无法解密。

两个值都必须至少 32 字节，不能留空，不能使用示例占位值，也不能彼此相同。本地可用下面的命令分别生成两次：

```bash
openssl rand -base64 32
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

### 管理员授权

管理端复用 `POST /api/auth/wxLogin`。用户首次登录后默认是普通用户，需要由运维按微信 `openid` 标记为管理员：

```sql
UPDATE `user` SET `role` = 'admin' WHERE `openid` = '<openid>';
```

旧库升级需先补充角色列：

```sql
ALTER TABLE `user`
  ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT 'user/admin' AFTER `avatar`;
```

普通用户 Token 访问 `/api/admin/**` 会返回 403；管理员重新登录拿到带 `role=admin` 的 JWT 后可访问管理端接口。

参见 `../docs/architecture.md`、`../docs/api-contract.md`、`../docs/db-schema.sql`。
