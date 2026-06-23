<script setup lang="ts">
const title = 'AI 解梦'
</script>

<template>
  <view class="page">
    <view class="content">
      <text class="eyebrow">Dream Insight</text>
      <text class="title">{{ title }}</text>
      <text class="subtitle">空白首页骨架</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  padding: 96rpx 40rpx;
  background:
    radial-gradient(circle at 20% 18%, rgba(247, 201, 72, 0.18), transparent 32%),
    linear-gradient(160deg, #151d35 0%, #39235f 54%, #0f172a 100%);
  color: #fff;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.eyebrow {
  color: #f7c948;
  font-size: 24rpx;
  font-weight: 600;
  letter-spacing: 0;
}

.title {
  color: #ffffff;
  font-size: 48rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.subtitle {
  color: rgba(255, 255, 255, 0.72);
  font-size: 28rpx;
  letter-spacing: 0;
}
</style>
