# IRAgent Pro (严肃教育版) 系统规格说明书

> **项目定位**：大厂实习求职作品集 — 展示 Java 后端工程深度与全栈协议设计能力
> **目标岗位**：后端开发实习（Java 方向）
> **项目周期**：12-15 周，单人开发，分 4 个 Phase 滚动推进

---

## 核心策略

不做微服务拆分（单体架构足够）。集中在 **4 个面试必杀技**：
1. Java 21 虚拟线程 + 自研 DAG 工作流引擎
2. 语义缓存 + Milvus 向量检索 + RRF 混合重排
3. 多租户算力隔离 + RocketMQ 异步削峰
4. SkyWalking 深度定制（不是装 Agent，是扩展 APM 解决业务特有盲区）

---

## 技术栈

| 类别 | 技术 | 面试可讲深度 |
|------|------|-------------|
| JDK | Java 21（从 17 升级） | 虚拟线程原理、Carrier Thread、I/O 密集型场景对比 |
| 框架 | Spring Boot 3.4.6（保持单体） | 模块化分层、清晰边界 |
| 消息队列 | Apache RocketMQ 5.x | 异步行为上报、批量消费、削峰 |
| 缓存 | Redis 7.2 + Lua 脚本 | 多租户令牌桶限流、语义缓存结果存储 |
| 向量库 | Milvus 2.4.x | HNSW 索引、条件向量检索、相似度调优 |
| AI 框架 | Spring AI + LangChain4j | RAG 架构、文档切片、Embedding |
| 自研引擎 | DAG 教育工作流执行引擎 | 拓扑排序、并行层识别、虚拟线程调度 |
| 自研算法 | RRF 倒数排名融合 | 多路召回、混合检索、重排序 |
| APM | SkyWalking 9.x（深度定制） | 自定义 FTT 埋点、跨 MQ Trace 传播、虚拟线程堆栈采样 |
| Web 前端 | Vue 3.5 + Ant Design Vue + AntV X6 | 全栈能力证明 |
| Android | 现有 IRAgent 项目（Java 11） | 协议设计、WebSocket 长连接 |

### 不做（有明确理由）

| 技术 | 不做的理由 |
|------|-----------|
| Spring Cloud / Nacos / Gateway | 1 人单体项目，面试会被追问"为什么微服务" |
| ShardingSphere 分库分表 | 数据量不足以证明需求，面试一问就崩 |

---

## 四大核心功能模块

### P0-1：虚拟线程驱动的 DAG 错题诊断引擎

- 错题根因拆解为 DAG 图的三节点：前置考点漏缺 → 核心公式混淆 → 计算步骤失误
- 拓扑排序识别可并发节点 → 虚拟线程并行执行 → 组装 Timeline 协议
- **面试点**：为什么虚拟线程比线程池更适合 LLM I/O 密集型场景

### P0-2：语义缓存 + RRF 混合检索真题召回

- 提问 → Embedding → Milvus 相似度检索（HNSW 索引）
- 相似度 > 0.96 → Redis 缓存命中返回（10s → 50ms，Token -90%）
- PostgreSQL 全文检索 + Milvus 向量检索双路召回 → RRF 融合重排
- **面试点**：阈值怎么定的？为什么不用单一向量检索？

### P0-3：多租户算力隔离

- JVM 内部 Semaphore 信号量配额：tenantId → 最大并发虚拟线程数
- Lua 脚本实现 Redis 分布式令牌桶限流
- **面试点**：为什么不在网关层限流？因为限的是最贵资源（大模型算力）不是 HTTP 请求

### P1：多端协同协议闭环

- Vue3 Web 教师端：学情大屏 + AntV X6 诊断流程画布编排
- WebSocket 实时推送诊断指令到 Android 端
- Android 端 WebView + KaTeX 执行 Timeline 黑板演算
- **面试点**：手写 12 种动作时间轴协议，全栈设计能力

### P2：SkyWalking 深度定制（APM 扩展能力证明）

**核心策略**：不做"装 Agent 就跑"的工具使用者，而是利用 SkyWalking 暴露的扩展点，解决通用 APM 无法覆盖的 3 个业务盲区。

**深度点 1 — 大模型 FTT 首字延迟自定义埋点**：
- 通用 APM 只能看到 HTTP 请求的总耗时（含整个 SSE 流）
- 通过 SkyWalking Manual Instrumentation API 在 SSE 拦截器中自定义 Span
- `onMessage` 收到第一个 chunk 时手动 `tag()` + `stopSpan()`
- **面试点**：为什么默认 APM 不行？SSE 流式响应的特殊性在哪？FTT 数据如何驱动语义缓存优化？

**深度点 2 — 跨 RocketMQ Trace Context 传播**：
- 刷题行为 → RocketMQ → 批量消费 → PostgreSQL，这是一条异步链路
- 通过 SkyWalking 跨进程 Context Carrier 在 RocketMQ 消息头中注入/提取 TraceID
- 生产端 `@Trace(operationName = "behaviorProduce")` + 消费端 `@Trace(operationName = "behaviorBatchConsume")`
- **面试点**：异步计费对账如何用 TraceID 做 100% 防丢？MQ 延迟/死信消息的 Trace 如何串联？

**深度点 3 — Noisy Neighbor 故障定位（虚拟线程堆栈采样）**：
- 多租户场景：某学校刷题高峰抢占虚拟线程，其他学校响应变慢
- 利用 SkyWalking Profile 功能对 JVM 虚拟线程堆栈进行动态采样
- 结合 Semaphore 配额数据，精准定位"吵闹邻居"
- **面试点**：如何区分"正常高负载"和"单租户恶意抢占"？虚拟线程的堆栈采样与平台线程有什么不同？

---

## 现有资产（直接复用）

- Android 12 种动作 Timeline 协议引擎（renderer.html）
- KaTeX + math.js + SVG 数学渲染
- OkHttp 180s 长超时 + SseParser 流式解析
- 火山 TTS + MediaPlayer 音画同步
- Spring Boot 后端：用户系统、会话管理、AI 对话/解题 API
- 苏格拉底式 v2 LearningSession 深度学习模块

## 技术栈冲突（需升级）

- Java 17 → Java 21
- 无消息队列 → RocketMQ 5.x
- 无向量库 → Milvus 2.4.x
- 无 Web 前端 → Vue 3.5 新建
