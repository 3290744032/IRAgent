# IRAgent Pro v3 — Android 架构文档

> **项目**：以个人知识库为中心的 AI 备考平台
> **技术栈**：Java 11, MVVM, Navigation Components, Retrofit 2, OkHttp 4, Material 1.13, Coil, MPAndroidChart
> **最后更新**：2026-05-28

---

## 一、项目概览

### 1.1 构建配置

| 属性 | 值 |
|------|-----|
| Application ID | `com.suiyuan.iragent_app` |
| Min SDK | 24 (Android 7.0) |
| Target SDK / Compile SDK | 36 |
| Java 版本 | 11 |
| MultiDex | 已启用 |
| API Host | `BuildConfig.API_HOST`（开发默认 `http://192.168.123.44:8080`） |

### 1.2 核心依赖

| 类别 | 依赖 | 用途 |
|------|------|------|
| AndroidX | `core-ktx`, `appcompat`, `navigation-*`, `lifecycle-*` | 基础框架 |
| Material | `com.google.android.material:material:1.13.0` | Material3 UI 组件 |
| 网络 | `retrofit2` + `converter-gson` + `okhttp3` + `logging-interceptor` | HTTP + SSE |
| UI | `MPAndroidChart:v3.1.0` | 掌握度雷达图 |
| 图片 | `coil-kt:coil` | 图片加载 |

### 1.3 声明组件

| 组件 | 说明 |
|------|------|
| `AuthActivity` | 启动入口（LAUNCHER），已登录跳转 MainActivity |
| `MainActivity` | 主壳（BottomNavigationView + NavHostFragment） |
| `OnboardingActivity` | 新用户引导（选考试 → 定目标 → 上传笔记） |
| `FileProvider` | 相机拍照文件共享 |

---

## 二、架构设计

### 2.1 MVVM + Repository 模式

```
┌──────────────────────────────────────────────┐
│  View (Fragment + XML)                        │
│  观察 LiveData，委托用户操作给 ViewModel        │
├──────────────────────────────────────────────┤
│  ViewModel (AndroidViewModel)                 │
│  持有 LiveData，调用 Repository，线程切换       │
├──────────────────────────────────────────────┤
│  Repository                                   │
│  封装数据源（Retrofit API / OkHttp SSE）       │
├──────────────────────────────────────────────┤
│  Remote (ApiService / NetworkClient)           │
│  Retrofit 接口 + OkHttp 配置 + 拦截器链         │
└──────────────────────────────────────────────┘
```

### 2.2 包结构

