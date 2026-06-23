<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { useDreamStore } from '../../stores/dream'

const store = useDreamStore()

function wait(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

onLoad(async () => {
  if (!store.pendingPayload) {
    uni.showToast({ title: '请先输入梦境', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 700)
    return
  }

  try {
    await Promise.all([store.interpretPending(), wait(1800)])
    uni.redirectTo({ url: '/pages/result/result' })
  } catch {
    uni.showToast({ title: '解梦失败，请稍后重试', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  }
})
</script>

<template>
  <view class="stars-page loading-page">
    <view class="loading-shell">
      <view class="orb-stage">
        <view class="orb-glow"></view>
        <view class="crystal-ball">
          <!-- #ifdef MP-WEIXIN -->
          <animation-view
            class="lottie-view"
            path="/static/lottie/star-crystal.json"
            autoplay
            loop
          ></animation-view>
          <!-- #endif -->
          <view class="fallback-stars">
            <view class="star star-a"></view>
            <view class="star star-b"></view>
            <view class="star star-c"></view>
            <view class="star star-d"></view>
          </view>
        </view>
      </view>

      <view class="loading-copy">
        <text class="loading-title">星点正在汇聚</text>
        <text class="loading-subtitle">正在拆解象征、情绪与双视角解读</text>
      </view>

      <view class="progress-track">
        <view class="progress-fill"></view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64rpx 40rpx;
}

.loading-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 54rpx;
}

.orb-stage {
  position: relative;
  width: 430rpx;
  height: 430rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.orb-glow {
  position: absolute;
  width: 350rpx;
  height: 350rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 213, 122, 0.34), rgba(120, 217, 255, 0.08) 54%, transparent 72%);
  animation: pulse 1800ms ease-in-out infinite;
}

.crystal-ball {
  position: relative;
  width: 292rpx;
  height: 292rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.72), transparent 10%),
    radial-gradient(circle at 52% 60%, rgba(120, 217, 255, 0.3), rgba(86, 44, 150, 0.24) 48%, rgba(255, 255, 255, 0.08));
  box-shadow:
    inset 0 0 70rpx rgba(255, 255, 255, 0.18),
    0 0 86rpx rgba(120, 217, 255, 0.22);
  overflow: hidden;
}

.lottie-view {
  width: 292rpx;
  height: 292rpx;
}

.fallback-stars {
  position: absolute;
  inset: 0;
}

.star {
  position: absolute;
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #fff7d6;
  box-shadow: 0 0 18rpx rgba(245, 213, 122, 0.9);
  animation: gather 1800ms ease-in-out infinite;
}

.star-a {
  left: 52rpx;
  top: 96rpx;
}

.star-b {
  right: 64rpx;
  top: 74rpx;
  animation-delay: 160ms;
}

.star-c {
  left: 90rpx;
  bottom: 70rpx;
  animation-delay: 260ms;
}

.star-d {
  right: 88rpx;
  bottom: 92rpx;
  animation-delay: 360ms;
}

.loading-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  text-align: center;
}

.loading-title {
  color: #ffffff;
  font-size: 42rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.loading-subtitle {
  color: rgba(255, 255, 255, 0.7);
  font-size: 26rpx;
  line-height: 1.6;
  letter-spacing: 0;
}

.progress-track {
  width: 420rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  overflow: hidden;
}

.progress-fill {
  width: 42%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #78d9ff, #f5d57a, #ff8ac5);
  animation: loading-line 1500ms ease-in-out infinite;
}

@keyframes pulse {
  50% {
    transform: scale(1.08);
    opacity: 0.75;
  }
}

@keyframes gather {
  50% {
    transform: translate(34rpx, 28rpx) scale(1.6);
    opacity: 0.45;
  }
}

@keyframes loading-line {
  0% {
    transform: translateX(-100%);
  }

  100% {
    transform: translateX(260%);
  }
}
</style>
