<p align="center">
  <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring_Boot-3.4.6-brightgreen?logo=springboot" alt="Spring Boot"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-7.0+-blue?logo=android" alt="Android"></a>
  <a href="https://milvus.io/"><img src="https://img.shields.io/badge/Milvus-2.4-blueviolet" alt="Milvus"></a>
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql" alt="PostgreSQL"></a>
  <a href="https://redis.io/"><img src="https://img.shields.io/badge/Redis-7.2-DC382D?logo=redis" alt="Redis"></a>
  <a href="https://rocketmq.apache.org/"><img src="https://img.shields.io/badge/RocketMQ-5.x-D77310?logo=apacherocketmq" alt="RocketMQ"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-yellow" alt="License"></a>
</p>

<h1 align="center">IRAgent Pro</h1>

<p align="center">
  <strong>以个人知识库为中心的 AI 备考平台</strong><br>
  50+ API · 18 Controller · 26+ Service · 15 张表 · 15+ Fragment · 9 SSE 流 · 自研 DAG 引擎 · 多模态拍照解题 · ECharts 知识图谱 · 智能组卷
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
> 学生上传自己的笔记 → AI 阅读理解并构建知识图谱 → 答疑时引用笔记原文讲解 → 错题用 DAG 引擎五路并行诊断 → 薄弱点由 AI 实时生成专攻题 → 间隔复习闭环完成。

**和其他平台的根本区别**：AI 不是在某些环节"辅助"，而是作为引擎驱动笔记理解、题目生成、错题诊断、解析锚定全部四个环节。题目不是从固定题库查出来的，是 AI 根据学生的笔记 + 错题记录 + 掌握度**实时生成**的。


---

## 📊 系统架构图

```mermaid
graph TB
    subgraph Client["📱 客户端"]
        Android["Android App<br/>Java 11 · MVVM · 5 Tab<br/>ECharts 知识图谱 · KaTeX 渲染"]
        Prototype["Web Prototype"]
    end

    subgraph Server["Spring Boot 3.4.6 · Java 21 虚拟线程"]
        Controller["18 Controller · 50+ 端点"]
        Service["26 Service"]

        subgraph Engine["🔧 核心引擎"]
            DAG["⚙️ 自研 DAG 工作流引擎<br/>Kahn 拓扑排序<br/>虚拟线程按层并发<br/>CompletableFuture 编排"]
            RAG["🔍 RAG Pipeline<br/>三路检索 · RRF 融合<br/>三级语义缓存"]
            AIQ["🤖 AI 出题引擎<br/>LLM 生成 · LaTeX 清洗<br/>Redis 缓存 · 自动入库"]
            Graph["🕸️ 知识图谱<br/>GraphDataService SQL聚合<br/>ECharts 力导向图<br/>考点/笔记/错题三元拓扑"]
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

    AI["🤖 火山方舟<br/>Doubao · DeepSeek<br/>自研 LLM Client + Embedding Client"]

    Android --> Controller
    Prototype --> Controller
    Controller --> Service
    Service --> DAG
    Service --> RAG
    Service --> AIQ
    Service --> Graph
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
        Context["笔记向量库<br/>+ 知识图谱<br/>+ 掌握度画像<br/>+ 错题记录<br/>+ 答题历史"]
    end

    subgraph 输出["个性化输出"]
        Chat["💬 笔记锚定答疑<br/>引用原文讲解"]
        Practice["✏️ AI 实时出题<br/>专攻薄弱考点"]
        Exam["📝 智能组卷<br/>AI 生成考纲风题"]
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

---

## 🤖 AI 出题引擎

```
用户请求出题（每日一练/组卷/真题模拟）
        │
        ▼
