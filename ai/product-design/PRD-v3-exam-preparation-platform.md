# IRAgent Pro v3 产品重定位方案

> **从"通用 AI 解题工具" → "以个人知识库为中心的备考平台"**
>
> 晓文 | 2026.05.22

---

## 一、产品定位一句话

**"你的私人备考 AI —— 读懂你的笔记，对标你的考纲，诊断你的每一道错题。"**

和现有产品的核心区别：

| | 作业帮/猿题库 | ChatGPT/通义 | **IRAgent Pro** |
|---|---|---|---|
| 知识来源 | 通用题库 | 互联网 | **你的笔记 + 你的教材** |
| 答疑方式 | 搜答案 | 直接给答案 | **对照你的笔记讲解** |
| 错题处理 | 收藏 | 无 | **三路诊断 → 溯源到笔记 → 推荐同类题** |
| 试卷批改 | 无/手动 | 无 | **拍照 → 批改 → 诊断 → 组卷** |
| 考纲对标 | 部分 | 无 | **考研/专升本/高考，逐考点覆盖** |

---

## 二、目标用户画像

```
用户 A：小李，22岁，二本计算机专业，备考 985 计算机考研
  - 痛点：数学笔记散乱（纸质 + Notion + iPad），做题时找不到对应的笔记内容
  - 需求：把所有笔记导入一个地方，做题时 AI 能告诉我"这题考的是你笔记第X页的XX知识点"

用户 B：小王，20岁，大专三年级，备考专升本高数 + 英语
  - 痛点：基础薄弱，错题太多，不知道从哪补起
  - 需求：拍照上传模拟卷 → 自动批改 → 告诉我哪些考点薄弱 → 给我类似题练习

用户 C：小张，17岁，高三理科，备考高考
  - 痛点：每天都在刷卷子，但错的题下次还是错
  - 需求：错题自动归类到知识点，定期提醒复习，看到自己的进步曲线
```

---

## 三、核心功能架构（五大模块）

```
┌─────────────────────────────────────────────────────────┐
│                     IRAgent Pro v3                       │
├─────────────┬─────────────┬─────────────┬───────────────┤
│  📚 知识库   │  💬 智能答疑  │  📝 试卷批改  │  📊 备考仪表盘  │
│  Knowledge  │    Q&A      │   Grading   │   Dashboard   │
│  Base       │             │             │               │
├─────────────┴─────────────┴─────────────┴───────────────┤
│              🔗 共享底层能力                               │
│  RAG召回 │ 语义缓存 │ DAG诊断引擎 │ 知识点图谱 │ 多租户隔离  │
└─────────────────────────────────────────────────────────┘
```

### 模块一：个人知识库（核心差异化）

**用户做什么：**
- 上传笔记（拍照/PDF/Word/Markdown）
- 上传教材/参考书（拍照/PDF）
- 选择考试类型（考研数学一/专升本高数/高考数学...）
- 系统自动构建个人知识图谱

**系统做什么：**
- OCR 识别 + 结构化解析
- 按知识点自动切分（不是按字数，是按"这个段落讲的是什么考点"）
- 向量化存入 Milvus（个人 Collection，按 tenantId 隔离）
- 构建 `考点 → 笔记片段` 的双向链接

**关键设计决策：每个用户一个独立的向量 Collection**
- 理由：个人笔记的语义空间和通用题库完全不同，混合检索会降低精度
- 技术实现：复用现有的 Milvus 配置，`tenantId` 作为 Collection 前缀

### 模块二：智能答疑（升级版）

**和 v2 的核心区别：**

```
v2:  学生提问 → AI 从通用知识回答
v3:  学生提问 → AI 检索学生的个人笔记 → 回答中引用笔记原文
     "根据你《高等数学上册》第 3 章笔记，极限的定义是……"
```

**新增 IntentRouter（意图路由）：**
```
用户提问
  → 轻量分类（规则引擎，不消耗 Token）
    ├─ HINT_NEEDED       → 苏格拉底式引导（DeepLearnFlow）
    ├─ FULL_EXPLANATION  → 即时答疑 + Timeline 动画
    ├─ NOTE_REFERENCE    → 直接检索笔记原文
    └─ PRACTICE_READY    → 跳过讲解，直接推题
```

