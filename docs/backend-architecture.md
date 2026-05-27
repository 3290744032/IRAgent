# IRAgent Pro v3 — Spring Boot 后端架构文档

> **项目**：以个人知识库为中心的 AI 备考平台
> **技术栈**：Java 21 + Spring Boot 3.4.6 + MyBatis-Plus + PostgreSQL 16 + Redis 7.2 + Milvus 2.4 + RocketMQ 5.x
> **最后更新**：2026-05-25

---

## 一、项目概览

### 1.1 启动入口

- **主类**：`com.suiyuan.iragent.IrAgentApplication`
- **注解**：`@SpringBootApplication` + `@MapperScan("com.suiyuan.iragent.mapper")`
- **端口**：`${SERVER_PORT:8080}`
- **Context Path**：`/api`

### 1.2 配置 Profile

| Profile | 文件 | 用途 |
|---------|------|------|
| `default` | `application.yaml` | 主配置，所有环境通用 |
| `dev` | `application-dev.yaml` | 开发环境覆盖 |

### 1.3 核心配置项

```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api

spring:
  threads.virtual.enabled: true       # Java 21 虚拟线程
  datasource:
    url: jdbc:postgresql://localhost:5432/iragent
    username: postgres
    password: 123456
  data.redis:
    host: localhost
    port: 6379

spring.ai.volcengine:
  api-key: ${VOLC_API_KEY}            # 环境变量注入
  model: deepseek-v3-2-251201
  base-url: https://ark.cn-beijing.volces.com/api/v3
  embedding-model: doubao-embedding-vision-250615

rocketmq.name-server: 127.0.0.1:9876
milvus:
  host: localhost
  port: 19530
  database: default

app:
  security.token-expire-hours: 24
  security.captcha-expire-minutes: 5
  rate-limit.ai-requests-per-second: 10
```

---

## 二、架构分层

```
┌────────────────────────────────────────────┐
│  Controller 层（14 个，42 个端点）            │
│  @RestController + @RequestMapping          │
├────────────────────────────────────────────┤
│  Service 层（26 个 Service）                 │
│  @Service + 接口/实现分离                      │
├────────────┬──────────────┬────────────────┤
│  DAG 引擎   │  RAG Pipeline │  基础设施        │
│  Kahn 拓扑  │  Milvus 检索  │  Tenant 隔离     │
│  虚拟线程   │  RRF 融合     │  MQ 异步         │
│  3 路并行   │  语义缓存      │  SkyWalking     │
├────────────┴──────────────┴────────────────┤
│  PostgreSQL 16  │  Redis 7.2  │  Milvus 2.4 │
└────────────────────────────────────────────┘
```

### 2.1 Controller 层（14 个）

| Controller | 路径前缀 | 端点数 | 认证 | 版本 |
|-----------|---------|--------|------|------|
| `AuthController` | `/auth` | 3 | 公开 | V1 |
| `AIController` | `/ai` | 2 | Token | V1 |
| `ConversationController` | `/conversations` | 8 | Token | V1 |
| `TimelineController` | `/timeline` | 2 | 公开 | V1 |
| `LearningController` | `/v2/sessions` | 7 | Token | V2 |
| `RecallController` | `/recall` | 1 | Token | V2 |
| `CacheStatsController` | `/cache` | 1 | 公开 | V2 |
| `DiagnosisController` | `/diagnosis` | 1 | Token | V2 |
| `TraceController` | `/admin` | 3 | 公开 | V3 |
| `KnowledgeBaseController` | `/v3/kb` | 4 | Token | **V3** |
| `GradingController` | `/v3/grading` | 1 | Token | **V3** |
| `ErrorBookController` | `/v3/errors` | 5 | Token | **V3** |
| `DashboardController` | `/v3/dashboard` | 4 | Token | **V3** |
| `ChatControllerV3` | `/v3/chat` | 1 | Token | **V3** |