┌──────────────────────────────────────────────────────┐
│  Step 1: Redis 精确缓存（< 1ms）                      │
│    命中 → 从缓存池随机抽取 n 道 → 前端随机打乱 → 返回  │
│    未命中 ↓                                           │
├──────────────────────────────────────────────────────┤
│  Step 2: PG 语义缓存改写（< 5ms）                       │
│    同考点+题型+难度±1 → 从 question 表复用已有题目      │
│    命中 → 返回，减少重复 LLM 调用                       │
│    未命中 ↓                                             │
├──────────────────────────────────────────────────────┤
│  Step 3: LLM 完整生成                                  │
│    System Prompt: 考点 + 难度 + 题型 + 笔记原文         │
│    → LLM 生成题目 + 解析                               │
│    → JSON 提取 + 结构化校验                             │
│    → 存入 question 表 → 写入 Redis 缓存池               │
└──────────────────────────────────────────────────────┘
```

---

## 🕸️ 知识图谱

对标 Obsidian 双链模型，ECharts 5.5 力导向图 + 本地库渲染（零网络依赖）。

```
GET /v3/kb/graph-data → GraphDataService
  → 考点节点: note_chunk GROUP BY + mastery_records 掌握度
  → 笔记节点: note ORDER BY updated_at
  → 错题节点: error_book ORDER BY created_at
  → 边: 考点↔笔记(belongs_to) + 笔记↔错题(linked) + 考点↔错题(tests)
  → cleanNodeName(): 去 Markdown/LaTeX/公式 → 2~12 字纯净名
  → Base64 → atob → decodeURIComponent(UTF-8 还原) → ECharts 渲染
```

**交互**：全局骨架视图（仅考点）→ 点击展开 2-hop 邻居 → 过滤器（显示错题/已掌握）→ 搜索定位 → JSBridge 导航到详情

---

## 🧠 技术决策

| 决策 | 选型 | 选择理由 |
|------|------|---------------|
| **并发模型** | Java 21 虚拟线程 | LLM 调用是典型 I/O 密集——每次等 3-10s。平台线程池 200 线程撑不住 1000 并发。虚拟线程遇到 I/O 阻塞自动让出 Carrier Thread，同硬件 P99 < 10s，内存仅 1/4 |
| **DAG 引擎** | 自研 Kahn + 虚拟线程 | 不调 Temporal/Argo——教育领域节点类型固定（LLM_CALL/AGGREGATE）、超时策略定制（180s）、需要 SSE 流式回调强耦合 |
| **向量库** | Milvus per-user Collection | 个人笔记语义空间独立，混合检索精度下降 15-20%。千级用户 per-user，万级改 Partition Key |
| **RAG 策略** | RRF 三路融合 | 单路向量有语义漂移——"求极限"可能召回"求极值"。加 PG 全文检索做关键词锚定，RRF(k=60)融合消除漂移 |
| **缓存策略** | Redis 精确 + PG 近似匹配 + LLM 完整生成 | 三级递进：Redis 精确命中直接返回 → PG 同考点+题型+难度±1 复用已有题目 → LLM 完整生成。越靠前越便宜 |
| **多租户** | JVM Semaphore | 限的是最贵资源（LLM Token 算力），不是 HTTP 请求。网关限流无法区分 10ms 的 Redis 请求和 10s 的 LLM 请求 |
| **单体架构** | Spring Boot 单体 | 1 人项目拆微服务是负优化，需额外处理服务发现、分布式事务、数据一致性 |
| **出题引擎** | Redis + PG 近似匹配 + LLM 生成 | 先查 Redis 精确缓存，再查 PG 同考点近似题复用，都未命中才调 LLM。逐步降低 Token 消耗 |
| **图谱渲染** | ECharts + WebView 本地库 | 1MB 本地化，零网络依赖。`atob()`+`decodeURIComponent` 解决中文 UTF-8 乱码。onPageFinished 后注入数据消除 JS 未就绪竞态 |
| **公式渲染** | KaTeX + marked 本地库 | ~350KB assets/libs/，`file:///android_asset/` 加载，无 CDN 延迟/不可用风险 |

---

## 📊 项目规模

| 维度 | 数据 |
|------|------|
| **后端 API** | 50+ 端点（含 9 个 SSE 流式：答疑/批改/组卷/诊断/深度学习） |
| **Controller** | 18 个 |
| **Service** | 26 个（含自研 DAG 引擎 12 类、RAG Pipeline 10 组件、AI 出题引擎、GraphDataService） |
| **数据库** | PostgreSQL 16，15 张表，GIN 全文索引 + JSONB 行为日志 |
| **中间件** | Redis 7.2 · Milvus 2.4 · RocketMQ 5.x · SkyWalking 9.x |
| **Android** | 5 Tab · 15+ Fragment · 8 ViewModel · 8 Repository · 25 数据模型 |
| **Android 本地库** | KaTeX (~350KB) · ECharts (~1MB) · marked (~50KB) — 全部零网络依赖 |
| **文档** | 后端架构文档 · Android 架构文档 |

---

## 🏗️ 实现细节

### 为什么自研 DAG 引擎

