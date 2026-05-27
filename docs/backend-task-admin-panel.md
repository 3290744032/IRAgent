# 管理端开发任务文档

> **写给前后端开发人员**：本文档包含 IRAgent 管理端（Web Admin Panel）的完整开发需求。前端 Vue 3 + Ant Design Vue，后端 Spring Boot 增量接口。
>
> **前置阅读**：`docs/backend-architecture.md`（现有架构）、`docs/system-flow-diagrams.md`（业务逻辑）

---

## 一、项目定位

管理端是一个**基于 Web 的管理后台**，用于系统管理员查看运行状态、管理用户、审核 AI 题目、配置 API Key。采用 **Vue 3 + Vite + Ant Design Vue 4.x** 技术栈，对标若依（RuoYi）的登录 + 侧边栏布局模式。

**目标用户**：系统管理员（单人使用，不需要 RBAC 角色权限）。

---

## 二、后端改动

### 2.1 API Key 存储从 yaml 迁移到 Redis（三 Key）

**问题**：当前 `application.yaml` 只有一个 `api-key`，但项目实际使用三个独立的 Key：

| Key | 用途 | Redis Key |
|-----|------|-----------|
| **豆包 Chat Key** | 对话答疑 + SSE 流式 + 苏格拉底教学 | `admin:doubao-chat-key` |
| **DeepSeek Key** | DAG 诊断 + Timeline 生成 + 视频流 | `admin:deepseek-key` |
| **豆包 Embedding Key** | 向量化 + 语义检索 + 笔记入库 | `admin:doubao-embedding-key` |

**application.yaml 改动**：

```yaml
spring.ai.volcengine:
  doubao-chat-key: ${DOUBAO_CHAT_KEY:}
  deepseek-key: ${DEEPSEEK_KEY:}
  embedding-api-key: ${DOUBAO_EMBEDDING_KEY:}   # 空时 fallback 到 doubao-chat-key
  model: doubao-seed-1-8-251028                 # Chat 对话模型
  deepseek-model: deepseek-v3-2-251201          # DAG 诊断 / Timeline 模型
  base-url: https://ark.cn-beijing.volces.com/api/v3
  embedding-model: doubao-embedding-vision-250615
```

**改动文件**：

`config/ApiKeyProvider.java` — **新建**，封装三 Key 的热刷新：

```java
@Component
public class ApiKeyProvider {
    private volatile String doubaoChatKey;
    private volatile String deepseekKey;
    private volatile String doubaoEmbeddingKey;
    
    public String getDoubaoChatKey()      { return doubaoChatKey; }
    public String getDeepseekKey()         { return deepseekKey; }
    public String getDoubaoEmbeddingKey() { return doubaoEmbeddingKey; }
    
    public void refreshDoubaoChatKey(String key) {
        redis.opsForValue().set("admin:doubao-chat-key", key);
        this.doubaoChatKey = key;
    }
    public void refreshDeepseekKey(String key) {
        redis.opsForValue().set("admin:deepseek-key", key);
        this.deepseekKey = key;
    }
    public void refreshDoubaoEmbeddingKey(String key) {
        redis.opsForValue().set("admin:doubao-embedding-key", key);
        this.doubaoEmbeddingKey = key;
    }
}
```

`config/AIConfiguration.java` — Chat/Streaming 客户端注入 `keyProvider.getDoubaoChatKey()`；DAG/Timeline 客户端注入 `keyProvider.getDeepseekKey()`。
`rag/embedding/VolcengineEmbeddingClient.java` — 注入 `keyProvider.getDoubaoEmbeddingKey()`，空时 fallback 到 Chat Key。

### 2.2 新增后端 API（6 个）

所有新增接口放在 `controller/AdminController.java`（已有 `TraceController`，扩展它或新建 `AdminController`）。

#### 2.2.1 API Key 管理（三 Key）

> ⚠️ 项目实际使用三个独立的 Key：**豆包 Chat Key**（对话答疑）、**DeepSeek Key**（DAG 诊断 + Timeline + 视频流）、**豆包 Embedding Key**（向量化 + 语义检索）。

