# 接口契约（M0 冻结草案）

- BasePath：`/api`
- 鉴权：除 `wxLogin` 外均需 `Authorization: Bearer <JWT>`
- 统一返回体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

- 错误码约定：`0` 成功；`401` 未登录/Token 失效；`429` 限流/次数用尽；`4xx` 业务错误；`500` 服务异常。
- 解梦结果 `data` 结构以 `docs/dream-result.schema.json` 为唯一事实来源。

## 1. 鉴权

### POST /api/auth/wxLogin
请求：`{ "code": "<wx.login code>" }`
响应 data：`{ "token": "<jwt>", "user": { "id":1, "nickname":"", "avatar":"" } }`

## 2. 解梦

### POST /api/dream/interpret
请求：
```json
{ "dreamText": "梦见一条蛇...", "tags": ["反复出现"], "school": "" }
```
- `school` 表示客户端展示偏好；空 = 展示全部流派，指定 `传统文化` / `心理学` = 客户端优先展示该流派。
- 后端结果仍始终返回双视角，`result.interpretations` 一期必须同时包含 `传统文化` + `心理学`。
响应 data：
```json
{ "dreamRecordId": 10, "dreamResultId": 20, "school": "心理学", "result": { /* dream-result.schema.json */ } }
```
缓存命中且当前用户没有对应历史记录时，后端直接返回解读结果但不持久化，`dreamRecordId` / `dreamResultId` 为 `null`。
限流：按用户/IP；超每日免费次数返回 `429`。

### GET /api/dream/history?page=1&size=20
响应 data：`{ "total": 100, "list": [ { "dreamRecordId":10, "dreamText":"", "summary":"", "createdAt":"", "school":"心理学" } ] }`

### GET /api/dream/{id}
响应 data：`{ "dreamRecord": {...}, "result": { /* schema */ } }`

## 3. 收藏

### POST /api/favorite
请求：`{ "dreamResultId": 20, "action": "add" }`  // add / remove
响应 data：`{ "favorited": true }`

### GET /api/favorite/list?page=1&size=20
响应 data：`{ "total": 5, "list": [ ... ] }`

## 4. 管理端（需管理员鉴权）

### AI 渠道
- `GET    /api/admin/ai/config` 列表
- `POST   /api/admin/ai/config` 新增/更新（含启停、优先级、权重）
- `DELETE /api/admin/ai/config/{id}`
- `POST   /api/admin/ai/config/{id}/test` 连通测试
- `POST   /api/admin/ai/refresh` 刷新配置缓存（evict Caffeine）

### 提示词模板
- `GET  /api/admin/prompt`
- `POST /api/admin/prompt`
- `DELETE /api/admin/prompt/{id}`

### 敏感词
- `GET /api/admin/sensitive`
- `POST /api/admin/sensitive`
- `DELETE /api/admin/sensitive/{id}`

### 解梦记录
- `GET /api/admin/dream/records?page=1&size=20` 查看输入/输出/provider/token/status

> 契约变更需同步本文件与前后端实现；解梦结果结构变更需同步 `dream-result.schema.json`。