错题诊断需要 5 个 LLM 调用（考点漏缺、公式混淆、计算失误、审题偏差、其他原因），这 5 个调用互不依赖，天然适合并行。方案选型时考虑过 Temporal/Argo 这类成熟工作流引擎，但它们引入 50MB+ 依赖，而本项目的 DAG 节点类型固定（只有 LLM_CALL 和 AGGREGATE），不需要通用引擎的全部能力。

最终用 **Kahn 拓扑排序 + Java 21 虚拟线程** 实现了轻量版：`DagGraph` 加载 JSON 配置 → `TopologicalSorter` BFS 分层 → `DagExecutor` 按层并发执行 → `AggregateNode` 汇总。单个 LLM 调用超时 180s，单节点失败不影响同层其他节点。

### 多租户限流为什么放在 JVM 层

LLM API 调用是系统里最贵的资源（一次诊断可能消耗上万 Token），但 HTTP 请求的成本差异巨大——查一次 Redis 10ms，一次 LLM 诊断 10s。如果在网关层按 QPS 限流，会把两者等同处理。

用 `java.util.concurrent.Semaphore` 按用户 ID 控制 LLM 并发数，默认 5 个并发。拦截器只拦截 `/diagnosis/` 和 `/ai/solve/` 两条 LLM 路径，其他请求不占配额。

### 为什么 AI 出题而不是纯题库

如果用纯人工录入题库，题目数量受限于录入人力，而且所有学生看到的题都一样。AI 生成可以做两件事：根据学生的错题记录定向出专攻题，以及根据笔记内容生成变式题。

但 LLM 生成的题目可能存在幻觉（题干与答案不匹配、条件矛盾）。用 Redis 精确缓存（相同考点+题型+难度）避免高频重复调用，Milvus 语义缓存作为改写降级。

### 技术选型：自研 vs LangChain / Spring AI

本项目未引入 LangChain4j 或 Spring AI。两者都是优秀的框架，但 IRAgent 的场景特殊：

- **已有更强的 DAG 引擎**：`DagExecutor` + `LlmCallNode` 比 LangChain 的 LCEL 更灵活，且深度绑定了 SSE 流式推送和移动端需求。LangChain 的链式调用在此场景下是降级。
- **RAG 深度定制**：三路召回（Milvus 向量 + PG 全文 + RRF 融合）针对个人笔记场景做了批量 Embedding 调度和准确度调优，LangChain 默认的向量检索 + 拼 Prompt 模式覆盖不了。
- **唯一 LLM 供应商**：项目只对接火山方舟一家，OkHttp 直连比多一层抽象更轻、更可控。LangChain 的多供应商适配优势用不上。
- **记忆管理自研更适配**：`RedisChatMemoryRepository` 整合了 PG 持久化 + 7 天 TTL + 对话摘要自动截断，比 LangChain 的 `ConversationBufferWindowMemory` 更贴合移动端场景。

结论：IRAgent 的自研方案在流式性能、SSE 移动端推送、多租户路由、批量 Embedding 调度上更贴合实际需求，LangChain 在此场景下不是更优选择。

### KaTeX + ECharts 为什么本地化

WebView 加载 CDN 资源有两个致命问题：国内网络访问 jsdelivr/cdnjs 不稳定，以及 `onPageFinished` 回调时外部脚本可能尚未下载完毕导致渲染竞态。

将 KaTeX (~350KB) 和 ECharts (~1MB) 放到 `assets/libs/`，通过 `file:///android_asset/` 加载。ECharts 数据用 Base64 → `atob()` → `decodeURIComponent(%HH 编码)` 管道解决中文 UTF-8 乱码。WebView 等 `onPageFinished` 后再注入数据，消除 JS 未就绪竞态。

### 知识图谱从静态 SVG 到动态 ECharts

初版是一个写死的 SVG 文件（29 行假数据）。重构后：后端 `GraphDataService` 聚合 SQL（note_chunk + mastery_records + error_book）返回 `{nodes, edges}` JSON，经过 `cleanNodeName()` 清洗 Markdown/LaTeX 后推送到前端 ECharts 力导向图。支持全局骨架视图、点击展开 2-hop 邻居、浮动过滤器、搜索定位、JSBridge 导航。

### 实际遇到的问题

