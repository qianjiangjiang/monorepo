export type OverallTone = 'positive' | 'neutral' | 'negative' | 'mixed'

export type InterpretationSchool = '传统文化' | '心理学' | '现代象征'

export interface DreamSymbol {
  keyword: string
  meaning: string
  category?: string
}

export interface DreamEmotion {
  primary: string
  description: string
}

export interface DreamInterpretation {
  school: InterpretationSchool
  content: string
}

export interface DreamFortune {
  tendency: string
  disclaimer: string
}

export interface DreamResult {
  title: string
  summary: string
  overallTone: OverallTone
  symbols: DreamSymbol[]
  emotion: DreamEmotion
  interpretations: DreamInterpretation[]
  fortune: DreamFortune
  suggestions: string[]
  tags?: string[]
}

export interface DreamRecord {
  dreamRecordId: number
  dreamResultId: number
  dreamText: string
  summary: string
  createdAt: string
  tags: string[]
  school: '' | InterpretationSchool
  favorited?: boolean
  result: DreamResult
}

export interface InterpretDreamPayload {
  dreamText: string
  tags: string[]
  school: '' | InterpretationSchool
}

export interface InterpretDreamResponse {
  dreamRecordId: number
  dreamResultId: number
  school?: '' | InterpretationSchool
  result: DreamResult
}

export interface HistoryResponse {
  total: number
  list: DreamRecord[]
}

export interface DreamDetailResponse {
  dreamRecord: DreamRecord
  result: DreamResult
}

export interface FavoriteResponse {
  favorited: boolean
}

export interface UserProfile {
  id: number
  nickname: string
  avatar: string
}

export interface LoginResponse {
  token: string
  user: UserProfile
}
