# IRAgent Pro v3 — Spring Boot 后端架构文档

> **项目**：以个人知识库为中心的 AI 备考平台
> **技术栈**：Java 21 + Spring Boot 3.4.6 + MyBatis-Plus + PostgreSQL 16 + Redis 7.2 + Milvus 2.4 + RocketMQ 5.x
> **最后更新**：2026-05-28

---

## 一、项目概览

### 1.1 启动入口

- **主类**：`com.suiyuan.iragent.IrAgentApplication`
- **注解**：`@SpringBootApplication` + `@MapperScan("com.suiyuan.iragent.mapper")`
- **端口**：`${SERVER_PORT:8080}`
- **Context Path**：`/api`

### 1.2 核心配置

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api
    multipart.max-file-size: 20MB       # 支持手机拍照上传

spring:
  threads.virtual.enabled: true         # Java 21 虚拟线程
  datasource:
    url: jdbc:postgresql://localhost:5432/iragent

# AI 模型
app:
  doubao-chat-key: ${DOUBAO_CHAT_KEY}           # 豆包 Chat（可热刷新）
  deepseek-key: ${DEEPSEEK_KEY}                  # DeepSeek（可热刷新）
  doubao-embedding-key: ${DOUBAO_EMBEDDING_KEY} # Embedding（可热刷新，fallback 到 chat key）
```

### 1.3 部署架构

```
Docker Compose 一键部署：
  PostgreSQL 16 (5432)  — 14 张业务表 + GIN 全文索引
  Redis 7.2 (6379)      — 语义缓存 + Session
  Milvus 2.4 (19530)    — Per-user Collection 向量检索
  RocketMQ 5.x (9876)   — 行为日志异步上报
  SkyWalking            — 全链路追踪（可选）
```

---

## 二、架构分层

```
┌──────────────────────────────────────────────────────┐
│  Controller 层（18 个，50+ 个端点）                     │
├──────────────────────────────────────────────────────┤
│  Service 层（26 个）                                   │
│  KnowledgeBase / NoteAnchoredChat / SmartPaper         │
│  DailyPractice / GradingPipeline / Diagnosis          │
├──────────────┬──────────────┬─────────────────────────┤
│  DAG 引擎     │  RAG Pipeline │  基础设施                │
│  Kahn 拓扑    │  Milvus 检索  │  Tenant Semaphore 隔离   │
│  虚拟线程     │  RRF 3路融合  │  RocketMQ 异步            │
│  5路并行诊断  │  语义缓存      │  ApiKey 热刷新            │
├──────────────┴──────────────┴─────────────────────────┤
│  PostgreSQL 16  │  Redis 7.2  │  Milvus 2.4           │
└──────────────────────────────────────────────────────┘
```

---

## 三、Controller 层（18 个，50+ 端点）

| Controller | 路径前缀 | 端点数 | 版本 |
|-----------|---------|--------|------|
| `AuthController` | `/auth` | 3 | V1 |
| `AIController` | `/ai` | 2 | V1 |
| `ConversationController` | `/conversations` | 8 | V1 |
| `TimelineController` | `/timeline` | 2 | V1 |
| `LearningController` | `/v2/sessions` | 7 | V2 |
| `RecallController` | `/recall` | 1 | V2 |
| `CacheStatsController` | `/cache` | 1 | V2 |
| `DiagnosisController` | `/diagnosis` | 1 | V2 |
| `KnowledgeBaseController` | `/v3/kb` | 7 | V3 |
| `ChatControllerV3` | `/v3/chat` | 2 | V3 |
| `GradingController` | `/v3/grading` | 2 | V3 |
| `ErrorBookController` | `/v3/errors` | 5 | V3 |
| `DashboardController` | `/v3/dashboard` | 4 | V3 |
| `ExamArchiveController` | `/v3/exam-archive` | 4 | V3 |
| `SmartPaperController` | `/v3/smart-paper` | 1 | V3 |
| `DailyPracticeController` | `/v3/daily-practice` | 3 | V3 |
| `AdminController` | `/admin` | 8 | V3 |
| `FeedbackController` | `/v3/feedback` | 1 | V3 |

---

## 四、V3 API 完整端点

> 所有路径前缀 `/api`。Token 通过 `token` 请求头传递（`LoginInterceptor` 拦截校验）。

### 4.1 知识库 — `/api/v3/kb`（7 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/kb/upload` | 上传笔记（MultipartFile：PDF/DOCX/TXT/MD/JPG/PNG） |
| `GET` | `/kb/notes` | 笔记列表（分页 + subject 筛选） |
| `GET` | `/kb/notes/{id}` | 笔记详情（含 chunk、关联考点、关联题目） |
| `PUT` | `/kb/notes/{id}` | 编辑笔记（title/subject/chapter/tags/content） |
| `DELETE` | `/kb/notes/{id}` | 删除笔记（级联删除 chunk） |
| `POST` | `/kb/notes/{id}/optimize` | AI 优化笔记（支持自定义指令） |
| `POST` | `/kb/search` | 语义搜索个人笔记 |

