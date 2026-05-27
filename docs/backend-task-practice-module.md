# 刷题模块后端开发任务

> **写给后端开发人员**：本文档包含真题库、每日一练、智能组卷三个功能的完整后端需求。试卷批改、错题本、仪表盘 API 已实现，无需改动。
>
> **前置阅读**：`docs/刷题模块后端API需求.md`（接口契约）、`docs/backend-architecture.md`（现有架构）

---

## 一、任务总览

| 任务 | 新 API | 依赖 | 难度 |
|------|--------|------|------|
| 1. question 表扩展 | 无（DDL 变更） | 无 | 低 |
| 2. 真题库 | `GET /v3/exam-archive` | 任务 1 | 低 |
| 3. 每日一练 | `GET /v3/daily-practice` | 任务 1 + DashboardService | 中 |
| 4. 智能组卷 | `POST /v3/paper/smart` | 任务 1 | 中 |

---

## 二、任务 1：question 表扩展

### 2.1 当前表结构（需废弃）

```sql
-- 现有表（4 字段，仅支持全文检索，不满足需求）
CREATE TABLE question (
    id            VARCHAR(64) PRIMARY KEY,
    question_text TEXT NOT NULL,
    tags          VARCHAR(1024),
    province      VARCHAR(32),
    year          INT
);
```

### 2.2 新表结构（替换现有表）

```sql
DROP TABLE IF EXISTS question CASCADE;

CREATE TABLE question (
    -- 基础
    id                VARCHAR(64) PRIMARY KEY,
    question_text     TEXT NOT NULL,
    question_type     VARCHAR(32) NOT NULL DEFAULT 'calculation',
    -- 题型: single_choice / multiple_choice / fill_blank / true_false / calculation / short_answer

    -- 答案
    options           JSONB,           -- 选择题选项，如 ["A. π", "B. 2π", "C. 3π", "D. 4π"]
    correct_answer    TEXT NOT NULL,
    explanation       TEXT,            -- 解析

    -- 分类
    difficulty        SMALLINT DEFAULT 3,  -- 1(易) ~ 5(难)
    subject           VARCHAR(32) NOT NULL,    -- 数学 / 英语 / 政治 / 物理 / 化学
    chapter           VARCHAR(128),           -- 章节，如 "导数与微分"
    knowledge_point   VARCHAR(256),           -- 考点，如 "闭区间最值"
    tags              JSONB,                  -- 灵活标签 ["考研", "高频", "易错"]

    -- 来源
    year              INT,                    -- 真题年份，如 2024
    exam_type         VARCHAR(64),            -- 考研数学一 / 专升本高数 / 高考数学
    source            VARCHAR(32) DEFAULT 'official',  -- official / ai-variant / user-contributed
    linked_official_id VARCHAR(64),           -- 如果是 AI 变式题，指向原始官方真题 ID

    -- 状态
    status            VARCHAR(20) DEFAULT 'published',  -- draft / published / archived
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);

-- 索引
CREATE INDEX idx_question_subject ON question(subject);
CREATE INDEX idx_question_year ON question(year);
CREATE INDEX idx_question_exam ON question(exam_type);
CREATE INDEX idx_question_kp ON question(knowledge_point);
CREATE INDEX idx_question_type ON question(question_type);
CREATE INDEX idx_question_diff ON question(difficulty);
CREATE INDEX idx_question_source ON question(source);
CREATE INDEX idx_question_tags ON question USING GIN(tags);
CREATE INDEX idx_question_text_gin ON question USING GIN(to_tsvector('simple', question_text));
```

### 2.3 种子数据

从 `resources/data/sample-questions.json` 迁移数据到新表结构。该文件当前有 50 道题，需要按新字段补充 `question_type`、`options`、`explanation`、`difficulty`、`exam_type` 等字段。

如果 `sample-questions.json` 数据不足，可以用 AI 辅助生成初次种子数据（见附录 A）。

### 2.4 用户答题记录表（新增）

```sql
CREATE TABLE user_answer_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    question_id     VARCHAR(64) NOT NULL REFERENCES question(id),
    selected_answer TEXT,                -- 用户提交的答案
    is_correct      BOOLEAN,
    time_used       INT,                 -- 答题耗时（秒）
    source          VARCHAR(32),         -- 来源: daily_practice / smart_paper / exam_archive / grading
    session_id      VARCHAR(64),         -- 同一次练习的标识
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_uqr_user ON user_answer_record(user_id);
CREATE INDEX idx_uqr_question ON user_answer_record(question_id);
CREATE INDEX idx_uqr_created ON user_answer_record(created_at);
CREATE UNIQUE INDEX idx_uqr_dedup ON user_answer_record(user_id, question_id, created_at::date);
-- 去重索引：同一用户同一天对同一题的记录去重
```

