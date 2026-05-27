# IRAgent Pro v3 — Android 架构文档

> **项目**：以个人知识库为中心的 AI 备考平台
> **技术栈**：Java 11, MVVM, Navigation Components, Retrofit 2, OkHttp 4, Room, MPAndroidChart, Markwon
> **最后更新**：2026-05-25

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

```kotlin
// AndroidX
implementation("androidx.core:core-ktx")
implementation("androidx.appcompat:appcompat")
implementation("com.google.android.material:material")
implementation("androidx.navigation:navigation-fragment")
implementation("androidx.navigation:navigation-ui")
implementation("androidx.lifecycle:lifecycle-viewmodel")
implementation("androidx.lifecycle:lifecycle-livedata")

// 网络
implementation("com.squareup.retrofit2:retrofit")
implementation("com.squareup.retrofit2:converter-gson")
implementation("com.squareup.okhttp3:okhttp")
implementation("com.squareup.okhttp3:logging-interceptor")

// 数据库
implementation("androidx.room:room-runtime")

// UI
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")  // 雷达图
implementation("io.noties.markwon:core:4.6.2")              // Markdown
implementation("io.coil-kt:coil")                           // 图片加载
```

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
├── ui/screens/
│   ├── auth/                          # AuthActivity + AuthViewModel
│   ├── main/                          # MainActivity（5 Tab 壳）
│   ├── onboarding/                    # OnboardingActivity（3 步引导）
│   ├── knowledge/                     # 知识库 Tab
│   │   ├── KnowledgeListFragment      # 笔记列表 + 知识图谱 + 上传
│   │   ├── KnowledgeListViewModel
│   │   ├── KnowledgeDetailFragment    # 笔记详情（内容 + 考点 + 题目）
│   │   └── KnowledgeDetailViewModel
│   ├── study/                         # 答疑 Tab
│   │   ├── StudyFragmentV3            # 流式对话 + 笔记引用卡片
│   │   ├── StudyViewModelV3
│   │   ├── StudyFragment              # V2 旧版（保留兼容）
│   │   └── StudyViewModel
│   ├── practice/                      # 刷题 Tab
│   │   ├── PracticeHubFragment        # 入口 + 批改流程
│   │   └── PracticeHubViewModel
│   ├── errors/                        # 错题本 Tab
│   │   ├── ErrorsListFragment         # 错题列表 + 筛选 + 复习提醒
│   │   ├── ErrorsListViewModel
│   │   ├── ErrorsDetailFragment       # 三路诊断 + 笔记溯源 + 同类题
│   │   └── ErrorsDetailViewModel
│   ├── profile/                       # 我的 Tab
│   │   ├── DashboardFragment          # 备考仪表盘
│   │   ├── DashboardViewModel
│   │   ├── CoverageRingView           # 自定义覆盖率环形图
│   │   ├── ProfileFragment            # V2 旧版
│   │   └── ProfileViewModel
│   ├── deeplearn/                     # 深度学习（苏格拉底式）
│   │   ├── DeepLearnFragment
│   │   └── DeepLearnViewModel
│   ├── video/                         # 视频讲解（Timeline 黑板演算）
│   │   └── VideoLessonFragment
│   ├── conversation/                  # 历史对话列表
│   │   ├── ConversationListFragment
│   │   └── ConversationListViewModel
│   └── home/                          # V2 旧首页
│       └── HomeFragment
├── data/
│   ├── remote/
│   │   ├── ApiService.java            # V1 Retrofit 接口
│   │   ├── NetworkClient.java         # V1 网络配置
│   │   ├── v2/ApiServiceV2.java       # V2 接口
│   │   ├── v2/NetworkClientV2.java    # V2 网络配置
│   │   └── v3/
│   │       ├── ApiServiceV3.java      # V3 Retrofit 接口（13 端点）
│   │       └── NetworkClientV3.java   # V3 网络配置
│   ├── repository/
│   │   ├── AuthRepository.java
│   │   ├── ChatRepository.java        # V1
│   │   ├── ConversationRepository.java # V1
│   │   ├── v2/DeepLearnRepository.java
│   │   └── v3/
│   │       ├── KnowledgeRepository.java
│   │       ├── PracticeRepository.java  # OkHttp SSE 流式
│   │       ├── ErrorsRepository.java
│   │       ├── DashboardRepository.java
│   │       ├── ChatRepositoryV3.java    # OkHttp SSE 流式
│   │       └── ConversationRepositoryV3.java
│   ├── model/
│   │   ├── v2/                         # V2 模型（Session, KnowledgeGraph 等）
│   │   └── v3/                         # 22 个 V3 模型类
│   └── local/                          # Room 数据库 + DAO
├── util/
│   ├── SseParser.java                  # SSE 流解析器（V2 + V3 双模式）
│   ├── TtsManager.java                 # TTS 语音合成
│   └── ...
└── res/
    ├── layout/                         # 布局文件（8 个 v3 layout）
    ├── menu/bottom_nav_menu.xml        # 5 Tab 定义
    ├── navigation/nav_graph_v3.xml     # 导航图
    └── drawable/                       # 图标资源
