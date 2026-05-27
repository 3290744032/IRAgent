# IRAgent Pro v3 开发任务清单

> **产品定位**：以个人知识库为中心的 AI 备考平台（考研/专升本/高考）
> **PRD 文档**：`ai/product-design/PRD-v3-exam-preparation-platform.md`
> **UI 原型**：`ui-prototype-v3/`（12 个 screen，5 Tab 架构）
> **项目周期**：12-15 周，单人开发，4 个 Phase

---

## 当前状态总览

| 层 | 组件 | 状态 |
|---|------|------|
| 后端核心 | DAG 引擎、语义缓存、RRF 融合、虚拟线程、多租户 | ✅ 已完成 |
| 后端业务 v1/v2 | AI 对话、学习 v2、Timeline 生成、会话管理 | ✅ 已完成 |
| 后端业务 v3 | 知识库 API、升级版答疑、试卷批改、错题本、仪表盘 | ✅ 已完成（14 Controller, 42 端点） |
| 数据层 | PostgreSQL、Redis、Milvus | ✅ Docker Compose 齐全，schema.sql 14 张表 |
| 消息队列 | RocketMQ 5.x 行为上报 | ✅ 已完成（Producer + BatchConsumer + TraceContext） |
| 监控 | SkyWalking 9.x 深度定制 | ✅ 已完成（FTT 埋点 + 跨 MQ Trace + Noisy Neighbor） |
| 前端原型 | ui-prototype-v3（5 Tab、12 screen） | ✅ 已完成 |
| Android | v3 5 Tab（Navigation Components） | ✅ 已完成（6 Fragment + 6 ViewModel + 6 Repository） |
| Vue3 Web | 教师端 | ❌ 已取消（产品决策：管理端已覆盖后端管理需求，教师端非必需） |
| Web 管理端 | 系统概览/用户管理/题目审核/API Key | ✅ 已完成（900 行原型 + 11 后端 API） |
| 文档 | 8 份完整文档 | ✅ PRD · 系统流程 · 后端架构 · Android 架构 · 面试自述 · 刷题模块 · 管理端 · API 需求 |

---

# Phase 0：后端 v3 核心 API（2-3 周）

> **目标**：让原型里的核心叙事链路（知识库 → 答疑 → 批改 → 错题溯源）对接真实数据跑通
> **面试产出**：个人知识库 RAG + 试卷批改 Pipeline + 笔记锚定答疑

---

### [ ] 任务 0.1：知识库上传与检索 API

**描述**：实现笔记上传、解析、向量化入库、检索的完整链路。每个用户独立的 Milvus Collection，按知识点切分笔记内容。

**验收标准**：
- `POST /api/v3/kb/upload` — 接收文件（PDF/图片/Markdown），OCR 解析，按知识点切分 chunk，Embedding 后存入用户专属 Milvus Collection
- `GET /api/v3/kb/notes` — 返回用户笔记列表，按科目/章节分组，含考点标签和关联题目数
- `GET /api/v3/kb/notes/{id}` — 返回笔记详情，含原文、关联考点（带掌握度）、关联题目
- `POST /api/v3/kb/search` — 语义搜索个人笔记，返回 Top-K 相关笔记片段
- 笔记上传后 30 秒内完成解析入库（单份 < 10 页）
- 笔记检索 Recall@5 > 0.85

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/controller/KnowledgeBaseController.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/KnowledgeBaseService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/NoteChunkingService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/rag/pipeline/NoteIngestionPipeline.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/rag/retrieval/PersonalNoteRetriever.java`
- `Spring-Boot/IRAgent/src/main/resources/db/migration/V4__knowledge_base.sql`

**对应 PRD**：模块一 — 个人知识库

---

### [ ] 任务 0.2：答疑升级 — 笔记锚定 + IntentRouter

**描述**：升级现有 AI 对话，注入用户个人笔记作为上下文，AI 回答末尾自动挂载笔记引用卡片。新增 IntentRouter 意图路由，根据用户问题类型自动选择答疑模式。

**验收标准**：
- 升级 `POST /api/v3/chat/stream` — SSE 流式返回，System Prompt 注入检索到的个人笔记片段
- AI 回答结尾自动生成 `noteRefs` 字段（JSON 数组，含笔记 ID、标题、引用片段），前端据此渲染 note-ref-card
- IntentRouter 规则引擎：根据用户问题关键词和上下文判断意图
  - `HINT_NEEDED` → 苏格拉底引导（走 DeepLearn 流程）
  - `FULL_EXPLANATION` → 即时答疑 + 笔记引用
  - `NOTE_SEARCH` → 直接检索笔记原文
  - `PRACTICE_READY` → 跳过讲解，直接推同类题
- RRF 三路融合：个人笔记检索 + 真题库检索 + PostgreSQL 全文检索 → 融合重排
- 向后兼容：不改动现有 v1/v2 API

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/controller/ChatControllerV3.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/IntentRouterService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/NoteAnchoredChatService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/rag/retrieval/RrfRanker.java`（扩展为 3 路）

