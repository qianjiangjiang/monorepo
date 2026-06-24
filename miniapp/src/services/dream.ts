import type {
  DreamDetailResponse,
  DreamRecord,
  FavoriteResponse,
  HistoryResponse,
  InterpretDreamPayload,
  InterpretDreamResponse,
  LoginResponse,
} from '../types/dream'
import { request } from './request'
import { createDreamRecord, createSeedHistory } from '../utils/mock'
import {
  getFavoriteIds,
  getHistoryRecords,
  setFavoriteIds,
  setHistoryRecords,
} from '../utils/storage'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

function wait(ms = 420) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

function ensureHistory() {
  const records = getHistoryRecords()
  if (records.length > 0) {
    return records
  }

  const seeded = createSeedHistory()
  setHistoryRecords(seeded)
  return seeded
}

export async function wxLogin(code: string): Promise<LoginResponse> {
  if (!USE_MOCK) {
    return request<LoginResponse>({
      url: '/auth/wxLogin',
      method: 'POST',
      data: { code },
      auth: false,
      loadingText: '登录中',
    })
  }

  await wait(260)
  return {
    token: `mock-token-${Date.now()}`,
    user: {
      id: 1,
      nickname: '星梦旅人',
      avatar: '',
    },
  }
}

export async function interpretDream(payload: InterpretDreamPayload): Promise<InterpretDreamResponse> {
  if (!USE_MOCK) {
    return request<InterpretDreamResponse>({
      url: '/dream/interpret',
      method: 'POST',
      data: payload,
      loading: false,
    })
  }

  await wait(900)
  const record = createDreamRecord(payload)
  const nextHistory = [record, ...ensureHistory()].slice(0, 40)
  setHistoryRecords(nextHistory)

  return {
    dreamRecordId: record.dreamRecordId,
    dreamResultId: record.dreamResultId,
    school: record.school,
    result: record.result,
  }
}

export async function getHistory(page = 1, size = 20): Promise<HistoryResponse> {
  if (!USE_MOCK) {
    return request<HistoryResponse>({
      url: `/dream/history?page=${page}&size=${size}`,
      loadingText: '读取历史',
    })
  }

  await wait(180)
  const records = ensureHistory()
  const start = (page - 1) * size
  return {
    total: records.length,
    list: records.slice(start, start + size),
  }
}

export async function getDreamDetail(id: number): Promise<DreamDetailResponse> {
  if (!USE_MOCK) {
    return request<DreamDetailResponse>({
      url: `/dream/${id}`,
      loadingText: '读取详情',
    })
  }

  await wait(160)
  const favorites = getFavoriteIds()
  const record = ensureHistory().find((item) => item.dreamRecordId === id) || ensureHistory()[0]
  const decorated: DreamRecord = {
    ...record,
    favorited: favorites.includes(record.dreamResultId),
  }

  return {
    dreamRecord: decorated,
    result: decorated.result,
  }
}

export async function toggleFavorite(dreamResultId: number, action: 'add' | 'remove'): Promise<FavoriteResponse> {
  if (!USE_MOCK) {
    return request<FavoriteResponse>({
      url: '/favorite',
      method: 'POST',
      data: { dreamResultId, action },
      loading: false,
    })
  }

  await wait(120)
  const favorites = new Set(getFavoriteIds())
  if (action === 'add') {
    favorites.add(dreamResultId)
  } else {
    favorites.delete(dreamResultId)
  }
  const ids = Array.from(favorites)
  setFavoriteIds(ids)

  const history = ensureHistory().map((record) => ({
    ...record,
    favorited: ids.includes(record.dreamResultId),
  }))
  setHistoryRecords(history)

  return {
    favorited: ids.includes(dreamResultId),
  }
}

export async function getFavoriteList(page = 1, size = 20): Promise<HistoryResponse> {
  if (!USE_MOCK) {
    return request<HistoryResponse>({
      url: `/favorite/list?page=${page}&size=${size}`,
      loadingText: '读取收藏',
    })
  }

  await wait(160)
  const ids = getFavoriteIds()
  const records = ensureHistory().filter((record) => ids.includes(record.dreamResultId))
  const start = (page - 1) * size
  return {
    total: records.length,
    list: records.slice(start, start + size).map((record) => ({ ...record, favorited: true })),
  }
}
