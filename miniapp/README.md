# miniapp

uni-app + Vue3 + TypeScript + Pinia + uview-plus 微信小程序骨架。当前 M0 提供可编译空首页，正式页面在后续里程碑实现。

## 本地启动

```bash
cd miniapp
npm install
npm run dev:mp-weixin
```

编译产物输出到 `dist/dev/mp-weixin`，可用微信开发者工具导入该目录预览。正式 AppID 到位后，填入 `src/manifest.json` 的 `mp-weixin.appid`。

H5 调试：

```bash
npm run dev:h5
```

## 构建验证

```bash
npm run build:mp-weixin
```

参见 `../docs/architecture.md`、`../docs/dream-result.schema.json`。
