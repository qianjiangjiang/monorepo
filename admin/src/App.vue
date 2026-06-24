<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  CirclePlus,
  Connection,
  Delete,
  Document,
  Edit,
  Key,
  Lock,
  Refresh,
  SwitchButton,
  Tickets,
  View,
  Warning,
} from '@element-plus/icons-vue'
import {
  ApiError,
  adminLogin,
  apiBaseUrl,
  clearToken,
  getToken,
  request,
  setToken,
  type AdminDreamRecord,
  type AiProviderConfig,
  type AiProviderPayload,
  type AiProviderTestResponse,
  type PageResponse,
  type PromptTemplate,
  type PromptTemplatePayload,
  type SensitiveWord,
  type SensitiveWordPayload,
} from './api'

type TabKey = 'ai' | 'prompt' | 'sensitive' | 'records'
type LoadingKey = 'ai' | 'prompt' | 'sensitive' | 'records' | 'refresh'

interface ProviderForm extends AiProviderPayload {
  apiKey: string
}

interface PromptForm extends PromptTemplatePayload {
  schemaJson: string
  remark: string
}

interface SensitiveForm extends SensitiveWordPayload {
  id: number | null
}

const tabs: Array<{ key: TabKey; label: string; icon: typeof Connection; subtitle: string }> = [
  { key: 'ai', label: 'AI 渠道', icon: Connection, subtitle: '模型渠道、权重、连通性与配置缓存' },
  { key: 'prompt', label: '提示词模板', icon: Document, subtitle: '场景版本、Schema、预览与启停' },
  { key: 'sensitive', label: '敏感词', icon: Warning, subtitle: '阻断与审核词库维护' },
  { key: 'records', label: '解梦记录', icon: Tickets, subtitle: '输入输出、渠道、Token 与状态回看' },
]

const activeTab = ref<TabKey>('ai')
const isAuthed = ref(Boolean(getToken()))
const loginForm = reactive({ username: '', password: '' })
const loginLoading = ref(false)
const loading = reactive<Record<LoadingKey, boolean>>({
  ai: false,
  prompt: false,
  sensitive: false,
  records: false,
  refresh: false,
})

const providers = ref<AiProviderConfig[]>([])
const prompts = ref<PromptTemplate[]>([])
const sensitiveWords = ref<SensitiveWord[]>([])
const records = ref<AdminDreamRecord[]>([])
const recordsPage = reactive({ page: 1, size: 10, total: 0 })

const providerDrawerVisible = ref(false)
const promptDrawerVisible = ref(false)
const sensitiveDrawerVisible = ref(false)
const bulkSensitiveDrawerVisible = ref(false)
const testingProviderId = ref<number | null>(null)
const recordDrawer = reactive<{ visible: boolean; record: AdminDreamRecord | null }>({
  visible: false,
  record: null,
})

const providerForm = reactive<ProviderForm>(newProviderForm())
const promptForm = reactive<PromptForm>(newPromptForm())
const sensitiveForm = reactive<SensitiveForm>(newSensitiveForm())
const sensitiveBulkForm = reactive({ words: '', type: 'block' })
const previewForm = reactive({
  dreamText: '梦见在水边寻找一扇门，远处有人叫我的名字。',
  school: '全部，必须包含传统文化与心理学；用户展示偏好：心理学',
})

const router = useRouter()
const route = useRoute()