**对应 PRD**：模块二 — 智能答疑（升级版）

---

### [ ] 任务 0.3：试卷批改 API（OCR + 批改 + SSE 进度）

**描述**：实现拍照上传试卷 → OCR 识别 → 逐题批改 → 错题触发 DAG 诊断 → 推荐同类题的完整 Pipeline，通过 SSE 推送分步进度。

**验收标准**：
- `POST /api/v3/grading/submit` — 接收试卷图片，SSE 流式推送 4 步进度：
  - `step: "ocr"` — OCR 识别手写文字
  - `step: "extract"` — 提取题目与答案，结构化
  - `step: "grade"` — 逐题 AI 批改比对，计算得分
  - `step: "diagnose"` — 错题触发 DAG 三路诊断（复用现有引擎）
  - `step: "complete"` — 返回完整批改报告 JSON
- 批改报告包含：总分、正确/错误题数、正确率、逐题详情（题目、用户答案、标准答案、对错、得分、考点标签）
- 错题自动触发 DAG 诊断（复用 `DiagnosisService`），诊断结果包含笔记溯源
- 错题自动入库（`grading_question_result` 表 + 错题本）
- 错题关联考点掌握度自动更新（`user_kp_mastery` 表 -0.05 ~ -0.15）
- 推荐 3 道同类变式题（走 RAG 检索）
- 客观题（选择/填空）用规则匹配优先，零 Token 消耗
- 解答题用 LLM 批改，低置信度标记 `reviewSuggested: true`

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/controller/GradingController.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/GradingService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/GradingOcrService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/GradingPipelineService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/dto/request/GradingRequest.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/dto/response/GradingReportResponse.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/dto/response/GradingSseEvent.java`
- `Spring-Boot/IRAgent/src/main/resources/db/migration/V5__grading_tables.sql`

**对应 PRD**：模块三 — 试卷批改

---

### [ ] 任务 0.4：备考仪表盘 API

**描述**：提供学生备考进度、考点掌握度、学习周报的后端数据接口。

**验收标准**：
- `GET /api/v3/dashboard/overview` — 返回考点覆盖率、已掌握/薄弱/未学考点数、笔记数、累计刷题数、本周学习时长
- `GET /api/v3/dashboard/weekly-report` — 返回学习周报（本周学习时长、做题数、正确率、新掌握考点数）
- `GET /api/v3/dashboard/mastery-radar` — 返回 5 维掌握度雷达图数据（各考点掌握度百分比）
- `GET /api/v3/dashboard/today-tasks` — 返回今日任务列表（复习队列、每日一练、专项突破建议）

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/controller/DashboardController.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/DashboardService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/MasteryService.java`

**对应 PRD**：模块四 — 备考仪表盘

---

### [ ] 任务 0.5：错题本 API

**描述**：提供错题列表、错题详情（含诊断结果）、间隔复习队列的后端接口。

