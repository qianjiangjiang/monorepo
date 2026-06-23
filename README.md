# 解梦微信小程序 Monorepo

AI 解梦微信小程序：用户输入梦境，AI 返回**结构化解梦报告**（象征解析 + 情绪 + 传统/心理双视角 + 行动建议），支持历史、收藏、分享。AI 模型 / 提示词 / 参数**运行时可配置**。

## 已锁定决策

- **前端**：uni-app + Vue3 + TS + Pinia + uview-plus，编译为微信小程序，主题 **A 神秘星空** 🌌
- **后端**：Spring Boot 3.x + MyBatis-Plus + MySQL + Redis
- **AI**：默认主力 **DeepSeek**，多渠道可配置 + 兜底降级，JSON 结构化输出
- **解读流派**：一期含「传统文化 + 心理学」双视角
- **管理端**：一期含可视化管理端（AI 渠道 / 提示词 / 敏感词 / 解梦记录）

## 仓库结构

```
.
├── backend/    Spring Boot 后端服务（API + AI 引擎 + 配置热加载）
├── miniapp/    uni-app 微信小程序前端
├── admin/      可视化管理端（Vue3 + Element-Plus）
└── docs/       契约与设计文档（M0 冻结）
    ├── architecture.md           架构与模块划分
    ├── api-contract.md           接口契约
    ├── dream-result.schema.json  解梦结果 JSON Schema（前后端共用）
    └── db-schema.sql             数据库 DDL
```

## 文档

- 调研文档与实现计划：见 Multica issue **WS-1**
- 契约以 `docs/` 为准，变更需同步更新前后端

## 开发约定

- 后端、前端、管理端在各自子目录独立构建
- 解梦结果结构以 `docs/dream-result.schema.json` 为唯一事实来源
- 接口以 `docs/api-contract.md` 为准

## M0 启动入口

```bash
# backend
cd backend
mvn spring-boot:run

# miniapp
cd miniapp
npm install
npm run dev:mp-weixin

# admin
cd admin
npm install
npm run dev
```