- **知识库 `image_url` 字段**：图片 OCR 后存了 Base64 到 note 表，列表接口每条都返回图片数据，响应直接撑爆。后来删掉存图逻辑，只保留 OCR 文字。
- **AI 输出格式不稳定**：优化笔记时 AI 偶尔用 `\begin{align*}` 或 `\(...\)` 而非 `\begin{aligned}` 和 `$...$`，前端 KaTeX 渲染失败。在 Prompt 里加了严格的格式约束后解决。
- **Milvus per-user Collection**：`Collection 数量 = 用户数` 的设计在千级用户时没有性能问题（这也是 Milvus 官方推荐的隔离方案），生产环境扩展预留了 Partition Key 迁移路径。
- **知识图谱节点名过长**：`note_chunk.knowledge_point` 存的是整段 Markdown（含公式），做 `cleanNodeName()` 清洗流水线（取首行 → 去 Markdown → 去 LaTeX → 去标点 → 截断 12 字）。
- **ECharts 中文乱码**：`atob()` 返回字节串，中文 UTF-8 多字节序列被当 Latin-1 单字节，`JSON.parse` 乱码。改 `decodeURIComponent(%HH编码)` 还原。
- **智能组卷 JSON 块**：Prompt 要求 LLM 末尾输出 `\`\`\`json` 结构化数据，流式推送时 JSON 块被 `onChunk` 推到界面。检测到标记后停止推送 + `fullContent` 正则兜底过滤。

---

## 🖼️ 系统截图

### 首页 & 注册
<p align="center">
  <img src="docs/images/screenshots/1.1.首页.jpg" width="30%" alt="首页">
  &nbsp;
  <img src="docs/images/screenshots/1.2.注册.jpg" width="30%" alt="注册">
  &nbsp;
  <img src="docs/images/screenshots/2.引导界面.jpg" width="30%" alt="引导界面">
</p>

### 知识库 — 笔记上传 & 详情
<p align="center">
  <img src="docs/images/screenshots/3.1.知识库.jpg" width="30%" alt="知识库">
  &nbsp;
  <img src="docs/images/screenshots/3.2.知识库上传.jpg" width="30%" alt="上传">
  &nbsp;
  <img src="docs/images/screenshots/3.3.笔记.jpg" width="30%" alt="笔记详情">
</p>

### 答疑 — 流式对话 · 函数图像 · RAG 笔记锚定
<p align="center">
  <img src="docs/images/screenshots/4.1.1.答疑.jpg" width="30%" alt="答疑">
  &nbsp;
  <img src="docs/images/screenshots/4.1.2.答疑解答.jpg" width="30%" alt="解答">
  &nbsp;
  <img src="docs/images/screenshots/4.1.3.函数图像+RAG.jpg" width="30%" alt="函数图像+RAG">
</p>

### 深度学习 & 视频讲解
<p align="center">
  <img src="docs/images/screenshots/4.2.深度解答界面.jpg" width="45%" alt="深度学习">
  &nbsp;
  <img src="docs/images/screenshots/4.3.1视频解答.jpg" width="45%" alt="视频讲解">
</p>

### 刷题 — 每日一练 & 真题库
<p align="center">
  <img src="docs/images/screenshots/5.1.刷题界面.jpg" width="45%" alt="刷题">
  &nbsp;
  <img src="docs/images/screenshots/5.4刷题界面.jpg" width="45%" alt="每日一练">
</p>

### 试卷批改 — 拍照上传 → 4 步 SSE → 批改报告
<p align="center">
  <img src="docs/images/screenshots/5.2.1.试卷批改界面.jpg" width="23%" alt="批改界面">
  <img src="docs/images/screenshots/5.2.2.上传图片界面.jpg" width="23%" alt="上传图片">
  <img src="docs/images/screenshots/5.2.3.批改过程界面.jpg" width="23%" alt="批改进度">
  <img src="docs/images/screenshots/5.2.4.批改结果界面.jpg" width="23%" alt="批改结果">
</p>

### 智能组卷 — AI 流式生成 → 在线答题 → PDF 导出
<p align="center">
  <img src="docs/images/screenshots/5.3.1.智能组卷界面.jpg" width="23%" alt="组卷界面">
  <img src="docs/images/screenshots/5.3.2.智能组卷演示1.jpg" width="23%" alt="演示1">
  <img src="docs/images/screenshots/5.3.3.智能组卷演示2.jpg" width="23%" alt="演示2">
  <img src="docs/images/screenshots/5.3.4.智能组卷演示3.jpg" width="23%" alt="演示3">
  <img src="docs/images/screenshots/5.3.5.智能组卷导出PDF.jpg" width="23%" alt="导出PDF">
  <img src="docs/images/screenshots/5.3.6.智能组卷PDF保存界面.jpg" width="23%" alt="PDF保存">
  <img src="docs/images/screenshots/5.3.7.智能组卷PDF成果.jpg" width="23%" alt="PDF成果">