```
com.suiyuan.iragent_app/
├── IRAgentApplication.java            # Application（全局状态 + 未授权回调）
├── config/
│   └── SubjectConfig.java             # 学科配置（统一管理，动态生成 UI 气泡）
├── ui/screens/
│   ├── auth/                          # AuthActivity + AuthViewModel
│   ├── main/                          # MainActivity（5 Tab 壳）
│   ├── onboarding/                    # OnboardingActivity（3 步引导）
│   ├── knowledge/                     # 知识库 Tab
│   │   ├── KnowledgeListFragment      # 笔记列表 + 搜索 + 上传 + 删除
│   │   ├── KnowledgeListViewModel
│   │   ├── KnowledgeDetailFragment    # 笔记详情（KaTeX 本地渲染 + 编辑 + AI 优化）
│   │   ├── KnowledgeDetailViewModel
│   │   └── NoteCardAdapter           # RecyclerView Adapter
│   ├── study/                         # 答疑 Tab
│   │   ├── StudyFragmentV3            # 流式对话 + 图片答疑 + 笔记引用卡片
│   │   ├── StudyViewModelV3
│   │   ├── StudyFragment              # V2 旧版（保留兼容）
│   │   └── StudyViewModel
│   ├── practice/                      # 刷题 Tab
│   │   ├── PracticeHubFragment        # 入口 Hub + 批改流程
│   │   ├── PracticeHubViewModel
│   │   ├── DailyPracticeFragment      # 每日一练 / 同类题巩固
│   │   ├── DailyPracticeViewModel
│   │   ├── SmartPaperFragment         # AI 智能组卷
│   │   ├── SmartPaperViewModel
│   │   ├── ExamArchiveFragment        # 真题库 + 试卷上传
│   │   └── ExamArchiveViewModel
│   ├── errors/                        # 错题本 Tab
│   │   ├── ErrorsListFragment         # 错题列表 + 筛选 + 复习提醒
│   │   ├── ErrorsListViewModel
│   │   ├── ErrorsDetailFragment       # AI 三路诊断 + 同类题内联展示
│   │   └── ErrorsDetailViewModel
│   ├── profile/                       # 我的 Tab
│   │   ├── DashboardFragment          # 备考仪表盘
│   │   ├── DashboardViewModel
│   │   └── CoverageRingView           # 自定义覆盖率环形图
│   ├── deeplearn/                     # 深度学习（苏格拉底式）
│   ├── video/                         # 视频讲解（Timeline 黑板演算）
│   ├── conversation/                  # 历史对话列表
│   └── home/                          # V2 旧首页
├── data/
│   ├── remote/v3/
│   │   ├── ApiServiceV3.java          # V3 Retrofit 接口（20+ 端点）
│   │   └── NetworkClientV3.java       # V3 网络配置
│   ├── repository/v3/
│   │   ├── KnowledgeRepository.java   # 知识库（list/detail/upload/update/delete/optimize/search）
│   │   ├── PracticeV2Repository.java  # 每日一练 + 智能组卷
│   │   ├── ErrorsRepository.java      # 错题本
│   │   ├── DashboardRepository.java   # 仪表盘
│   │   ├── ChatRepositoryV3.java      # SSE 流式答疑
│   │   └── ConversationRepositoryV3.java
│   ├── model/v3/                      # 25 个 V3 模型类
│   └── local/                         # Room 数据库 + DAO
├── util/
│   ├── SseParser.java                 # SSE 流解析器（V2 + V3 双模式）
│   └── TtsHttpClient.java             # TTS HTTP 客户端
└── res/
    ├── assets/
    │   ├── libs/                       # KaTeX + marked 本地库（零网络依赖）
    │   │   ├── katex.min.js           # 273KB KaTeX 核心
    │   │   ├── katex.min.css          # 23KB KaTeX 样式
    │   │   ├── auto-render.min.js     # 3KB 自动渲染
    │   │   └── marked.min.js          # 50KB Markdown 解析
    │   ├── math_template.html          # Markdown + LaTeX 渲染模板（本地库）
    │   ├── engine/renderer.html        # Timeline 黑板演算引擎
    │   ├── geogebra/                   # GeoGebra 2D/3D 渲染
    │   ├── libs/                       # KaTeX + ECharts 本地库（零网络依赖）
    │   │   ├── katex.min.js (273KB) + katex.min.css (23KB) + auto-render.min.js (3KB)
    │   │   ├── marked.min.js (50KB)    # Markdown 解析
    │   │   └── echarts.min.js (1MB)    # ECharts 5.5 力导向图
    │   ├── math_template.html          # 笔记内容渲染（KaTeX 本地库）
    │   ├── knowledge_graph.html        # ECharts 知识图谱（骨架/聚焦/过滤/搜索）
    │   ├── engine/renderer.html        # Timeline 黑板演算引擎
    │   └── geogebra/                   # GeoGebra 2D/3D 渲染
    ├── layout/                         # 布局文件（15+ 个 v3 layout）
    ├── menu/bottom_nav_menu.xml        # 5 Tab 定义
    ├── navigation/nav_graph_v3.xml     # 导航图
    └── drawable/                       # 图标资源
```

---

## 三、5 Tab 导航架构