```

---

## 三、5 Tab 导航架构

### 3.1 Tab 定义（bottom_nav_menu.xml）

| 序号 | Tab ID | 标签 | 默认 Fragment |
|------|--------|------|--------------|
| 1 | `nav_knowledge` | 知识库 | `KnowledgeListFragment` |
| 2 | `nav_chat` | 答疑 | `StudyFragmentV3` |
| 3 | `nav_practice` | 刷题 | `PracticeHubFragment` |
| 4 | `nav_errors` | 错题本 | `ErrorsListFragment` |
| 5 | `nav_profile` | 我的 | `DashboardFragment` |

### 3.2 导航图（nav_graph_v3.xml）

```
nav_knowledge ──→ nav_knowledge_detail (笔记详情)
nav_chat ──→ nav_conversation_list (历史对话)
         ──→ nav_deeplearn (深度学习)
         ──→ nav_video (视频讲解)
nav_practice (批改流程内嵌 Fragment 状态机)
nav_errors ──→ nav_errors_detail (错题详情)
nav_profile (设置项 → 重走 Onboarding / 退出登录)
```

### 3.3 MainActivity

```java
BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
NavigationUI.setupWithNavController(bottomNav, navController);
```

使用 Android Navigation Components 绑定，Fragment 切换自动保持状态。

---

## 四、核心 Fragment 详解

### 4.1 KnowledgeListFragment（知识库）

**Layout**：`fragment_knowledge_list.xml`
**ViewModel**：`KnowledgeListViewModel`
**Repository**：`KnowledgeRepository`

| UI 组件 | 说明 |
|---------|------|
| 统计行 | 笔记数、科目数、考点数、覆盖率（4 个 TextView） |
| 搜索栏 | EditText + 300ms 防抖 |
| 学科筛选 | 横向滚动 Chips（全部/数学/物理/化学/英语/政治/历史），点击筛选 |
| 知识图谱 | WebView 加载 `knowledge_graph.html`（考点↔笔记↔题目 SVG） |
| 笔记列表 | RecyclerView + NoteCardAdapter（item_note_card.xml） |
| 上传按钮 | FAB → `ActivityResultContracts.GetContent("*/*")` → 调 `KnowledgeRepository.uploadNote()` |

### 4.2 StudyFragmentV3（答疑）

**Layout**：`fragment_study_v3.xml`
**ViewModel**：`StudyViewModelV3`
**Repository**：`ChatRepositoryV3`（SSE 流式）+ `ConversationRepositoryV3`

| UI 组件 | 说明 |
|---------|------|
| 消息列表 | ScrollView + LinearLayout 动态添加消息 View |
| 消息渲染 | Markwon Markdown + WebView LaTeX 数学公式（200ms 节流渲染） |
| 笔记引用卡片 | SSE `note_refs` 事件 → 底部显示 NoteRefCard（item_note_ref_card.xml） |
| 函数图像 | `【PLOT】...【END】` → GeoGebraView 2D 渲染 |
| 3D 图像 | `【PLOT3D】...【END】` → WebView 3D renderer |
| 附件 | 相机/相册/文件 选择器 |
| 顶栏按钮 | 新建对话、深度学习入口、视频讲解入口、历史对话 BottomSheet |
| 输入栏 | EditText + 发送按钮（↑） |

**SSE 流式流程**：

```
发送问题 → ChatRepositoryV3.chatStream()
  → SseParser 解析 SSE 事件
    ├─ onChunk: 追加文本 → 节流渲染 WebView
    ├─ onNoteRefs: 注入 NoteRefCard 到消息底部
    ├─ onPlot/onPlot3d: 提取 GeoGebra/3D 参数 → 渲染图像
    └─ onDone: 标记流结束，保存对话
