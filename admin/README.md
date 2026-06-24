# admin（M5 承载目录）

Vue3 + Vite + TypeScript + Element Plus 管理端承载目录。管理端按 M5 交付口径推进，当前不计入已交付功能面。

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