### 模块三：试卷批改（新模块）

**完整流程：**

```
拍照上传试卷
  → OCR 识别（题目区 + 答案区分离）
  → 题目结构化（识别题型：选择/填空/解答）
  → 逐题批改（LLM 判断对错 + 给出得分）
  → 错题触发 DAG 三路诊断
  → 诊断结果溯源到个人笔记
  → 推荐 3 道同类变式题
  → 生成试卷分析报告（得分率、考点覆盖、薄弱环节）
```

### 模块四：备考仪表盘（新模块）

**核心看板：**
- **考点覆盖进度**：考研数学一 186 个考点，已掌握 142 个，薄弱 23 个，未学 21 个
- **错题分布热力图**：按章节/考点维度的错题分布
- **掌握度雷达图**：复用现有 5 维能力模型
- **备考时间线**：距离考试还有 X 天，建议每日刷题量 Y 道

---

## 四、App 信息架构（重新设计）

```
底部 Tab Bar（5 个）
│
├─ 📖 知识库
│   ├── 笔记列表（按科目/章节）
│   ├── 知识图谱（力导向图，考点→笔记→题目）
│   ├── 上传入口（拍照/文件/粘贴）
│   └── 笔记详情（原文 + 关联题目 + 掌握度）
│
├─ 💬 答疑
│   ├── 对话界面（升级：回答末尾显示"📝 参考你的笔记"）
│   ├── 深度学习（苏格拉底式）
│   ├── 拍照提问
│   └── 历史对话
│
├─ 📝 刷题
│   ├── 上传试卷（拍照 → 批改 → 诊断）
│   ├── 智能组卷（基于薄弱点自动出卷）
│   ├── 每日一练（根据备考计划推送）
│   └── 真题库（按年份/省份/题型筛选）
│
├─ ❌ 错题本
│   ├── 错题列表（按时间/章节/错误类型）
│   ├── 错题详情（原题 → 诊断 → 笔记溯源 → 同类题）
│   ├── 间隔复习（今天该复习的错题）
│   └── 错题统计（错误类型分布、改善趋势）
│
└─ 👤 我的
    ├── 备考仪表盘（考点覆盖、掌握度变化）
    ├── 考试目标设置（考研/专升本/高考 + 目标分数）
    ├── 学习周报（本周学习时长、刷题数、掌握度变化）
    └── 设置（账号、知识库管理、数据导出）
```

---

## 五、关键用户流程

### 流程 1：新用户上手（Onboarding）

```
Step 1: 选择考试
  "你正在备考哪个考试？"
  [考研] [专升本] [高考] [其他]

Step 2: 细化目标
  "考研 → 数学一/数学二/数学三？目标院校？目标分数？"
  [数学一] [120分]

Step 3: 导入资料
  "上传你的学习资料，让 AI 更懂你的学习内容"
  [拍照上传笔记] [上传 PDF] [稍后再说]
  └─ 拍照 → OCR → 自动归类到考点

Step 4: 初始摸底（可选）
  "要不要做 10 道题，让 AI 快速了解你的水平？"
  [开始摸底] [跳过]
  └─ 从真题库中按考纲抽样 10 题 → 评估薄弱点 → 初始化掌握度画像

Step 5: 进入首页
  显示：备考进度、今日推荐、薄弱考点提醒
```

### 流程 2：拍照上传试卷批改

```
Step 1: 拍照
  拍照/从相册选择试卷图片（支持多页连拍）

Step 2: 确认区域
  AI 自动识别题目区和答案区，用户可手动调整

Step 3: 批改中
  进度条：OCR → 题目提取 → 逐题批改 → 错题诊断
  SSE 流式推送进度

Step 4: 批改报告
  ┌──────────────────────────┐
  │  📊 试卷批改报告           │
  │  总分：112/150（74.7%）    │
  │  正确：18题  错误：5题     │
  │                          │
  │  ⚠️ 薄弱考点 Top 3：       │
  │  1. 中值定理应用（错2题）   │
  │  2. 二次积分换序（错1题）   │
  │  3. 特征值求解（错1题）     │
  │                          │
  │  [查看错题详解] [推荐练习]   │
  └──────────────────────────┘

Step 5: 错题详情
  点击某道错题 → 三路诊断（复用 DAG 引擎）
  → 每个诊断维度标注"📝 参考你的笔记《XXX》第X页"
  → 下方推荐 3 道同类变式题
```