| 序号 | Tab ID | 标签 | 默认 Fragment | 子页面 |
|------|--------|------|--------------|--------|
| 1 | `nav_knowledge` | 知识库 | `KnowledgeListFragment` | → KnowledgeDetailFragment |
| 2 | `nav_chat` | 答疑 | `StudyFragmentV3` | → ConversationList / DeepLearn / Video |
| 3 | `nav_practice` | 刷题 | `PracticeHubFragment` | → DailyPractice / SmartPaper / ExamArchive |
| 4 | `nav_errors` | 错题本 | `ErrorsListFragment` | → ErrorsDetailFragment |
| 5 | `nav_profile` | 我的 | `DashboardFragment` | → Onboarding（重设目标） |

MainActivity 使用自定义 `setOnItemSelectedListener`，点击已选中 Tab 自动 `popBackStack` 回到根页面，解决跨 Tab 跳转后无法返回的问题。

---

## 四、核心 Fragment 详解

### 4.1 KnowledgeListFragment（知识库）

**Layout**：`fragment_knowledge_list.xml` | **ViewModel**：`KnowledgeListViewModel`

| UI 组件 | 说明 |
|---------|------|
| 统计行 | 笔记数、科目数、考点数、覆盖率（4 个 TextView） |
| 搜索栏 | EditText + 300ms 防抖 → `KnowledgeRepository.searchNotes()` |
| 学科筛选 | 横向滚动 Chips，由 `SubjectConfig.SUBJECTS_WITH_ALL` 动态生成 |
| 知识图谱 | ECharts 5.5 力导向图（WebView + `echarts.min.js` 本地库） |
| 笔记列表 | RecyclerView + NoteCardAdapter |
| 长按删除 | AlertDialog 确认 → `KnowledgeRepository.deleteNote()` |
| 上传卡片 | 点击 → 文件/相机选择器 → 预览 + 命名 → `uploadNote()`（支持 AI 分类结果预览） |
| 空状态 | 无笔记时显示引导文案 |

**知识图谱架构**（对标 Obsidian 交互水准）：
```
KnowledgeListFragment.setupKnowledgeGraph()
  → WebView 加载 knowledge_graph.html（ECharts 5.5 本地 1MB）
  → onPageFinished 后触发 viewModel.loadGraphData()
  → GET /v3/kb/graph-data → GraphDataService 聚合 SQL 返回 {nodes, edges}
  → Base64 编码 JSON → evaluateJavascript("renderFromBase64('...')")
  → ECharts 力导向渲染（动态 repulsion 公式 + 莫兰迪配色）
```

**图谱交互**：
| 操作 | 行为 |
|------|------|
| 默认视图 | 骨架模式：仅显示考点节点（`type=knowledge_point`），清爽无噪 |
| 点击考点 | 2-hop 聚焦视图：展开关联笔记+错题，无关节点淡化 opacity 0.15 |
| 双击空白 | 重置全局骨架视图 |
| 左上过滤器 | 毛玻璃面板：`显示错题` / `显示已掌握考点` 实时过滤 |
| 右上搜索 | 输入关键字 → 自动定位节点 + 1200ms 高亮闪烁 |
| 点击笔记/错题 | JSBridge `onNodeClick(type, actualId)` → Android 导航到详情页 |

### 4.2 KnowledgeDetailFragment（笔记详情）

**Layout**：`fragment_knowledge_detail.xml` | **ViewModel**：`KnowledgeDetailViewModel`

| UI 组件 | 说明 |
|---------|------|
| 题头 | 学科 · 章节 + 日期 + 标题 |
| 内容区 | WebView 加载 `math_template.html`，KaTeX 本地库渲染 Markdown+LaTeX |
| 标签区 | 动态 Tag Chips（`bg_tag_chip` 背景 + 白色文字） |
| 关联考点 | 知识点列表（本笔记 chunk + 相似知识点），`stripLatex()` 清洗 LaTeX |
| 关联题目 | 题目卡片列表，左侧紫色竖线 + 题目文本 |
| 编辑模式 | 切换显示 EditText（标题/学科/章节/标签/内容） |
| AI 优化 | BottomSheet 输入指令 → `optimizeNote()` → 重新渲染 |
| 操作栏 | 编辑 / AI 优化（正常模式） → 保存 / 取消（编辑模式） |

