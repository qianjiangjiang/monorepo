<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResultCards from '../../components/ResultCards.vue'
import { useDreamStore } from '../../stores/dream'

const store = useDreamStore()
const loading = ref(true)
const record = computed(() => store.currentRecord)
const result = computed(() => store.currentResult)

onLoad(async (query) => {
  const id = Number(query?.id)
  if (!id) {
    loading.value = false
    return
  }

  try {
    await store.loadDetail(id)
  } finally {
    loading.value = false
  }
})

async function toggleFavorite() {
  await store.toggleCurrentFavorite()
  uni.showToast({
    title: store.isCurrentFavorite ? '已加入收藏' : '已取消收藏',
    icon: 'none',
  })
}

function createPoster() {
  uni.navigateTo({ url: '/pages/poster/poster' })
}

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="stars-page safe-bottom">
    <view v-if="!loading && result && record" class="page-shell">
      <view class="detail-head">
        <text class="brand">Dream Detail</text>
        <text class="page-title">历史详情</text>
        <text class="page-subtitle">{{ record.dreamText }}</text>
      </view>

      <ResultCards :result="result" :preferred-school="record.school" compact />

      <view class="detail-actions">
        <button class="secondary-action detail-button" hover-class="button-hover" @tap="toggleFavorite()">
          {{ store.isCurrentFavorite ? '已收藏' : '收藏' }}
        </button>
        <button class="primary-action detail-button" hover-class="button-hover" @tap="createPoster()">
          分享海报
        </button>
      </view>
    </view>

    <view v-else-if="!loading" class="page-shell">
      <view class="glass-panel empty-state">
        <text class="empty-title">没有找到这条记录</text>
        <button class="primary-action empty-button" hover-class="button-hover" @tap="goBack()">返回</button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.detail-head {
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

.detail-actions {
  display: grid;
  grid-template-columns: 1fr 1.3fr;
  gap: 16rpx;
  margin-top: 8rpx;
}

.detail-button {
  min-height: 82rpx;
  font-size: 27rpx;
  line-height: 82rpx;
}

.empty-state {
  padding: 46rpx 32rpx;
  text-align: center;
}

.empty-title {
  display: block;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.empty-button {
  margin-top: 28rpx;
}

.button-hover {
  opacity: 0.86;
}
</style>