</p>

### 错题本 — 列表 & AI 三维诊断
<p align="center">
  <img src="docs/images/screenshots/6.1.错题本.jpg" width="45%" alt="错题本">
  &nbsp;
  <img src="docs/images/screenshots/6.2.错题本诊断界面.jpg" width="45%" alt="错题诊断">
</p>

---

## ⚡ 快速开始

```bash
# 1. 克隆
git clone https://github.com/3290744032/IRAgent.git && cd IRAgent/Spring-Boot/IRAgent

# 2. 启动中间件（一键全栈）
docker compose up -d                                     # PostgreSQL + Redis + Milvus + RocketMQ + SkyWalking
# 或按需启动：
# docker compose -f docker-compose-simple.yml up -d      # PostgreSQL + Redis（最小）

# 3. 建表
psql -h localhost -U postgres -d iragent -f src/main/resources/schema.sql

# 4. 配置
export VOLC_API_KEY=your-api-key

# 5. 启动
./mvnw spring-boot:run
# → Swagger UI: http://localhost:8080/api/swagger-ui.html

# 6. Android（可选）
cd ../Android/IRAgentAPP && ./gradlew installDebug
```

---

## 📁 项目结构

```
IRAgent/
├── Spring-Boot/IRAgent/              # 🔧 后端（50+ API · DAG 引擎 · RAG Pipeline）
│   └── src/main/java/.../
│       ├── controller/               # 18 个 Controller
│       ├── service/                  # 26 个 Service（含 AI 出题引擎、GraphDataService）
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
│       └── config/                   # ApiKeyProvider 热刷新 · VolcEngine 客户端
│
├── Android/IRAgentAPP/               # 📱 Android 端（5 Tab · MVVM）
│   └── app/src/main/
│       ├── java/.../
│       │   ├── config/               #   SubjectConfig 学科统一配置
│       │   ├── ui/screens/          #   15+ Fragment（知识库/答疑/刷题/错题本/我的）
│       │   ├── data/remote/v3/      #   V3 API 层（20+ 端点）
│       │   ├── data/repository/v3/  #   8 Repository
│       │   └── data/model/v3/       #   25 数据模型
│       └── assets/
│           ├── libs/                 #   KaTeX + marked + ECharts 本地库（~1.4MB）
│           ├── math_template.html    #   笔记内容 KaTeX 渲染模板
│           ├── knowledge_graph.html  #   ECharts 知识图谱（骨架/聚焦/过滤/搜索）
│           └── engine/               #   Timeline 黑板演算引擎
│
├── docs/                             # 📖 文档
│   ├── android-architecture.md       #   Android 架构文档
│   ├── backend-architecture.md       #   后端架构文档
│   └── images/                       #   截图资源
│
└── ai/                               # 📋 产品设计（空，预留）
```

---

## 📈 进度

| 模块 | 状态 |
|------|:---:|
| 后端 50+ API + 18 Controller + 26 Service | ✅ |
| 自研 DAG 引擎 + 虚拟线程并发调度 | ✅ |
| 多模态拍照批改（图片OCR + DAG 错题诊断） | ✅ |
| RAG Pipeline + RRF 三路融合 + 三级缓存 | ✅ |
| AI 出题引擎 + Redis 缓存管线 | ✅ |
| 知识库全格式（图片OCR/PDF/DOCX/MD）+ AI 自动分类 + 编辑优化 | ✅ |
| ECharts 知识图谱（考点/笔记/错题三元拓扑 + Obsidian 级交互） | ✅ |
| 错题本（三维诊断 + 同类题推荐 + 艾宾浩斯复习） | ✅ |
| 智能组卷（SSE 流式生成 + PDF 导出 + 在线答题） | ✅ |
| 每日一练（AI 出题优先 + 同类题巩固模式） | ✅ |
| KaTeX + ECharts 本地化（零网络依赖） | ✅ |
| 多租户 Semaphore 隔离 + API Key 热刷新 | ✅ |
| RocketMQ 异步上报 + TraceContext 传播 | ✅ |
| SkyWalking 深度定制（FTT/MQ/Profile） | ✅ |
| 15 张表 Schema | ✅ |
| Android 5 Tab + 15+ Fragment | ✅ |
| Docker 全量一键部署 | ✅ |

---

## 📄 许可证

MIT License

---

<p align="center">
  <sub>Made with ☕ by IRAgent Team | 从"搜答案"到"学方法"</sub>
</p>