```

### 4.3 PracticeHubFragment（刷题）

**Layout**：`fragment_practice_hub.xml`
**ViewModel**：`PracticeHubViewModel`
**Repository**：`PracticeRepository`（OkHttp + SSE）

**双视图设计**：

```
Hub 视图（默认）              批改流程视图
┌──────────────────┐        ┌──────────────────┐
│  ✏️ 刷题练习       │        │  1. 输入内容       │
│  ┌─────┬─────┐   │   →   │  2. 选择科目/满分   │
│  │📸批改│🎯组卷│   │        │  3. 提交           │
│  ├─────┼─────┤   │        │  4. 进度条（SSE）   │
│  │📅每日│📚真题│   │        │  5. 批改报告        │
│  └─────┴─────┘   │        │  6. 逐题详情        │
└──────────────────┘        └──────────────────┘
```

**SSE 进度事件**：

| 事件 | UI 表现 |
|------|---------|
| `step: ocr` | 进度 25% + "📷 正在识别文字..." |
| `step: extract` | 进度 50% + "📋 提取题目与答案..." |
| `step: grade` | 进度 75% + "✅ 正在批改比对..." |
| `step: diagnose` | 进度 90% + "🔬 诊断错因中..." |
| `complete` | 显示完整批改报告（总分、正确/错误数、正确率、逐题详情） |

### 4.4 ErrorsListFragment（错题本）

**Layout**：`fragment_errors_list.xml`
**ViewModel**：`ErrorsListViewModel`
**Repository**：`ErrorsRepository`

| UI 组件 | 说明 |
|---------|------|
| 筛选栏 | 横向 Chips（全部/数学/物理/待复习/考点漏缺/公式混淆） |
| 复习提醒 | 黄色 Banner："今天有 N 道错题需要复习" → 点击筛选待复习 |
| 错题列表 | RecyclerView + ErrorCardAdapter |
| 错题卡片 | 来源、题目摘要、错误答案（红线删除）→ 正确答案（绿色）、错误类型标签 |

### 4.5 ErrorsDetailFragment（错题详情）

**Layout**：`fragment_errors_detail.xml`
**ViewModel**：`ErrorsDetailViewModel`
**Repository**：`ErrorsRepository`

| UI 组件 | 说明 |
|---------|------|
| 题目标头 | 红色背景卡片：来源 + 题目 + 你的答案（删除线）+ 正确答案（绿色） |
| 三路诊断卡片 | 左侧彩色竖线区分：蓝色=考点漏缺、紫色=公式混淆、琥珀色=计算失误 |
| 笔记溯源 | 每路诊断下方挂 note_ref_card，点击跳转笔记详情 |
| 同类题推荐 | 3 道变式题（中等/较难标签），点击加入练习队列 |
| 操作按钮 | "标记掌握"（调 `markMastered` API）、"练同类题" |

### 4.6 DashboardFragment（我的）

**Layout**：`fragment_dashboard.xml`
**ViewModel**：`DashboardViewModel`
**Repository**：`DashboardRepository`

| UI 组件 | 说明 |
|---------|------|
| 渐变 Header | "下午好 ☀️" + 用户名 + 考试目标 |
| 覆盖率环形图 | 自定义 `CoverageRingView`（Canvas 绘制 conic-gradient） |
| 统计行 | 已掌握考点、笔记数、累计刷题、本周学习时长 |
| 今日任务 | 3 个 task item（复习错题/每日一练/专项突破），点击跳转对应 Tab |
| 学习周报 | 4 格数据（学习时长、做题数、正确率、新掌握数） |
| 掌握度雷达图 | MPAndroidChart `RadarChart`（5 维掌握度） |
| 设置项 | 考试目标、知识库管理、数据导出、重设考试目标（→Onboarding）、退出登录 |

**数据加载**：`loadAllDashboard()` 一次发起 4 个并发请求（overview/radar/tasks/report），`AtomicInteger` 计数完成。

---

## 五、V3 API 层

### 5.1 ApiServiceV3（Retrofit 接口）

Base URL：`BuildConfig.API_HOST + "/api/v3/"`

**知识库**：

| 注解 | 路径 | 返回类型 |
|------|------|---------|
| `@GET` | `kb/notes` | `ApiResponse<List<NoteItem>>` |
| `@GET` | `kb/notes/{id}` | `ApiResponse<NoteDetail>` |
| `@Multipart @POST` | `kb/upload` | `ApiResponse<UploadResult>` |
| `@POST` | `kb/search` | `ApiResponse<List<NoteFragment>>` |

**错题本**：

| 注解 | 路径 | 返回类型 |
|------|------|---------|
| `@GET` | `errors/list` | `ApiResponse<List<ErrorItem>>` |
| `@GET` | `errors/{id}` | `ApiResponse<ErrorDetail>` |
| `@GET` | `errors/review-queue` | `ApiResponse<List<ReviewItem>>` |
| `@PUT` | `errors/{id}/mark-mastered` | `ApiResponse<Map>` |
| `@POST` | `errors/{id}/similar` | `ApiResponse<List<SimilarQuestion>>` |

**仪表盘**：

| 注解 | 路径 | 返回类型 |
|------|------|---------|
| `@GET` | `dashboard/overview` | `ApiResponse<DashboardOverview>` |
| `@GET` | `dashboard/mastery-radar` | `ApiResponse<MasteryRadarData>` |
| `@GET` | `dashboard/today-tasks` | `ApiResponse<List<TaskItem>>` |
| `@GET` | `dashboard/weekly-report` | `ApiResponse<WeeklyReport>` |

### 5.2 NetworkClientV3（网络配置）

| 配置项 | 值 |
|--------|-----|
| Base URL | `BuildConfig.API_HOST + "/api/v3/"` |
| 标准超时 | 180 秒（connect/read/write） |
| 流式超时 | 300 秒（read），180 秒（connect/write） |

**拦截器链**（按顺序）：

1. **Auth 拦截器** → 注入 `token` 请求头
2. **Logging 拦截器** → `BODY`（标准）/ `HEADERS`（流式）
3. **401 拦截器** → 触发 `IRAgentApplication.onUnauthorized()` 跳转登录页
4. **网络拦截器**（仅流式）→ `Accept-Encoding: identity` + `Connection: keep-alive` + `Accept: text/event-stream`

**Retrofit 实例**：

| 实例 | Base URL | 用途 |
|------|---------|------|
| `retrofit` | `/api/v3/` | 标准 HTTP 调用 |
| `streamRetrofit` | `/api/v3/` | SSE 流式调用（300s 超时） |
| `conversationRetrofit` | `/api/` | V1 会话管理（兼容） |

### 5.3 Repository 层

| Repository | API 方式 | 方法数 |
|-----------|---------|--------|
| `KnowledgeRepository` | Retrofit `ApiServiceV3` | 4（list, detail, upload, search） |
| `ErrorsRepository` | Retrofit `ApiServiceV3` | 5（list, detail, reviewQueue, markMastered, similar） |
| `DashboardRepository` | Retrofit `ApiServiceV3` | 4（overview, radar, tasks, report） |
| `PracticeRepository` | OkHttp + SSE | 1（submitGrading） |
| `ChatRepositoryV3` | OkHttp + SSE | 1（chatStream） |
| `ConversationRepositoryV3` | Retrofit `ApiService`（V1） | 4（list, create, messages, title） |

所有 Repository 使用统一的 `ResultCallback<T>` 回调模式（`onSuccess`、`onError`、`onException`）。

---

## 六、V3 数据模型（22 个类）

| 模型 | 用途 |
|------|------|
| `NoteItem` | 笔记列表条目 |
| `NoteDetail` | 笔记详情（含 chunk、关联考点/题目） |
| `NoteChunk` | 笔记切分结果 |
| `NoteFragment` | 语义搜索匹配结果 |
| `NoteRef` | AI 回答中的笔记引用 |
| `LinkedKnowledgePoint` | 关联知识点 |
| `LinkedQuestion` | 关联题目 |
| `UploadResult` | 上传响应 |
| `SearchRequest` | 搜索请求体 |
| `ErrorItem` | 错题列表条目 |
| `ErrorDetail` | 错题详情（含诊断 JSON） |
| `DiagnosisJson` | 三维诊断容器 |
| `DiagnosisItem` | 单项诊断 |
| `ReviewItem` | 复习队列条目 |
| `SimilarQuestion` | 同类题推荐 |
| `GradingReport` | 批改报告 |
| `GradedQuestion` | 批改后单题 |
| `GradingRequest` | 批改请求体 |
| `DashboardOverview` | 仪表盘概览 |
| `MasteryRadarData` | 雷达图数据 |
| `TaskItem` | 今日任务条目 |
| `WeeklyReport` | 学习周报 |
| `ChatRequestV3` | V3 聊天请求 |

所有模型使用 Gson `@SerializedName` 注解映射 JSON 字段。

---

## 七、SSE 流式解析器（SseParser）

**文件**：`util/SseParser.java`

双模式设计，同时支持 V2 和 V3 SSE 事件格式：

```java
// V2 回调（旧版，保持兼容）
public interface Callback {
    void onSegment(String segment);
    void onComplete();
    void onError(Exception e);
}