---

## 三、V3 API 完整端点

> 所有路径前缀 `/api`。Token 通过 `token` 请求头传递（`LoginInterceptor` 拦截校验）。

### 3.1 知识库 — `/api/v3/kb`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| `POST` | `/api/v3/kb/upload` | 上传笔记文件 | `@RequestParam file` (MultipartFile, txt/md) |
| `GET` | `/api/v3/kb/notes` | 笔记列表（分页+筛选） | `?subject=&page=0&size=20` |
| `GET` | `/api/v3/kb/notes/{id}` | 笔记详情（含关联考点+题目） | `@PathVariable id` |
| `POST` | `/api/v3/kb/search` | 语义搜索个人笔记 | `@Body { query, topK }` |

**上传流程**：文件 → `NoteIngestionPipeline` → 文本清洗 → `NoteChunkingService` 按标题切分 → `EmbeddingService` 向量化 → 写入用户专属 Milvus Collection + PostgreSQL `note`/`note_chunk` 表。

### 3.2 答疑 V3 — `/api/v3/chat`

| 方法 | 路径 | 说明 | Content-Type |
|------|------|------|-------------|
| `POST` | `/api/v3/chat/stream` | SSE 流式答疑（笔记锚定） | `text/event-stream` |

**请求体**：`{ "question": "...", "conversationId": "..." }`

**SSE 事件类型**：

| 事件 | 数据 | 说明 |
|------|------|------|
| `chunk` | `{ "content": "..." }` | 流式文本片段 |
| `note_refs` | `{ "noteRefs": [{id, title, snippet}] }` | 笔记引用卡片（回答末尾发送） |
| `plot` | `{ "plotData": "..." }` | 2D 函数图像（PLOT 协议） |
| `plot3d` | `{ "plotData": "..." }` | 3D 函数图像（PLOT3D 协议） |
| `done` | `{}` | 流结束 |
| `error` | `{ "code": "...", "message": "..." }` | 错误 |

**内部流程**：`IntentRouterService` 意图路由 → `PersonalNoteRetriever` 检索个人笔记 → 注入 System Prompt → LLM 流式调用 → `NoteAnchoredChatService` 生成 noteRefs。

### 3.3 真题库 + AI 出题 — `/api/v3/exam-archive`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| `GET` | `/api/v3/exam-archive` | 真题列表（5 维筛选+分页） | `?subject=&year=&examType=&knowledgePoint=&difficulty=&page=&size=` |
| `GET` | `/api/v3/exam-archive/filters` | 筛选选项聚合 | — |
| `POST` | `/api/v3/exam-archive/simulate` | AI 生成真题风格模拟题 | `@Body { subject, examType, count }` |
| `POST` | `/api/v3/exam-archive/feedback` | 用户报错反馈 | `@Body { questionId, reason }` |

**AI 出题引擎**（`AIQuestionGenerator`）：LLM 生成题目 → LaTeX 规范化 → SymPy 符号验证 → 合理性检查 → Redis 缓存 + question 表入库。三个刷题 Service 在 SQL 题库空竭时自动降级调用。

### 3.4 试卷批改 — `/api/v3/grading`

| 方法 | 路径 | 说明 | Content-Type |
|------|------|------|-------------|
| `POST` | `/api/v3/grading/submit` | SSE 流式批改 | `text/event-stream` |

**请求体**：`{ "content": "试卷文本...", "subjectType": "数学", "maxScore": 150 }`

**SSE 事件类型**（4 步进度）：

| 事件 | 数据 | 说明 |
|------|------|------|
| `step` | `{ "step": "ocr", "progress": 25 }` | 步骤 1：文本解析 |
| `step` | `{ "step": "extract", "progress": 50 }` | 步骤 2：题目提取 |
| `step` | `{ "step": "grade", "progress": 75 }` | 步骤 3：AI 批改 |
| `step` | `{ "step": "diagnose", "progress": 90 }` | 步骤 4：错题诊断 |
| `complete` | `{ "reportId": "...", "totalScore": 110, ... }` | 批改完成 |