```
GET /admin/api-key
  → 返回三个 Key（均脱敏）
  ← {
    "doubaoChatKey":     "26ae****e1de",   // 豆包 Chat —— 对话 + SSE 流式
    "doubaoChatStatus":  "正常",
    "deepseekKey":       "sk-d****b3a2",   // DeepSeek —— DAG 诊断 + Timeline
    "deepseekStatus":    "正常",
    "embedKey":          "eb12****a3f5",   // 豆包 Embedding —— 向量化
    "embedStatus":       "正常",
    "embedFallback":     false             // true=Embedding fallback 到 Chat Key
  }

PUT /admin/api-key
  请求体：{ "type": "doubao-chat",     "apiKey": "新的豆包 Chat Key" }
          { "type": "deepseek",        "apiKey": "新的 DeepSeek Key" }
          { "type": "embedding",       "apiKey": "新的 Embedding Key" }
  → ApiKeyProvider.refreshXxxKey()
  ← { "success": true, "key": "26ae****e1de" }
```

#### 2.2.2 用户管理

```
GET /admin/users?page=0&size=20&keyword=
  → 查 users 表，支持分页 + 账号/邮箱模糊搜索
  ← {
    "data": [
      {
        "userId": 1,
        "account": "student_demo",
        "email": "student@example.com",
        "nickname": "小明",
        "status": 1,
        "createTime": "2026-03-15T10:00:00"
      }
    ],
    "total": 42
  }

PUT /admin/users/{userId}/status
  请求体：{ "status": 0 }   // 0=禁用, 1=启用
  ← { "success": true }
```

#### 2.2.3 AI 题目审核

```
GET /admin/questions/flagged?page=0&size=20
  → 查 question 表 WHERE status = 'flagged'
  ← {
    "data": [
      {
        "id": "q-xxx",
        "questionText": "...",
        "correctAnswer": "B",
        "source": "ai-generated",
        "flaggedAt": "2026-05-26T14:00:00",
        "reportCount": 2,        // 被报错次数
        "reporterIds": [1, 3]    // 报错用户 ID
      }
    ],
    "total": 5
  }

PUT /admin/questions/{id}/review
  请求体：{ "action": "approve" }   // approve=通过（恢复published）, reject=驳回（下架）
  ← { "success": true }
```

#### 2.2.4 系统健康检查

```
GET /admin/health
  → ping PG / Redis / Milvus / RocketMQ 连接，返回各组件状态和延迟
  ← {
    "postgresql": { "status": "up", "latencyMs": 2 },
    "redis":      { "status": "up", "latencyMs": 1 },
    "milvus":     { "status": "up", "latencyMs": 15 },
    "rocketmq":   { "status": "degraded", "latencyMs": 102 }
  }
```

> 前端阈值：`latencyMs < 50` 绿，`50-200` 黄，`>200 或异常` 红。RocketMQ 12ms 不会被误判为黄色。

#### 2.2.5 Dashboard 统计数据标准化

`GET /cache/stats` 已存在，需扩展返回格式以统一趋势口径：

```
GET /cache/stats
  ← {
    "cacheHitRate":    { "value": 83.4, "trend": 2.1, "period": "day" },
    "tokenSaveRate":   { "value": 81,   "trend": -15, "period": "week" },
    "activeUsers":     { "value": 12,   "trend": 3,   "period": "week" },
    "aiQuestionTotal": { "value": 156,  "flagged": 5 }
  }
```

> `period` 字段让前端自动翻译文案（`"day"` → "较昨日"、`"week"` → "较上周"），避免前端硬编码。

### 2.3 已有接口（直接复用）

| 接口 | 说明 |
|------|------|
| `POST /auth/login` | 登录（需小幅改造：返回值增加 `role` 字段） |
| `GET /cache/stats` | 缓存统计（命中率、条目数、节省 Token 量） |
| `PUT /admin/tenants/{id}/quota` | 租户并发配额调整 |
| `GET /admin/trace/{id}/billing` | TraceID 计费对账 |
| `GET /admin/noisy-neighbor/diagnose` | Noisy Neighbor 检测 |

