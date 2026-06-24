<script lang="ts">
import { useDreamStore } from './stores/dream'

export default {
  onLaunch() {
    // 小程序惯例：启动时静默登录，确保后续鉴权请求带得上 token。
    // 失败时静默处理，具体动作会在调用前再兜底登录。
    const store = useDreamStore()
    if (!store.isLoggedIn) {
      store.login().catch(() => {})
    }
  },
}
</script>

<style lang="scss">
@import 'uview-plus/index.scss';

page {
  min-height: 100%;
  background: #0a0820;
  color: #ffffff;
  font-family:
    -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  letter-spacing: 0;
}

button {
  letter-spacing: 0;
}

.stars-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 42rpx 32rpx 56rpx;
  background:
    radial-gradient(circle at 16% 8%, rgba(245, 213, 122, 0.2), transparent 24%),
    radial-gradient(circle at 88% 14%, rgba(120, 217, 255, 0.18), transparent 28%),
    radial-gradient(circle at 58% 76%, rgba(255, 138, 197, 0.16), transparent 30%),
    linear-gradient(158deg, #1a1033 0%, #2c1855 48%, #090820 100%);
  color: #ffffff;
  overflow-x: hidden;
  position: relative;
}

.stars-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.82) 0 1rpx, transparent 2rpx),
    radial-gradient(circle, rgba(245, 213, 122, 0.7) 0 1rpx, transparent 2rpx);
  background-position:
    18rpx 28rpx,
    88rpx 124rpx;
  background-size:
    142rpx 182rpx,
    218rpx 246rpx;
  opacity: 0.34;
}

.page-shell {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 28rpx;
}

.glass-card,
.glass-panel {
  box-sizing: border-box;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 28rpx 80rpx rgba(5, 3, 20, 0.34);
  backdrop-filter: blur(18rpx);
}

.glass-panel {
  border-radius: 8rpx;
}

.primary-action,
.secondary-action,
.ghost-action {
  min-height: 92rpx;
  border-radius: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 92rpx;
}

.primary-action::after,
.secondary-action::after,
.ghost-action::after {
  border: 0;
}

.primary-action {
  background: linear-gradient(135deg, #f5d57a 0%, #ffb26b 100%);
  color: #21133f;
  box-shadow: 0 18rpx 48rpx rgba(245, 213, 122, 0.26);
}

.secondary-action {
  border: 1rpx solid rgba(245, 213, 122, 0.44);
  background: rgba(245, 213, 122, 0.12);
  color: #fff7d6;
}

.ghost-action {
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
}

.page-title {
  color: #ffffff;
  font-size: 48rpx;
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: 0;
}

.page-subtitle {
  color: rgba(255, 255, 255, 0.72);
  font-size: 27rpx;
  line-height: 1.7;
  letter-spacing: 0;
}

.section-title {
  color: #f5d57a;
  font-size: 26rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.safe-bottom {
  padding-bottom: calc(28rpx + env(safe-area-inset-bottom));
}
</style>