// V3 回调（新版，按事件类型分发）
public interface V3Callback {
    void onChunk(String content);
    void onNoteRefs(List<NoteRef> refs);
    void onPlot(String plotData);
    void onPlot3d(String plotData);
    void onDone();
    void onError(String code, String message);
}
```

解析逻辑：按 `\n\n` 分割 SSE 帧 → 提取 `event:` 和 `data:` 字段 → 按事件类型分发。

---

## 八、自定义 View

### CoverageRingView（覆盖率环形图）

**文件**：`ui/screens/profile/CoverageRingView.java`

使用 Canvas `drawArc()` 绘制 conic-gradient 效果：
- 内部百分比文字
- 外环已掌握区域（主题色渐变）
- 外环未掌握区域（灰色）

### GeoGebraView

**文件**：`ui/geogebra/GeoGebraView.java`

WebView 封装，加载 `assets/geogebra/index.html`，通过 `evaluateJavascript()` 注入数学表达式，渲染 2D/3D 函数图像。

---

## 九、布局文件清单

### V3 核心布局

| 文件 | 对应 Fragment |
|------|-------------|
| `fragment_knowledge_list.xml` | KnowledgeListFragment |
| `fragment_knowledge_detail.xml` | KnowledgeDetailFragment |
| `fragment_study_v3.xml` | StudyFragmentV3 |
| `fragment_practice_hub.xml` | PracticeHubFragment |
| `fragment_errors_list.xml` | ErrorsListFragment |
| `fragment_errors_detail.xml` | ErrorsDetailFragment |
| `fragment_dashboard.xml` | DashboardFragment |
| `activity_main.xml` | MainActivity（NavHost + BottomNav） |

### 组件布局

| 文件 | 说明 |
|------|------|
| `item_note_card.xml` | 笔记卡片（科目、标题、摘要、标签、关联题目数） |
| `item_note_ref_card.xml` | 笔记引用卡片（"参考你的笔记" + 标题 + 片段） |
| `bottom_nav_menu.xml` | 5 Tab 菜单定义 |

### V2 兼容布局（保留）

`fragment_home.xml`、`fragment_study.xml`、`fragment_deep_learn.xml`、`fragment_video_lesson.xml`、`fragment_conversation_list.xml`、`fragment_profile.xml`、`activity_auth.xml`、`activity_onboarding.xml`

---

## 十、构建与运行

### 10.1 配置 API 地址

在 `app/build.gradle.kts` 中：

```kotlin
defaultConfig {
    buildConfigField("String", "API_HOST", "\"http://192.168.123.44:8080\"")
}
```

或在 `local.properties` 中覆盖。

### 10.2 编译

```bash
cd Android/IRAgentAPP
./gradlew assembleDebug
```

### 10.3 运行

```bash
./gradlew installDebug
# 或 Android Studio → Run 'app'
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

