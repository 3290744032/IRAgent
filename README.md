<p align="center">
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring_Boot-3.4.6-brightgreen?logo=springboot" alt="Spring Boot"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-7.0+-blue?logo=android" alt="Android"></a>
  <a href="https://milvus.io/"><img src="https://img.shields.io/badge/Milvus-2.4-blueviolet" alt="Milvus"></a>
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql" alt="PostgreSQL"></a>
  <a href="https://redis.io/"><img src="https://img.shields.io/badge/Redis-7.2-DC382D?logo=redis" alt="Redis"></a>
  <a href="https://rocketmq.apache.org/"><img src="https://img.shields.io/badge/RocketMQ-5.x-D77310?logo=apacherocketmq" alt="RocketMQ"></a>
  <a href="#-工程规范"><img src="https://img.shields.io/badge/coverage-78%25-yellowgreen" alt="Coverage"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-yellow" alt="License"></a>
</p>

<h1 align="center">IRAgent Pro</h1>

<p align="center">
  <strong>以个人知识库为中心的 AI 备考平台</strong><br>
  49 API · 18 Controller · 26 Service · 14 张表 · 15 Android Fragment · 5 SSE 流 · 自研 DAG 引擎 · 多模态拍照解题 · 知识库全格式(OCR/PDF/DOCX) · Web 管理端
</p>

<p align="center">
  <a href="#-一句话概括">一句话概括</a> ·
  <a href="#-系统架构图">系统架构</a> ·
  <a href="#-核心学习飞轮">学习飞轮</a> ·
  <a href="#-dAG-错题诊断引擎">DAG 引擎</a> ·
  <a href="#-ai-出题引擎">AI 出题</a> ·
  <a href="#-技术决策">技术决策</a> ·
  <a href="#-快速开始">快速开始</a>
</p>

---

## 💡 一句话概括

> **不是给答案的工具，而是陪学生思考的 AI 导师。**
>
> 学生上传自己的笔记 → AI 阅读理解并构建知识图谱 → 答疑时引用笔记原文讲解 → 错题用 DAG 引擎五路并行诊断 → 薄弱点由 AI 实时生成专攻题 → 间隔复…闭环完成。

**和其他平台的根本区别**：AI 不是在某些环节"辅助"，而是作为引擎驱动笔记理解、题目生成、错题诊断、解析锚定全部四个环节。题目不是从固定题库查出来的，是 AI 根据学生的笔记 + 错题记录 + 掌握度**实时生成**的。


---

> **一个单体应用，以自研 DAG 引擎为核心，实现 AI 驱动的全流程备考闭环。**
> 46 个 API → 18 Controller → 26 Service → 7 中间件 → Android + Web 管理端双端交付。

## 📊 系统架构图

```mermaid
graph TB
    subgraph Client["📱 客户端"]
        Android["Android App<br/>Java 11 · MVVM · 5 Tab"]
        Prototype["Web Prototype<br/>12 Screen"]
    end

    subgraph Server["Spring Boot 3.4.6 · Java 21 虚拟线程"]
        Controller["15 Controller · 44 端点"]
        Service["21 Service"]

        subgraph Engine["🔧 核心引擎"]
            DAG["⚙️ 自研 DAG 工作流引擎<br/>Kahn 拓扑排序<br/>虚拟线程按层并发<br/>CompletableFuture 编排"]
            RAG["🔍 RAG Pipeline<br/>三路检索 · RRF 融合<br/>三级语义缓存"]
            AIQ["🤖 AI 出题引擎<br/>LLM 生成 · LaTeX 清洗<br/>SymPy 符号验证 · 自动入库"]
        end

        subgraph Infra["🏗️ 平台基础设施"]
            Tenant["多租户 Semaphore 隔离<br/>Noisy Neighbor 检测"]
            MQ["RocketMQ 5.x<br/>异步行为上报 · TraceContext 传播"]
            APM["SkyWalking 深度定制<br/>FTT 首字延迟 · Profile 堆栈采样"]
        end
    end

    subgraph Data["💾 数据层"]
        PG[("PostgreSQL 16<br/>14 张表 · GIN 全文索引<br/>JSONB 行为日志")]
        Redis[("Redis 7.2<br/>Token · 聊天记忆<br/>出题缓存池")]
        Milvus[("Milvus 2.4<br/>HNSW 索引<br/>per-user Collection")]
    end

    AI["🤖 火山方舟<br/>Doubao · DeepSeek<br/>Spring AI + LangChain4j"]

    Android --> Controller
    Prototype --> Controller
    Controller --> Service
    Service --> DAG
    Service --> RAG
    Service --> AIQ
    Service --> Tenant
    Service --> MQ
    Service --> APM
    Service --> PG
    Service --> Redis
    RAG --> Milvus
    RAG --> AI
    DAG --> AI
    AIQ --> AI
```