**验收标准**：
- `GET /api/v3/errors/list` — 返回错题列表，支持按科目/错误类型/时间筛选，支持分页
- `GET /api/v3/errors/{id}` — 返回错题详情，含三路诊断结果 + 笔记溯源 + 同类题推荐
- `GET /api/v3/errors/review-queue` — 返回今日待复习错题队列（基于艾宾浩斯遗忘曲线）
- `PUT /api/v3/errors/{id}/mark-mastered` — 标记错题已掌握，更新掌握度和复习周期
- `POST /api/v3/errors/{id}/similar` — 返回该错题的同类变式题推荐

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/controller/ErrorBookController.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/ErrorBookService.java`
- `Spring-Boot/IRAgent/src/main/java/com/suiyuan/iragent/service/SpacedRepetitionService.java`

**对应 PRD**：模块三 — 错题本（Tab 4）

---

## Phase 0 质量检查
- [ ] 知识库上传 → 检索全链路通过（上传 5 份笔记 → 语义搜索能命中）
- [ ] 答疑回答末尾包含正确的 noteRefs（笔记引用准确率 > 80%）
- [ ] 试卷批改 4 步 SSE 进度推送正常，批改报告数据完整
- [ ] 错题诊断结果正确触发 DAG 引擎，笔记溯源正确
- [ ] 所有 v3 API 通过 Knife4j/Swagger 文档可查看
- [ ] v1/v2 API 不受影响（回归测试通过）
- [ ] 原型中的核心叙事链路能用 curl/Postman 完整跑通

---

# Phase 1：Android 5 Tab 重构（2-3 周）

> **目标**：Android 端从 v2 的旧 Tab 结构重构为 v3 的 5 Tab 架构，对接 Phase 0 的后端 API
> **面试产出**：MVVM 多 Tab 架构 + SSE 流式 + WebView 数学渲染 + 笔记引用卡片

---

### [ ] 任务 1.1：Tab 导航框架重构

**描述**：重构 Android 端底部导航，从现有的 Activity 结构迁移到 5 Tab（知识库/答疑/刷题/错题本/我的）。

**验收标准**：
- 使用 BottomNavigationView 或自定义 TabBar，5 个 Tab 对应 Fragment
- Tab 图标和标签与原型一致
- Tab 切换保持 Fragment 状态（不销毁重建）
- 导航栈正确：Tab 内子页面 push/pop 不影响 Tab 切换

**需要创建/修改的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/main/MainActivity.java`（重构）
- `Android/IRAgentAPP/app/src/main/res/layout/activity_main.xml`（重构）
- `Android/IRAgentAPP/app/src/main/res/menu/bottom_nav_menu.xml`（新增）
- `Android/IRAgentAPP/app/src/main/res/drawable/ic_tab_*.xml`（新增 5 个图标）

---

### [ ] 任务 1.2：知识库 Tab（KnowledgeFragment）

**描述**：实现知识库 Tab，含笔记列表、知识图谱 SVG 展示、笔记详情、上传入口。

**验收标准**：
- 笔记列表（RecyclerView）按科目分组，支持搜索和科目筛选
- 知识图谱区域（WebView 加载 SVG 或 Canvas 绘制）展示考点→笔记→题目三列关系
- 笔记详情页显示原文、考点标签（带掌握度颜色）、关联题目列表
- 上传入口（Bottom Sheet）：拍照/相册/文件/粘贴 四个选项
- 对接 API：`GET /api/v3/kb/notes`、`GET /api/v3/kb/notes/{id}`、`POST /api/v3/kb/upload`

