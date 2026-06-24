<script setup lang="ts">
import { computed } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import ResultCards from '../../components/ResultCards.vue'
import { useDreamStore } from '../../stores/dream'

const store = useDreamStore()
const record = computed(() => store.currentRecord)
const result = computed(() => store.currentResult)
const canFavorite = computed(() => typeof record.value?.dreamResultId === 'number')
const favoriteText = computed(() => (store.isCurrentFavorite ? '已收藏' : '收藏'))

onLoad(() => {
  if (!store.currentRecord) {
    uni.showToast({ title: '暂无解梦结果', icon: 'none' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 700)
  }
})

onShareAppMessage(() => ({
  title: result.value?.title || '神秘星空解梦',
  path: '/pages/index/index',
}))

async function toggleFavorite() {
  const changed = await store.toggleCurrentFavorite()
  if (!changed) {
    uni.showToast({ title: '缓存结果暂不支持收藏', icon: 'none' })
    return
  }
  uni.showToast({
    title: store.isCurrentFavorite ? '已加入收藏' : '已取消收藏',
    icon: 'none',
  })
}

function createPoster() {
  uni.navigateTo({ url: '/pages/poster/poster' })
}

function replay() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="stars-page result-page safe-bottom">
    <view v-if="result && record" class="page-shell">
      <view class="result-hero">
        <text class="brand">Dream Result</text>
        <text class="page-title">{{ result.title }}</text>
        <text class="page-subtitle">{{ record.dreamText }}</text>
      </view>

      <ResultCards :result="result" :preferred-school="record.school" />

      <view class="bottom-actions" :class="{ 'without-favorite': !canFavorite }">
        <button v-if="canFavorite" class="ghost-action action-button" hover-class="action-hover" @tap="toggleFavorite()">
          {{ favoriteText }}
        </button>
        <button class="secondary-action action-button" hover-class="action-hover" open-type="share">
          转发
        </button>
        <button class="secondary-action action-button" hover-class="action-hover" @tap="createPoster()">
          海报
        </button>
        <button class="primary-action action-button" hover-class="action-hover" @tap="replay()">
          再解一次
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.result-page {
  padding-bottom: 148rpx;
}

.result-hero {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding-top: 18rpx;
}

.brand {
  color: #f5d57a;
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.bottom-actions {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  z-index: 10;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  padding: 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 8rpx;
  background: rgba(13, 8, 38, 0.84);
  box-shadow: 0 18rpx 60rpx rgba(0, 0, 0, 0.34);
  backdrop-filter: blur(18rpx);
}

.bottom-actions.without-favorite {
  grid-template-columns: repeat(3, 1fr);
}

.action-button {
  box-sizing: border-box;
  min-height: 78rpx;
  padding: 0;
  margin: 0;
  font-size: 24rpx;
  line-height: 78rpx;
}

.action-hover {
  opacity: 0.86;
}
</style>