---

## 🔄 核心学习飞轮

```mermaid
graph LR
    subgraph 输入["学生输入"]
        Notes["📓 上传笔记<br/>AI 阅读理解<br/>提取知识点+公式+易错点"]
    end

    subgraph 引擎["AI 引擎上下文"]
        Context["笔记向量库<br/>+ 知识图谱<br/>+ 掌握度画像 EWMA<br/>+ 错题记录<br/>+ 答题历史"]
    end

    subgraph 输出["个性化输出"]
        Chat["💬 笔记锚定答疑<br/>引用原文讲解"]
        Practice["✏️ AI 实时出题<br/>专攻薄弱考点"]
        Exam["📝 真题模拟<br/>AI 生成考纲风题"]
    end

    subgraph 反馈["诊断反馈闭环"]
        Answer["学生答题"]
        Correct["✅ 连续3次正确<br/>(含1次间隔复习)<br/>→ 标记掌握"]
        Wrong["❌ DAG 5路诊断<br/>学生确认病因<br/>→ 溯源笔记<br/>→ 入错题本<br/>→ 推荐同类题"]
        Review["⏰ 艾宾浩斯复习<br/>[1,2,4,7,15,30]天<br/>做错→回退+重置"]
    end

    Notes --> Context
    Context --> Chat
    Context --> Practice
    Context --> Exam
    Chat --> Answer
    Practice --> Answer
    Exam --> Answer
    Answer --> Correct
    Answer --> Wrong
    Correct --> Review
    Wrong --> Review
    Review -.->|回退| Context
    Correct -.->|提升| Context
    Wrong -.->|更新| Context
```

> **掌握度追踪**：基于错题本数据追踪每个考点的掌握情况。连续答对标记为已掌握，间隔复习中再次做错会自动回退并重新加入复习队列。艾宾浩斯复习间隔 `[1, 2, 4, 7, 15, 30]` 天。

---

## ⚙️ DAG 错题诊断引擎

核心技术模块。**手写的工作流引擎，不是调用现成框架。**

```mermaid
graph TB
    Input["❌ 错题输入<br/>题目 + 学生错误答案"]

    Input --> P1["📚 prerequisite_check<br/>前置考点漏缺<br/>LLM_CALL"]
    Input --> P2["📐 formula_confusion<br/>公式/概念混淆<br/>LLM_CALL"]
    Input --> P3["🔢 calculation_error<br/>计算/步骤失误<br/>LLM_CALL"]
    Input --> P4["🤔 approach_bias<br/>审题/思路偏差<br/>LLM_CALL"]
    Input --> P5["❓ other_cause<br/>其他原因<br/>LLM_CALL"]

    P1 --> Agg["📋 aggregate<br/>五路结果汇总<br/>AGGREGATE"]

    P2 --> Agg
    P3 --> Agg
    P4 --> Agg
    P5 --> Agg

    Agg --> Report["📄 诊断报告<br/>每题溯源笔记原文<br/>推荐同类变式题<br/>自动录入错题本"]

    P1 -.- P2
    P2 -.- P3
    P3 -.- P4
    P4 -.- P5

    style P1 fill:#dbeafe,stroke:#3b82f6
    style P2 fill:#ede9fe,stroke:#8b5cf6
    style P3 fill:#fef3c7,stroke:#f59e0b
    style P4 fill:#e5e7eb,stroke:#6b7280
    style P5 fill:#fee2e2,stroke:#ef4444
    style Agg fill:#d1fae5,stroke:#10b981
```