### 流程 3：日常答疑（笔记锚定）

```
学生提问：
  "极限怎么求？"

AI 处理：
  1. Embedding → 检索学生个人知识库
  2. 找到笔记：《高数上》第2章-极限的运算法则
  3. 检索通用 RAG（真题库中相关题目）
  4. LLM 生成回答，嵌入笔记引用

AI 回答：
  "根据你的笔记《高等数学上册》第2章的总结，极限求解的核心步骤是：
  [引用笔记原文片段]

  如果遇到 0/0 型，优先考虑洛必达法则。
  如果遇到 ∞/∞ 型，可以先化简再判断。

  📝 参考：你的笔记「极限运算法则总结」
  📚 相关真题：2024 考研数学一 第 3 题
  🎯 推荐练习：[类似题1] [类似题2] [类似题3]"
```

---

## 六、技术架构设计

### 6.1 整体架构（复用 + 新增）

```
                        Android App (Java 11, MVVM)
                       /        |         \
                      /         |          \
                     /          |           \
              HTTP/SSE     WebSocket     HTTP/SSE
                    /          |              \
                   /           |               \
    ┌──────────────┬───────────┬───────────────┬──────────────┐
    │  API Gateway │ Knowledge │   Grading     │  Dashboard   │
    │  (现有)       │ Controller│  Controller   │  Controller  │
    │              │  [NEW]    │   [NEW]       │   [NEW]      │
    ├──────────────┴───────────┴───────────────┴──────────────┤
    │                    Service Layer                         │
    │  ┌─────────────┐ ┌──────────────┐ ┌─────────────────┐  │
    │  │ AIProxy     │ │ KnowledgeBase│ │ GradingService  │  │
    │  │ Service(现有)│ │ Service [NEW]│ │ [NEW]           │  │
    │  └─────────────┘ └──────────────┘ └─────────────────┘  │
    │  ┌─────────────┐ ┌──────────────┐ ┌─────────────────┐  │
    │  │ Diagnosis   │ │ IntentRouter │ │ Recommendation  │  │
    │  │ Service(现有)│ │ [NEW]        │ │ Service [NEW]   │  │
    │  └─────────────┘ └──────────────┘ └─────────────────┘  │
    ├─────────────────────────────────────────────────────────┤
    │                      Core Layer (现有，扩展)              │
    │  DAG Engine │ RAG Pipeline │ Semantic Cache │ Tenant    │
    │  虚拟线程   │ RRF 融合      │ Embedding      │ Semaphore │
    ├─────────────────────────────────────────────────────────┤
    │  PostgreSQL │ Redis │ Milvus (per-tenant Collection)    │
    │  + 个人笔记表  │       │ + 个人笔记向量                   │
    └─────────────────────────────────────────────────────────┘
```

### 6.2 新增数据模型