**需要创建的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/knowledge/KnowledgeFragment.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/knowledge/KnowledgeViewModel.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/knowledge/NoteDetailActivity.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/data/repository/KnowledgeRepository.java`
- `Android/IRAgentAPP/app/src/main/res/layout/fragment_knowledge.xml`

---

### [ ] 任务 1.3：答疑 Tab 升级（StudyFragment v3）

**描述**：升级现有 StudyFragment，AI 回答底部增加笔记引用卡片（NoteRefCard），增加意图切换（答疑/深度学习/视频讲解）。

**验收标准**：
- SSE 流式对话（复用现有 OkHttp SseParser）
- AI 回答底部的 NoteRefCard 组件：显示笔记标题、引用片段，点击跳转笔记详情
- 顶栏三个模式切换按钮：💬 答疑 / 🧠 深度学习 / 🎬 视频讲解
- 深度学习模式复用现有 DeepLearnFragment（苏格拉底分步教学）
- 视频讲解模式复用现有 Timeline + WebView 黑板演算
- 对接 API：`POST /api/v3/chat/stream`

**需要创建/修改的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/study/StudyFragment.java`（升级）
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/study/StudyViewModel.java`（升级）
- `Android/IRAgentAPP/app/src/main/res/layout/item_note_ref_card.xml`（新增）

---

### [ ] 任务 1.4：刷题 Tab（PracticeFragment）

**描述**：实现刷题 Tab，含入口页面（上传试卷/智能组卷/每日一练/真题库）和试卷批改流程。

**验收标准**：
- 入口页面：4 个卡片（上传试卷批改、智能组卷、每日一练、真题库）
- 上传试卷 → 拍照/选图 → 上传 → SSE 进度条（4 步动画）→ 批改报告页
- 批改报告页：总分大数字、正确/错误/正确率三统计、逐题列表（点击查看详情）
- 单题详情：题目、用户答案（红/删除线）、正确答案（绿）、AI 解析
- 从报告页一键跳转错题本查看诊断
- 对接 API：`POST /api/v3/grading/submit`（SSE）

**需要创建的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/practice/PracticeFragment.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/practice/PracticeViewModel.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/practice/GradingReportActivity.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/data/repository/GradingRepository.java`

---

### [ ] 任务 1.5：错题本 Tab（ErrorBookFragment）

**描述**：实现错题本 Tab，含筛选栏、错题列表、复习提醒、错题详情（三路诊断 + 笔记溯源 + 同类题）。

**验收标准**：
- 筛选栏（横向滚动 chips）：全部/按科目/待复习/考点漏缺/公式混淆/计算失误
- 错题列表卡片：来源、题目摘要、错误答案→正确答案、错误类型标签、复习时间提醒
- 间隔复习提醒 Banner（今天有 N 道待复习错题）
- 错题详情页：三路诊断（左侧彩色竖线区分）、每路诊断下方挂笔记引用卡片
- 同类题推荐列表（点击加入练习队列）
- "标记掌握"按钮（调 `PUT /api/v3/errors/{id}/mark-mastered`）
- 对接 API：错误本和复习队列相关接口

**需要创建的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/errors/ErrorBookFragment.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/errors/ErrorBookViewModel.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/errors/ErrorDetailActivity.java`
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/data/repository/ErrorBookRepository.java`

---

### [ ] 任务 1.6：我的 Tab 升级（ProfileFragment v3）

**描述**：升级现有 ProfileFragment，增加备考仪表盘（考点覆盖率环形图、今日任务、学习周报、掌握度雷达图）。

**验收标准**：
- 顶部渐变 Header：问候语、用户名、考试目标
- 考点覆盖率环形图（Canvas 绘制 conic-gradient 效果）
- 今日任务列表（点击跳转对应 Tab）
- 学习周报卡片（4 格数据：学习时长、做题数、正确率、新掌握）
- 掌握度雷达图（WebView Canvas 或 MPAndroidChart）
- 设置项：考试目标、知识库管理、数据导出、重设考试目标、退出登录
- 对接 API：`GET /api/v3/dashboard/*`

**需要创建/修改的文件**：
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/profile/ProfileFragment.java`（升级）
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/ui/screens/profile/ProfileViewModel.java`（升级）
- `Android/IRAgentAPP/app/src/main/java/com/suiyuan/iragent_app/data/repository/DashboardRepository.java`（新增）

---

