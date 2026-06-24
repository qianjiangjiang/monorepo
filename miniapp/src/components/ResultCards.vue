<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { DreamResult, InterpretationSchool } from '../types/dream'

const props = defineProps<{
  result: DreamResult
  preferredSchool?: '' | InterpretationSchool
  compact?: boolean
}>()

const visibleInterpretations = computed(() => {
  if (!props.preferredSchool) {
    return props.result.interpretations
  }

  const matched = props.result.interpretations.filter((item) => item.school === props.preferredSchool)
  return matched.length > 0 ? matched : props.result.interpretations
})

const activeSchool = ref<InterpretationSchool>(visibleInterpretations.value[0]?.school || '传统文化')

const toneLabel = computed(() => {
  const labels = {
    positive: '明亮',
    neutral: '平衡',
    negative: '低回',
    mixed: '交织',
  }
  return labels[props.result.overallTone]
})

const activeInterpretation = computed(() => {
  return (
    visibleInterpretations.value.find((item) => item.school === activeSchool.value) ||
    visibleInterpretations.value[0]
  )
})

const interpretationTitle = computed(() => {
  return props.preferredSchool && visibleInterpretations.value.length === 1
    ? `${props.preferredSchool}解读`
    : '双视角解读'
})

watch(
  visibleInterpretations,
  (items) => {
    activeSchool.value = items[0]?.school || '传统文化'
  },
)

watch(
  () => props.result.title,
  () => {
    activeSchool.value = visibleInterpretations.value[0]?.school || '传统文化'
  },
)
</script>

<template>
  <view class="result-cards" :class="{ compact }">
    <view class="glass-card result-card card-summary fade-card">
      <view class="card-head">
        <text class="card-kicker">概要</text>
        <text class="tone-pill">{{ toneLabel }}</text>
      </view>
      <text class="result-title">{{ result.title }}</text>
      <text class="result-summary">{{ result.summary }}</text>
      <view v-if="result.tags?.length" class="tag-row">
        <text v-for="tag in result.tags" :key="tag" class="soft-chip">{{ tag }}</text>
      </view>
    </view>

    <view class="glass-card result-card fade-card symbols-card">
      <view class="card-head">
        <text class="card-kicker">象征</text>
        <text class="card-count">{{ result.symbols.length }}</text>
      </view>
      <view class="symbol-list">
        <view v-for="symbol in result.symbols" :key="symbol.keyword" class="symbol-item">
          <view class="symbol-meta">
            <text class="symbol-keyword">{{ symbol.keyword }}</text>
            <text v-if="symbol.category" class="symbol-category">{{ symbol.category }}</text>
          </view>
          <text class="symbol-meaning">{{ symbol.meaning }}</text>
        </view>
      </view>
    </view>

    <view class="glass-card result-card fade-card emotion-card">
      <view class="card-head">
        <text class="card-kicker">情绪</text>
        <text class="emotion-primary">{{ result.emotion.primary }}</text>
      </view>
      <text class="body-text">{{ result.emotion.description }}</text>
    </view>

    <view class="glass-card result-card fade-card">
      <view class="card-head">
        <text class="card-kicker">{{ interpretationTitle }}</text>
      </view>
      <view v-if="visibleInterpretations.length > 1" class="school-tabs">
        <button
          v-for="item in visibleInterpretations"
          :key="item.school"
          class="school-tab"
          :class="{ active: activeSchool === item.school }"
          hover-class="school-tab-hover"
          @tap="activeSchool = item.school"
        >
          {{ item.school }}
        </button>
      </view>
      <text class="body-text interpretation-text">{{ activeInterpretation?.content }}</text>
    </view>

    <view class="glass-card result-card fade-card">
      <view class="card-head">
        <text class="card-kicker">倾向</text>
        <text class="fortune-tendency">{{ result.fortune.tendency }}</text>
      </view>
      <text class="body-text">{{ result.fortune.disclaimer }}</text>
    </view>

    <view class="glass-card result-card fade-card">
      <view class="card-head">
        <text class="card-kicker">建议</text>
      </view>
      <view class="suggestion-list">
        <view v-for="(suggestion, index) in result.suggestions" :key="suggestion" class="suggestion-item">
          <text class="suggestion-index">{{ index + 1 }}</text>
          <text class="suggestion-text">{{ suggestion }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.result-cards {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.result-cards.compact {
  gap: 18rpx;
}

.result-card {
  padding: 30rpx;
  border-radius: 8rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 18rpx;
}

.card-kicker {
  color: #f5d57a;
  font-size: 24rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.tone-pill,
.card-count,
.emotion-primary,
.fortune-tendency {
  min-width: 92rpx;
  padding: 8rpx 18rpx;
  border: 1rpx solid rgba(245, 213, 122, 0.46);
  border-radius: 999rpx;
  color: #fff7d6;
  font-size: 22rpx;
  text-align: center;
  letter-spacing: 0;
}

.result-title {
  display: block;
  color: #ffffff;
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0;
}

.result-summary {
  display: block;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 28rpx;
  line-height: 1.7;
  letter-spacing: 0;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 22rpx;
}

.soft-chip {
  padding: 8rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 22rpx;
  letter-spacing: 0;
}

.symbol-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.symbol-item {
  padding: 22rpx;
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.07);
}

.symbol-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 10rpx;
}

.symbol-keyword {
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
  letter-spacing: 0;
}

.symbol-category {
  color: rgba(245, 213, 122, 0.78);
  font-size: 22rpx;
  letter-spacing: 0;
}

.symbol-meaning,
.body-text,
.suggestion-text {
  display: block;
  color: rgba(255, 255, 255, 0.78);
  font-size: 26rpx;
  line-height: 1.7;
  letter-spacing: 0;
}

.school-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-bottom: 22rpx;
}

.school-tab {
  min-height: 76rpx;
  padding: 0 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.14);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.78);
  font-size: 26rpx;
  line-height: 76rpx;
  letter-spacing: 0;
}

.school-tab::after {
  border: 0;
}

.school-tab.active {
  border-color: rgba(245, 213, 122, 0.62);
  background: rgba(245, 213, 122, 0.16);
  color: #fff7d6;
}

.school-tab-hover {
  opacity: 0.88;
}

.interpretation-text {
  min-height: 132rpx;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.suggestion-item {
  display: grid;
  grid-template-columns: 44rpx minmax(0, 1fr);
  gap: 16rpx;
  align-items: flex-start;
}

.suggestion-index {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(245, 213, 122, 0.18);
  color: #f5d57a;
  font-size: 22rpx;
  line-height: 44rpx;
  text-align: center;
  letter-spacing: 0;
}

.fade-card {
  opacity: 0;
  transform: translateY(22rpx);
  animation: card-in 520ms ease forwards;
}

.fade-card:nth-child(2) {
  animation-delay: 80ms;
}

.fade-card:nth-child(3) {
  animation-delay: 160ms;
}

.fade-card:nth-child(4) {
  animation-delay: 240ms;
}

.fade-card:nth-child(5) {
  animation-delay: 320ms;
}

.fade-card:nth-child(6) {
  animation-delay: 400ms;
}

@keyframes card-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