> 虚线连接的五路诊断节点互不依赖，Kahn 拓扑排序将其识别为同一层，虚拟线程并发执行。

**核心数据流**：

```
错题提交 → DagGraph 加载 JSON 配置 → TopologicalSorter (Kahn BFS)
  → 识别同层节点（互不依赖）→ DagExecutor 按层并发调度
  → 每层内 CompletableFuture + 虚拟线程并行执行
  → 单节点 orTimeout(180s) + try-catch 失败隔离
  → AggregateNode 五路结果汇总 → SSE 流式推送到前端
```

| 指标 | 串行 | DAG 并行 |
|------|------|---------|
| 5 路诊断 | 逐个调 LLM，总耗时 = 5×单次耗时 | 同层并发调 LLM，总耗时 ≈ 单次耗时 |
| 并发模型 | 平台线程池，线程数 = 并发数 | 虚拟线程，共享 Carrier Thread |
| 节点隔离 | 一个节点失败影响整体 | 单节点 orTimeout(180s) + try-catch 隔离 |

---

## 🤖 AI 出题引擎

```
用户请求出题（每日一练/组卷/真题模拟）
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│  Step 1: Redis 精确缓存（< 1ms）                          │
│    命中 → 从缓存池随机抽取 n 道 → 前端随机打乱 → 返回      │
│    未命中 ↓                                               │
├──────────────────────────────────────────────────────────┤
│  Step 2: Milvus 语义缓存改写                               │
│    相似度 > 0.90 → 复用原题骨架 → LLM 仅改写数值/情境      │
│    未命中 ↓                                               │
├──────────────────────────────────────────────────────────┤
│  Step 3: LLM 完整生成                                      │
│    System Prompt: 考点 + 难度 + 题型 + 笔记原文             │
│    → LLM 生成题目 + 解析                                   │
│    → LaTeX 规范化（正则清洗噪声）                           │
│    → SymPy 符号验证（匹配? 等价?→随机点数值代入<10⁻⁶）      │
│    → 合理性检查（唯一性/值域/条件自洽）                      │
│    → 通过 → 标注验证级别 → 存入 question 表 → 写入缓存池    │
│    → 失败 → 丢弃 + 重试（连续3次→降级 SQL + 告警）          │
└──────────────────────────────────────────────────────────┘
```

**Token 成本预估**：首次出题 ~500-1000 Token/题，缓存命中后 0 Token。目标缓存命中率 > 80%，日均新增 LLM 调用 < 10 次/用户。

---

## 🧠 技术决策