**内部流程**：`GradingPipelineService` → 正则提取题目 → LLM 逐题批改（客观题规则匹配优先）→ 错题触发 `DiagnosisService`（DAG 三路诊断）→ 自动录入错题本 → 更新考点掌握度 → 推荐同类题。

### 3.4 错题本 — `/api/v3/errors`

| 方法 | 路径 | 说明 | 参数 |
|------|------|------|------|
| `GET` | `/api/v3/errors/list` | 错题列表（分页+筛选） | `?subject=&errorType=&page=0&size=20` |
| `GET` | `/api/v3/errors/{id}` | 错题详情（含诊断+推荐） | `@PathVariable id` |
| `GET` | `/api/v3/errors/review-queue` | 今日复习队列 | — |
| `PUT` | `/api/v3/errors/{id}/mark-mastered` | 标记掌握 | `@PathVariable id` |
| `POST` | `/api/v3/errors/{id}/similar` | 同类题推荐 | `@PathVariable id` |

**复习队列**：基于艾宾浩斯遗忘曲线，间隔 [0, 1, 3, 7, 15, 30] 天。`SpacedRepetitionService` 计算 `next_review_at`。

### 3.5 备考仪表盘 — `/api/v3/dashboard`

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v3/dashboard/overview` | 备考概览（考点覆盖、笔记数、刷题数、学习时长） |
| `GET` | `/api/v3/dashboard/weekly-report` | 学习周报（时长、做题数、正确率、新掌握） |
| `GET` | `/api/v3/dashboard/mastery-radar` | 掌握度雷达图数据（labels + values） |
| `GET` | `/api/v3/dashboard/today-tasks` | 今日任务（复习/练习/专项突破） |

---

## 四、V1/V2 已有 API（向下兼容）

### 4.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/login` | 登录（account + password + captcha）→ token |
| `POST` | `/api/auth/register` | 注册 |
| `GET` | `/api/auth/getVerifiCodeImage` | 获取图片验证码（JPEG 流 + X-Verification-UUID 头） |

### 4.2 AI 解题（V1）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/ai/solve/stream` | SSE 流式解题 |
| `GET` | `/api/ai/chat/messages/{conversationId}` | 获取会话消息 |

### 4.3 会话管理（V1）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/conversations` | 创建会话 |
| `GET` | `/api/conversations` | 会话列表（分页） |
| `GET` | `/api/conversations/all` | 全部会话 |
| `GET/PUT/DELETE` | `/api/conversations/{id}` | 会话 CRUD |
| `GET/DELETE` | `/api/conversations/{id}/messages` | 消息管理 |

### 4.4 深度学习（V2）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v2/sessions/history` | 学习历史 |
| `POST` | `/api/v2/sessions` | 创建学习会话 |
| `GET` | `/api/v2/sessions/{id}` | 会话详情 |
| `GET` | `/api/v2/sessions/{id}/teach` | SSE 流式教学（苏格拉底分步） |
| `GET` | `/api/v2/sessions/{id}/summary` | 学习总结 |
| `POST` | `/api/v2/sessions/{id}/answer` | 学生回答 |

### 4.5 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/timeline/title` | 生成教学标题 |
| `POST` | `/api/timeline/generate` | 生成 Timeline 动画 JSON |
| `POST` | `/api/recall/adaptive` | 自适应真题召回 |
| `GET` | `/api/cache/stats` | 语义缓存统计 |
| `POST` | `/api/diagnosis/stream` | SSE 错题诊断（DAG 引擎） |
| `GET` | `/api/admin/trace/{id}/billing` | TraceID 计费对账 |
| `PUT` | `/api/admin/tenants/{id}/quota` | 租户配额管理 |
| `GET` | `/api/admin/noisy-neighbor/diagnose` | 吵闹邻居诊断 |