```sql
-- 个人知识库
CREATE TABLE personal_knowledge_base (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    exam_type   VARCHAR(32),        -- 考研数学一 / 专升本高数 / 高考数学
    subject     VARCHAR(32),        -- 数学 / 英语 / 政治
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 笔记/资料文件
CREATE TABLE knowledge_document (
    id          BIGSERIAL PRIMARY KEY,
    kb_id       BIGINT REFERENCES personal_knowledge_base(id),
    title       VARCHAR(256),        -- 《高等数学上册》第三章笔记
    file_type   VARCHAR(32),         -- PDF / IMAGE / MARKDOWN
    file_url    VARCHAR(512),        -- OSS 存储路径
    status      VARCHAR(32),         -- PARSING / READY / FAILED
    chunk_count INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 笔记切片（按知识点切分）
CREATE TABLE knowledge_chunk (
    id          BIGSERIAL PRIMARY KEY,
    doc_id      BIGINT REFERENCES knowledge_document(id),
    kp_id       BIGINT REFERENCES knowledge_point(id),  -- 关联考点
    content     TEXT,                -- 切片原文
    embedding   vector(1024),        -- pgvector 或存于 Milvus
    chunk_index INT,                 -- 在原文档中的顺序
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 知识点（对标考纲）
CREATE TABLE knowledge_point (
    id          BIGSERIAL PRIMARY KEY,
    exam_type   VARCHAR(32),
    subject     VARCHAR(32),
    chapter     VARCHAR(128),        -- 第一章 函数与极限
    section     VARCHAR(128),        -- 1.1 函数的概念
    kp_name     VARCHAR(256),        -- 函数的有界性
    kp_code     VARCHAR(32) UNIQUE,  -- MATH1-1-1-3
    level       INT DEFAULT 1,       -- 1/2/3 级知识点（父/子/孙）
    parent_id   BIGINT               -- 父知识点 ID
);

-- 试卷批改记录
CREATE TABLE grading_record (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    paper_title VARCHAR(256),
    total_score INT,                  -- 满分
    user_score  INT,                  -- 得分
    correct_count INT DEFAULT 0,
    wrong_count   INT DEFAULT 0,
    exam_type   VARCHAR(32),
    paper_images TEXT[],              -- 试卷图片 URL 数组
    report_json JSONB,                -- 完整批改报告
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 单题批改结果
CREATE TABLE grading_question_result (
    id              BIGSERIAL PRIMARY KEY,
    grading_id      BIGINT REFERENCES grading_record(id),
    question_number INT,
    question_text   TEXT,
    question_type   VARCHAR(32),      -- CHOICE / FILL / ANSWER
    user_answer     TEXT,
    correct_answer  TEXT,
    is_correct      BOOLEAN,
    score           INT,
    max_score       INT,
    kp_ids          BIGINT[],         -- 涉及的考点 ID
    diagnosis_json  JSONB,            -- DAG 诊断结果
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 用户考点掌握度
CREATE TABLE user_kp_mastery (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    kp_id       BIGINT REFERENCES knowledge_point(id),
    mastery     DECIMAL(3,2) DEFAULT 0.00,  -- 0.00 ~ 1.00
    total_attempts INT DEFAULT 0,
    correct_attempts INT DEFAULT 0,
    last_practiced_at TIMESTAMP,
    next_review_at    TIMESTAMP,      -- 间隔复习时间
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, kp_id)
);
```

### 6.3 核心 AI Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│                    AI Pipeline (请求 → 响应)                    │
│                                                             │
│  用户请求（文字/图片/试卷）                                      │
│       │                                                     │
│       ▼                                                     │
│  ┌──────────────┐                                           │
│  │ IntentRouter │  规则引擎 + 轻量分类                          │
│  │ (NEW)        │  → Q&A / PAPER_GRADE / NOTE_SEARCH          │
│  └──────┬───────┘                                           │
│         │                                                   │
│         ▼                                                   │
│  ┌──────────────────────────────────────┐                   │
│  │         RAG Multi-Route Retrieval     │                   │
│  │  (扩展现有 RAG，增加个人笔记路由)         │                   │
│  │                                      │                   │
│  │  Route 1: 个人笔记 Milvus Coll        │                   │
│  │  Route 2: 真题库 Milvus Coll          │                   │
│  │  Route 3: PostgreSQL 全文检索          │                   │
│  │                                      │                   │
│  │  → RRF 融合重排 (复用现有)              │                   │
│  └──────────────────┬───────────────────┘                   │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────┐                   │
│  │         Semantic Cache (复用现有)      │                   │
│  │  相似度 > 0.96 → 直接返回               │                   │
│  │  未命中 → 走 LLM                       │                   │
│  └──────────────────┬───────────────────┘                   │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────┐                   │
│  │         LLM 调用 (Doubao / DeepSeek)   │                   │
│  │  System Prompt 注入:                   │                   │
│  │  - 考试类型 + 考纲范围                   │                   │
│  │  - 检索到的个人笔记片段                   │                   │
│  │  - 检索到的真题上下文                    │                   │
│  │  - 用户薄弱考点列表                     │                   │
│  └──────────────────┬───────────────────┘                   │
│                     │                                       │
│                     ▼                                       │
│  ┌──────────────────────────────────────┐                   │
│  │         Post-Processing               │                   │
│  │  - 笔记引用锚定 (标注"参考你的笔记...")    │                   │
│  │  - 推荐题目生成                        │                   │
│  │  - 掌握度更新                         │                   │
│  │  - 错题自动收录                        │                   │
│  └──────────────────┬───────────────────┘                   │
│                     │                                       │
│                     ▼                                       │
│              返回用户 (SSE 流式)                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 七、与现有代码的复用关系

