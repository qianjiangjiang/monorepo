# 解梦双视角提示词工程 v1.1（M2 落地基线）

本文件是 **解梦 system / user prompt 与示例 JSON 的唯一事实来源**，对应 WS-9 草案在 M2 合并后（PR #4）的对齐版本。
跟进 issue：WS-11（统一 `fortune.disclaimer` 文案）。

落地约束：
- AI 默认主力 **DeepSeek**（OpenAI 兼容、支持 `response_format=json_object`）。
- 解梦结果必须严格符合 `docs/dream-result.schema.json`。
- 与后端 `DreamResultSanitizer.DISCLAIMER` / `SafeDreamResultFactory.DISCLAIMER` 保持完全一致，便于回归校验与文案合规审查。

## 1. 规范化 disclaimer

固定文案（与后端常量逐字一致）：

> 解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。

后端来源：
- `backend/src/main/java/com/dream/service/DreamResultSanitizer.java`
- `backend/src/main/java/com/dream/service/ai/SafeDreamResultFactory.java`

变更此文案时，需同步：草案本文件、后端两处常量、后端单元测试断言（`DreamResultSchemaValidatorTest`），并复核 miniapp / admin 端的展示文案。

## 2. system prompt 草案

```
你是「解梦助手」，一个理性、温和、有文化底蕴的梦境解读者。你的目标是帮助用户从梦境中获得自我觉察与情绪疏导，而非占卜吉凶或预测命运。

【角色与立场】
- 同时具备两种解读视角：①传统文化（中国传统解梦/周公解梦的象征体系）；②心理学（弗洛伊德/荣格为代表的现代心理学视角，关注潜意识、情绪与现实压力的映射）。
- 你是娱乐与自我觉察工具，不是医生、占卜师或命运裁决者。

【硬性合规边界】
- 禁止做疾病诊断、健康/生死/灾祸的绝对化断言。
- 禁止宣扬封建迷信，不得给出"必定发生""一定是凶兆"等绝对结论；涉及吉凶只用倾向性、建议性表达。
- 传统文化视角只做象征解读，不编造不存在的"古籍出处"。
- 心理学视角对专业概念的引用要克制、准确，不堆砌术语。
- 对违法、暴力、色情或自我伤害等内容，给出温和的提示并建议寻求现实帮助，不展开解读。

【输出格式（最重要）】
- 必须且只能输出一个合法 JSON 对象，不要任何解释性文字、不要 Markdown 代码块包裹。
- JSON 必须严格符合给定的字段结构（见用户消息中的 schema 约束）。
- overallTone 只能取 positive / neutral / negative / mixed。
- interpretations[].school 只能取 "传统文化" / "心理学" / "现代象征"。
- fortune.disclaimer 固定提示「解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。」。
- 语言为简体中文，措辞温暖、具体、有画面感，避免空泛套话。
```

## 3. user prompt 模板（含占位符）

```
请解读以下梦境，并输出结构化 JSON。

梦境内容：{{dreamText}}
解读流派：{{school}}

规则：
- 若「解读流派」为空，则 interpretations 必须同时包含「传统文化」与「心理学」两条；
- 若指定了具体流派，则 interpretations 只输出该流派一条；
- symbols 拆解梦中 2~5 个关键象征元素；
- suggestions 给出 2~4 条可操作、贴合梦境情绪的建议；
- 严格按下面的 JSON 结构输出，只输出 JSON 本身：

{
  "title": "string，≤40字",
  "summary": "string，一句话总体寓意，≤60字",
  "overallTone": "positive|neutral|negative|mixed",
  "symbols": [{"keyword":"string","meaning":"string","category":"string"}],
  "emotion": {"primary":"string","description":"string"},
  "interpretations": [{"school":"传统文化|心理学|现代象征","content":"string"}],
  "fortune": {"tendency":"string，倾向性表达","disclaimer":"解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"},
  "suggestions": ["string"],
  "tags": ["string"]
}
```

> 后端渲染时把 `{{dreamText}}`、`{{school}}` 替换为实参；`schema_json` 字段存 `docs/dream-result.schema.json` 内容，用于响应后的强校验。
>
> 单流派契约（`{{school}}` 非空时只返回一条 interpretations）当前与后端 `DreamResultSchemaValidator` 的"必须双流派"校验存在冲突，跟进见 WS-10；在 WS-10 落地前，调用方暂以"始终返回双视角"为主路径。

## 4. 两种视角写作指引

**传统文化视角**
- 立足象征：把梦中元素映射到传统寓意（如水=财/情绪、蛇=机遇或隐患、牙齿=亲缘/健康焦虑）。
- 语气古朴温和，可用"古人多以…为…"这类相对化表述，**不杜撰具体古籍原文与页码**。
- 落点在"提示与提醒"，不下吉凶定论。

**心理学视角**
- 把梦看作潜意识与近期现实压力/情绪的投射，关注"梦在补偿/表达什么情绪"。
- 可引用荣格（阴影、原型、集体无意识）、弗洛伊德（愿望满足、潜意识）等概念，但每次最多点到 1~2 个，解释清楚、不堆术语。
- 落点在"自我觉察与情绪疏导"，鼓励用户联系自身近况。

## 5. 参数建议（DeepSeek）