### 2.4 后端改造清单汇总

| 文件 | 改动 |
|------|------|
| `config/AIConfiguration.java` | Chat 客户端用 `keyProvider.getDoubaoChatKey()`；DAG/Timeline 客户端用 `keyProvider.getDeepseekKey()` |
| `rag/embedding/VolcengineEmbeddingClient.java` | 改用 `keyProvider.getDoubaoEmbeddingKey()`，空时 fallback 到 Chat Key |
| `application.yaml` | 新增 `doubao-chat-key`、`deepseek-key`、`embedding-api-key` 三个字段 |
| `controller/AdminController.java` | 新增 6 个接口（key管理/用户列表/题目审核） |
| `controller/AuthController.java` | 登录返回值新增 `"isAdmin": true/false` |
| `service/AdminService.java` | **新建**，封装用户查询/题目审核/API Key 脱敏逻辑 |
| `question` 表 | 确认 `report_count`、`reporter_ids` 字段是否存在，不存在则新增（见下方 DDL） |
| — | 其余复用现有 `users`、`question` 表 |

**question 表需要确认的字段**（如果现有表没有，执行以下 DDL）：

```sql
ALTER TABLE question ADD COLUMN IF NOT EXISTS report_count INT DEFAULT 0;
ALTER TABLE question ADD COLUMN IF NOT EXISTS reporter_ids BIGINT[] DEFAULT '{}';
ALTER TABLE question ADD COLUMN IF NOT EXISTS flagged_at TIMESTAMP;
```

> `report_count`：被用户报错的累计次数。`reporter_ids`：报错用户 ID 列表（用于去重——同一用户不重复计数）。

**工作量**：后端约 1-2 天。

---

## 三、前端：Vue 3 项目搭建

### 3.1 技术选型

| 层 | 选型 |
|----|------|
| 框架 | Vue 3.5 + Composition API (`<script setup>`) |
| 构建 | Vite 6 |
| UI 库 | Ant Design Vue 4.x（中文支持好，和若依风格一致） |
| 图表 | ECharts 5.6（系统概览的 Token 消耗趋势） |
| HTTP | Axios + 请求/响应拦截器 |
| 路由 | Vue Router 4.x（`beforeEach` 守卫） |
| 状态 | Pinia（token + 用户信息） |
| CSS | Ant Design Vue 内置样式 + 少量 Less 变量覆盖 |

### 3.2 项目结构

```
IRAgent-Web/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── .env.development              # VITE_API_BASE=http://localhost:8080/api
├── .env.production               # VITE_API_BASE=/api
├── public/
│   └── favicon.ico
└── src/
    ├── main.ts                    # createApp + 注册 Router/Pinia/Ant Design
    ├── App.vue                    # <router-view />
    ├── router/
    │   └── index.ts               # 路由表 + beforeEach 守卫
    ├── stores/
    │   ├── user.ts                # token + userInfo + login() + logout()
    │   └── app.ts                 # sidebarCollapsed
    ├── api/
    │   ├── request.ts             # Axios 实例（baseURL + token拦截器 + 401跳转）
    │   ├── auth.ts                # login() / getUserInfo()
    │   ├── admin.ts               # getKey() / updateKey() / getUsers() / updateUserStatus()
    │   └── question.ts            # getFlaggedQuestions() / reviewQuestion()
    ├── layout/
    │   ├── AdminLayout.vue        # 侧边栏 + 顶部栏 + <router-view />（若依经典布局）
    │   ├── Sidebar.vue            # 左侧菜单（a-menu 组件）
    │   └── Header.vue             # 顶部面包屑 + 用户头像 + 退出按钮
    ├── views/
    │   ├── login/
    │   │   └── Login.vue          # 登录页（账号+密码+验证码，独立布局无侧边栏）
    │   ├── dashboard/
    │   │   └── Dashboard.vue      # 系统概览看板
    │   ├── user/
    │   │   └── UserList.vue       # 用户列表 + 配额调整
    │   ├── question/
    │   │   └── Review.vue         # AI 题目审核队列
    │   └── apikey/
    │       └── KeyManage.vue      # API Key 管理
    ├── components/
    │   ├── StatCard.vue           # 统计卡片（标题+数值+图标+趋势箭头）
    │   ├── HealthDot.vue          # 服务健康状态指示灯（绿/黄/红）
    │   └── QuestionCard.vue       # 题目审核卡片（题干+答案+报错次数+操作按钮）
    └── styles/
        └── global.less            # 全局样式覆盖
```