### 10.4 首次启动流程

```
AuthActivity（登录/注册）
  ↓ 登录成功 → 检查是否首次
  ├─ 新用户 → OnboardingActivity（选考试 → 定目标 → 上传笔记）
  └─ 老用户 → MainActivity（默认 Tab：知识库）
```

---

## 十一、WebView 资源（assets/）

| 文件 | 用途 |
|------|------|
| `math_template.html` | Markdown + LaTeX 数学公式渲染模板 |
| `engine/renderer.html` | Timeline 12 种动作黑板演算引擎 |
| `geogebra/index.html` | GeoGebra 2D 函数图像渲染 |
| `geogebra/3d_renderer.html` | GeoGebra 3D 函数图像渲染 |
| `knowledge_graph.html` | 知识图谱 SVG 可视化 |

---

## 十二、认证流程

1. `AuthActivity` → `POST /api/auth/login` → 获取 token
2. token 存入 `PreferencesManager`（SharedPreferences）
3. 所有 API 请求通过 `NetworkClient` 拦截器注入 `token` 请求头
4. 401 响应 → `IRAgentApplication.onUnauthorized()` → 清除 token → 跳转 `AuthActivity`

---

## 十三、版本兼容策略

| 层 | V1 | V2 | V3 |
|----|-----|-----|-----|
| 答疑对话 | `StudyFragment` + `ApiService` | — | `StudyFragmentV3` + `ChatRepositoryV3` |
| 深度学习 | — | `DeepLearnFragment` + `DeepLearnRepository` | — |
| 视频讲解 | — | `VideoLessonFragment` | — |
| 知识库 | — | — | `KnowledgeListFragment` + `KnowledgeRepository` |
| 刷题批改 | — | — | `PracticeHubFragment` + `PracticeRepository` |
| 错题本 | — | — | `ErrorsListFragment` + `ErrorsRepository` |
| 仪表盘 | — | — | `DashboardFragment` + `DashboardRepository` |
| 会话管理 | `ConversationRepository` | — | `ConversationRepositoryV3`（内部走 V1 API） |

V1/V2 组件保留可用，V3 组件通过 Navigation Components 无缝切换。用户无需感知版本差异。
