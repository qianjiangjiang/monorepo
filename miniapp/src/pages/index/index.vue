<script setup lang="ts">
import { computed, ref } from 'vue'
import { useDreamStore } from '../../stores/dream'
import type { InterpretationSchool } from '../../types/dream'

const store = useDreamStore()
const dreamText = ref('')
const selectedTags = ref<string[]>([])
const selectedSchool = ref<'' | InterpretationSchool>(store.preferredSchool)

const quickTags = ['星空', '飞行', '水', '旧人', '反复出现', '焦虑', '压力', '清醒梦']
const schools: Array<{ label: string; value: '' | InterpretationSchool }> = [
  { label: '默认', value: '' },
  { label: '传统优先', value: '传统文化' },
  { label: '心理优先', value: '心理学' },
]

const canSubmit = computed(() => dreamText.value.trim().length >= 6)
const remainingText = computed(() => `${dreamText.value.trim().length}/500`)

function onInput(event: Event) {
  const uniEvent = event as Event & { detail?: { value?: string } }
  const target = event.target as HTMLTextAreaElement | null
  dreamText.value = (uniEvent.detail?.value || target?.value || '').slice(0, 500)
}

function toggleTag(tag: string) {
  selectedTags.value = selectedTags.value.includes(tag)
    ? selectedTags.value.filter((item) => item !== tag)
    : [...selectedTags.value, tag]
}

function selectSchool(value: '' | InterpretationSchool) {
  selectedSchool.value = value
  store.setPreferredSchool(value)
}

function submitDream() {
  const text = dreamText.value.trim()
  if (text.length < 6) {
    uni.showToast({ title: '请写下更完整的梦境', icon: 'none' })
    return
  }

  store.setPending({
    dreamText: text,
    tags: selectedTags.value,
    school: selectedSchool.value,
  })

  uni.navigateTo({ url: '/pages/loading/loading' })
}

function goHistory() {
  uni.navigateTo({ url: '/pages/history/history' })
}

function goMine() {
  uni.navigateTo({ url: '/pages/mine/mine' })
}
</script>

<template>
  <view class="stars-page safe-bottom">
    <view class="page-shell">
      <view class="top-bar">
        <view>
          <text class="brand">Dream Insight</text>
          <text class="page-title">神秘星空解梦</text>
        </view>
        <view class="top-actions">
          <button class="icon-button" hover-class="icon-button-hover" @tap="goHistory">历</button>
          <button class="icon-button" hover-class="icon-button-hover" @tap="goMine">我</button>
        </view>
      </view>

      <view class="hero-copy">
        <text class="page-subtitle">把醒来后还发亮的片段写下来，系统会按象征、情绪与双视角生成结构化解读。</text>
      </view>

      <view class="glass-panel input-panel">
        <view class="input-head">
          <text class="section-title">梦境记录</text>
          <text class="count-label">{{ remainingText }}</text>
        </view>
        <textarea
          class="dream-input"
          :value="dreamText"
          maxlength="500"
          placeholder="例如：我梦见自己站在星空下，推开一扇发光的门……"
          placeholder-style="color: rgba(255,255,255,0.42);"
          auto-height
          @input="onInput"
        />
      </view>

      <view class="section-block">
        <view class="section-row">
          <text class="section-title">梦境标签</text>
          <text class="section-hint">可多选</text>
        </view>
        <view class="chip-grid">
          <button
            v-for="tag in quickTags"
            :key="tag"
            class="tag-chip"
            :class="{ active: selectedTags.includes(tag) }"
            hover-class="chip-hover"
            @tap="toggleTag(tag)"
          >
            {{ tag }}
          </button>
        </view>
      </view>

      <view class="section-block">
        <view class="section-row">
          <text class="section-title">显示偏好</text>
        </view>
        <view class="school-grid">
          <button
            v-for="school in schools"
            :key="school.label"
            class="school-option"
            :class="{ active: selectedSchool === school.value }"
            hover-class="chip-hover"
            @tap="selectSchool(school.value)"
          >
            {{ school.label }}
          </button>
        </view>
      </view>

      <button
        class="primary-action submit-button"
        :class="{ disabled: !canSubmit }"
        hover-class="primary-hover"
        @tap="submitDream"
      >
        解梦
      </button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.top-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
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

.top-actions {
  display: flex;
  gap: 12rpx;
}

.icon-button {
  width: 72rpx;
  height: 72rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  color: #fff7d6;
  font-size: 24rpx;
  line-height: 72rpx;
  text-align: center;
  letter-spacing: 0;
}

.icon-button::after {
  border: 0;
}

.icon-button-hover,
.chip-hover,
.primary-hover {
  opacity: 0.86;
}

.hero-copy {
  max-width: 650rpx;
}

.input-panel {
  padding: 30rpx;
}

.input-head,
.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 20rpx;
}

.count-label,
.section-hint {
  color: rgba(255, 255, 255, 0.52);
  font-size: 22rpx;
  letter-spacing: 0;
}

.dream-input {
  width: 100%;
  min-height: 256rpx;
  color: #ffffff;
  font-size: 30rpx;
  line-height: 1.72;
  letter-spacing: 0;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.tag-chip,
.school-option {
  min-height: 70rpx;
  margin: 0;
  padding: 0 22rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.78);
  font-size: 25rpx;
  line-height: 70rpx;
  letter-spacing: 0;
}

.tag-chip::after,
.school-option::after {
  border: 0;
}

.tag-chip.active,
.school-option.active {
  border-color: rgba(245, 213, 122, 0.68);
  background: rgba(245, 213, 122, 0.16);
  color: #fff7d6;
}

.school-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14rpx;
}

.school-option {
  border-radius: 8rpx;
  padding: 0 10rpx;
}

.submit-button {
  margin-top: 10rpx;
}

.submit-button.disabled {
  opacity: 0.58;
}
</style>