### 3.3 路由设计

```typescript
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '系统概览', icon: 'DashboardOutlined' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'UserOutlined' }
      },
      {
        path: 'question-review',
        name: 'QuestionReview',
        component: () => import('@/views/question/Review.vue'),
        meta: { title: '题目审核', icon: 'SafetyOutlined' }
      },
      {
        path: 'api-key',
        name: 'ApiKey',
        component: () => import('@/views/apikey/KeyManage.vue'),
        meta: { title: 'API Key', icon: 'KeyOutlined' }
      }
    ]
  }
];
```

**路由守卫逻辑**（`router.beforeEach`）：

```
目标路由 requiresAuth = true？
  ├─ 是 → 检查 localStorage 是否有 token，且未过期
  │     ├─ token 存在 && exp > now → 放行
  │     ├─ token 存在但已过期 → 清除 → 跳转 /login?redirect=原路径
  │     └─ token 不存在 → 跳转 /login?redirect=原路径
  └─ 否 → 放行（登录页）
```

> 登录时将 token 过期时间（24h）写入 `localStorage.setItem('token_exp', Date.now() + 24*3600*1000)`。

### 3.4 Axios 拦截器设计

```typescript
// request.ts
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,  // from .env.development
  timeout: 30000
});

// 请求拦截器：注入 token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers['token'] = token;
  return config;
});

// 响应拦截器：401 → 保存当前路径 → 清除 token → 跳转登录页
service.interceptors.response.use(
  response => response.data,       // 直接返回 ApiResponse.data
  error => {
    if (error.response?.status === 401) {
      // 保存当前路径，登录后跳回（非登录页才保存）
      const currentPath = window.location.pathname;
      if (currentPath !== '/login') {
        sessionStorage.setItem('redirect', currentPath);
      }
      localStorage.removeItem('admin_token');
      localStorage.removeItem('token_exp');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 四、页面功能详细设计

### 4.1 登录页（Login.vue）

**对标若依**：左侧品牌区 + 右侧登录表单。

```
┌──────────────────────────────────────────────────┐
│                                                  │
│   📚 IRAgent Pro          ┌──────────────────┐   │
│   AI 备考平台管理端        │  账号: [________] │   │
│   以个人知识库为中心的     │  密码: [________] │   │
│   AI 学习系统             │  验证码: [___] 📷  │   │
│                           │  [      登 录    ] │   │
│                           └──────────────────┘   │
└──────────────────────────────────────────────────┘
```

**技术要点**：
- 调用 `POST /auth/login`，成功后 `localStorage.setItem('admin_token', token)`
- 验证码图片通过 `GET /auth/getVerifiCodeImage` 获取（✅ 现有 V1 API，无需新增），显示为 `<img>`
- 登录成功后 `router.push('/dashboard')`

### 4.2 系统概览（Dashboard.vue）

上排 4 个统计卡片 + 下方 2 个图表。

```
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ 缓存命中率 │ │ Token节省 │ │ 活跃用户  │ │ AI 题目   │
│  83.4%   │ │  81%     │ │   12      │ │   156     │
│  ↑ 2.1%  │ │  ↓ 15%   │ │ 本周新增3 │ │  审核中5  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘

┌─────────────────────┐ ┌──────────────────────────┐
│ Token 消耗趋势 (折线) │ │ 服务健康状态              │
│  周一~周日            │ │ 🟢 PostgreSQL   正常      │
│  ECharts 折线图       │ │ 🟢 Redis        正常      │
│                      │ │ 🟢 Milvus       正常      │
│                      │ │ 🟡 RocketMQ     延迟      │
└─────────────────────┘ └──────────────────────────┘
```

**数据来源**：
- 缓存命中率 → `GET /cache/stats`
- Token 消耗 → 从 Redis 计数器读取
- 活跃用户 → 查 `users` 表近期登录的
- AI 题目 → 查 `question` 表 `source='ai-generated'` 总数
- 服务健康 → 分别 ping PG/Redis/Milvus/RocketMQ 连接

### 4.3 用户管理（UserList.vue）

**对标若依**：搜索栏 + 表格 + 分页。

```
┌──────────────────────────────────────────────────┐
│ 🔍 [___搜索账号/邮箱___]    [查询] [重置]          │
├──────────────────────────────────────────────────┤
│ 账号         │ 邮箱           │ 状态 │ 配额 │ 操作  │
│ student_demo │ stu@test.com   │ 🟢启用│ 5   │ 编辑  │
│ user_001     │ user@test.com │ 🔴禁用│ 3   │ 编辑  │
│ ...          │ ...           │ ... │ ... │ ...  │
├──────────────────────────────────────────────────┤
│ < 1 2 3 ... 10 >                                 │
└──────────────────────────────────────────────────┘
```

**技术要点**：
- 用 `a-table` 组件，columns 配置
- 配额编辑用 `a-modal` 弹窗，里面是一个 `a-slider`（1-20）+ 确定按钮 → 调 `PUT /admin/tenants/{id}/quota`
- 禁用/启用调 `PUT /admin/users/{userId}/status`

### 4.4 题目审核（Review.vue）

**对标若依**：卡片列表 + 批量操作。

```
┌──────────────────────────────────────────────────┐
│ 待审核题目（5）                                    │
├──────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────┐   │
│ │ 📐 数学 · 闭区间最值   报错 2 次            │   │
│ │ 题干：设 f(x) 在 [0,4] 上连续...            │   │
│ │ AI答案：最大值=4，最小值=0                   │   │
│ │ [通过]  [驳回]                              │   │
│ └────────────────────────────────────────────┘   │
│ ┌────────────────────────────────────────────┐   │
│ │ ...                                        │   │
│ └────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

**技术要点**：
- 调 `GET /admin/questions/flagged` 获取列表
- 通过/驳回调 `PUT /admin/questions/{id}/review`
- 通过 → 题目 status 恢复为 `published`
- 驳回 → 题目 status 改为 `rejected`（不再推送）

### 4.5 API Key 管理（KeyManage.vue）

```
┌──────────────────────────────────────────────────┐
│ API Key 配置                                      │
│                                                  │
│ ┌─ 豆包 Chat Key（对话 + SSE 流式）──────────┐   │
│ │ 当前：26ae************e1de    🟢 正常        │   │
│ │ 更新：[___________________] [更新]           │   │
│ └─────────────────────────────────────────────┘   │
│                                                  │
│ ┌─ DeepSeek Key（DAG 诊断 + Timeline）──────┐   │
│ │ 当前：sk-d************b3a2    🟢 正常        │   │
│ │ 更新：[___________________] [更新]           │   │
│ └─────────────────────────────────────────────┘   │
│                                                  │
│ ┌─ 豆包 Embedding Key（向量化 + 语义检索）───┐  │
│ │ 当前：eb12************a3f5    🟢 正常        │   │
│ │ 更新：[___________________] [更新]           │   │
│ │ 未设置时 fallback 到豆包 Chat Key            │   │
│ └─────────────────────────────────────────────┘   │
│                                                  │
│ ⚠️ 更新后立即生效，无需重启服务                   │
└──────────────────────────────────────────────────┘
```

**技术要点**：
- `GET /admin/api-key` → 返回 `{ doubaoChatKey, deepseekKey, embedKey, ...status }`
- `PUT /admin/api-key` → body 带 `{ type: "doubao-chat"/"deepseek"/"embedding", apiKey }`
- 三个 Key 各自独立管理，Embedding Key 未设置时 fallback 到豆包 Chat Key

---

## 五、任务拆解与验收

### Phase 1：后端改造（1-2 天）