**上传流程**：
```
文件 → 格式判断 → PDF/DOCX 解析 / 图片 OCR（doubao-seed-1-8） / 文本直接读取
  → NoteIngestionPipeline（清洗 → 按标题切分 → Embedding → Milvus + PG）
  → AI 自动分类（classifyContent → JSON {subject, chapter, tags}）
  → 持久化到 note 表
```

**AI 优化流程**：
```
原始内容 + 用户指令 → LLM 优化（强制规范：$公式$/$$\begin{aligned}$$/确保\begin都有\end）
  → 更新 note.content → 删除旧 chunk → 重新 Ingest（ON CONFLICT DO UPDATE）
```

### 4.2 答疑 V3 — `/api/v3/chat`（2 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/chat/stream` | SSE 流式答疑（笔记锚定） |
| `POST` | `/chat/stream-image` | SSE 多模态答疑（图片 + 文字） |

**SSE 事件**：`chunk` | `note_refs` | `plot` | `plot3d` | `meta`（含 conversationId） | `done` | `error`

**内部流程**：`IntentRouterService` 意图路由 → 三路融合检索（个人笔记 Milvus + 真题 Milvus + PG 全文） → RRF 排序 → 注入 System Prompt → LLM 流式调用。

### 4.3 每日一练 — `/api/v3/daily-practice`（3 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/daily-practice` | 获取题目（subject/count/knowledgePoints） |
| `POST` | `/daily-practice/{id}/submit` | 提交答案（含拍照 photoBase64） |
| `POST` | `/daily-practice/{id}/feedback` | 题目报错 |

**出题策略**：AI 生成优先 → 题库降级，难度优先级 `{2,3,4,1,5}`（中等优先），`knowledgePoints` 参数支持同类题筛选。

### 4.4 智能组卷 — `/api/v3/smart-paper`（1 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/smart-paper/generate` | SSE 流式生成试卷（逐题推送） |

**SSE 事件**：`question_start` | `question_content` | `question_end` | `complete` | `error`

### 4.5 真题库 — `/api/v3/exam-archive`（4 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/exam-archive` | 真题列表（5 维筛选 + 分页） |
| `GET` | `/exam-archive/filters` | 筛选选项聚合 |
| `POST` | `/exam-archive/upload` | 上传试卷（MultipartFile → OCR → 入库） |
| `POST` | `/exam-archive/simulate` | AI 生成模拟真题 |

### 4.6 试卷批改 — `/api/v3/grading`（2 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/grading/submit` | SSE 文本批改 |
| `POST` | `/grading/submit-image` | SSE 图片批改（doubao-seed-1-8 多模态） |

**SSE 进度**：`step: ocr(25%)` → `step: extract(50%)` → `step: grade(75%)` → `step: diagnose(90%)` → `complete`

### 4.7 错题本 — `/api/v3/errors`（5 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/errors/list` | 错题列表（分页 + subject/errorType 筛选） |
| `GET` | `/errors/{id}` | 错题详情（含 diagonsis JSON + similar_questions） |
| `GET` | `/errors/review-queue` | 今日复习队列（艾宾浩斯曲线） |
| `PUT` | `/errors/{id}/mark-mastered` | 标记掌握/取消 |
| `POST` | `/errors/{id}/similar` | 同类题推荐（含 similarity/difficulty） |

### 4.8 仪表盘 — `/api/v3/dashboard`（4 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/dashboard/overview` | 备考概览 |
| `GET` | `/dashboard/weekly-report` | 学习周报 |
| `GET` | `/dashboard/mastery-radar` | 掌握度雷达图 |
| `GET` | `/dashboard/today-tasks` | 今日任务 |

### 4.9 管理后台 — `/api/admin`（8 端点）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/admin/api-key` | 获取当前 Key 状态（脱敏） |
| `PUT` | `/admin/api-key` | 热刷新 API Key（doubao-chat/deepseek/doubao-embedding） |
| `GET` | `/admin/users` | 用户列表 |
| `PUT` | `/admin/users/{id}/status` | 启用/禁用用户 |
| `GET` | `/admin/questions/flagged` | 被标记的题目 |
| `PUT` | `/admin/questions/{id}/review` | 审核题目 |
| `GET` | `/admin/dashboard/stats` | 平台统计 |
| `GET` | `/admin/health` | 健康检查 |