const currentTab = computed(() => tabs.find((tab) => tab.key === activeTab.value) || tabs[0])
const showingLogin = computed(() => route.name === 'login' || !isAuthed.value)
const totalProviderWeight = computed(() => providers.value.reduce((sum, item) => sum + Number(item.weight || 0), 0))
const enabledProviderCount = computed(() => providers.value.filter((item) => item.enabled).length)
const enabledPromptCount = computed(() => prompts.value.filter((item) => item.enabled).length)
const blockSensitiveCount = computed(() => sensitiveWords.value.filter((item) => item.type === 'block').length)
const reviewSensitiveCount = computed(() => sensitiveWords.value.filter((item) => item.type === 'review').length)
const promptSceneRowspans = computed(() =>
  prompts.value.map((prompt, index, list) => {
    if (index > 0 && list[index - 1].sceneCode === prompt.sceneCode) {
      return 0
    }
    const nextSceneIndex = list.slice(index).findIndex((item) => item.sceneCode !== prompt.sceneCode)
    return nextSceneIndex === -1 ? list.length - index : nextSceneIndex
  }),
)
const promptPreviewUser = computed(() =>
  renderPrompt(promptForm.userPromptTemplate, previewForm.dreamText, previewForm.school),
)
const recordResultPreview = computed(() => prettyJson(recordDrawer.record?.resultJson))

const providerOptions = ['deepseek', 'qwen', 'openai', 'zhipu']
const responseFormatOptions = ['json_object', 'function_call', 'text']
const sensitiveTypeOptions = [
  { label: '阻断', value: 'block' },
  { label: '审核', value: 'review' },
]

onMounted(() => {
  if (isAuthed.value) {
    void loadDashboard()
  }
})

function newProviderForm(): ProviderForm {
  return {
    id: null,
    name: '',
    provider: 'deepseek',
    baseUrl: '',
    apiKey: '',
    model: '',
    temperature: 0.7,
    maxTokens: 1024,
    topP: 1,
    timeoutMs: 30000,
    responseFormat: 'json_object',
    enabled: true,
    priority: 100,
    weight: 100,
  }
}

function newPromptForm(): PromptForm {
  return {
    id: null,
    sceneCode: 'interpret',
    version: 'v1',
    systemPrompt: '',
    userPromptTemplate:
      '请解读以下梦境：\n{{dreamText}}\n\n流派展示偏好：{{school}}\n\ninterpretations 必须同时包含“传统文化”和“心理学”两条。',
    schemaJson: '',
    enabled: true,
    remark: '',
  }
}

function newSensitiveForm(): SensitiveForm {
  return {
    id: null,
    word: '',
    type: 'block',
  }
}

async function submitLogin() {
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.warning('请输入管理员账号和密码')
    return
  }
  loginLoading.value = true
  try {
    const result = await adminLogin(loginForm.username.trim(), loginForm.password)
    if (result.role !== 'admin') {
      throw new ApiError('当前账号不是管理员', 401, 401)
    }
    setToken(result.token)
    isAuthed.value = true
    await router.replace({ name: 'console' })
    await loadDashboard()
    ElMessage.success('登录成功')
  } catch (error) {
    if (error instanceof ApiError && (error.status === 401 || error.code === 401)) {
      ElMessage.error('管理员账号或密码错误')
    } else {
      handleApiError(error)
    }
  } finally {
    loginLoading.value = false
  }
}

function logout() {
  clearToken()
  isAuthed.value = false
  void router.replace({ name: 'login' })
}

function handleMenuSelect(key: string) {
  activeTab.value = key as TabKey
  if (key === 'records' && records.value.length === 0) {
    void loadRecords()
  }
}

async function loadDashboard() {
  await Promise.all([loadProviders(), loadPrompts(), loadSensitiveWords(), loadRecords()])
}

async function withLoading(key: LoadingKey, job: () => Promise<void>) {
  loading[key] = true
  try {
    await job()
  } catch (error) {
    handleApiError(error)
  } finally {
    loading[key] = false
  }
}

function handleApiError(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.code === 401)) {
    clearToken()
    isAuthed.value = false
    void router.replace({ name: 'login' })
    ElMessage.error('登录已失效')
    return
  }
  ElMessage.error(error instanceof Error ? error.message : '请求失败')
}

async function loadProviders() {
  await withLoading('ai', async () => {
    providers.value = await request<AiProviderConfig[]>('/admin/ai/config')
  })
}

async function loadPrompts() {
  await withLoading('prompt', async () => {
    prompts.value = await request<PromptTemplate[]>('/admin/prompt')
  })
}