## Phase 1 质量检查
- [ ] 5 个 Tab 全部可切换，Tab 内子页面导航正常
- [ ] 知识库：上传笔记 → 列表刷新 → 点击查看详情 → 知识图谱展示
- [ ] 答疑：发送问题 → AI 流式回答 → 底部显示笔记引用卡片 → 点击跳转笔记详情
- [ ] 刷题：拍照 → 4 步进度动画 → 批改报告 → 单题详情
- [ ] 错题本：列表筛选 → 点击查看详情 → 三路诊断 + 笔记卡片 → 同类题推荐
- [ ] 我的：仪表盘数据正确展示，今日任务点击跳转正确
- [ ] 所有 API 错误状态有友好提示（网络错误、加载中、空数据）
- [ ] 不破坏现有登录、对话、Timeline 功能

---

# Phase 2：Web 教师端 + WebSocket 多端协同（3-4 周）

> **目标**：Vue 3 教师端（学情大屏 + 诊断画布）+ WebSocket 实时推送
> **面试产出**：全栈能力证明 + 多端协同演示视频

---

### [ ] 任务 2.1：Vue 3.5 项目初始化

**描述**：从零搭建 Vue 3.5 + TypeScript + Vite Web 教师端项目，集成 Ant Design Vue 4.x 和 AntV X6。

**验收标准**：
- `npm create vite@latest` 创建项目
- 安装依赖：`ant-design-vue 4.x`、`@antv/x6`、`vue-router 4.x`、`pinia`、`echarts 5.6`
- 项目结构：`views/` `components/` `stores/` `api/` `router/`
- ESLint + Prettier 配置完成

**需要创建的文件**：
- `IRAgent-Web/` 整个项目目录

---

### [ ] 任务 2.2：教师登录与学情大屏

**描述**：教师登录页（对接现有 `/auth/login`）+ ECharts 学情大屏（班级考点覆盖、薄弱学生预警）。

**验收标准**：
- 登录页使用 Ant Design Vue Form，Token 存入 localStorage，Pinia 管理状态
- 学情大屏含：班级考点覆盖率柱状图、学生掌握度分布、薄弱考点 Top 10、近期正确率趋势
- 数据从后端 API 拉取（需后端新增教师端 API）
- 适配 1920x1080 大屏

**需要创建的文件**：
- `IRAgent-Web/src/views/LoginView.vue`
- `IRAgent-Web/src/views/DashboardView.vue`
- `IRAgent-Web/src/components/*.vue`
- 后端配套：`TeacherController.java`、`TeacherService.java`

---

### [ ] 任务 2.3：AntV X6 诊断流程画布

**描述**：用 AntV X6 实现拖拽画布，教师可可视化编排错题诊断 DAG 流程。

**验收标准**：
- 左侧节点面板：LLM 诊断节点、聚合节点、条件分支节点（可拖拽）
- 画布支持：拖入节点、连线、删除、缩放、平移、撤销/重做
- 导出按钮：导出为 JSON（符合后端 DAG 配置格式）
- 导入按钮：从 JSON 加载已有 DAG 图

**需要创建的文件**：
- `IRAgent-Web/src/views/DiagnosisEditor.vue`
- `IRAgent-Web/src/components/x6/DagCanvas.vue`
- `IRAgent-Web/src/components/x6/NodePanel.vue`
- `IRAgent-Web/src/utils/dagSerializer.ts`

---

### [ ] 任务 2.4：WebSocket 后端 + Android 客户端

**描述**：Spring Boot 集成 WebSocket，Android 端实现 WebSocket 长连接，支持教师端推送诊断指令到学生端实时播放。

**验收标准**：
- Spring WebSocket 配置完成，支持按 userId 定向推送
- 3 种消息类型：`TIMELINE_PUSH`、`DIAGNOSIS_STATUS`、`HEARTBEAT`
- Android OkHttp WebSocket 客户端，支持自动重连（指数退避）
- 收到 `TIMELINE_PUSH` 后自动调起 Timeline 黑板演算播放
- Web 端点击"推送" → Android 端 3 秒内开始播放

**需要创建的文件**：
- `Spring-Boot/IRAgent/.../config/WebSocketConfig.java`
- `Spring-Boot/IRAgent/.../websocket/TimelineWebSocketHandler.java`
- `Spring-Boot/IRAgent/.../websocket/WebSocketSessionManager.java`
- `Android/.../data/remote/WebSocketClient.java`