| 任务 | 验收标准 |
|------|---------|
| Key 存储迁移到 Redis | `ApiKeyProvider` 替代直接读 yaml；Redis 无值时 fallback 环境变量 |
| 新增管理端 API（8 个） | Swagger 可查看：`/admin/api-key`(2)、`/admin/users`(2)、`/admin/questions`(2)、`/admin/health`、`/cache/stats`（扩展） |
| AuthController 登录加 isAdmin | 登录返回 `"isAdmin": true/false`（判 admin 账号） |

### Phase 2：前端搭建（3-4 天）

| 任务 | 验收标准 |
|------|---------|
| 项目初始化 | `npm create vite@latest` → `npm i ant-design-vue axios pinia vue-router echarts` → `npm run dev` 看到空白页 |
| 登录页 | 账号密码+验证码登录，token 存入 localStorage，跳转 dashboard |
| AdminLayout + 路由 | 侧边栏 4 个菜单 + 面包屑 + 退出按钮 + 路由守卫 |
| 系统概览页 | 4 个统计卡片 + Token 趋势图 + 健康灯（调 GET /cache/stats） |
| 用户管理页 | 搜索栏 + 表格 + 配额弹窗编辑 + 分页 |
| 题目审核页 | 卡片列表 + 通过/驳回按钮 |
| API Key 页 | 脱敏显示 + 修改表单 |

### Phase 3：联调（1 天）

| 任务 | 验收标准 |
|------|---------|
| 前后端联调 | 管理端所有页面数据正常展示，CRUD 操作生效 |
| 热更新验证 | PUT /admin/api-key (type=chat/embedding) 后对应调用使用新 Key |

---

## 七、前端集成细节（开发必读）

### 7.1 认证流程

```
Login.vue
  ↓ POST /auth/login  { account, password, captcha }
  ↓ 需要先调 GET /auth/getVerifiCodeImage 获取验证码图片（返回 JPEG 流 + X-Verification-UUID 头）
  ← { success: true, code: 200, data: { token: "xxx", isAdmin: true } }
  ↓
localStorage.setItem('admin_token', data.token)
localStorage.setItem('token_exp', Date.now() + 24*3600*1000)
  ↓
router.push('/dashboard')
  ↓
所有后续请求在 header 中携带：token: <admin_token>
```

> ⚠️ `POST /auth/login` 使用 `ApiResponse` 封装：`{ success, code, message, data: { token, isAdmin } }`。token 在 `data.token` 里，不是顶层。

### 7.2 响应格式差异（重要）

后端有两套响应格式，前端 Axios 拦截器必须区分处理：

| Controller | 格式 | 示例 |
|-----------|------|------|
| `AuthController` | `ApiResponse<T>` 包装 | `{ success: true, code: 200, data: { token: "..." } }` |
| `AdminController` | 裸 `Map` 返回 | `{ key: "26ae****e1de", status: "正常" }` |

**前端处理方案**：

```typescript
// Axios 响应拦截器
service.interceptors.response.use(
  response => {
    const body = response.data;
    // AuthController 返回 ApiResponse 包装
    if (body.success !== undefined && body.data !== undefined) {
      return body.data;  // 解包 ApiResponse
    }
    // AdminController 返回裸 Map，直接透传
    return body;
  }
);
```

### 7.3 接口速查表（按页面分组）

#### Dashboard 页

| 接口 | 返回值 | 映射到 UI |
|------|--------|----------|
| `GET /admin/dashboard/stats` | `{ userCount: {value, todayChange, trend, trendValue}, aiQuestionCount, flaggedCount, officialCount, trendPeriod }` | 4 个 stat-card：`userCount.value`→活跃用户、`aiQuestionCount.value`→AI 题目、`flaggedCount.value`→待审核数、缓存命中率暂用 Mock（`GET /cache/stats` 补充） |
| `GET /admin/health` | `{ postgresql: {status, latencyMs}, redis, milvus, rocketmq }` | health-grid 4 个灯：`latencyMs < 50` 绿、`50-200` 黄、`>200` 红 |

#### 用户管理页

