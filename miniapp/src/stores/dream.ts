import { defineStore } from 'pinia'
import {
  getDreamDetail,
  getFavoriteList,
  getHistory,
  interpretDream,
  toggleFavorite,
  wxLogin,
} from '../services/dream'
import type {
  DreamRecord,
  InterpretDreamPayload,
  InterpretationSchool,
  UserProfile,
} from '../types/dream'
import {
  getCurrentRecord,
  getFavoriteIds,
  getPreferredSchool,
  getToken,
  getUser,
  setCurrentRecord,
  setPreferredSchool,
  setToken,
  setUser,
} from '../utils/storage'

interface DreamState {
  token: string
  user: UserProfile | null
  pendingPayload: InterpretDreamPayload | null
  currentRecord: DreamRecord | null
  history: DreamRecord[]
  favorites: DreamRecord[]
  favoriteIds: number[]
  preferredSchool: '' | InterpretationSchool
  loading: boolean
}

export const useDreamStore = defineStore('dream', {
  state: (): DreamState => ({
    token: getToken(),
    user: getUser(),
    pendingPayload: null,
    currentRecord: getCurrentRecord(),
    history: [],
    favorites: [],
    favoriteIds: getFavoriteIds(),
    preferredSchool: getPreferredSchool(),
    loading: false,
  }),
  getters: {
    currentResult: (state) => state.currentRecord?.result ?? null,
    isLoggedIn: (state) => Boolean(state.token),
    isCurrentFavorite: (state) => {
      const id = state.currentRecord?.dreamResultId
      return Boolean(id && state.favoriteIds.includes(id))
    },
  },
  actions: {
    setPending(payload: InterpretDreamPayload) {
      this.pendingPayload = payload
    },
    async login() {
      const loginResult = await new Promise<UniApp.LoginRes>((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: resolve,
          fail: reject,
        })
      }).catch(() => ({ code: `mock-code-${Date.now()}` }) as UniApp.LoginRes)

      const response = await wxLogin(loginResult.code)
      this.token = response.token
      this.user = response.user
      setToken(response.token)
      setUser(response.user)
    },
    logout() {
      this.token = ''
      this.user = null
      setToken('')
      setUser(null)
    },
    setPreferredSchool(school: '' | InterpretationSchool) {
      this.preferredSchool = school
      setPreferredSchool(school)
    },
    async interpretPending() {
      if (!this.pendingPayload) {
        return null
      }

      this.loading = true
      try {
        const response = await interpretDream(this.pendingPayload)
        const record: DreamRecord = {
          dreamRecordId: response.dreamRecordId,
          dreamResultId: response.dreamResultId,
          dreamText: this.pendingPayload.dreamText,
          summary: response.result.summary,
          createdAt: new Date().toISOString(),
          tags: this.pendingPayload.tags,
          favorited: false,
          result: response.result,
        }
        this.currentRecord = record
        setCurrentRecord(record)
        await this.loadHistory()
        return record
      } finally {
        this.loading = false
      }
    },
    async loadHistory() {
      const response = await getHistory()
      this.history = response.list.map((record) => ({
        ...record,
        favorited: this.favoriteIds.includes(record.dreamResultId),
      }))
    },
    async loadDetail(id: number) {
      const response = await getDreamDetail(id)
      const record = {
        ...response.dreamRecord,
        result: response.result,
        favorited: this.favoriteIds.includes(response.dreamRecord.dreamResultId),
      }
      this.currentRecord = record
      setCurrentRecord(record)
      return record
    },
    async toggleCurrentFavorite() {
      if (!this.currentRecord) {
        return
      }

      const shouldAdd = !this.favoriteIds.includes(this.currentRecord.dreamResultId)
      const response = await toggleFavorite(this.currentRecord.dreamResultId, shouldAdd ? 'add' : 'remove')
      if (response.favorited) {
        this.favoriteIds = Array.from(new Set([...this.favoriteIds, this.currentRecord.dreamResultId]))
      } else {
        this.favoriteIds = this.favoriteIds.filter((id) => id !== this.currentRecord?.dreamResultId)
      }
      this.currentRecord = {
        ...this.currentRecord,
        favorited: response.favorited,
      }
      setCurrentRecord(this.currentRecord)
      await this.loadFavorites()
    },
    async loadFavorites() {
      this.favoriteIds = getFavoriteIds()
      const response = await getFavoriteList()
      this.favorites = response.list
    },
  },
})