---

### [ ] 任务 2.5：端到端联调 + 演示视频

**描述**：打通 Web → 后端 → Android 三段链路，录制面试演示视频。

**验收标准**：
- Web 端编排 DAG → 提交执行 → Android 端收到 Timeline → 黑板演算自动播放
- 端到端延迟 < 15 秒（含 LLM 推理）
- 录制 3 分钟演示视频，展示完整流程

**需要创建的文件**：
- `docs/demo-storyboard.md`

---

## Phase 2 质量检查
- [ ] Web 端画布导出 JSON 能被后端 DAG 引擎正确解析
- [ ] WebSocket 推送 Timeline 能被 Android 正确播放
- [ ] 教师端学情大屏数据与后端一致
- [ ] 演示视频录制完成

---

# Phase 3：SkyWalking 深度定制 + 面试准备（1-2 周）

> **目标**：验证已有 SkyWalking 定制代码 + 压测报告 + 面试自述
> **面试产出**：FTT 埋点 + 跨 MQ Trace + Noisy Neighbor 定位 + benchmark 数据

---

### [x] 任务 3.1：FTT 首字延迟自定义埋点 ✅

**已完成**：`VolcEngineStreamingClient.java` 已在 SSE `onMessage` 首帧回调中手动创建子 Span 并 `tag("ftt","true")` + `stopSpan()`。SkyWalking UI 可查询 FTT 指标。

---

### [x] 任务 3.2：跨 RocketMQ Trace Context 传播 ✅

**已完成**：`BehaviorMessageProducer.java` 发送消息时注入 TraceContext 到消息头；`BehaviorBatchConsumer.java` 消费端提取 TraceContext 创建子 Span。SkyWalking 拓扑图可展示完整异步链路。

---

### [x] 任务 3.3：Noisy Neighbor 故障定位 ✅

**已完成**：`NoisyNeighborDetector.java` 监控每个租户的 Semaphore 等待时间，结合 SkyWalking Profile 堆栈采样定位吵闹邻居。支持超阈值自动告警日志。

---

### [ ] 任务 3.4：压测报告 + 面试自述

**描述**：完善虚拟线程基准测试，输出完整压测报告；为每个 Phase 写面试自述。

**验收标准**：
- `benchmark-result.md` 含虚拟线程 vs 平台线程在 100/500/1000 并发下的对比数据
- Phase 0-3 各一份 200 字面试自述
- 演示视频最终版（含字幕）

**需要创建/修改的文件**：
- `Spring-Boot/IRAgent/benchmark-result.md`
- `docs/interview-self-report-phase0.md` ~ `phase3.md`

---

## 全局质量要求
- [ ] 所有新增 API 使用 ApiResponse 统一封装
- [ ] RESTful 规范（GET/POST/PUT/DELETE 语义正确）
- [ ] Knife4j/Swagger 文档注解完整
- [ ] DAG 引擎模块 README（架构图 + 使用示例）
- [ ] RAG 模块 README（架构图 + 缓存命中率数据）
- [ ] Android 端不改动现有稳定功能
- [ ] 所有 Docker 中间件通过一个 `docker-compose.yml` 统一编排
- [ ] 每个 Phase 结束后的面试自述能直接用在自我介绍里

---

## 技术栈

| 类别 | 技术 |
|------|------|
| JDK | Java 21（虚拟线程） |
| 框架 | Spring Boot 3.4.6 |
| 消息队列 | Apache RocketMQ 5.x |
| 缓存 | Redis 7.2 |
| 向量库 | Milvus 2.4（per-tenant Collection） |
| 数据库 | PostgreSQL 16 + pgvector |
| AI | 火山方舟 Doubao / DeepSeek（Spring AI + LangChain4j） |
| APM | SkyWalking 9.x（深度定制） |
| Web 前端 | Vue 3.5 + TypeScript + Vite + Ant Design Vue 4.x + AntV X6 + ECharts 5.6 |
| Android | Java 11, MVVM, Retrofit 2.11, OkHttp 4.12, Room, WebView + KaTeX |