| 接口 | 返回值 | 映射到 UI |
|------|--------|----------|
| `GET /admin/users?page=0&size=20&keyword=` | `{ data: [{userId, account, email, nickname, status, createTime}], total }` | a-table，`status: 1`→启用/`0`→禁用 |
| `PUT /admin/users/{userId}/status` | `{ success: true }` | Switch 切换后调此接口 |
| `PUT /admin/tenants/{userId}/quota` | `{ success: true }` | 配额弹窗 → a-slider → 确定后调用。⚠️ **tenantId 格式**：`tenant-{userId}`（如 `tenant-5`），不是裸数字 |

> 配额接口在 `TraceController` 而非 `AdminController`，路径仍为 `/admin/tenants/{tenantId}/quota`。

#### 题目审核页

| 接口 | 返回值 | 映射到 UI |
|------|--------|----------|
| `GET /admin/questions/flagged?page=0&size=20` | `{ data: [{id, questionText, correctAnswer, topic, source, flaggedAt}], total }` | review-card 列表，`topic`→科目+考点 |
| `PUT /admin/questions/{id}/review` | `{ success: true, status: "published"/"rejected" }` | 通过/驳回按钮 → 调此接口 |

#### API Key 页

| 接口 | 返回值 | 映射到 UI |
|------|--------|----------|
| `GET /admin/api-key` | `{ doubaoChatKey, deepseekKey, embedKey, ...status }` | 三个 key-display 区域：豆包 Chat / DeepSeek / 豆包 Embedding |
| `PUT /admin/api-key` | body `{ type: "doubao-chat"/"deepseek"/"embedding", apiKey }` → `{ success: true, key }` | 三个独立的 a-input-password + 更新按钮 |

### 7.4 环境变量配置

```bash
# .env.development
VITE_API_BASE=http://localhost:8080/api

# .env.production
VITE_API_BASE=/api
```

### 7.5 前端对接顺序（推荐）

1. **先做登录页** — 验证 `POST /auth/login` 能拿到 token → 存入 localStorage → 跳转
2. **再做 Dashboard** — 调 `GET /admin/dashboard/stats` + `GET /admin/health` → 替换 Mock 数据
3. **接着用户管理** — 调 `GET /admin/users` → 渲染表格 → Switch 调 status 接口 → 配额弹窗调 quota 接口
4. **再做题审核** — 调 `GET /admin/questions/flagged` → 渲染卡片 → 通过/驳回调 review 接口
5. **最后 API Key** — 调 `GET /admin/api-key` → 显示脱敏 Key → PUT 更新
| 热更新验证 | PUT /admin/api-key (type=chat/embedding) 后对应调用使用新 Key |

---

### 2.5 操作审计日志

管理端操作（修改 Key、禁用用户、审核题目）属于高风险操作。需要在后端打印结构化日志方便事后追溯。

```java
// AdminService 或 Controller 中每个修改操作前打印：
log.info("[ADMIN] action={}, operator={}, target={}, time={}",
    "UPDATE_API_KEY", userId, "api-key", Instant.now());
log.info("[ADMIN] action={}, operator={}, targetUserId={}, status={}",
    "DISABLE_USER", userId, targetUserId, 0);
log.info("[ADMIN] action={}, operator={}, questionId={}, result={}",
    "REVIEW_QUESTION", userId, questionId, "approve");
```

> 当前阶段不需要建表存储（自己一个人用的系统），结构化日志即可。如果需要，可复用 `student_behavior_log` 表的 JSONB `metadata` 字段。

---

## 六、与现有代码的关系

| 现有文件 | 是否改动 | 说明 |
|---------|:---:|------|
| `config/AIConfiguration.java` | ✅ 改 | Key 读取逻辑 |
| `config/VolcengineEmbeddingClient.java` | ✅ 改 | 同上 |
| `controller/AuthController.java` | ✅ 改 | 登录返回值加 role |
| `controller/TraceController.java` | 可扩展 | 新增接口加在此文件或新文件 |
| `users` 表 | 不改 | 复用现有 |
| `question` 表 | 不改 | 复用现有 |
| Android 端 | 不改 | 不受影响 |
| `ui-prototype-v3` | 不改 | 不受影响 |