| 现有组件 | 复用方式 | 改动 |
|---------|---------|------|
| DAG 引擎（DagNode/DagExecutor） | 直接复用，诊断逻辑不变 | 聚合节点增加"笔记溯源"字段 |
| 语义缓存（SemanticCacheService） | 直接复用 | 缓存 Key 增加 `exam_type` 维度 |
| RRF 融合排序（RrfRanker） | 扩展，从 2 路扩大到 3 路 | 增加个人笔记路由 |
| RAG 检索（RecallController） | 扩展 | 新增 `POST /api/recall/personal-notes` |
| 多租户隔离（TenantSemaphoreRegistry） | 直接复用 | 无需改动 |
| 流式 SSE | 直接复用 | 新增试卷批改进度 SSE |
| Timeline 渲染引擎 | 直接复用 | 错题诊断增加笔记引用步骤 |
| Android App | 重构 UI，保留核心组件 | 新增 5 个 Fragment，重构 TabBar |
| Vue 3 Web 端（未开始） | 从零搭建 | 教师端学情大屏 |

**一句话：技术底座 80% 复用，新增的是业务层和前端。**

---

## 八、分阶段实施路线图

### Phase 0：考纲数据结构 + 知识库 MVP（2-3 周）

目标：用户能上传笔记，AI 能检索到个人笔记。

- [ ] 考研数学一/二/三 考纲知识点录入（~200 个考点，结构化 JSON）
- [ ] 专升本高数/英语 考纲知识点录入
- [ ] `knowledge_point` 表 + `personal_knowledge_base` 表建表
- [ ] 笔记上传 API（`POST /api/kb/upload`）
- [ ] OCR + 结构化解析 Pipeline
- [ ] 个人笔记 Embedding + Milvus Collection 创建
- [ ] `POST /api/recall/personal-notes` — 检索个人笔记
- [ ] RRF 三路融合（个人笔记 + 真题库 + 全文检索）

### Phase 1：智能答疑升级 + 笔记锚定（2-3 周）

目标：AI 回答中能引用"你的笔记第X页"。

- [ ] IntentRouter 意图路由（规则引擎）
- [ ] System Prompt 模板升级（注入个人笔记上下文）
- [ ] 回答后处理：笔记引用自动锚定
- [ ] Android 端知识库 Tab（笔记列表 + 知识图谱）
- [ ] Android 端答疑界面升级（显示笔记引用卡片）

### Phase 2：试卷批改（3-4 周）

目标：拍照上传试卷 → 自动批改 → 错题诊断 → 推荐练习。

- [ ] OCR 试卷识别（题目区/答案区分割）
- [ ] 题目结构化提取（题型判断、题干提取）
- [ ] 逐题批改 LLM Pipeline
- [ ] `POST /api/grading/submit` + SSE 进度推送
- [ ] 批改报告生成
- [ ] 错题自动入库 + 考点掌握度更新
- [ ] Android 端拍照 → 批改 → 报告 → 错题详情 完整流程

### Phase 3：备考仪表盘 + 智能组卷（2-3 周）

目标：学生能看到备考进度，系统能自动组卷。

- [ ] 用户考点掌握度计算引擎
- [ ] `GET /api/dashboard/overview` — 备考总览 API
- [ ] `POST /api/practice/generate-paper` — 基于薄弱点智能组卷
- [ ] 间隔复习提醒（艾宾浩斯遗忘曲线）
- [ ] Android 端备考仪表盘 + 错题本 Tab
- [ ] 学习周报自动生成