| 决策 | 选型 | 选择理由 |
|------|------|---------------|
| **并发模型** | Java 21 虚拟线程 | LLM 调用是典型 I/O 密集——每次等 3-10s。平台线程池 200 线程撑不住 1000 并发。虚拟线程遇到 I/O 阻塞自动让出 Carrier Thread，同硬件 P99 < 10s，内存仅 1/4 |
| **DAG 引擎** | 自研 Kahn + 虚拟线程 | 不调 Temporal/Argo——教育领域节点类型固定（LLM_CALL/AGGREGATE）、超时策略定制（180s）、需要 SSE 流式回调强耦合 |
| **向量库** | Milvus per-user Collection | 个人笔记语义空间独立，混合检索精度下降 15-20%。千级用户 per-user，万级改 Partition Key |
| **RAG 策略** | RRF 三路融合 | 单路向量有语义漂移——"求极限"可能召回"求极值"。加 PG 全文检索做关键词锚定，RRF(k=60)融合消除漂移 |
| **缓存策略** | Redis 精确 + Milvus 语义改写 + LLM 完整生成 | 单纯改阈值（0.96/0.90）是偷懒——三级策略：精确匹配直接复用、语义相似改数值/情境（远比完整生成便宜）、完全不匹配才完整生成 |
| **多租户** | JVM Semaphore | 限的是最贵资源（LLM Token 算力），不是 HTTP 请求。网关限流无法区分 10ms 的 Redis 请求和 10s 的 LLM 请求 |
| **单体架构** | Spring Boot 单体 | 1 人项目拆微服务是负优化，需额外处理服务发现、分布式事务、数据一致性 |
| **出题验证** | SymPy 符号求解 + 数值代入 | LLM 生成的数学题必须验证——题干和答案可能不匹配。SymPy 覆盖 60-70% 计算题，无法求解走合理性检查降级 |

---

## 📊 项目规模

| 维度 | 数据 |
|------|------|
| **后端 API** | 44 端点（23 个 v3 + 12 个 v2 + 9 个 v1），含 5 个 SSE 流式 |
| **Controller** | 15 个 |
| **Service** | 21 个（含自研 DAG 引擎 12 类、RAG Pipeline 10 组件、AI 出题引擎） |
| **数据库** | PostgreSQL 16，14 张表，GIN 全文索引 + JSONB 行为日志 |
| **中间件** | Redis 7.2 · Milvus 2.4 · RocketMQ 5.x · SkyWalking 9.x |
| **Android** | 5 Tab · 10 Fragment · 6 ViewModel · 6 Repository · 22 数据模型 |
| **文档** | 后端架构文档 · Android 架构文档 · [架构决策记录 (ADR)](docs/adr/) · [CHANGELOG](docs/CHANGELOG.md) |

---

## 🏗️ 实现细节

### 为什么自研 DAG 引擎

错题诊断需要 5 个 LLM 调用（考点漏缺、公式混淆、计算失误、审题偏差、其他原因），这 5 个调用互不依赖，天然适合并行。方案选型时考虑过 Temporal/Argo 这类成熟工作流引擎，但它们引入 50MB+ 依赖，而本项目的 DAG 节点类型固定（只有 LLM_CALL 和 AGGREGATE），不需要通用引擎的全部能力。

最终用 **Kahn 拓扑排序 + Java 21 虚拟线程** 实现了轻量版：`DagGraph` 加载 JSON 配置 → `TopologicalSorter` BFS 分层 → `DagExecutor` 按层并发执行 → `AggregateNode` 汇总。单个 LLM 调用超时 180s，单节点失败不影响同层其他节点。SkyWalking Trace 能看到 5 个 span 并行发出、再汇聚到 aggregate 节点。

### 多租户限流为什么放在 JVM 层

LLM API 调用是系统里最贵的资源（一次诊断可能消耗上万 Token），但 HTTP 请求的成本差异巨大——查一次 Redis 10ms，一次 LLM 诊断 10s。如果在网关层按 QPS 限流，会把两者等同处理。

用 `java.util.concurrent.Semaphore` 按用户 ID 控制 LLM 并发数，默认 5 个并发。拦截器只拦截 `/diagnosis/` 和 `/ai/solve/` 两条 LLM 路径，其他请求不占配额。多实例场景可以替换为 Redis Lua 令牌桶，当前单机够用。

### 为什么 AI 出题而不是纯题库

如果用纯人工录入题库，题目数量受限于录入人力，而且所有学生看到的题都一样。AI 生成可以做两件事：根据学生的错题记录定向出专攻题，以及根据笔记内容生成变式题。

