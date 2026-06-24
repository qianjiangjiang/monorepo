import type {
  DreamRecord,
  DreamResult,
  InterpretDreamPayload,
  InterpretationSchool,
} from '../types/dream'

const symbolPool = [
  { keyword: '星空', category: '场景', meaning: '代表远方愿望、未知边界，以及你正在寻找更开阔的答案。' },
  { keyword: '水', category: '自然', meaning: '常指向情绪流动，提醒你观察近期的压力与柔软需求。' },
  { keyword: '门', category: '场景', meaning: '象征选择入口，暗示某个阶段正在临界转换。' },
  { keyword: '飞行', category: '动作', meaning: '意味着摆脱限制、恢复掌控感，或期待一种轻盈的变化。' },
  { keyword: '旧人', category: '人物', meaning: '多与未完成的情绪记忆有关，不一定指向真实关系回归。' },
]

const schools: InterpretationSchool[] = ['传统文化', '心理学', '现代象征']

export function createMockDreamResult(payload: InterpretDreamPayload): DreamResult {
  const text = payload.dreamText.trim()
  const pickedSymbols = symbolPool
    .filter((item) => text.includes(item.keyword) || payload.tags.includes(item.keyword))
    .concat(symbolPool)
    .slice(0, 3)
  const selectedSchools = schools.slice(0, 2)

  return {
    title: text.length > 0 ? `关于「${text.slice(0, 8)}」的解读` : '关于梦境的解读',
    summary: '这场梦更像一次来自潜意识的提醒：放慢节奏，辨认真正牵动你的事。',
    overallTone: payload.tags.includes('压力') ? 'mixed' : 'neutral',
    symbols: pickedSymbols.map((symbol) => ({ ...symbol })),
    emotion: {
      primary: payload.tags.includes('焦虑') || payload.tags.includes('压力') ? '焦虑' : '好奇',
      description:
        '梦中的画面带着探索感，也夹杂轻微不确定。它提示你近期正在把注意力投向一个尚未完全成形的选择。',
    },
    interpretations: selectedSchools.map((school) => ({
      school,
      content:
        school === '传统文化'
          ? '从传统意象看，梦境里的光亮与路径象征转机。宜先守住内心秩序，再顺势推进眼前计划。'
          : school === '心理学'
            ? '从心理学视角看，梦境把现实压力转化为可观察的象征。重点不是预示结果，而是识别你对变化的期待与担忧。'
            : '从现代象征看，这些画面像是生活节奏的压缩影像。它鼓励你把模糊目标拆成更具体的下一步。',
    })),
    fortune: {
      tendency: payload.tags.includes('压力') ? '宜静观' : '顺势而为',
      disclaimer: '解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。',
    },
    suggestions: [
      '把梦里最强烈的一个画面写下来，记录它对应的现实感受。',
      '今天先完成一件小而确定的事，用行动降低不确定感。',
      '若梦境反复造成困扰，建议与可信任的人或专业人士聊聊。',
    ],
    tags: payload.tags,
  }
}

export function createDreamRecord(payload: InterpretDreamPayload): DreamRecord {
  const now = Date.now()
  const result = createMockDreamResult(payload)

  return {
    dreamRecordId: now,
    dreamResultId: now + 1,
    dreamText: payload.dreamText,
    summary: result.summary,
    createdAt: new Date(now).toISOString(),
    tags: payload.tags,
    school: payload.school,
    favorited: false,
    result,
  }
}

export function createSeedHistory(): DreamRecord[] {
  return [
    createDreamRecord({
      dreamText: '我梦见自己在星空下推开一扇门，门后有很亮的水面。',
      tags: ['星空', '反复出现'],
      school: '',
    }),
    createDreamRecord({
      dreamText: '梦见自己在城市上空飞行，醒来后有一点焦虑。',
      tags: ['飞行', '焦虑'],
      school: '',
    }),
  ].map((record, index) => ({
    ...record,
    dreamRecordId: (record.dreamRecordId ?? Date.now()) - index * 86400000,
    dreamResultId: (record.dreamResultId ?? Date.now()) - index * 86400000,
    createdAt: new Date(Date.now() - index * 86400000).toISOString(),
  }))
}