**总计：44 个 API 端点**

---

## 五、DAG 工作流引擎

### 5.1 核心类

| 类 | 包 | 职责 |
|----|-----|------|
| `DagGraph` | `dag.core` | 图模型 + 环路检测 + 孤立节点检测 |
| `DagNode` | `dag.core` | 节点模型（id, type, config, dependsOn） |
| `DagEdge` | `dag.core` | 有向边（from → to） |
| `NodeType` | `dag.core` | 枚举：LLM_CALL / CONDITION / TRANSFORM / AGGREGATE |
| `ExecutionContext` | `dag.core` | 线程安全的 ConcurrentHashMap 上下文 |
| `NodeResult` | `dag.core` | 节点执行结果（success, durationMs, tokensUsed, error） |
| `TopologicalSorter` | `dag.engine` | Kahn 算法 BFS 分层排序 → `List<List<DagNode>>` |
| `DagExecutor` | `dag.engine` | 虚拟线程按层并发调度器（CompletableFuture + 180s 超时） |
| `LlmCallNode` | `dag.nodes` | LLM 调用节点处理器 |
| `TransformNode` | `dag.nodes` | 数据转换节点 |
| `AggregateNode` | `dag.nodes` | 聚合节点（合并上游输出为 JSON） |

### 5.2 错题诊断 DAG

**配置文件**：`resources/dag/diagnosis-dag.json`

```
                    ┌──────────────────┐
                    │ prerequisite_check│ ← LLM_CALL：前置考点漏缺
                    └────────┬─────────┘
                             │
                    ┌────────┴─────────┐
                    │ formula_confusion │ ← LLM_CALL：核心公式混淆
                    └────────┬─────────┘
                             │
                    ┌────────┴─────────┐
                    │ calculation_error │ ← LLM_CALL：计算步骤失误
                    └────────┬─────────┘
                             │
                    ┌────────┴─────────┐
                    │    aggregate     │ ← AGGREGATE：汇总三路结果
                    └──────────────────┘

三个 LLM 节点并行执行（拓扑排序识别为同一层），总耗时 ≈ max(单个节点耗时)
```

---

## 六、RAG Pipeline

### 6.1 组件

| 组件 | 包 | 职责 |
|------|-----|------|
| `EmbeddingService` | `rag.embedding` | Embedding 接口 |
| `VolcengineEmbeddingClient` | `rag.embedding` | 火山方舟 Embedding API（2048 维） |
| `QuestionVectorCollection` | `rag.vector` | Milvus Collection 管理（HNSW 索引，M=16, efConstruction=200） |
| `PersonalNoteRetriever` | `rag.retrieval` | 个人笔记向量检索（per-user Collection） |
| `FulltextSearchService` | `rag.retrieval` | PostgreSQL GIN 全文检索 + LIKE 降级 |
| `AdaptiveRecallService` | `rag.retrieval` | 自适应召回（薄弱考点标量过滤 + 向量检索） |
| `RrfRanker` | `rag.retrieval` | RRF 倒数排名融合（k=60，可配置权重） |
| `SemanticCacheService` | `rag.cache` | 两层缓存：Redis（快） + Redis TTL 7 天 |
| `NoteIngestionPipeline` | `rag.pipeline` | 笔记接入：清洗 → 切分 → Embedding → Milvus + PG |
| `QuestionIngestionPipeline` | `rag.pipeline` | 真题入库 Pipeline |

### 6.2 三路融合检索

```
用户提问
  → Route 1: PersonalNoteRetriever（个人笔记 Milvus Coll，L2→相似度转换）
  → Route 2: AdaptiveRecallService（真题 Milvus Coll + 标量过滤）
  → Route 3: FulltextSearchService（PostgreSQL tsvector + LIKE）
  → RrfRanker.merge(route1, route2, route3) → Top-10
```