**KaTeX 渲染架构**（解决 CDN 网络问题）：
```
renderNoteDetail()
  → escapeJsString(content) + 注入 <script>var _rawContent='...'
  → loadDataWithBaseURL(fullHtml)
  → math_template.html 内联脚本即时渲染
  → 本地 file:///android_asset/libs/* 加载（零延迟、零网络依赖）
```

**stripLatex 增强**：按顺序清洗 — `$$...$$` 块 → `$...$` 行内 → `\begin...\end` 环境 → LaTeX 命令 → 花括号 → `\n`→空格

### 4.3 StudyFragmentV3（答疑）

**Layout**：`fragment_study_v3.xml` | **ViewModel**：`StudyViewModelV3`

| UI 组件 | 说明 |
|---------|------|
| 消息列表 | ScrollView + LinearLayout 动态添加消息 View |
| 图片答疑 | 相机/相册选图 → 预览（可关闭重选）→ 气泡内显示图片 → 发送到多模态 API |
| 笔记引用卡片 | SSE `note_refs` 事件 → 底部 NoteRefCard |
| 函数图像 | `【PLOT】` → GeoGebraView 2D / `【PLOT3D】` → WebView 3D |
| 输入栏 | EditText + 相机按钮 + 发送按钮 |

**SSE 流式流程**：发送 → `ChatRepositoryV3.chatStream()` → `SseParser` 解析 → `onChunk` 节流更新 / `onNoteRefs` 注入引用 / `onPlot/Plot3d` 渲染图像 / `onDone` 保存

### 4.4 PracticeHubFragment（刷题入口）

**Layout**：`fragment_practice_hub.xml` | **ViewModel**：`PracticeHubViewModel`

**四宫格入口**：
```
┌──────────────┬──────────────┐
│ 📸 拍照批改    │ 🎯 智能组卷    │
├──────────────┼──────────────┤
│ 📅 每日一练    │ 📚 真题题库    │
└──────────────┴──────────────┘
```

- **拍照批改**：选图 → `submitImageGrading()` → SSE 进度（OCR→提取→批改→诊断）→ 批改报告
- **智能组卷** → `SmartPaperFragment`（AI 生成试卷）
- **每日一练** → `DailyPracticeFragment`
- **真题题库** → `ExamArchiveFragment`

### 4.5 DailyPracticeFragment（每日一练 / 同类题巩固）

**Layout**：`fragment_daily_practice.xml` | **ViewModel**：`DailyPracticeViewModel`

| UI 组件 | 说明 |
|---------|------|
| 模式横幅 | 从错题本跳转时显示"🎯 同类题巩固练习" + 目标知识点 |
| 题目卡片 | MaterialCardView 白卡：题号 + 题型 + 来源标签（真题/AI生成/用户上传）+ 题目文本 |
| 答案输入 | EditText + 拍照按钮（相机/相册） |
| 反馈按钮 | AI 生成题目显示"题目有误？" TextButton |
| 提交按钮 | MaterialButton 填充样式 + TextButton"跳过，部分提交" |
| 结果面板 | 分数（绿/黄/红色）+ 正确/错误/正确率统计 + 逐题详情 + 解析 |

**同类题流程**：ErrorsDetailFragment → 点击"开始练习" → `nav_daily_practice`（传入 `knowledge_points`） → 知识点筛选题目

### 4.6 SmartPaperFragment（AI 智能组卷）

**Layout**：`fragment_smart_paper.xml` | **ViewModel**：`SmartPaperViewModel`

AI 根据科目、题型、难度自动生成试卷。支持 SSE 流式逐题生成（`SseParser` 解析 `question_start/question_end/complete/error` 事件），每道题生成后立即显示。

### 4.7 ExamArchiveFragment（真题题库）

**Layout**：`fragment_exam_archive.xml` | **ViewModel**：`ExamArchiveViewModel`

