# IRAgent App - AI 学习助手

## 技术栈

| 分类 | 技术 |
|------|------|
| 语言 | Java 11 |
| 架构 | MVVM (ViewModel + LiveData) |
| 数据库 | Room |
| 网络 | Retrofit 2.11 + OkHttp 4.12 |
| 前端资源 | WebView + KaTeX + math.js + SVG/Canvas |
| TTS | 火山引擎 HTTP API + Android MediaPlayer |

## 项目结构

```
app/src/main/java/com/suiyuan/iragent_app/
├── data/
│   ├── local/              # Room DB (DAO, Entity)
│   ├── model/              # 数据模型
│   ├── remote/             # Retrofit ApiService + OkHttp NetworkClient
│   └── repository/         # ChatRepository, DeepLearnRepository
├── ui/
│   └── screens/
│       ├── auth/           # 登录注册 (AuthActivity)
│       ├── home/           # 首页 (HomeFragment) ← 默认启动页
│       ├── study/          # 提问答疑 (StudyFragment, StudyViewModel)
│       ├── deeplearn/      # 深度学习 + 视频讲解 (DeepLearnFragment)
│       ├── conversation/   # 会话列表
│       ├── main/           # MainActivity (宿主 Activity)
│       └── profile/        # 个人中心
└── util/
    ├── SseParser.java      # SSE 流式解析
    ├── TtsHttpClient.java  # 火山引擎 TTS 客户端
    ├── TtsManager.java     # TTS 管理
    └── MockSseSource.java  # 时间轴 Mock 数据

app/src/main/assets/
├── engine/
│   └── renderer.html       # 统一渲染引擎 (时间轴播放器 + SVG 坐标系 + TTS)
├── geogebra/
│   ├── index.html          # 2D Canvas 渲染
│   └── 3d_renderer.html    # 3D Canvas 渲染
└── math_template.html      # KaTeX 数学公式渲染模板
```

## 功能模块

### 1. 首页 (HomeFragment)
- 现代卡片式 UI：精选课程、快捷功能、视频讲解入口
- 支持输入知识点一键生成 Timeline 动画视频

### 2. 提问答疑 (StudyFragment)
- AI 对话 + SSE 流式响应
- 解析 `【PLOT】/【GEGEBRA】` / `【PLOT3D】` 标签渲染图像
- 会话侧边栏管理

### 3. 视频讲解 / 深度学习 (DeepLearnFragment)
- **视频讲解**: 后端生成 Timeline JSON → 前端引擎逐帧播放（SVG 绘图 + TTS 语音 + 打字文本）
- **深度学习**: 苏格拉底式分步引导学习 + 学习总结
- 12 种动作类型：title / show_grid / write_text / write_formula / draw_graph / annotate / highlight / draw_step / example / summary / clear_overlay / clear_board

### 4. 用户认证 (AuthActivity)
- 验证码登录 + 注册

## API 接口

### V1（`http://host:8080/api/`）

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `auth/getVerifiCodeImage` | 验证码图片 |
| POST | `auth/login` | 登录 |
| POST | `auth/register` | 注册 |
| POST | `ai/solve/stream` | SSE 流式解题 |
| GET | `ai/chat/messages/{conversationId}` | 聊天历史 |
| GET | `conversations` | 分页会话列表 |
| GET | `conversations/all` | 全部会话列表 |
| POST | `conversations` | 创建会话 |
| GET | `conversations/{conversationId}` | 会话详情 |
| PUT | `conversations/{conversationId}` | 更新会话 |
| DELETE | `conversations/{conversationId}` | 删除会话 |
| GET | `conversations/{conversationId}/messages` | 会话消息 |

### V2（`http://host:8080/api/v2/`）

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `sessions` | 创建深度学习会话 |
| GET | `sessions/{sessionId}` | 会话详情 |
| GET | `sessions/{sessionId}/teach?mode=` | SSE 流式教学 |
| POST | `sessions/{sessionId}/answer` | 提交回答 |
| GET | `sessions/{sessionId}/summary/stream` | SSE 学习总结 |
| GET | `sessions/history?page=&size=` | 学习历史 |
| DELETE | `sessions/{sessionId}` | 删除学习会话 |

### 独立接口

| 方法 | URL | 用途 |
|------|-----|------|
| POST | `http://host:8080/api/timeline/generate` | 生成时间轴视频 JSON |
| POST | `https://openspeech.bytedance.com/api/v1/tts` | 火山引擎 TTS |

**认证方式**: 请求头 `token: {uuid-token}`

## 时间轴动画协议

详细协议文档见 `docs/TIMELINE_PROTOCOL.md`，涵盖：
- 12 种动作类型定义
- TTS Native Playback 流程
- JS ↔ Android Bridge 通信
- 参考实现：`MockSseSource.buildTimelineJson()`

## 快速开始

```bash
# 环境: Android Studio, JDK 11, minSdk 24

# 配置服务器地址
# app/src/main/java/.../data/remote/NetworkClient.java — BASE_URL
# app/src/main/java/.../data/remote/v2/NetworkClientV2.java — BASE_URL

# 构建安装
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 关键决策

| 决策 | 说明 |
|------|------|
| 时间轴引擎 | 纯前端 JS (renderer.html) 驱动 `requestAnimationFrame` 循环 |
| TTS 播放 | Android 原生 MediaPlayer 播放，避免 Base64 传 JS 的性能开销 |
| 视频生成 | 后端生成 JSON 脚本 → 前端逐帧执行，支持 `audioTrigger` 音画同步 |
| 超时设置 | OkHttp readTimeout = 180s，适配后端 LLM 长时生成 |