---

## 七、多租户隔离

### 7.1 组件

| 组件 | 职责 |
|------|------|
| `TenantSemaphoreRegistry` | JVM Semaphore 配额管理（默认 5 并发/租户） |
| `TenantQuotaInterceptor` | 拦截 `/diagnosis/`、`/ai/solve/` 路径 |
| `NoisyNeighborDetector` | `@Scheduled(fixedRate=15000)` 监控等待队列 |

### 7.2 限流流程

```
请求进入 → TenantSemaphoreRegistry.acquire(userId)
  → 成功：执行 LLM 调用 → finally release()
  → 失败（信号量耗尽）：返回 HTTP 429 "当前使用人数过多，请稍后重试"
```

---

## 八、消息队列（RocketMQ 5.x）

| 组件 | 职责 |
|------|------|
| `BehaviorMessageProducer` | 异步发送行为消息到 `BEHAVIOR_TOPIC`（含 SkyWalking TraceContext 注入） |
| `BehaviorBatchConsumer` | 批量消费（50 条 或 10 秒间隔）→ 写入 `student_behavior_log` 表 |

---

## 九、SkyWalking 深度定制

| 定制点 | 文件 | 实现 |
|--------|------|------|
| **FTT 首字延迟埋点** | `VolcEngineStreamingClient` | SSE `onMessage` 首帧回调中手动创建子 Span → `tag("ftt","true")` → `stopSpan()` |
| **跨 MQ Trace 传播** | `BehaviorMessageProducer` / `BehaviorBatchConsumer` | Producer 注入 `sw8` 头，Consumer 提取创建子 Span |
| **Noisy Neighbor 检测** | `NoisyNeighborDetector` | 监控 Semaphore 等待队列 → 日志告警 + Profile 自动采样 |

---

## 十、数据库 Schema（14 张表）

| 表 | 说明 | 版本 |
|----|------|------|
| `users` | 用户表（account, password, email, telphone） | V1 |
| `conversation` | 会话表（user_id, name, status） | V1 |
| `message` | 消息表（conversation_id, sender_type, content） | V1 |
| `learning_sessions` | 学习会话（user_id, question, topic, total_steps） | V2 |
| `learning_steps` | 学习步骤（session_id, step_index, content, status） | V2 |
| `mastery_records` | 掌握度记录（user_id, knowledge_point, proficiency） | V2 |
| `learning_summaries` | 学习总结（session_id, knowledge_graph, recommendations） | V2 |
| `note` | 笔记表（user_id, subject, chapter, title, content, tags） | **V3** |
| `note_chunk` | 笔记切片（note_id, knowledge_point, content） | **V3** |
| `question` | 真题表（question_text, tags, province, year, GIN 索引） | **V3** |
| `error_book` | 错题本（user_id, question_text, diagnosis_json, review_level） | **V3** |
| `grading_report` | 批改报告（user_id, total_score, correct_count, accuracy） | **V3** |
| `grading_question_result` | 单题批改结果（report_id, is_correct, score, diagnosis_json） | **V3** |
| `student_behavior_log` | 行为日志（user_id, action, duration_ms, metadata JSONB） | **V3** |

**建表**：执行 `resources/schema.sql`（14 张表全部 `CREATE TABLE IF NOT EXISTS`，可重复执行）。

---

## 十一、启动指南

### 11.1 前置条件

```bash
# 1. 启动中间件（按需选择）
docker compose -f docker-compose-simple.yml up -d    # PostgreSQL + Redis（必须）
docker compose -f docker-compose-milvus.yml up -d   # Milvus（知识库/检索需要）
docker compose -f docker-compose-rocketmq.yml up -d # RocketMQ（行为上报需要）
docker compose -f docker-compose-skywalking.yml up -d # SkyWalking（可选）

# 2. 建表
psql -h localhost -U postgres -d iragent -f src/main/resources/schema.sql

# 3. 设置 API Key
export VOLC_API_KEY=your-actual-api-key
```

