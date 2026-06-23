<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useDreamStore } from '../../stores/dream'
import type { DreamRecord, InterpretationSchool } from '../../types/dream'

const store = useDreamStore()
const schoolOptions: Array<{ label: string; value: '' | InterpretationSchool }> = [
  { label: '双视角', value: '' },
  { label: '传统文化', value: '传统文化' },
  { label: '心理学', value: '心理学' },
]

const avatarText = computed(() => store.user?.nickname?.slice(0, 1) || '星')

onShow(() => {
  store.loadFavorites()
})

async function login() {
  await store.login()
  uni.showToast({ title: '登录成功', icon: 'none' })
}

function logout() {
  store.logout()
  uni.showToast({ title: '已退出登录', icon: 'none' })
}

function openFavorite(record: DreamRecord) {
  uni.navigateTo({ url: `/pages/detail/detail?id=${record.dreamRecordId}` })
}

function goHome() {
  uni.reLaunch({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="stars-page safe-bottom">
    <view class="page-shell">
      <view class="mine-head">
        <text class="brand">Profile</text>
        <text class="page-title">我的星梦</text>
      </view>

      <view class="glass-panel profile-card">
        <view class="avatar">
          <text>{{ avatarText }}</text>
        </view>
        <view class="profile-info">
          <text class="profile-name">{{ store.user?.nickname || '未登录用户' }}</text>
          <text class="profile-copy">{{ store.isLoggedIn ? '已连接微信身份' : '登录后同步收藏与偏好' }}</text>
        </view>
        <button
          class="profile-action"
          hover-class="button-hover"
          @tap="store.isLoggedIn ? logout() : login()"
        >
          {{ store.isLoggedIn ? '退出' : '登录' }}
        </button>
      </view>

      <view class="glass-panel section-card">
        <view class="section-row">
          <text class="section-title">流派偏好</text>
        </view>
        <view class="school-grid">
          <button
            v-for="option in schoolOptions"
            :key="option.label"
            class="school-chip"
            :class="{ active: store.preferredSchool === option.value }"
            hover-class="button-hover"
            @tap="store.setPreferredSchool(option.value)"
          >
            {{ option.label }}
          </button>
        </view>
      </view>

      <view class="glass-panel section-card">
        <view class="section-row">
          <text class="section-title">我的收藏</text>
          <text class="section-count">{{ store.favorites.length }}</text>
        </view>
        <view v-if="store.favorites.length" class="favorite-list">
          <view
            v-for="record in store.favorites"
            :key="record.dreamRecordId"
            class="favorite-item"
            @tap="openFavorite(record)"
          >
            <text class="favorite-title">{{ record.result.title }}</text>
            <text class="favorite-summary">{{ record.summary }}</text>
          </view>
        </view>
        <view v-else class="favorite-empty">
          <text>收藏后的解梦会出现在这里</text>
        </view>
      </view>

      <view class="glass-panel section-card">
        <view class="setting-row">
          <text>每日提醒</text>
          <switch color="#f5d57a" />
        </view>
        <view class="setting-row">
          <text>隐私模式</text>
          <switch color="#f5d57a" checked />
        </view>
        <view class="setting-row link-row" @tap="goHome">
          <text>返回首页</text>
          <text class="row-arrow">›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.mine-head {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding-top: 18rpx;
}

.brand {
  color: #f5d57a;
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.profile-card {
  display: grid;
  grid-template-columns: 104rpx minmax(0, 1fr) 108rpx;
  gap: 22rpx;
  align-items: center;
  padding: 30rpx;
}

.avatar {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 24%, rgba(255, 255, 255, 0.68), transparent 18%),
    linear-gradient(135deg, #f5d57a, #7c4dff);
  color: #21133f;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 104rpx;
  text-align: center;
  letter-spacing: 0;
}

.profile-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.profile-name {
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.profile-copy {
  color: rgba(255, 255, 255, 0.64);
  font-size: 24rpx;
  line-height: 1.5;
  letter-spacing: 0;
}

.profile-action {
  width: 108rpx;
  height: 62rpx;
  padding: 0;
  border: 1rpx solid rgba(245, 213, 122, 0.48);
  border-radius: 8rpx;
  background: rgba(245, 213, 122, 0.14);
  color: #fff7d6;
  font-size: 24rpx;
  line-height: 62rpx;
  letter-spacing: 0;
}

.profile-action::after,
.school-chip::after {
  border: 0;
}

.section-card {
  padding: 28rpx;
}

.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.section-count {
  color: rgba(255, 255, 255, 0.56);
  font-size: 24rpx;
  letter-spacing: 0;
}

.school-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
}

.school-chip {
  min-height: 70rpx;
  padding: 0 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.76);
  font-size: 24rpx;
  line-height: 70rpx;
  letter-spacing: 0;
}

.school-chip.active {
  border-color: rgba(245, 213, 122, 0.66);
  background: rgba(245, 213, 122, 0.16);
  color: #fff7d6;
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.favorite-item {
  padding: 20rpx;
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.07);
}

.favorite-title {
  display: block;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0;
}

.favorite-summary,
.favorite-empty {
  display: block;
  margin-top: 10rpx;
  color: rgba(255, 255, 255, 0.62);
  font-size: 24rpx;
  line-height: 1.55;
  letter-spacing: 0;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 82rpx;
  color: rgba(255, 255, 255, 0.84);
  font-size: 27rpx;
  letter-spacing: 0;
}

.setting-row + .setting-row {
  border-top: 1rpx solid rgba(255, 255, 255, 0.1);
}

.row-arrow {
  color: rgba(245, 213, 122, 0.86);
  font-size: 38rpx;
  letter-spacing: 0;
}

.button-hover {
  opacity: 0.86;
}
</style>