---

## 五、V1/V2 已有 API（向下兼容）

### 5.1 认证 — `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/auth/login` | 登录 → token |
| `POST` | `/auth/register` | 注册 |
| `GET` | `/auth/getVerifiCodeImage` | 图片验证码 |

### 5.2 AI 解题 — `/api/ai` + 会话 — `/api/conversations` + 深度学习 — `/api/v2/sessions`

（共 22 个端点，保持向下兼容）

---

## 六、DAG 工作流引擎

### 6.1 核心组件

| 类 | 职责 |
|----|------|
| `DagGraph` / `DagNode` / `DagEdge` | 图模型 + 环路检测 |
| `TopologicalSorter` | Kahn 算法 BFS 分层排序 |
| `DagExecutor` | 虚拟线程按层并发（CompletableFuture + 180s 超时） |
| `LlmCallNode` / `TransformNode` / `AggregateNode` | 节点处理器 |

### 6.2 错题诊断 DAG

```
prerequisite_check (LLM) ─┐
formula_confusion (LLM)  ─┼─ 3 路并行（同层节点）→ aggregate → 诊断结果
calculation_error (LLM)  ─┘
```

配置文件：`resources/dag/diagnosis-dag.json`

---

## 七、RAG Pipeline

### 7.1 组件

| 组件 | 职责 |
|------|------|
| `VolcengineEmbeddingClient` | 豆包 Embedding API（2048 维） |
| `PersonalNoteRetriever` | Per-user Milvus Collection 向量检索（HNSW, M=16, efConstruction=200） |
| `FulltextSearchService` | PostgreSQL GIN 全文检索 + LIKE 降级 |
| `RrfRanker` | RRF 倒数排名融合（k=60） |
| `SemanticCacheService` | 三层缓存：Redis 精确 → Milvus 语义改写 → LLM 生成 |
| `NoteIngestionPipeline` | 笔记接入：清洗 → 按标题切分 → Embedding → Milvus + PG（ON CONFLICT DO UPDATE） |
| `AIQuestionGenerator` | LLM 生成题目 → SymPy 符号验证 → Redis 缓存 + question 表入库 |

### 7.2 三路融合检索

```
用户提问
  → Route 1: PersonalNoteRetriever（个人笔记 Milvus）
  → Route 2: AdaptiveRecallService（真题 Milvus + 标量过滤）
  → Route 3: FulltextSearchService（PG tsvector + LIKE）
  → RrfRanker.merge() → Top-10
```

---

## 八、多租户隔离

| 组件 | 职责 |
|------|------|
| `TenantSemaphoreRegistry` | JVM Semaphore 配额（默认 5 并发/用户） |
| `TenantQuotaInterceptor` | 拦截 LLM 调用路径 |
| `NoisyNeighborDetector` | `@Scheduled(fixedRate=15000)` 监控等待队列 |

**为什么用 JVM Semaphore 而不是网关？** 限流对象是最贵资源（LLM 算力），不是 HTTP 请求数。网关看不出底层 LLM 调用并发，JVM 层面贴近真实瓶颈。

---

## 九、ApiKey 热刷新

**类**：`config/ApiKeyProvider`

三把 Key 独立管理：
- `refreshDoubaoChatKey()` — 豆包 Chat 模型
- `refreshDeepseekKey()` — DeepSeek 模型
- `refreshDoubaoEmbeddingKey()` — Embedding 模型（未设时 fallback 到 Chat Key）

通过 `PUT /api/admin/api-key` 热刷新，无需重启。`VolcEngineStreamingClient` 和 `VolcengineEmbeddingClient` 每次调用时从 Provider 获取最新 Key。

---

## 十、数据库 Schema（14 张表）

| 表 | 版本 | 说明 |
|----|------|------|
| `users` | V1 | 用户（account, password, email） |
| `conversation` | V1 | 会话 |
| `message` | V1 | 消息 |
| `learning_sessions` | V2 | 深度学习会话 |
| `learning_steps` | V2 | 学习步骤 |
| `mastery_records` | V2 | 掌握度记录 |
| `learning_summaries` | V2 | 学习总结 |
| `note` | V3 | 笔记（subject/chapter/title/content/tags/image_url/chunk_count/linked_question_count） |
| `note_chunk` | V3 | 笔记切片（knowledge_point VARCHAR(256) 截断保护） |
| `question` | V3 | 真题（GIN 全文索引） |
| `error_book` | V3 | 错题本（diagnosis_json JSONB, review_level） |
| `grading_report` | V3 | 批改报告 |
| `grading_question_result` | V3 | 单题批改结果 |
| `student_behavior_log` | V3 | 行为日志（JSONB metadata） |

