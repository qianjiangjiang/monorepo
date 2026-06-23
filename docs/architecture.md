# 架构与模块划分（M0）

## 总览

```
微信小程序(miniapp/uni-app) ──┐
管理端(admin/Vue3)         ──┼──► 后端 REST(backend/Spring Boot) ──► MySQL / Redis
                              │            │
                              │            └──► AI 适配层 ──► DeepSeek(主) / 兜底渠道
```

## backend/（Spring Boot 3.x）

```
backend/
└─ src/main/java/com/dream/
   ├─ controller/   REST 接口
   ├─ service/
   │   └─ ai/       AI 适配层（策略模式）
   │       ├─ AiProvider.java          接口
   │       ├─ DeepSeekAdapter.java     主力
   │       ├─ <Fallback>Adapter.java   兜底
   │       └─ AiProviderRouter.java    选渠道 + 主备降级
   ├─ config/       Caffeine 缓存 + 配置热加载
   ├─ domain/       实体（MyBatis-Plus）
   ├─ mapper/       DAO
   └─ common/       统一返回/异常/JWT/加密/敏感词
```

技术栈：Spring Boot 3.x + MyBatis-Plus + MySQL 8 + Redis + JWT + WebClient/OkHttp。

### AI 配置热加载
启动加载 `ai_provider_config` / `prompt_template` 进 Caffeine 缓存；管理端改动后调 `/api/admin/ai/refresh` 主动 evict，实现运行时可配置、零新增中间件。

### 结构化输出保障链
JSON mode 调用 → JSON Schema 校验 → 失败一次「修复重试」→ 仍失败降级为安全占位结构（绝不抛异常给前端）。

## miniapp/（uni-app + Vue3 + TS）

- Pinia 状态、uview-plus 组件、lottie 动效、canvas 海报。
- 主题 A 神秘星空：深紫渐变背景 + 玻璃拟态卡 + 金色点缀。
- 页面：首页/输入、Loading、结果、历史、详情、我的。

## admin/（Vue3 + Element-Plus）

- AI 渠道配置、提示词模板、敏感词、解梦记录查看、刷新缓存、管理员登录。

## 契约事实来源

- 解梦结果结构：`docs/dream-result.schema.json`
- 接口：`docs/api-contract.md`
- 数据库：`docs/db-schema.sql`