| 功能 | 说明 |
|------|------|
| 5 维筛选 | 学科/年份/考试类型/知识点/难度 Spinner + RecyclerView 题目列表 |
| 上传试卷 | FAB → 文件选择器 → Multipart 上传 → OCR 识别入库 |
| AI 模拟 | FAB → 学科/数量选择 → `POST /v3/exam-archive/simulate` |

### 4.8 ErrorsListFragment（错题本）

**Layout**：`fragment_errors_list.xml`

| UI 组件 | 说明 |
|---------|------|
| 筛选栏 | 横向 Chips（全部/按学科/待复习/按错误类型） |
| 复习提醒 | 黄色 Banner："今天有 N 道错题需要复习" |
| 错题卡片 | 题目摘要 + 错误答案（红线删除）→ 正确答案（绿色）+ 错误类型标签 |

### 4.9 ErrorsDetailFragment（错题详情）

**Layout**：`fragment_errors_detail.xml` | **ViewModel**：`ErrorsDetailViewModel`

| UI 组件 | 说明 |
|---------|------|
| 题目标头 | `error_light` 背景卡片：学科·知识点 + 题目（粗体）+ 你的答案（删除线红）+ 正确答案（粗体绿） |
| AI 三维诊断 | 3 张诊断卡片（蓝色=考点漏缺/紫色=公式混淆/琥珀色=计算失误），每张含分析文本 + "题目有误？"反馈按钮 |
| 同类题推荐 | **内联展示**（非 BottomSheet），自动加载；每张卡片含相似度标签、题目预览、知识点 Tag、难度、"开始练习"按钮 |
| 操作按钮 | MaterialButton 轮廓样式"标记为已掌握" + 填充样式"练同类题"（滚动到同类题区 + 刷新） |

**设计演进**：BottomSheet 弹窗 → 页面内联展示，减少交互层级，进入即加载。

### 4.10 DashboardFragment（我的）

**Layout**：`fragment_dashboard.xml` | **ViewModel**：`DashboardViewModel`

| UI 组件 | 说明 |
|---------|------|
| 渐变 Header | "下午好 ☀️" + 用户名（`PreferencesManager.getAccount()`） |
| 覆盖率环形图 | 自定义 `CoverageRingView`（Canvas 绘制 conic-gradient） |
| 统计行 | 已掌握考点、笔记数、累计刷题、本周学习时长 |
| 今日任务 | 3 个 task item，点击跳转对应 Tab |
| 学习周报 | 4 格数据 |
| 掌握度雷达图 | MPAndroidChart `RadarChart`（5 维） |

---

## 五、V3 API 层

### 5.1 ApiServiceV3（Retrofit 接口，20+ 端点）

Base URL：`BuildConfig.API_HOST + "/api/v3/"`

**知识库**：

| 注解 | 路径 | 说明 |
|------|------|------|
| `@GET` | `kb/notes` | 笔记列表（subject/page/size） |
| `@GET` | `kb/notes/{id}` | 笔记详情（含 chunk、关联考点/题目） |
| `@Multipart @POST` | `kb/upload` | 上传文件（PDF/DOCX/图片 OCR） |
| `@PUT` | `kb/notes/{id}` | 编辑笔记元数据 |
| `@DELETE` | `kb/notes/{id}` | 删除笔记 |
| `@POST` | `kb/notes/{id}/optimize` | AI 优化内容 |
| `@POST` | `kb/search` | 语义搜索 |
| `@GET` | `kb/graph-data` | 知识图谱数据（考点/笔记/错题三元拓扑） |

**答疑 V3**：

| 注解 | 路径 | 说明 |
|------|------|------|
| `@Streaming @POST` | `chat/stream` | SSE 流式答疑 |
| `@Streaming @POST` | `chat/stream-image` | SSE 多模态答疑 |

**刷题**：