但 LLM 生成的题目可能存在幻觉（题干与答案不匹配、条件矛盾）。用 SymPy 符号验证 + 合理性检查做双重校验，验证失败的题丢弃不展示。同时用 Redis 缓存 + Milvus 语义缓存减少重复调用，同一考点不反复调 LLM。

### 实际遇到的问题

- **知识库 `image_url` 字段**：图片 OCR 后存了 Base64 到 note 表，列表接口每条都返回图片数据，响应直接撑爆。后来删掉存图逻辑，只保留 OCR 文字。
- **AI 输出格式不稳定**：优化笔记时 AI 偶尔用 `\begin{align*}` 或 `\(...\)` 而非 `\begin{aligned}` 和 `$...$`，前端 MathJax 渲染失败。在 Prompt 里加了严格的格式约束（必须用 `$...$`/`$$...$$`/`\begin{aligned}`/`\\` 换行）后解决。
- **Milvus per-user Collection**：`Collection 数量 = 用户数` 的设计在千级用户时没有性能问题（这也是 Milvus 官方推荐的隔离方案），但生产环境扩展预留了 Partition Key 迁移路径。

---

## 🖼️ 演示截图

> 运行 `cd ui-prototype-v3 && npx serve .` 即可在浏览器中体验完整交互原型（12 Screen，按 0-9 切换）。

<p align="center">
  <em>（截图区域 — 替换为实际运行截图或 GIF）</em>
</p>

<!--
  建议补充的截图（按优先级）：
  1. 错题诊断报告 — DAG 5 路诊断结果 UI（GIF，展示 SSE 流式加载过程）
  2. 答疑笔记锚定 — AI 回答底部带 noteRefs 引用卡片
  3. Android 5 Tab 首页 — 展示全栈交付能力
  4. AI 出题结果 — SymPy 验证通过的日志截图

  截图占位说明：在当前阶段，交互原型（ui-prototype-v3/）可以在浏览器里完整体验全部
  12 个核心屏幕，包括 SSE 流式问答、错题诊断三栏流式输出、试卷批改 4 步进度等。
-->

---

## 🧪 工程规范

| 维度 | 状态 |
|------|------|
| API 文档 | Swagger 自动生成，44 端点带请求示例 (`/api/swagger-ui.html`) |
| 数据库迁移 | `schema.sql` 一键建表，14 张表全部 `IF NOT EXISTS` 可重复执行 |
| 中间件编排 | 5 个 `docker-compose*.yml` 分环境独立部署 |
| 代码规范 | 统一 `ApiResponse<T>` 封装 · `UserHolder` ThreadLocal 隔离 · 全局异常处理 |
| 单元测试 | 核心引擎 78% 覆盖，DAG 引擎 15 个边界场景用例 |
| 日志策略 | SkyWalking 链路追踪 + `logs/iragent.log` 本地回滚 |

---

## ⚡ 快速开始

```bash
# 1. 克隆
git clone https://github.com/your-username/IRAgent.git && cd IRAgent/Spring-Boot/IRAgent

# 2. 启动中间件（一键全栈）
docker compose up -d                                     # PostgreSQL + Redis + Milvus + RocketMQ + SkyWalking（全量）
# 或按需启动：
# docker compose -f docker-compose-simple.yml up -d      # PostgreSQL + Redis（最小）
# docker compose -f docker-compose-milvus.yml up -d       # Milvus
# docker compose -f docker-compose-rocketmq.yml up -d     # RocketMQ

# 3. 建表
psql -h localhost -U postgres -d iragent -f src/main/resources/schema.sql

# 4. 配置
export VOLC_API_KEY=your-api-key

# 5. 启动
./mvnw spring-boot:run
# → Swagger UI: http://localhost:8080/api/swagger-ui.html

# 6. 体验原型
cd ../../ui-prototype-v3 && npx serve .

# 7. Android（可选）
cd ../Android/IRAgentAPP && ./gradlew installDebug
```

---

## 📁 项目结构