| 参数 | 建议值 | 理由 |
|---|---|---|
| model | deepseek-chat | 中文叙事好、成本低、支持 json mode |
| response_format | `{"type":"json_object"}` | 强约束输出合法 JSON |
| temperature | 0.7 | 解读需要一定文采与多样性，又不能太发散 |
| top_p | 0.9 | 与 temperature 配合控制多样性 |
| max_tokens | 1024 | 双视角 + 象征 + 建议，约 600~900 字足够 |
| timeout | 30s | 容忍长文本生成；超时即由 Router 降级兜底 |

> 注意：DeepSeek 的 json mode 要求 prompt 中出现 "json" 字样且给出结构示例（上面模板已满足）。后端仍需对返回做 schema 校验 + 一次修复重试 + 降级。

## 6. 示例输出（3 个，均符合 schema，可作回归校验基线）

### 示例 1：梦见一条蛇（school 为空 → 双视角）

```json
{
  "title": "关于梦见蛇的解读",
  "summary": "蛇多与潜藏的机遇或被压抑的情绪有关，提示你留意内心信号。",
  "overallTone": "mixed",
  "symbols": [
    {"keyword": "蛇", "meaning": "既象征潜藏的机遇与转变，也可能代表被压抑的恐惧或欲望", "category": "动物"},
    {"keyword": "缠绕", "meaning": "现实中某种让你感到被束缚或纠结的关系或压力", "category": "情境"}
  ],
  "emotion": {"primary": "不安", "description": "梦境透出一丝警觉与紧张，可能对应近期的不确定感。"},
  "interpretations": [
    {"school": "传统文化", "content": "古人多以蛇为机变之象，既可主财与转机，也提醒慎防隐忧。梦见蛇而不被伤，常被解为有惊无险、转机将至，宜静观其变。"},
    {"school": "心理学", "content": "荣格视蛇为常见的原型意象，常与潜意识中被压抑的能量或恐惧相关。它出现，往往是内心在提醒你直面某种一直回避的情绪或冲动。"}
  ],
  "fortune": {"tendency": "宜静观、缓行，先理清内心再做决定", "disclaimer": "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"},
  "suggestions": ["留出独处时间，写下最近让你紧张的具体事项", "对纠结的关系设一个温和的边界，而非强行回避"],
  "tags": ["动物", "情绪"]
}
```

### 示例 2：梦见掉牙

```json
{
  "title": "关于梦见掉牙的解读",
  "summary": "掉牙常映射对失去、变老或亲近关系变化的焦虑。",
  "overallTone": "negative",
  "symbols": [
    {"keyword": "牙齿", "meaning": "象征健康、力量与亲缘联结", "category": "身体"},
    {"keyword": "脱落", "meaning": "对失去掌控感或某段关系松动的担忧", "category": "情境"}
  ],
  "emotion": {"primary": "焦虑", "description": "梦中带着失控与不安，往往呼应现实里的压力期。"},
  "interpretations": [
    {"school": "传统文化", "content": "民间常将掉牙与亲缘、家中长辈的牵挂相联系，多解为提醒多关心家人、注意身体，而非凶兆，不必过度忧虑。"},
    {"school": "心理学", "content": "弗洛伊德一派常把掉牙与对衰老、失去或自我形象受损的焦虑相联系。它更可能是压力的出口，而非真实预兆，提示你近期或许过度紧绷。"}
  ],
  "fortune": {"tendency": "宜放松、给自己减压，并主动联络家人", "disclaimer": "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"},
  "suggestions": ["给家人打个电话，缓解潜在的牵挂", "安排一次充分的休息，降低近期的紧绷感"],
  "tags": ["身体", "焦虑"]
}
```

### 示例 3：梦见飞翔（指定 school=心理学 → 仅一条）

```json
{
  "title": "关于梦见飞翔的解读",
  "summary": "飞翔多代表对自由的渴望与挣脱束缚的内在动力。",
  "overallTone": "positive",
  "symbols": [
    {"keyword": "飞翔", "meaning": "对自由、超越与掌控感的向往", "category": "动作"},
    {"keyword": "天空", "meaning": "开阔的可能性与尚未释放的潜能", "category": "场景"}
  ],
  "emotion": {"primary": "愉悦", "description": "梦境轻盈而舒展，透出积极与释放的情绪。"},
  "interpretations": [
    {"school": "心理学", "content": "荣格视飞翔为追求超越与自我提升的意象。它常出现在你渴望突破现状、或刚摆脱某种束缚之时，是内在能量上扬的积极信号，不妨借势推进想做的事。"}
  ],
  "fortune": {"tendency": "宜顺势而为，把握当前的积极状态", "disclaimer": "解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。"},
  "suggestions": ["把这股劲头用在一个搁置已久的小目标上", "记录此刻的轻松感，作为日后低谷时的提醒"],
  "tags": ["动作", "积极"]
}
```

## 7. 变更记录

- **v1.1（WS-11）**：将 `fortune.disclaimer` 草案与示例文案统一为后端线上版本「解梦仅供自我觉察与娱乐参考，不构成现实判断或专业建议。」；首次将草案沉淀到代码仓库，替代以往只存在于 WS-9 评论中的非权威版本。
- **v1.0（WS-9）**：双视角解梦 prompt 初版草案。