async function loadSensitiveWords() {
  await withLoading('sensitive', async () => {
    sensitiveWords.value = await request<SensitiveWord[]>('/admin/sensitive')
  })
}

async function loadRecords() {
  await withLoading('records', async () => {
    const page = await request<PageResponse<AdminDreamRecord>>(
      `/admin/dream/records?page=${recordsPage.page}&size=${recordsPage.size}`,
    )
    records.value = page.list
    recordsPage.total = page.total
  })
}

async function refreshConfig(showMessage = true) {
  loading.refresh = true
  try {
    await request<{ refreshed: boolean }>('/admin/ai/refresh', { method: 'POST' })
    if (showMessage) {
      ElMessage.success('配置缓存已刷新')
    }
  } catch (error) {
    handleApiError(error)
  } finally {
    loading.refresh = false
  }
}

function openCreateProvider() {
  Object.assign(providerForm, newProviderForm())
  providerDrawerVisible.value = true
}

function openEditProvider(row: AiProviderConfig) {
  Object.assign(providerForm, {
    id: row.id,
    name: row.name,
    provider: row.provider,
    baseUrl: row.baseUrl,
    apiKey: '',
    model: row.model,
    temperature: Number(row.temperature),
    maxTokens: row.maxTokens,
    topP: Number(row.topP),
    timeoutMs: row.timeoutMs,
    responseFormat: row.responseFormat,
    enabled: row.enabled,
    priority: row.priority,
    weight: row.weight,
  })
  providerDrawerVisible.value = true
}

async function saveProvider() {
  if (!providerForm.name.trim() || !providerForm.provider.trim() || !providerForm.baseUrl.trim() || !providerForm.model.trim()) {
    ElMessage.warning('请补全渠道名称、Provider、Base URL 和模型')
    return
  }
  if (!providerForm.id && !providerForm.apiKey.trim()) {
    ElMessage.warning('新增渠道需要 API Key')
    return
  }
  const payload = providerPayload(providerForm)
  try {
    await request<AiProviderConfig>('/admin/ai/config', { method: 'POST', body: payload })
    await refreshConfig(false)
    providerDrawerVisible.value = false
    ElMessage.success('AI 渠道已保存')
    await loadProviders()
  } catch (error) {
    handleApiError(error)
  }
}

async function setProviderEnabled(row: AiProviderConfig, value: string | number | boolean) {
  const previous = row.enabled
  row.enabled = Boolean(value)
  try {
    await request<AiProviderConfig>('/admin/ai/config', {
      method: 'POST',
      body: providerPayload({ ...row, apiKey: '' }),
    })
    await refreshConfig(false)
    ElMessage.success(row.enabled ? '渠道已启用' : '渠道已停用')
  } catch (error) {
    row.enabled = previous
    handleApiError(error)
  }
}

async function deleteProvider(row: AiProviderConfig) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除 AI 渠道', { type: 'warning' })
    await request<void>(`/admin/ai/config/${row.id}`, { method: 'DELETE' })
    await refreshConfig(false)
    ElMessage.success('AI 渠道已删除')
    await loadProviders()
  } catch (error) {
    if (error !== 'cancel') {
      handleApiError(error)
    }
  }
}

async function testProvider(row: AiProviderConfig) {
  testingProviderId.value = row.id
  try {
    const result = await request<AiProviderTestResponse>(`/admin/ai/config/${row.id}/test`, { method: 'POST' })
    ElMessage({
      type: result.success ? 'success' : 'warning',
      message: `${result.provider}/${result.model}: ${result.message}`,
    })
  } catch (error) {
    handleApiError(error)
  } finally {
    testingProviderId.value = null
  }
}

