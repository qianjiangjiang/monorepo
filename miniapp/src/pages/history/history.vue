<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useDreamStore } from '../../stores/dream'
import type { DreamRecord } from '../../types/dream'

const store = useDreamStore()
const history = computed(() => store.history)

onShow(() => {
  store.loadHistory()
})

function formatTime(value: string) {
  const date = new Date(value)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}.${day} ${hour}:${minute}`
}

function openDetail(record: DreamRecord) {
  uni.navigateTo({ url: `/pages/detail/detail?id=${record.dreamRecordId}` })
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="stars-page safe-bottom">
    <view class="page-shell">
      <view class="history-head">
        <view>
          <text class="brand">Timeline</text>
          <text class="page-title">梦境历史</text>
        </view>
        <button class="ghost-action head-button" hover-class="button-hover" @tap="goHome">记录新梦</button>
      </view>

      <view v-if="history.length" class="timeline">
        <view v-for="record in history" :key="record.dreamRecordId" class="timeline-row" @tap="openDetail(record)">
          <view class="time-rail">
            <view class="time-dot"></view>
            <view class="time-line"></view>
          </view>
          <view class="glass-card history-card">
            <view class="history-meta">
              <text class="history-time">{{ formatTime(record.createdAt) }}</text>
              <text v-if="record.favorited" class="favorite-mark">收藏</text>
            </view>
            <text class="history-title">{{ record.summary }}</text>
            <text class="history-text">{{ record.dreamText }}</text>
            <view class="tag-row">
              <text v-for="tag in record.tags" :key="tag" class="history-tag">{{ tag }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="glass-panel empty-state">
        <text class="empty-title">还没有梦境记录</text>
        <text class="empty-copy">写下第一个梦，历史时间轴会自动保存解读。</text>
        <button class="primary-action empty-button" hover-class="button-hover" @tap="goHome">开始记录</button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.history-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22rpx;
  padding-top: 18rpx;
}

.brand {
  display: block;
  margin-bottom: 12rpx;
  color: #f5d57a;
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.head-button {
  min-width: 172rpx;
  min-height: 72rpx;
  padding: 0 18rpx;
  font-size: 24rpx;
  line-height: 72rpx;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-row {
  display: grid;
  grid-template-columns: 42rpx minmax(0, 1fr);
  gap: 18rpx;
}

.time-rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.time-dot {
  width: 18rpx;
  height: 18rpx;
  margin-top: 34rpx;
  border: 4rpx solid rgba(245, 213, 122, 0.34);
  border-radius: 50%;
  background: #f5d57a;
  box-shadow: 0 0 22rpx rgba(245, 213, 122, 0.68);
}

.time-line {
  position: absolute;
  top: 62rpx;
  bottom: 0;
  width: 2rpx;
  background: rgba(255, 255, 255, 0.16);
}

.history-card {
  margin-bottom: 24rpx;
  padding: 26rpx;
  border-radius: 8rpx;
}

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.history-time,
.favorite-mark {
  color: rgba(245, 213, 122, 0.84);
  font-size: 22rpx;
  letter-spacing: 0;
}

.favorite-mark {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(245, 213, 122, 0.14);
}

.history-title {
  display: block;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: 0;
}

.history-text {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 25rpx;
  line-height: 1.65;
  letter-spacing: 0;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.history-tag {
  padding: 6rpx 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  border-radius: 999rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 21rpx;
  letter-spacing: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 46rpx 32rpx;
  text-align: center;
}

.empty-title {
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.empty-copy {
  color: rgba(255, 255, 255, 0.68);
  font-size: 26rpx;
  line-height: 1.6;
  letter-spacing: 0;
}

.empty-button {
  margin-top: 10rpx;
}

.button-hover {
  opacity: 0.86;
}
</style>