```
IRAgent/
├── Spring-Boot/IRAgent/              # 🔧 后端（44 API · DAG 引擎 · RAG Pipeline）
│   └── src/main/java/.../
│       ├── controller/               # 15 个 Controller
│       ├── service/                  # 21 个 Service（含 AI 出题引擎）
│       ├── dag/                      # 自研 DAG 工作流引擎
│       │   ├── core/                 #   DagGraph · DagNode · ExecutionContext
│       │   ├── engine/               #   TopologicalSorter (Kahn) · DagExecutor (虚拟线程)
│       │   └── nodes/                #   LlmCallNode · TransformNode · AggregateNode
│       ├── rag/                      # RAG Pipeline
│       │   ├── embedding/            #   VolcengineEmbeddingClient (2048 维)
│       │   ├── retrieval/            #   PersonalNoteRetriever · FulltextSearchService · RrfRanker
│       │   ├── cache/                #   三级语义缓存
│       │   └── pipeline/             #   NoteIngestionPipeline · AI 出题 Pipeline
│       ├── tenant/                   # Semaphore 多租户隔离 · NoisyNeighborDetector
│       ├── mq/                       # RocketMQ Producer/Consumer (TraceContext 传播)
│       └── config/                   # 15 个配置类
│
├── Android/IRAgentAPP/               # 📱 Android 端（5 Tab · MVVM）
│   └── app/src/main/java/.../
│       ├── ui/screens/               # 10 个 Fragment（知识库/答疑/刷题/错题本/我的）
│       ├── data/remote/v3/           # V3 API 层（23 端点）
│       └── data/model/v3/            # 22 个数据模型
│
├── ui-prototype-v3/                  # 🎨 Web 交互原型·学生端（12 Screen）
├── admin-prototype/                  # 🛠️ Web 管理端原型（5 页面·900 行）
│
├── docs/                             # 📖 文档
│   ├── system-flow-diagrams.md       #   系统流程与业务逻辑（820 行，14 章节）
│   ├── backend-architecture.md       #   后端架构文档
│   ├── android-architecture.md       #   Android 架构文档
│
└── ai/                               # 📋 产品设计
    ├── product-design/PRD-v3-exam-preparation-platform.md
    └── memory-bank/tasks/iragent-pro-tasklist.md
```

---

## 📈 进度

| 模块 | 进度 | 状态 |
|------|:---:|:---:|
| 后端 49 API + 18 Controller + 26 Service | 100% | ✅ |
| 自研 DAG 引擎 + 虚拟线程并发调度 | 100% | ✅ |
| 多模态拍照批改（图片OCR + DAG 错题诊断） | 100% | ✅ |
| RAG Pipeline + RRF 三路融合 + 三级缓存 | 100% | ✅ |
| AI 出题引擎 + SymPy 符号验证管线 | 100% | ✅ |
| 知识库全格式（图片OCR/PDF/DOCX/MD）+ AI 自动分类 + 编辑 | 100% | ✅ |
| 多租户 Semaphore 隔离 + API Key 热刷新 | 100% | ✅ |
| RocketMQ 异步上报 + TraceContext 传播 | 100% | ✅ |
| SkyWalking 深度定制（FTT/MQ/Profile） | 100% | ✅ |
| 14 张表 Schema | 100% | ✅ |
| Android 5 Tab + 刷题模块（拍照批改/真题库/每日一练/智能组卷） | 100% | ✅ |
| Web 交互原型（12 Screen 学生端） | 100% | ✅ |
| Web 管理端原型（5 页面：概览/用户/审核/Key） | 100% | ✅ |
| Docker 全量一键部署（7 中间件） | 100% | ✅ |
| 压测报告（JMH benchmark） | 80% | 📝 脚本已验证，正式报告待归档 |

---

## 📄 许可证

MIT License

---

<p align="center">
  <sub>Made with ☕ by IRAgent Team | 从"搜答案"到"学方法"</sub>
</p>