function providerPayload(source: ProviderForm | (AiProviderConfig & { apiKey: string })): AiProviderPayload {
  return {
    id: source.id,
    name: source.name.trim(),
    provider: source.provider.trim(),
    baseUrl: source.baseUrl.trim(),
    apiKey: source.apiKey.trim() || undefined,
    model: source.model.trim(),
    temperature: Number(source.temperature),
    maxTokens: Number(source.maxTokens),
    topP: Number(source.topP),
    timeoutMs: Number(source.timeoutMs),
    responseFormat: source.responseFormat,
    enabled: source.enabled,
    priority: Number(source.priority),
    weight: Number(source.weight),
  }
}

function openCreatePrompt() {
  Object.assign(promptForm, newPromptForm())
  promptDrawerVisible.value = true
}

function openEditPrompt(row: PromptTemplate) {
  Object.assign(promptForm, {
    id: row.id,
    sceneCode: row.sceneCode,
    version: row.version,
    systemPrompt: row.systemPrompt,
    userPromptTemplate: row.userPromptTemplate,
    schemaJson: row.schemaJson || '',
    enabled: row.enabled,
    remark: row.remark || '',
  })
  promptDrawerVisible.value = true
}

async function savePrompt() {
  if (!promptForm.sceneCode.trim() || !promptForm.version.trim() || !promptForm.systemPrompt.trim() || !promptForm.userPromptTemplate.trim()) {
    ElMessage.warning('请补全场景、版本、系统提示词和用户提示词')
    return
  }
  try {
    await request<PromptTemplate>('/admin/prompt', { method: 'POST', body: promptPayload(promptForm) })
    await refreshConfig(false)
    promptDrawerVisible.value = false
    ElMessage.success('提示词模板已保存')
    await loadPrompts()
  } catch (error) {
    handleApiError(error)
  }
}

async function setPromptEnabled(row: PromptTemplate, value: string | number | boolean) {
  const previous = row.enabled
  row.enabled = Boolean(value)
  try {
    await request<PromptTemplate>('/admin/prompt', {
      method: 'POST',
      body: promptPayload({ ...row, schemaJson: row.schemaJson || '', remark: row.remark || '' }),
    })
    await refreshConfig(false)
    ElMessage.success(row.enabled ? '模板已启用' : '模板已停用')
  } catch (error) {
    row.enabled = previous
    handleApiError(error)
  }
}

async function deletePrompt(row: PromptTemplate) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.sceneCode} / ${row.version}」？`, '删除提示词模板', { type: 'warning' })
    await request<void>(`/admin/prompt/${row.id}`, { method: 'DELETE' })
    await refreshConfig(false)
    ElMessage.success('提示词模板已删除')
    await loadPrompts()
  } catch (error) {
    if (error !== 'cancel') {
      handleApiError(error)
    }
  }
}

function promptPayload(source: PromptForm): PromptTemplatePayload {
  return {
    id: source.id,
    sceneCode: source.sceneCode.trim(),
    version: source.version.trim(),
    systemPrompt: source.systemPrompt,
    userPromptTemplate: source.userPromptTemplate,
    schemaJson: source.schemaJson.trim() || null,
    enabled: source.enabled,
    remark: source.remark.trim() || null,
  }
}

function openCreateSensitiveWord() {
  Object.assign(sensitiveForm, newSensitiveForm())
  sensitiveDrawerVisible.value = true
}

function openBulkSensitiveWords() {
  sensitiveBulkForm.words = ''
  sensitiveBulkForm.type = 'block'
  bulkSensitiveDrawerVisible.value = true
}

function openEditSensitiveWord(row: SensitiveWord) {
  Object.assign(sensitiveForm, {
    id: row.id,
    word: row.word,
    type: row.type,
  })
  sensitiveDrawerVisible.value = true
}

async function saveSensitiveWord() {
  if (!sensitiveForm.word.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  try {
    await request<SensitiveWord>('/admin/sensitive', { method: 'POST', body: sensitivePayload(sensitiveForm) })
    sensitiveDrawerVisible.value = false
    ElMessage.success('敏感词已保存')
    await loadSensitiveWords()
  } catch (error) {
    handleApiError(error)
  }
}

async function saveBulkSensitiveWords() {
  const words = Array.from(new Set(
    sensitiveBulkForm.words
      .split(/[\n,，;；]+/)
      .map((word) => word.trim())
      .filter(Boolean),
  ))
  if (words.length === 0) {
    ElMessage.warning('请输入要批量新增的敏感词')
    return
  }
  try {
    for (const word of words) {
      await request<SensitiveWord>('/admin/sensitive', {
        method: 'POST',
        body: { word, type: sensitiveBulkForm.type },
      })
    }
    bulkSensitiveDrawerVisible.value = false
    ElMessage.success(`已新增 ${words.length} 个敏感词`)
    await loadSensitiveWords()
  } catch (error) {
    handleApiError(error)
  }
}

async function deleteSensitiveWord(row: SensitiveWord) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.word}」？`, '删除敏感词', { type: 'warning' })
    await request<void>(`/admin/sensitive/${row.id}`, { method: 'DELETE' })
    ElMessage.success('敏感词已删除')
    await loadSensitiveWords()
  } catch (error) {
    if (error !== 'cancel') {
      handleApiError(error)
    }
  }
}

