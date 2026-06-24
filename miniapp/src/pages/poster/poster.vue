<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onReady, onShow } from '@dcloudio/uni-app'
import { useDreamStore } from '../../stores/dream'

const store = useDreamStore()
const posterPath = ref('')
const drawing = ref(false)

const canvasId = 'dreamPosterCanvas'
const canvasWidth = 640
const canvasHeight = 900
const fallbackDisclaimer = '解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。'

function drawRoundRect(ctx: UniApp.CanvasContext, x: number, y: number, width: number, height: number, radius: number) {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.lineTo(x + width - radius, y)
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
  ctx.lineTo(x + width, y + height - radius)
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
  ctx.lineTo(x + radius, y + height)
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
  ctx.lineTo(x, y + radius)
  ctx.quadraticCurveTo(x, y, x + radius, y)
  ctx.closePath()
}

function estimateTextWidth(text: string, fontSize: number) {
  return text.replace(/[^\x00-\xff]/g, 'aa').length * fontSize * 0.5
}

function wrapText(
  ctx: UniApp.CanvasContext,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
  maxLines: number,
) {
  let line = ''
  let lineCount = 0
  const chars = text.split('')

  for (let i = 0; i < chars.length; i += 1) {
    const testLine = line + chars[i]
    const measure = 'measureText' in ctx ? ctx.measureText(testLine).width : estimateTextWidth(testLine, 26)
    if (measure > maxWidth && line) {
      lineCount += 1
      if (lineCount >= maxLines) {
        ctx.fillText(`${line.slice(0, Math.max(0, line.length - 1))}…`, x, y)
        return y + lineHeight
      }
      ctx.fillText(line, x, y)
      line = chars[i]
      y += lineHeight
    } else {
      line = testLine
    }
  }

  if (line) {
    ctx.fillText(line, x, y)
    y += lineHeight
  }
  return y
}

function drawPoster() {
  const record = store.currentRecord
  const result = store.currentResult
  if (!record || !result) {
    return
  }

  drawing.value = true
  // 旧版 canvas 的绘制坐标系 = 画布元素的逻辑像素尺寸；元素是 640rpx 宽，
  // 在非 750px 宽的设备上 ≠ 640，按 upx2px 缩放，避免右半内容被裁切。
  const renderScale = uni.upx2px(canvasWidth) / canvasWidth
  const ctx = uni.createCanvasContext(canvasId)
  ctx.scale(renderScale, renderScale)

  ctx.setFillStyle('#1a1033')
  ctx.fillRect(0, 0, canvasWidth, canvasHeight)

  ctx.setFillStyle('rgba(120,217,255,0.16)')
  ctx.beginPath()
  ctx.arc(500, 110, 170, 0, Math.PI * 2)
  ctx.fill()

  ctx.setFillStyle('rgba(245,213,122,0.16)')
  ctx.beginPath()
  ctx.arc(98, 190, 120, 0, Math.PI * 2)
  ctx.fill()

  ctx.setFillStyle('rgba(255,255,255,0.78)')
  for (let i = 0; i < 48; i += 1) {
    const x = (i * 83) % canvasWidth
    const y = (i * 137) % canvasHeight
    ctx.beginPath()
    ctx.arc(x, y, i % 4 === 0 ? 2.2 : 1.2, 0, Math.PI * 2)
    ctx.fill()
  }

  ctx.setFillStyle('rgba(255,255,255,0.12)')
  drawRoundRect(ctx, 48, 66, 544, 768, 18)
  ctx.fill()
  ctx.setStrokeStyle('rgba(255,255,255,0.26)')
  ctx.setLineWidth(2)
  ctx.stroke()

  ctx.setFillStyle('#f5d57a')
  ctx.setFontSize(24)
  ctx.fillText('DREAM INSIGHT', 84, 122)

  ctx.setFillStyle('#ffffff')
  ctx.setFontSize(42)
  let nextY = wrapText(ctx, result.title, 84, 188, 472, 52, 2)

  ctx.setFillStyle('rgba(255,255,255,0.78)')
  ctx.setFontSize(26)
  nextY = wrapText(ctx, record.dreamText, 84, nextY + 22, 472, 38, 3)

  ctx.setFillStyle('rgba(245,213,122,0.18)')
  drawRoundRect(ctx, 84, nextY + 22, 472, 126, 14)
  ctx.fill()

  ctx.setFillStyle('#fff7d6')
  ctx.setFontSize(28)
  wrapText(ctx, result.summary, 108, nextY + 72, 424, 38, 2)

  const symbolY = nextY + 190
  ctx.setFillStyle('#f5d57a')
  ctx.setFontSize(24)
  ctx.fillText('象征关键词', 84, symbolY)

  result.symbols.slice(0, 3).forEach((symbol, index) => {
    const x = 84 + index * 154
    ctx.setFillStyle('rgba(255,255,255,0.1)')
    drawRoundRect(ctx, x, symbolY + 28, 132, 68, 34)
    ctx.fill()
    ctx.setFillStyle('#ffffff')
    ctx.setFontSize(24)
    ctx.fillText(symbol.keyword.slice(0, 4), x + 26, symbolY + 72)
  })

  const suggestionY = symbolY + 150
  ctx.setFillStyle('#f5d57a')
  ctx.setFontSize(24)
  ctx.fillText('今日建议', 84, suggestionY)

  ctx.setFillStyle('rgba(255,255,255,0.78)')
  ctx.setFontSize(25)
  wrapText(ctx, result.suggestions[0] || '把梦境写下来，观察它与现实情绪的连接。', 84, suggestionY + 48, 472, 36, 3)

  ctx.setFillStyle('rgba(255,255,255,0.52)')
  ctx.setFontSize(22)
  wrapText(ctx, result.fortune.disclaimer || fallbackDisclaimer, 84, 764, 472, 28, 2)
  ctx.setFillStyle('#f5d57a')
  ctx.setFontSize(26)
  ctx.fillText('长按保存你的星空解梦卡片', 84, 840)

  ctx.draw(false, () => {
    uni.canvasToTempFilePath({
      canvasId,
      width: uni.upx2px(canvasWidth),
      height: uni.upx2px(canvasHeight),
      destWidth: canvasWidth * 2,
      destHeight: canvasHeight * 2,
      success: (response) => {
        posterPath.value = response.tempFilePath
      },
      complete: () => {
        drawing.value = false
      },
    })
  })
}