### Phase 4：Web 教师端 + 打磨（3-4 周）

目标：完成多端闭环 + 面试作品集。

- [ ] Vue 3 教师端（学情大屏 + 考纲覆盖监控）
- [ ] WebSocket 实时推送（教师端 → 学生端）
- [ ] SkyWalking 深度定制（FTT 埋点 + MQ Trace）
- [ ] 压测报告 + 演示视频

---

## 九、关键设计决策（含权衡）

### 决策 1：每个用户一个 Milvus Collection vs 共享 Collection

**选：每个用户一个 Collection**

| | Per-User Collection | Shared Collection |
|---|---|---|
| 检索精度 | 高（用户笔记的语义空间独立）| 中（需要额外过滤） |
| 隔离性 | 天然隔离 | 需要 tenant_id 过滤 |
| 运维成本 | 高（Collection 数 = 用户数）| 低 |
| 扩展性 | Milvus 单集群支持 10,000+ Collection | 无限制 |

**原因**：AI 教育产品的核心价值在"个性化"，笔记检索的精度比运维成本重要得多。而且 Milvus 2.4 standalone 支持 10,000+ Collection，初期完全够用。

### 决策 2：考点知识图谱是人工录入还是 LLM 自动构建

**选：人工录入核心考纲 + LLM 辅助扩展**

- 考研/专升本/高考的官方考纲 → 人工结构化录入（保证准确性）
- 用户笔记 → 考点的映射 → LLM 自动完成（容忍一定误差）
- 用户上传的新考点 → LLM 建议 → 人工审核（弱依赖）

### 决策 3：试卷批改是完全 AI 还是保留人工复核入口

**选：AI 自动批改 + 争议题人工复核**

- 选择题/填空题 → 规则匹配（100% 准确，零 Token 消耗）
- 解答题 → LLM 批改（显示置信度，低置信度标记"建议复核"）
- 主观题（政治/英语作文）→ LLM 批改 + 显示"AI 评分仅供参考"

---

## 十、成功指标

| 指标 | 当前（v2） | v3 目标 | 如何衡量 |
|------|-----------|---------|---------|
| 笔记上传后完成解析率 | N/A | > 95% | 解析成功数 / 上传总数 |
| 笔记检索 Recall@5 | N/A | > 0.85 | 用户搜索"XX 知识点"，前5条笔记是否相关 |
| AI 回答中笔记引用准确率 | N/A | > 80% | 引用的笔记内容确实和当前问题相关 |
| 试卷批改准确率（客观题）| N/A | > 99% | 和人工批改对比 |
| 试卷批改准确率（解答题）| N/A | > 85% | 和老师批改对比 |
| 错题 7 日重测正确率 | N/A | 60%+ | 同一考点错题，7天后练习能做对的比率 |
| 考点覆盖率（使用 4 周后）| N/A | > 70% | 至少练习过 70% 的考纲考点 |
| 语义缓存命中率 | > 80%（目标）| > 80% | 维持不变 |
| 单次诊断 Token 成本 | 基准 | -40% | 缓存 + 个人笔记优先检索 |

---

## 十一、最大的风险 & 应对

| 风险 | 概率 | 影响 | 应对 |
|------|------|------|------|
| OCR 识别准确率不足（手写笔记） | 高 | 知识库质量差 | 先用印刷体/PDF，手写 OCR 作为 Phase 2 优化项 |
| 考纲考点人工录入工作量大 | 中 | Phase 0 延期 | 先只做考研数学一（186 考点），其他按需扩展 |
| LLM 批改主观题不准 | 高 | 用户投诉 | 明确标注"AI 评分仅供参考"，提供申诉入口 |
| Milvus per-user Collection 运维复杂 | 低 | 后期成本高 | 预留切换到 pgvector + tenant_id 过滤的降级方案 |
| 用户不愿意上传笔记（隐私顾虑）| 中 | 核心功能失效 | 提供"不上传也能用"的基础模式，知识库作为增值功能 |