**用途**：每日一练和智能组卷需要知道用户做过哪些题，避免重复推荐。

---

## 三、任务 2：真题库 API

### 3.1 接口

```
GET /api/v3/exam-archive?subject=数学&year=2024&examType=考研数学一&page=0&size=20
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `subject` | String | 否 | 科目筛选（数学/英语/政治） |
| `year` | Integer | 否 | 真题年份（2024/2023/2022...） |
| `examType` | String | 否 | 考试类型（考研数学一/专升本高数） |
| `knowledgePoint` | String | 否 | 考点筛选 |
| `difficulty` | Integer | 否 | 难度筛选（1-5） |
| `page` | Integer | 否 | 页码，默认 0 |
| `size` | Integer | 否 | 每页条数，默认 20 |

**响应格式**：

```json
{
  "success": true,
  "code": 200,
  "data": [
    {
      "id": "q-2024-math1-03",
      "questionText": "设函数 f(x) 在 [0,1] 上连续...",
      "questionType": "single_choice",
      "options": ["A. 0", "B. 1", "C. 2", "D. 3"],
      "correctAnswer": "B",
      "explanation": "由积分中值定理...",
      "difficulty": 3,
      "subject": "数学",
      "chapter": "积分学",
      "knowledgePoint": "积分中值定理",
      "year": 2024,
      "examType": "考研数学一",
      "source": "official",
      "tags": ["高频", "中值定理"]
    }
  ]
}
```

> ⚠️ 不返回分页元数据（与现有错误本 API 惯例一致）。

### 3.2 实现要点

| 文件 | 说明 |
|------|------|
| `controller/ExamArchiveController.java` | `@RequestMapping("/v3/exam-archive")`，Token 认证 |
| `service/ExamArchiveService.java` | 查询 `question` 表，支持多条件组合筛选 + `ORDER BY year DESC, id ASC` |
| 无需新增 DTO | 返回 `Map<String, Object>`，与现有 Controller 风格一致 |

### 3.3 筛选逻辑

```sql
SELECT * FROM question
WHERE status = 'published'
  AND (:subject IS NULL OR subject = :subject)
  AND (:year IS NULL OR year = :year)
  AND (:examType IS NULL OR exam_type = :examType)
  AND (:knowledgePoint IS NULL OR knowledge_point = :knowledgePoint)
  AND (:difficulty IS NULL OR difficulty = :difficulty)
ORDER BY year DESC, id ASC
LIMIT :size OFFSET :offset
```

### 3.4 年费/考试类型枚举接口（可选，提升前端体验）

```
GET /api/v3/exam-archive/filters
```

返回可用的筛选选项：

```json
{
  "success": true,
  "data": {
    "years": [2024, 2023, 2022, 2021, 2020],
    "examTypes": ["考研数学一", "考研数学二", "考研数学三", "专升本高数", "高考数学"],
    "subjects": ["数学", "英语", "政治"]
  }
}
```

从 question 表 `SELECT DISTINCT` 聚合得到。

---

## 四、任务 3：每日一练 API

### 4.1 接口

```
GET /api/v3/daily-practice?subject=数学&count=5
```

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `subject` | String | 否 | 默认取用户的考试科目 |
| `count` | Integer | 否 | 题目数量，默认 5 |

**响应格式**：

```json
{
  "success": true,
  "code": 200,
  "data": {
    "sessionId": "dp-20260526-abc123",
    "questions": [
      {
        "id": "q-2024-math1-03",
        "questionText": "设函数 f(x) 在 [0,1] 上连续...",
        "questionType": "single_choice",
        "options": ["A. 0", "B. 1", "C. 2", "D. 3"],
        "difficulty": 3,
        "knowledgePoint": "积分中值定理",
        "chapter": "积分学"
      }
    ],
    "totalCount": 5,
    "estimatedTime": 15
  }
}
```

> ⚠️ 每日一练**不直接返回答案**。用户答题后通过另一个接口提交答案并获取批改结果。

### 4.2 选题策略

按优先级取题（每种取够数量再取下一优先级）：

```
优先级 1：用户薄弱考点 + 7 天以上未练习的题目（占 60%，3 道）
  → 从 user_kp_mastery 取 mastery < 0.7 的考点
  → 从 question 表取该考点下用户未做过的题
  → 或已做过但 7 天前做错且未重做的题

优先级 2：高频考点 + 随机抽查（占 40%，2 道）
  → 从 question 表取 tags 包含 "高频" 的题
  → 排除用户今天已经做过的题（idx_uqr_dedup 去重）
