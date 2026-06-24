# admin

Vue3 + Vite + TypeScript + Element Plus 管理端，覆盖管理员登录、AI 渠道配置、提示词模板、敏感词、解梦记录查看与配置缓存刷新。

## 本地启动

```bash
cd admin
npm install
npm run dev
```

默认开发地址：`http://localhost:5173`

默认 API Base 为 `/api`，可通过 `VITE_API_BASE_URL` 指向后端地址。管理员账号由后端 `DREAM_ADMIN_USERNAME` / `DREAM_ADMIN_PASSWORD` 提供。

## 构建验证

```bash
npm run build
```

参见 `../docs/api-contract.md`（管理端接口）。
