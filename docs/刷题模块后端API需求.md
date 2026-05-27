# 刷题模块后端 API 需求文档（v2 — 对齐后端实现）

## 功能总览

| 功能 | 客户端状态 | 后端要求 |
|---|---|---|
| 试卷批改 | ✅ 已实现（SSE 流式） | **已实现** |
| 错题本 CRUD | ✅ 已实现（REST） | **已实现** |
| 仪表盘数据 | ✅ 已实现（REST） | **已实现** |
| 智能组卷 | ❌ 占位（Toast） | 待设计 |
| 每日一练 | ❌ 占位（Toast） | 待设计 |
| 真题库 | ❌ 占位（Toast） | 待设计 |

---

## 通用约定

- **认证**：所有接口在 Header 中携带 `token` 字段
- **401 处理**：后端返回 401 时客户端自动跳转登录页
- **响应格式**：统一使用 `ApiResponse<T>` 包裹
  ```json
  { "success": true, "code": 200, "message": "操作成功", "data": <T>, "timestamp": "..." }
  ```
- **字段命名**：统一 camelCase（Jackson 默认序列化）
- **JSONB 字段**：`diagnosisJson`、`similarQuestions` 使用 `{"type":"jsonb","value":"<转义后的JSON字符串>"}` 格式，value 是 **字符串** 而非对象/数组
- **Token**：登录接口返回的 token，客户端保存在 `SharedPreferences`

---

## 1. 试卷批改 — `POST /api/v3/grading/submit`

### 请求头
```
Content-Type: application/json
Accept: text/event-stream
token: <用户token>
```

### 请求体
```json
{
  "content": "试卷文本内容（支持OCR识别的手写文字粘贴）",
  "subjectType": "数学",
  "maxScore": 100
}
```
`subjectType` 支持的值：`数学`, `物理`, `化学`, `英语`, `政治`, `历史`

### 响应：SSE 事件流

按顺序发送以下 SSE 事件，每行 `data:` + 空格，空行分隔：

```
data: {"type":"step","data":{"step":"ocr","text":"","current":0,"total":0}}

data: {"type":"step","data":{"step":"extract","text":"8","current":0,"total":8}}

data: {"type":"step","data":{"step":"grade","text":"","current":1,"total":8}}

data: {"type":"step","data":{"step":"diagnose","text":"","current":3,"total":8}}

data: {"type":"complete","data":{"reportId":"xxx","totalScore":85,...}}

```
`complete` 事件后直接 `emitter.complete()` 关闭连接，**不需要 `done` 事件**。

| 事件 `type` | `data` 结构 | 说明 |
|---|---|---|
| `step` | `{"step":"ocr","text":"","current":0,"total":0}` | OCR 识别手写文字 |
| `step` | `{"step":"extract","text":"8","current":0,"total":8}` | 提取题目（`text` = 题目数） |
| `step` | `{"step":"grade","text":"","current":1,"total":8}` | 逐题批改（发多次，current 递增） |
| `step` | `{"step":"diagnose","text":"","current":3,"total":8}` | 诊断错题（发多次） |
| `complete` | 完整 GradingReport 对象 | 批改报告 |
| `error` | `{"type":"error","code":500,"message":"..."}` | 错误（可选） |

> ⚠️ 注意：客户端根据 `step` 值做中文映射：
> - `ocr` → "OCR 识别手写文字..."
> - `extract` → "提取 N 道题目"
> - `grade` → "批改 1/8"
> - `diagnose` → "诊断错题 3"
> - `complete` → "批改完成"

### GradingReport 完整格式
```json
{
  "reportId": "uuid-string",
  "totalScore": 85,
  "maxScore": 100,
  "correctCount": 6,
  "wrongCount": 2,
  "accuracy": 0.75,
  "questions": [
    {
      "id": "q001",
      "index": 1,
      "questionText": "已知 sinα=3/5 求 cosα",
      "studentAnswer": "cosα=0.6",
      "correctAnswer": "cosα=±4/5",
      "isCorrect": false,
      "score": 0,
      "maxScore": 10,
      "knowledgePoint": "三角函数基本关系",
      "diagnosis": { ... },
      "similarQuestions": [ ... ]
    }
  ]
}
```

---

## 2. 错题本 REST API

所有端点前缀：`/api/v3/`

### 2.1 列表 — `GET errors/list?subject=&errorType=&page=0&size=20`

```json
{
  "success": true, "code": 200, "message": "操作成功",
  "data": [
    {
      "id": "6b0a5efc22714acc",
      "questionText": "已知 sinα=3/5 求 cosα",
      "studentAnswer": "cosα=0.6",
      "correctAnswer": "±4/5",
      "knowledgePoint": "三角函数基本关系",
      "errorType": "考点漏缺",
      "subject": "math",
      "reviewLevel": 1,
      "mastered": false,
      "createdAt": "2026-05-26T10:00:00Z",
      "reviewedAt": null,
      "nextReviewAt": "2026-05-27T10:00:00Z"
    }
  ]
}
```

> ⚠️ 不返回 pagination 元数据，直接返回数组。

### 2.2 详情 — `GET errors/{id}`