```

**实现要点**：

| 文件 | 说明 |
|------|------|
| `controller/DailyPracticeController.java` | `@RequestMapping("/v3/daily-practice")` |
| `service/DailyPracticeService.java` | 选题逻辑 + `user_answer_record` 去重 |

### 4.3 提交答案接口（配套）

```
POST /api/v3/daily-practice/submit
```

```json
// 请求体
{
  "sessionId": "dp-20260526-abc123",
  "answers": [
    { "questionId": "q-2024-math1-03", "selectedAnswer": "B", "timeUsed": 45 },
    { "questionId": "q-2024-math1-07", "selectedAnswer": "3", "timeUsed": 120 }
  ]
}

// 响应
{
  "success": true,
  "data": {
    "totalCount": 5,
    "correctCount": 3,
    "wrongCount": 2,
    "accuracy": 0.6,
    "totalTime": 380,
    "details": [
      {
        "questionId": "q-2024-math1-03",
        "isCorrect": true,
        "correctAnswer": "B",
        "explanation": "由积分中值定理..."
      },
      {
        "questionId": "q-2024-math1-07",
        "isCorrect": false,
        "selectedAnswer": "3",
        "correctAnswer": "6",
        "explanation": "f'(x)=2xlnx+x..."
      }
    ]
  }
}
```

提交后自动：
1. 写入 `user_answer_record` 表
2. 更新 `user_kp_mastery` 掌握度
3. 错题自动录入 `error_book` 表

---

## 五、任务 4：智能组卷 API

### 5.1 接口

```
POST /api/v3/paper/smart
```

**请求体**：

```json
{
  "subject": "数学",
  "examType": "考研数学一",
  "title": "薄弱考点专项练习",
  "questionCount": 10,
  "difficulty": 3,
  "knowledgePoints": ["闭区间最值", "积分中值定理", "二次积分换序"],
  "excludeDone": true
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `subject` | String | 是 | 科目 |
| `examType` | String | 否 | 考试类型 |
| `title` | String | 否 | 试卷标题 |
| `questionCount` | Integer | 否 | 题目总数，默认 10 |
| `difficulty` | Integer | 否 | 目标难度（1-5），默认 3 |
| `knowledgePoints` | Array | 否 | 指定考点列表（不传则自动选薄弱考点） |
| `excludeDone` | Boolean | 否 | 是否排除已做对的题，默认 true |

**响应格式**：

```json
{
  "success": true,
  "data": {
    "paperId": "sp-20260526-xyz789",
    "title": "薄弱考点专项练习",
    "subject": "数学",
    "examType": "考研数学一",
    "totalScore": 100,
    "questionCount": 10,
    "estimatedTime": 60,
    "questions": [
      {
        "index": 1,
        "questionId": "q-2024-math1-03",
        "questionText": "...",
        "questionType": "single_choice",
        "options": ["A. 0", "B. 1", "C. 2", "D. 3"],
        "score": 5,
        "knowledgePoint": "闭区间最值"
      }
    ]
  }
}
```

### 5.2 组卷策略

```
Step 1: 确定考点分布
  - 如果传了 knowledgePoints → 按给定考点平均分配题量
  - 如果没传 → 从 user_kp_mastery 取 mastery 最低的 3-5 个考点

Step 2: 确定难度分布（基于用户当前水平）
  - 查 DashboardService.getMasteryRadar() 获取当前平均掌握度
  - 平均掌握度 < 40% → 60% 简单 + 30% 中等 + 10% 困难
  - 平均掌握度 40-70% → 20% 简单 + 50% 中等 + 30% 困难
  - 平均掌握度 > 70% → 10% 简单 + 40% 中等 + 50% 困难

Step 3: 按题型比例分配
  - 选择题 40% + 填空题 30% + 解答题 30%（可配置）

Step 4: 从 question 表查题
  - 每个考点 × 每种难度 × 每种题型 → 随机取 N 道
  - excludeDone=true 时排除 user_answer_record 中已做对的题
  - 不足时降级（减少难度要求或扩展考点范围）

Step 5: 组装试卷
  - 生成 paperId（UUID 前缀 + 日期）
  - 每题分配分数（按总分数 / 题数，解答题权重 ×2）
  - 选择题在前、填空题居中、解答题在后
```

**实现要点**：

| 文件 | 说明 |
|------|------|
| `controller/SmartPaperController.java` | `@RequestMapping("/v3/paper")` |
| `service/SmartPaperService.java` | 组卷策略 + 查询编排 |
| `service/PaperStrategyService.java` | 难度分布计算 + 题型比例 |

### 5.3 提交组卷答案接口（配套，复用每日一练的提交逻辑）

```
POST /api/v3/paper/submit
```

请求体和响应格式与 `POST /api/v3/daily-practice/submit` 一致，差异在于 `source` 字段标记为 `"smart_paper"`。

---

## 六、与现有代码的关系

### 需要新增的文件

```
Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/
├── controller/
│   ├── ExamArchiveController.java      # 真题库
│   ├── DailyPracticeController.java    # 每日一练
│   └── SmartPaperController.java       # 智能组卷
├── service/
│   ├── ExamArchiveService.java
│   ├── DailyPracticeService.java
│   ├── SmartPaperService.java
│   └── PaperStrategyService.java       # 组卷策略
└── resources/
    └── data/
        └── seed-questions.sql           # 种子数据 SQL（从 sample-questions.json 转换）
```

### 需要修改的现有文件

| 文件 | 修改内容 |
|------|---------|
| `resources/schema.sql` | 替换 question 表 DDL + 新增 user_answer_record 表 |
| `resources/data/sample-questions.json` | 废弃（数据迁移到 seed-questions.sql 后删除） |

### 可以复用的现有组件

| 组件 | 用途 |
|------|------|
| `AdaptiveRecallService` | 每日一练的薄弱考点检索 |
| `DashboardService.getMasteryRadar()` | 智能组卷的难度分布决策 |
| `ErrorBookService.addFromGrading()` | 每日一练/组卷提交后的错题自动入库 |
| `SpacedRepetitionService` | 错题复习周期计算 |
| `UserHolder` | 获取当前用户 ID |
| `ApiResponse` | 统一响应封装 |

---

## 七、数据初始化 Plan

### 第一步：录入 20-30 道官方真题（手工）

从公开渠道（考研数学历年真题 PDF / 专升本真题网站）收集题目，按新表结构录入 `seed-questions.sql`。每题标注 `source = 'official'`。

**覆盖范围**：

| 考试 | 科目 | 建议题量 |
|------|------|---------|
| 考研数学一 | 数学 | 10 道 |
| 考研数学二 | 数学 | 5 道 |
| 专升本高数 | 数学 | 10 道 |
| 高考数学 | 数学 | 5 道 |

### 第二步：AI 生成变式题（辅助）

用 LLM 对每道官方真题生成 2-3 道变式题（改参数、改数字、改场景）。录入时标注：

```sql
source = 'ai-variant'
linked_official_id = 'q-2024-math1-03'  -- 指向原始真题
```

### 第三步：运行时自动扩展

当用户查询某个考点但题库为空时，后端降级调用 LLM 生成题目（`source = 'ai-generated'`）。这是兜底策略，不依赖。

---

## 八、验收标准

### 真题库
- [ ] `GET /v3/exam-archive` 支持 subject/year/examType/knowledgePoint/difficulty 组合筛选
- [ ] `GET /v3/exam-archive/filters` 返回可用的年份、考试类型、科目列表
- [ ] 题库至少有 20 道 `source = 'official'` 的题目
- [ ] Android 真题库页面能正确展示列表 + 筛选 + 题目详情

### 每日一练
- [ ] `GET /v3/daily-practice` 返回 5 道题，优先薄弱考点
- [ ] 不重复推荐用户今天已做过的题
- [ ] `POST /v3/daily-practice/submit` 正确批改 + 记录 + 错题入库
- [ ] Android 每日一练能完整走通"看题 → 答题 → 提交 → 看结果"

### 智能组卷
- [ ] `POST /v3/paper/smart` 支持指定考点/难度/题量
- [ ] 难度分布根据不同水平用户自适应调整
- [ ] 题型比例合理（选择 40% + 填空 30% + 解答 30%）
- [ ] `POST /v3/paper/submit` 正确批改 + 记录
- [ ] Android 智能组卷能走通"设置参数 → 生成试卷 → 答题 → 提交"

---

## 附录 A：AI 生成题目的 System Prompt 模板

```
你是一位考研数学命题专家。请根据以下要求生成一道数学题：

考试类型：{examType}
科目：{subject}
章节：{chapter}
考点：{knowledgePoint}
题型：{questionType}
难度：{difficulty}/5

要求：
1. 题干清晰、无歧义
2. 如果是选择题，4 个选项的干扰项要有迷惑性（常见错误答案）
3. 提供完整的解析过程
4. 题目必须符合该考试的考纲范围
5. 输出格式为 JSON：
{
  "questionText": "...",
  "questionType": "single_choice",
  "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
  "correctAnswer": "B",
  "explanation": "...",
  "difficulty": 3,
  "knowledgePoint": "..."
}

请不要生成和历年真题一模一样的数据，参数需要改编。
```