### 11.2 启动应用

```bash
cd Spring-Boot/IRAgent
mvn spring-boot:run
```

### 11.3 验证

- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- Knife4j 文档：`http://localhost:8080/api/doc.html`
- 健康检查：`GET http://localhost:8080/api/cache/stats`

---

## 十二、项目结构

```
Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/
├── IrAgentApplication.java           # 启动入口
├── config/                            # 15 个配置类
│   ├── AIConfiguration.java           # LLM Bean 注册
│   ├── VolcEngineChatClient.java      # 同步 LLM 客户端（Jackson 解析）
│   ├── VolcEngineStreamingClient.java # SSE 流式客户端（含 FTT 埋点）
│   ├── AsyncConfig.java               # 虚拟线程 @Async 配置
│   ├── MilvusConfig.java              # Milvus 客户端（@ConditionalOnProperty）
│   ├── RocketMQConfig.java            # RocketMQ 配置（@ConditionalOnProperty）
│   ├── WebMvcConfig.java              # 拦截器注册
│   ├── GlobalExceptionHandler.java    # 全局异常处理（SSE 安全）
│   └── ...
├── controller/                        # 18 个 Controller
│   ├── KnowledgeBaseController.java   # /v3/kb
│   ├── ChatControllerV3.java          # /v3/chat
│   ├── GradingController.java         # /v3/grading
│   ├── ErrorBookController.java       # /v3/errors
│   ├── DashboardController.java       # /v3/dashboard
│   └── ... (9 个 V1/V2 Controller)
├── service/                           # 26 个 Service
│   ├── KnowledgeBaseService.java      # 知识库 CRUD
│   ├── NoteChunkingService.java       # 笔记切分
│   ├── IntentRouterService.java       # 意图路由
│   ├── NoteAnchoredChatService.java   # 笔记锚定答疑
│   ├── AIQuestionGenerator.java        # AI 出题引擎（LLM 生成 + SymPy 验证 + 缓存入库）
│   ├── GradingPipelineService.java    # 批改 Pipeline（4 步）
│   ├── DiagnosisService.java          # DAG 诊断编排
│   ├── DiagnosisTimelineService.java  # 诊断 → Timeline 转换
│   ├── SpacedRepetitionService.java   # 艾宾浩斯间隔复习
│   ├── ErrorBookService.java          # 错题本 CRUD
│   ├── DashboardService.java          # 仪表盘聚合
│   └── ...
├── dag/                               # DAG 工作流引擎
│   ├── core/                          # DagGraph, DagNode, ExecutionContext 等 7 个类
│   ├── engine/                        # TopologicalSorter, DagExecutor
│   └── nodes/                         # LlmCallNode, TransformNode, AggregateNode
├── rag/                               # RAG Pipeline
│   ├── embedding/                     # EmbeddingService + VolcengineEmbeddingClient
│   ├── vector/                        # QuestionVectorCollection
│   ├── cache/                         # SemanticCacheService
│   ├── retrieval/                     # PersonalNoteRetriever, FulltextSearchService, RrfRanker
│   └── pipeline/                      # NoteIngestionPipeline, QuestionIngestionPipeline
├── tenant/                            # 多租户
│   ├── TenantSemaphoreRegistry.java
│   ├── TenantQuotaInterceptor.java
│   └── NoisyNeighborDetector.java
├── mq/                                # 消息队列
│   ├── BehaviorMessageProducer.java
│   └── BehaviorBatchConsumer.java
├── entity/                            # MyBatis-Plus 实体
├── mapper/                            # MyBatis-Plus Mapper 接口
├── dto/request/ + dto/response/       # DTO
├── enums/                             # 枚举（LLM 模型等）
└── utils/                             # 工具类（ApiResponse, UserHolder, ContentParser 等）
```