function sensitivePayload(source: SensitiveForm): SensitiveWordPayload {
  return {
    id: source.id,
    word: source.word.trim(),
    type: source.type,
  }
}

function openRecord(row: AdminDreamRecord) {
  recordDrawer.record = row
  recordDrawer.visible = true
}

function handleRecordPageChange(page: number) {
  recordsPage.page = page
  void loadRecords()
}

function handleRecordSizeChange(size: number) {
  recordsPage.size = size
  recordsPage.page = 1
  void loadRecords()
}

function renderPrompt(template: string, dreamText: string, school: string) {
  return template.replaceAll('{{dreamText}}', dreamText).replaceAll('{{school}}', school)
}

function prettyJson(value: string | null | undefined) {
  if (!value) {
    return ''
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function tokenTotal(row: AdminDreamRecord) {
  return Number(row.tokenIn || 0) + Number(row.tokenOut || 0)
}

function spanPromptScene({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (columnIndex !== 0) {
    return { rowspan: 1, colspan: 1 }
  }
  const rowspan = promptSceneRowspans.value[rowIndex] || 0
  return { rowspan, colspan: rowspan === 0 ? 0 : 1 }
}
</script>

<template>
  <main v-if="showingLogin" class="login-shell">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="brand">
        <span class="brand-mark">D</span>
        <div>
          <h1 id="login-title">解梦管理端</h1>
          <p>管理员账号登录 · {{ apiBaseUrl() }}</p>
        </div>
      </div>

      <el-form class="login-form" :model="loginForm" label-position="top" @submit.prevent="submitLogin">
        <el-form-item label="管理员账号">
          <el-input
            v-model="loginForm.username"
            autocomplete="username"
            placeholder="admin"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            autocomplete="current-password"
            placeholder="password"
            show-password
          />
        </el-form-item>
        <el-button class="login-button" size="large" type="primary" native-type="submit" :loading="loginLoading">
          <el-icon><Lock /></el-icon>
          登录
        </el-button>
      </el-form>
    </section>
  </main>

  <el-container v-else class="console-shell">
    <el-aside class="sidebar" width="232px">
      <div class="sidebar-brand">
        <span class="brand-mark">D</span>
        <div>
          <strong>解梦管理端</strong>
          <span>AI Ops Console</span>
        </div>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeTab" @select="handleMenuSelect">
        <el-menu-item v-for="item in tabs" :key="item.key" :index="item.key">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar" height="72px">
        <div>
          <h1>{{ currentTab.label }}</h1>
          <p>{{ currentTab.subtitle }}</p>
        </div>
        <div class="topbar-actions">
          <el-button :icon="Refresh" :loading="loading.refresh" @click="() => refreshConfig()">刷新配置缓存</el-button>
          <el-button :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="workspace">
        <section v-show="activeTab === 'ai'" class="workspace-section">
          <div class="section-toolbar">
            <div class="metric-strip">
              <span>渠道 {{ providers.length }}</span>
              <span>启用 {{ enabledProviderCount }}</span>
              <span>总权重 {{ totalProviderWeight }}</span>
            </div>
            <div class="toolbar-actions">
              <el-button :icon="Refresh" :loading="loading.ai" @click="loadProviders">刷新</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openCreateProvider">新增渠道</el-button>
            </div>
          </div>

          <el-table v-loading="loading.ai" :data="providers" height="calc(100vh - 184px)" border>
            <el-table-column prop="name" label="名称" min-width="150" fixed />
            <el-table-column label="Provider / Model" min-width="190">
              <template #default="{ row }">
                <div class="stacked-cell">
                  <strong>{{ row.provider }}</strong>
                  <span>{{ row.model }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="baseUrl" label="Base URL" min-width="260" show-overflow-tooltip />
            <el-table-column label="参数" min-width="190">
              <template #default="{ row }">
                <div class="tag-row">
                  <el-tag size="small">P{{ row.priority }}</el-tag>
                  <el-tag size="small" type="success">W{{ row.weight }}</el-tag>
                  <el-tag size="small" type="info">T{{ row.temperature }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="apiKeyMasked" label="API Key" min-width="140" />
            <el-table-column label="启用" width="92" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" @change="setProviderEnabled(row, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :icon="Connection" :loading="testingProviderId === row.id" @click="testProvider(row)">
                  测试
                </el-button>
                <el-button size="small" :icon="Edit" @click="openEditProvider(row)">编辑</el-button>
                <el-button size="small" type="danger" :icon="Delete" @click="deleteProvider(row)" />
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-show="activeTab === 'prompt'" class="workspace-section">
          <div class="section-toolbar">
            <div class="metric-strip">
              <span>模板 {{ prompts.length }}</span>
              <span>启用 {{ enabledPromptCount }}</span>
            </div>
            <div class="toolbar-actions">
              <el-button :icon="Refresh" :loading="loading.prompt" @click="loadPrompts">刷新</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openCreatePrompt">新增模板</el-button>
            </div>
          </div>

          <el-table v-loading="loading.prompt" :data="prompts" :span-method="spanPromptScene" height="calc(100vh - 184px)" border>
            <el-table-column prop="sceneCode" label="场景" min-width="130" fixed />
            <el-table-column prop="version" label="版本" min-width="130" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column prop="systemPrompt" label="系统提示词" min-width="260" show-overflow-tooltip />
            <el-table-column prop="userPromptTemplate" label="用户提示词" min-width="280" show-overflow-tooltip />
            <el-table-column label="Schema" width="96" align="center">
              <template #default="{ row }">
                <el-tag :type="row.schemaJson ? 'success' : 'info'" size="small">
                  {{ row.schemaJson ? '已关联' : '未关联' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="92" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" @change="setPromptEnabled(row, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :icon="Edit" @click="openEditPrompt(row)">编辑</el-button>
                <el-button size="small" type="danger" :icon="Delete" @click="deletePrompt(row)" />
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-show="activeTab === 'sensitive'" class="workspace-section">
          <div class="section-toolbar">
            <div class="metric-strip">
              <span>词条 {{ sensitiveWords.length }}</span>
              <span>阻断 {{ blockSensitiveCount }}</span>
              <span>审核 {{ reviewSensitiveCount }}</span>
            </div>
            <div class="toolbar-actions">
              <el-button :icon="Refresh" :loading="loading.sensitive" @click="loadSensitiveWords">刷新</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openCreateSensitiveWord">新增词条</el-button>
              <el-button type="primary" plain :icon="CirclePlus" @click="openBulkSensitiveWords">批量新增</el-button>
            </div>
          </div>

          <el-table v-loading="loading.sensitive" :data="sensitiveWords" height="calc(100vh - 184px)" border>
            <el-table-column prop="word" label="敏感词" min-width="180" fixed />
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="row.type === 'block' ? 'danger' : 'warning'">
                  {{ row.type === 'block' ? '阻断' : '审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :icon="Edit" @click="openEditSensitiveWord(row)">编辑</el-button>
                <el-button size="small" type="danger" :icon="Delete" @click="deleteSensitiveWord(row)" />
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-show="activeTab === 'records'" class="workspace-section">
          <div class="section-toolbar">
            <div class="metric-strip">
              <span>总数 {{ recordsPage.total }}</span>
              <span>当前页 {{ records.length }}</span>
            </div>
            <el-button :icon="Refresh" :loading="loading.records" @click="loadRecords">刷新</el-button>
          </div>

          <el-table v-loading="loading.records" :data="records" height="calc(100vh - 232px)" border>
            <el-table-column prop="dreamRecordId" label="记录 ID" width="98" fixed />
            <el-table-column prop="dreamText" label="输入" min-width="260" show-overflow-tooltip />
            <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
            <el-table-column label="渠道" min-width="170">
              <template #default="{ row }">
                <div class="stacked-cell">
                  <strong>{{ row.provider || '-' }}</strong>
                  <span>{{ row.model || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Token" width="128">
              <template #default="{ row }">
                {{ row.tokenIn || 0 }} / {{ row.tokenOut || 0 }}
                <span class="muted">({{ tokenTotal(row) }})</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'">
                  {{ row.status || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="108" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :icon="View" @click="openRecord(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="recordsPage.page"
              v-model:page-size="recordsPage.size"
              :total="recordsPage.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              @current-change="handleRecordPageChange"
              @size-change="handleRecordSizeChange"
            />
          </div>
        </section>
      </el-main>
    </el-container>
  </el-container>

  <el-drawer v-model="providerDrawerVisible" :title="providerForm.id ? '编辑 AI 渠道' : '新增 AI 渠道'" size="560px">
    <el-form class="drawer-form" :model="providerForm" label-position="top">
      <div class="form-grid two">
        <el-form-item label="名称">
          <el-input v-model="providerForm.name" placeholder="DeepSeek 主渠道" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="providerForm.provider" filterable allow-create>
            <el-option v-for="item in providerOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="Base URL">
        <el-input v-model="providerForm.baseUrl" placeholder="https://api.deepseek.com" />
      </el-form-item>
      <div class="form-grid two">
        <el-form-item label="模型">
          <el-input v-model="providerForm.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="providerForm.apiKey" :prefix-icon="Key" show-password :placeholder="providerForm.id ? '留空保持原密钥' : 'sk-...'" />
        </el-form-item>
      </div>
      <div class="form-grid three">
        <el-form-item label="Temperature">
          <el-input-number v-model="providerForm.temperature" :min="0" :max="2" :step="0.1" :precision="2" />
        </el-form-item>
        <el-form-item label="Top P">
          <el-input-number v-model="providerForm.topP" :min="0" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="providerForm.maxTokens" :min="1" :max="32000" />
        </el-form-item>
      </div>
      <div class="form-grid three">
        <el-form-item label="超时 ms">
          <el-input-number v-model="providerForm.timeoutMs" :min="1000" :max="120000" :step="1000" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="providerForm.priority" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="providerForm.weight" :min="0" :max="10000" />
        </el-form-item>
      </div>
      <div class="form-grid two">
        <el-form-item label="响应格式">
          <el-select v-model="providerForm.responseFormat">
            <el-option v-for="item in responseFormatOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="providerForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="providerDrawerVisible = false">取消</el-button>
      <el-button type="primary" @click="saveProvider">保存</el-button>
    </template>
  </el-drawer>

  <el-drawer v-model="promptDrawerVisible" :title="promptForm.id ? '编辑提示词模板' : '新增提示词模板'" size="720px">
    <el-form class="drawer-form" :model="promptForm" label-position="top">
      <div class="form-grid three">
        <el-form-item label="场景">
          <el-input v-model="promptForm.sceneCode" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="promptForm.version" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="promptForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model="promptForm.remark" />
      </el-form-item>
      <el-form-item label="系统提示词">
        <el-input v-model="promptForm.systemPrompt" type="textarea" :autosize="{ minRows: 4, maxRows: 8 }" />
      </el-form-item>
      <el-form-item label="用户提示词">
        <el-input v-model="promptForm.userPromptTemplate" type="textarea" :autosize="{ minRows: 6, maxRows: 12 }" />
      </el-form-item>
      <el-form-item label="Schema JSON">
        <el-input v-model="promptForm.schemaJson" type="textarea" :autosize="{ minRows: 5, maxRows: 12 }" />
      </el-form-item>

      <div class="preview-panel">
        <div class="form-grid two">
          <el-input v-model="previewForm.dreamText" />
          <el-input v-model="previewForm.school" />
        </div>
        <div class="preview-grid">
          <div>
            <strong>System</strong>
            <pre>{{ promptForm.systemPrompt }}</pre>
          </div>
          <div>
            <strong>User</strong>
            <pre>{{ promptPreviewUser }}</pre>
          </div>
        </div>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="promptDrawerVisible = false">取消</el-button>
      <el-button type="primary" @click="savePrompt">保存</el-button>
    </template>
  </el-drawer>

  <el-drawer v-model="sensitiveDrawerVisible" :title="sensitiveForm.id ? '编辑敏感词' : '新增敏感词'" size="420px">
    <el-form class="drawer-form" :model="sensitiveForm" label-position="top">
      <el-form-item label="敏感词">
        <el-input v-model="sensitiveForm.word" />
      </el-form-item>
      <el-form-item label="类型">
        <el-segmented v-model="sensitiveForm.type" :options="sensitiveTypeOptions" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="sensitiveDrawerVisible = false">取消</el-button>
      <el-button type="primary" @click="saveSensitiveWord">保存</el-button>
    </template>
  </el-drawer>

  <el-drawer v-model="bulkSensitiveDrawerVisible" title="批量新增敏感词" size="460px">
    <el-form class="drawer-form" :model="sensitiveBulkForm" label-position="top">
      <el-form-item label="类型">
        <el-segmented v-model="sensitiveBulkForm.type" :options="sensitiveTypeOptions" />
      </el-form-item>
      <el-form-item label="敏感词">
        <el-input
          v-model="sensitiveBulkForm.words"
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 18 }"
          placeholder="每行一个，也可用逗号或分号分隔"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="bulkSensitiveDrawerVisible = false">取消</el-button>
      <el-button type="primary" @click="saveBulkSensitiveWords">保存</el-button>
    </template>
  </el-drawer>

  <el-drawer v-model="recordDrawer.visible" title="解梦记录详情" size="760px">
    <div v-if="recordDrawer.record" class="record-detail">
      <dl>
        <div>
          <dt>记录</dt>
          <dd>#{{ recordDrawer.record.dreamRecordId }} / 用户 {{ recordDrawer.record.userId }}</dd>
        </div>
        <div>
          <dt>渠道</dt>
          <dd>{{ recordDrawer.record.provider || '-' }} / {{ recordDrawer.record.model || '-' }}</dd>
        </div>
        <div>
          <dt>Token</dt>
          <dd>{{ recordDrawer.record.tokenIn || 0 }} / {{ recordDrawer.record.tokenOut || 0 }} / {{ tokenTotal(recordDrawer.record) }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>{{ recordDrawer.record.status || '-' }}</dd>
        </div>
      </dl>
      <section>
        <h2>输入</h2>
        <p>{{ recordDrawer.record.dreamText }}</p>
        <div class="tag-row">
          <el-tag v-for="tag in recordDrawer.record.tags" :key="tag" size="small">{{ tag }}</el-tag>
        </div>
      </section>
      <section>
        <h2>输出</h2>
        <pre>{{ recordResultPreview }}</pre>
      </section>
    </div>
  </el-drawer>
</template>