| 注解 | 路径 | 说明 |
|------|------|------|
| `@GET` | `daily-practice` | 每日一练（subject/count/knowledgePoints） |
| `@POST` | `daily-practice/{id}/submit` | 提交答案 |
| `@POST` | `daily-practice/{id}/feedback` | 题目报错 |
| `@Streaming @POST` | `smart-paper/generate` | SSE 智能组卷 |
| `@GET` | `exam-archive` | 真题列表 |
| `@GET` | `exam-archive/filters` | 筛选选项 |
| `@Multipart @POST` | `exam-archive/upload` | 上传试卷 |
| `@POST` | `exam-archive/simulate` | AI 模拟题 |
| `@Streaming @POST` | `grading/submit-image` | SSE 拍照批改 |

**错题本**：

| 注解 | 路径 | 说明 |
|------|------|------|
| `@GET` | `errors/list` | 错题列表 |
| `@GET` | `errors/{id}` | 错题详情（含诊断 JSON） |
| `@PUT` | `errors/{id}/mark-mastered` | 标记掌握/取消 |
| `@POST` | `errors/{id}/similar` | 同类题推荐 |
| `@POST` | `errors/{id}/feedback` | 诊断反馈 |

**仪表盘**：

| 注解 | 路径 | 说明 |
|------|------|------|
| `@GET` | `dashboard/overview` | 备考概览 |
| `@GET` | `dashboard/mastery-radar` | 雷达图数据 |
| `@GET` | `dashboard/today-tasks` | 今日任务 |
| `@GET` | `dashboard/weekly-report` | 学习周报 |

### 5.2 NetworkClientV3

| 配置项 | 值 |
|--------|-----|
| 标准超时 | 180s（connect/read/write） |
| 流式超时 | 300s（read），180s（connect/write） |

**拦截器链**：Auth 拦截器 → Logging 拦截器 → 401 拦截器（触发 `onUnauthorized()` → 跳转登录）

### 5.3 Repository 层

| Repository | 方式 | 主要方法 |
|-----------|------|---------|
| `KnowledgeRepository` | Retrofit | listNotes, getNoteDetail, uploadNote, updateNote, deleteNote, optimizeNote, searchNotes |
| `PracticeV2Repository` | Retrofit + OkHttp SSE | getDailyPractice, submitDailyFeedback, generateSmartPaper, getExamArchive, uploadPaper, simulateExam |
| `ErrorsRepository` | Retrofit | listErrors, getErrorDetail, markMastered, getSimilarQuestions, submitFeedback |
| `DashboardRepository` | Retrofit | getOverview, getRadar, getTasks, getReport |
| `ChatRepositoryV3` | OkHttp SSE | chatStream, chatStreamWithImage |
| `ConversationRepositoryV3` | Retrofit (V1) | list, create, messages, title |

---

## 六、V3 数据模型（25 个类）

| 模型 | 用途 | 新增字段 |
|------|------|---------|
| `NoteItem` | 笔记列表条目 | — |
| `NoteDetail` | 笔记详情（content/chunks/linkedKp/linkedQuestions） | — |
| `NoteChunk` | 笔记切分结果 | — |
| `NoteFragment` | 语义搜索匹配 | — |
| `LinkedKnowledgePoint` | 关联知识点 | — |
| `LinkedQuestion` | 关联题目 | — |
| `UploadResult` | 上传响应（含 AI 分类） | `Classification` 内部类 |
| `SimilarQuestion` | 同类题推荐 | `similarity` / `difficulty` / `questionType` |
| `ErrorDetail` | 错题详情（含诊断 JSON） | — |
| `DiagnosisJson` / `DiagnosisItem` | 三维诊断 | — |
| `PracticeQuestion` | 每日一练题目 | — |
| `DailyPracticeSession` | 每日一练会话 | — |
| `SubmitAnswerRequest` / `SubmitAnswerResult` | 提交答案 | `photoBase64` |
| `SmartPaperRequest` / `SmartPaperSession` | 智能组卷 | — |
| `ExamQuestion` / `ExamFilterData` | 真题数据 | — |
| `GradedQuestion` / `GradingReport` | 批改结果 | — |
| `DashboardOverview` / `MasteryRadarData` / `TaskItem` / `WeeklyReport` | 仪表盘 | — |
| `ChatRequestV3` | V3 聊天请求 | `imageBase64` |