建表：`resources/schema.sql`（全部 `CREATE TABLE IF NOT EXISTS`，幂等可重复执行）

---

## 十一、Service 层（26 个）

| Service | 版本 | 核心职责 |
|---------|------|---------|
| `KnowledgeBaseService` | V3 | 笔记 CRUD + 多格式上传（PDF/DOCX/图片） + AI 分类 + AI 优化 |
| `NoteAnchoredChatService` | V3 | 笔记锚定答疑（Prompt 注入 + noteRefs 生成 + PLOT/PLOT3D 规范） |
| `SmartPaperService` | V3 | AI 出题优先 + SQL 降级，难度优先级 {2,3,4,1,5} |
| `DailyPracticeService` | V3 | 每日一练（支持 knowledgePoints 同类题筛选） |
| `ExamArchiveService` | V3 | 真题管理 + OCR 识别入库 |
| `GradingPipelineService` | V3 | 批改 Pipeline（OCR→提取→批改→诊断） |
| `DiagnosisService` | V2 | DAG 诊断编排（5 路并行） |
| `ErrorBookService` | V3 | 错题本 CRUD + 同类题推荐 |
| `DashboardService` | V3 | 仪表盘聚合（4 路并发） |
| `AIQuestionGenerator` | V3 | AI 出题（LLM 生成 + SymPy 验证 + 缓存入库） |
| `NoteChunkingService` | V3 | 按标题切分 + knowledgePoint 250 字符截断 |
| `IntentRouterService` | V3 | 意图路由（答疑/组卷/批改） |
| `SpacedRepetitionService` | V3 | 艾宾浩斯间隔复习 [0,1,3,7,15,30] 天 |
| `ApiKeyProvider` | V3 | 3 Key 热刷新管理 |
| ... | | 12 个 V1/V2 Service |

---

## 十二、项目结构

```
Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/
├── IrAgentApplication.java
├── config/
│   ├── ApiKeyProvider.java              # 3 Key 热刷新
│   ├── VolcEngineChatClient.java        # 同步 LLM
│   ├── VolcEngineStreamingClient.java   # SSE 流式 + 多模态（streamChatWithImage）
│   ├── MilvusConfig.java                # Milvus 客户端（@ConditionalOnProperty）
│   ├── RocketMQConfig.java
│   ├── AsyncConfig.java                 # 虚拟线程 @Async
│   ├── WebMvcConfig.java                # 拦截器注册
│   └── GlobalExceptionHandler.java
├── controller/                          # 18 个
│   ├── KnowledgeBaseController.java     # /v3/kb（7 端点）
│   ├── ChatControllerV3.java            # /v3/chat（2 端点）
│   ├── GradingController.java           # /v3/grading（2 端点）
│   ├── ErrorBookController.java         # /v3/errors（5 端点）
│   ├── DashboardController.java         # /v3/dashboard（4 端点）
│   ├── ExamArchiveController.java       # /v3/exam-archive（4 端点）
│   ├── SmartPaperController.java        # /v3/smart-paper
│   ├── DailyPracticeController.java     # /v3/daily-practice（3 端点）
│   ├── AdminController.java             # /admin（8 端点）
│   └── ... (9 个 V1/V2)
├── service/                             # 26 个
├── dag/                                 # DAG 引擎（core/engine/nodes）
├── rag/                                 # RAG Pipeline（embedding/vector/cache/retrieval/pipeline）
├── tenant/                              # 多租户（Semaphore + Interceptor + NoisyNeighbor）
├── mq/                                  # RocketMQ（Producer + Consumer）
├── entity/ + mapper/                    # MyBatis-Plus
├── dto/                                 # DTO
└── utils/                               # ApiResponse, UserHolder, ContentParser 等
```

---

## 十三、启动指南

### 13.1 前置条件

```bash
# 1. 启动 PostgreSQL + Redis（必须）
docker compose -f docker-compose-simple.yml up -d

# 2. 可选：Milvus（知识库检索需要）
docker compose -f docker-compose-milvus.yml up -d

# 3. 建表
psql -h localhost -U postgres -d iragent -f src/main/resources/schema.sql

# 4. 设置 API Key
export DOUBAO_CHAT_KEY=your-key
export DEEPSEEK_KEY=your-key
export DOUBAO_EMBEDDING_KEY=your-key
```

### 13.2 启动

```bash
cd Spring-Boot/IRAgent
mvn spring-boot:run
```

### 13.3 验证

- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- 健康检查：`GET http://localhost:8080/api/admin/health`
