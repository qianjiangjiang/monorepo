import type { DreamRecord, UserProfile, InterpretationSchool } from '../types/dream'

const TOKEN_KEY = 'dream_token'
const USER_KEY = 'dream_user'
const HISTORY_KEY = 'dream_history'
const CURRENT_RECORD_KEY = 'dream_current_record'
const FAVORITES_KEY = 'dream_favorite_result_ids'
const SCHOOL_KEY = 'dream_preferred_school'

type StorageValue =
  | string
  | number
  | boolean
  | null
  | Record<string, unknown>
  | unknown[]

export function getStorage<T>(key: string, fallback: T): T {
  try {
    const value = uni.getStorageSync(key)
    return value === '' || value === undefined || value === null ? fallback : (value as T)
  } catch {
    return fallback
  }
}

export function setStorage(key: string, value: StorageValue) {
  try {
    uni.setStorageSync(key, value)
  } catch {
    // Storage can fail in private mode or low-space devices. Keep UI usable.
  }
}

export function removeStorage(key: string) {
  try {
    uni.removeStorageSync(key)
  } catch {
    // No-op: storage cleanup is best effort.
  }
}

export function getToken() {
  return getStorage<string>(TOKEN_KEY, '')
}

export function setToken(token: string) {
  setStorage(TOKEN_KEY, token)
}

export function clearToken() {
  removeStorage(TOKEN_KEY)
}

export function getUser() {
  return getStorage<UserProfile | null>(USER_KEY, null)
}

export function setUser(user: UserProfile | null) {
  if (user) {
    setStorage(USER_KEY, user as unknown as Record<string, unknown>)
  } else {
    removeStorage(USER_KEY)
  }
}

export function getHistoryRecords() {
  return getStorage<DreamRecord[]>(HISTORY_KEY, [])
}

export function setHistoryRecords(records: DreamRecord[]) {
  setStorage(HISTORY_KEY, records as unknown as unknown[])
}

export function getCurrentRecord() {
  return getStorage<DreamRecord | null>(CURRENT_RECORD_KEY, null)
}

export function setCurrentRecord(record: DreamRecord | null) {
  if (record) {
    setStorage(CURRENT_RECORD_KEY, record as unknown as Record<string, unknown>)
  } else {
    removeStorage(CURRENT_RECORD_KEY)
  }
}

export function getFavoriteIds() {
  return getStorage<number[]>(FAVORITES_KEY, [])
}

export function setFavoriteIds(ids: number[]) {
  setStorage(FAVORITES_KEY, ids)
}

export function getPreferredSchool() {
  return getStorage<'' | InterpretationSchool>(SCHOOL_KEY, '')
}

export function setPreferredSchool(school: '' | InterpretationSchool) {
  setStorage(SCHOOL_KEY, school)
}