---

## 七、SSE 流式解析器（SseParser）

双模式设计，同时支持 V2 和 V3 SSE 事件：

- **V2 回调**：`onSegment` / `onComplete` / `onError`
- **V3 回调**：`onChunk` / `onNoteRefs` / `onPlot` / `onPlot3d` / `onDone` / `onError`
- **SmartPaper 回调**：`onQuestionStart` / `onQuestionContent` / `onQuestionEnd` / `onComplete` / `onError`

解析逻辑：按 `\n\n` 分割 SSE 帧 → 提取 `event:` 和 `data:` 字段 → 按事件类型分发。

---

## 八、布局文件清单

### V3 核心布局

| 文件 | 对应 Fragment |
|------|-------------|
| `fragment_knowledge_list.xml` | KnowledgeListFragment |
| `fragment_knowledge_detail.xml` | KnowledgeDetailFragment |
| `fragment_study_v3.xml` | StudyFragmentV3 |
| `fragment_practice_hub.xml` | PracticeHubFragment |
| `fragment_daily_practice.xml` | DailyPracticeFragment |
| `fragment_smart_paper.xml` | SmartPaperFragment |
| `fragment_exam_archive.xml` | ExamArchiveFragment |
| `fragment_errors_list.xml` | ErrorsListFragment |
| `fragment_errors_detail.xml` | ErrorsDetailFragment |
| `fragment_dashboard.xml` | DashboardFragment |
| `activity_main.xml` | MainActivity |

### 组件布局

| 文件 | 说明 |
|------|------|
| `item_note_card.xml` | 笔记卡片 |
| `item_note_ref_card.xml` | 笔记引用卡片 |
| `item_similar_question.xml` | 同类题卡片（MaterialCardView + 相似度/难度/标签/开始练习） |
| `dialog_ai_optimize.xml` | AI 优化指令输入对话框 |
| `bottom_nav_menu.xml` | 5 Tab 菜单 |

---

## 九、assets/ 资源

| 路径 | 用途 |
|------|------|
| `libs/katex.min.js` + `katex.min.css` | KaTeX 本地渲染引擎（273KB） |
| `libs/auto-render.min.js` | KaTeX 自动渲染扩展（3KB） |
| `libs/marked.min.js` | Markdown→HTML 解析（50KB） |
| `math_template.html` | 笔记内容渲染模板（本地库引用，零网络依赖） |
| `engine/renderer.html` | Timeline 12 种动作黑板演算引擎 |
| `geogebra/index.html` | GeoGebra 2D 函数图像渲染 |
| `geogebra/3d_renderer.html` | GeoGebra 3D 函数图像渲染 |
| `knowledge_graph.html` | 知识图谱 SVG 可视化 |

**KaTeX 本地化**：所有 JS/CSS 从 CDN 迁移到 `assets/libs/`，WebView 通过 `file:///android_asset/libs/` 加载，零网络依赖，即时渲染无竞态。

---

## 十、认证流程

1. `AuthActivity` → `POST /api/auth/login` → 获取 token
2. token 存入 `PreferencesManager`（SharedPreferences）
3. API 请求通过 `NetworkClient` 拦截器注入 `token` 请求头
4. 401 响应 → `IRAgentApplication.onUnauthorized()` → 清除 token → 跳转 `AuthActivity`

---

## 十一、构建与运行

### 11.1 配置 API 地址

```kotlin
// app/build.gradle.kts
defaultConfig {
    buildConfigField("String", "API_HOST", "\"http://192.168.123.44:8080\"")
}
```

### 11.2 编译运行

```bash
cd Android/IRAgentAPP
./gradlew assembleDebug    # 编译
./gradlew installDebug     # 安装
```

### 11.3 首次启动

```
AuthActivity（登录/注册）
  ↓ 登录成功 → 检查是否首次
  ├─ 新用户 → OnboardingActivity（选考试 → 定目标 → 上传笔记）
  └─ 老用户 → MainActivity（默认 Tab：知识库）
```