```json
{
  "success": true, "code": 200, "message": "操作成功",
  "data": {
    "id": "6b0a5efc22714acc",
    "questionText": "已知 sinα=3/5 求 cosα",
    "studentAnswer": "cosα=0.6",
    "correctAnswer": "±4/5",
    "knowledgePoint": "三角函数基本关系",
    "errorType": "考点漏缺",
    "subject": "math",
    "mastered": false,
    "diagnosisJson": {
      "type": "jsonb",
      "value": "{\"aggregate\":null,\"calculationError\":\"```json\\n{\\\"steps\\\":[{\\\"step\\\":\\\"...\\\",\\\"correct\\\":\\\"...\\\",\\\"possibleMistake\\\":\\\"...\\\"}],\\\"mostLikelyError\\\":\\\"...\\\"}\\n```\",\"formulaConfusion\":\"...\",\"prerequisiteCheck\":\"...\"}"
    },
    "similarQuestions": {
      "type": "jsonb",
      "value": "[{\"id\":\"33ee3cd8-ef0e-4f\",\"text\":\"...\",\"tags\":[\"三角函数\"],\"score\":0.85}]"
    }
  }
}
```

**JSONB value 内部结构**（反序列化后的 JSON 对象）：
```json
{
  "aggregate": null,
  "calculationError": "```json\n{\"steps\":[...],\"mostLikelyError\":\"...\"}\n```",
  "formulaConfusion": "```json\n{\"confusions\":[{\"wrong\":\"...\",\"correct\":\"...\"}],\"analysis\":\"...\"}\n```",
  "prerequisiteCheck": "```json\n{\"gaps\":[\"...\"],\"analysis\":\"...\"}\n```"
}
```
每个诊断字段的值是 markdown 代码块 ` ```json...``` ` 包裹的 JSON 字符串。

### 2.3 复习队列 — `GET errors/review-queue`

```json
{
  "success": true, "code": 200,
  "data": [
    {
      "errorId": "6b0a5efc22714acc",
      "questionText": "已知 sinα=3/5 求 cosα",
      "knowledgePoint": "三角函数基本关系",
      "subject": "math",
      "reviewLevel": 1,
      "nextReviewAt": "2026-05-27T10:00:00Z"
    }
  ]
}
```

### 2.4 标记已掌握 — `PUT errors/{id}/mark-mastered`

请求体：无

```json
{ "success": true, "code": 200, "message": "操作成功", "data": {} }
```

### 2.5 同类题 — `POST errors/{id}/similar`

请求体：无

```json
{
  "success": true, "code": 200,
  "data": [
    { "id": "33ee3cd8-ef0e-4f", "text": "...", "tags": ["三角函数"], "score": 0.85 }
  ]
}
```

### 2.6 errorType 可选值

| 值 | 含义 | 客户端显示 |
|---|---|---|
| `考点漏缺` | 知识点漏洞 | AI 三维诊断 → 考点漏缺 |
| `公式混淆` | 公式混淆 | AI 三维诊断 → 公式混淆 |
| `计算失误` | 计算错误 | AI 三维诊断 → 计算失误 |

**使用中文值**，与 UI 显示一致，客户端无需翻译层。

---

## 3. 仪表盘 REST API

### 3.1 概览 — `GET dashboard/overview`

```json
{
  "success": true, "code": 200,
  "data": {
    "totalNotes": 12,
    "totalErrors": 42,
    "masteredErrors": 18,
    "errorRate": 0.35,
    "subjectStats": [
      { "subject": "math", "noteCount": 5, "errorCount": 20 },
      { "subject": "physics", "noteCount": 3, "errorCount": 10 }
    ]
  }
}
```

### 3.2 掌握度雷达 — `GET dashboard/mastery-radar`

```json
{
  "success": true, "code": 200,
  "data": {
    "labels": ["函数与导数", "三角函数", "数列", "概率统计", "解析几何", "立体几何"],
    "values": [0.75, 0.6, 0.85, 0.7, 0.55, 0.8]
  }
}
```

### 3.3 今日任务 — `GET dashboard/today-tasks`

```json
{
  "success": true, "code": 200,
  "data": [
    { "type": "review", "title": "复习 3 道错题", "count": 3, "description": "三角函数、导数" },
    { "type": "practice", "title": "每日一练", "count": 0, "description": "10 道题" }
  ]
}
```
任务 `type` 为 `"practice"` 时，客户端点击会跳转到刷题页面。

### 3.4 周报 — `GET dashboard/weekly-report`

```json
{
  "success": true, "code": 200,
  "data": {
    "weekErrors": 15,
    "weekReports": 8,
    "weekNotes": 2,
    "totalActivity": 25
  }
}
```
客户端用 `weekReports` 作为"累计刷题"数显示。

---

## 4. 待实现功能（产品设计自由定义）

客户端目前只有 Toast 占位，无具体 UI 逻辑。建议接口：

| 功能 | 建议端点 | 说明 |
|---|---|---|
| 智能组卷 | `POST /api/v3/paper/smart` | 请求：科目、知识点、难度、题量；返回试卷题目列表 |
| 每日一练 | `GET /api/v3/daily-practice` | 返回每日推荐练习题列表 |
| 真题库 | `GET /api/v3/exam-archive?subject=&year=` | 返回历年真题试卷列表和详情 |

---

## 5. 对照结果总结

| 项目 | v1 需求文档 | 修正为 | 改动方 |
|---|---|---|---|
| 字段命名 | snake_case | camelCase | ✅ Android 已改 `@SerializedName` |
| SSE 结束事件 | 需要 `done` 事件 | 不要 `done`，`complete` 后直接 `emitter.complete()` | 文档已修正 |
| 错题列表分页 | 含 `pagination` | 纯数组，无分页元数据 | 文档已修正 |
| errorType 值 | `knowledge_gap`（英文） | `考点漏缺`（中文） | 文档已修正 |