function previewPoster() {
  if (!posterPath.value) {
    drawPoster()
    return
  }
  uni.previewImage({ urls: [posterPath.value] })
}

function savePoster() {
  if (!posterPath.value) {
    drawPoster()
    return
  }
  uni.saveImageToPhotosAlbum({
    filePath: posterPath.value,
    success: () => {
      uni.showToast({ title: '已保存到相册', icon: 'none' })
    },
    fail: () => {
      uni.showToast({ title: '保存失败，请检查相册权限', icon: 'none' })
    },
  })
}

onLoad(() => {
  if (!store.currentRecord) {
    uni.showToast({ title: '暂无可分享结果', icon: 'none' })
    setTimeout(() => {
      uni.navigateBack()
    }, 700)
  }
})

onReady(() => {
  setTimeout(drawPoster, 120)
})

onShow(() => {
  setTimeout(drawPoster, 120)
})
</script>

<template>
  <view class="stars-page poster-page safe-bottom">
    <view class="page-shell">
      <view class="poster-head">
        <text class="brand">Share Poster</text>
        <text class="page-title">分享海报</text>
        <text class="page-subtitle">生成一张适合转发与保存的星空解梦卡片。</text>
      </view>

      <view class="poster-frame">
        <canvas :canvas-id="canvasId" :id="canvasId" class="poster-canvas"></canvas>
      </view>

      <view class="poster-actions">
        <button class="secondary-action poster-button" hover-class="button-hover" @tap="previewPoster()">
          {{ drawing ? '生成中' : '预览' }}
        </button>
        <button class="primary-action poster-button" hover-class="button-hover" @tap="savePoster()">保存</button>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.poster-page {
  padding-bottom: 64rpx;
}

.poster-head {
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

.poster-frame {
  display: flex;
  justify-content: center;
  padding: 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.08);
}

.poster-canvas {
  width: 640rpx;
  height: 900rpx;
  border-radius: 8rpx;
  overflow: hidden;
}

.poster-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.poster-button {
  min-height: 84rpx;
  font-size: 28rpx;
  line-height: 84rpx;
}

.button-hover {
  opacity: 0.86;
}
</style>
