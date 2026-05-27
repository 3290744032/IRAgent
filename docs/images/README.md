# 截图清单

将你的截图放入 `docs/images/screenshots/` 目录，然后在 README 中取消注释对应的 `<img>` 标签即可。

## 截图列表

| 文件名 | 内容说明 | 建议尺寸 |
|--------|----------|----------|
| `banner.png` | 项目封面图（logo + 产品名 + 一句话定位），居中横条 | 1200×400 |
| `hero-mockup.png` | 手机模拟器截图（展示主界面），带设备边框更佳 | 400×800 |
| `tab-knowledge.png` | 知识库 Tab — 笔记列表 + 学科筛选 + 上传入口 | 360×780 |
| `tab-chat.png` | 答疑 Tab — SSE 流式对话 + 笔记引用卡片 | 360×780 |
| `tab-grading.png` | 刷题 Tab — 试卷批改 4 步进度 | 360×780 |
| `tab-errors.png` | 错题本 Tab — 三维诊断结果展示 | 360×780 |
| `tab-dashboard.png` | 我的 Tab — 仪表盘（环形图 + 雷达图） | 360×780 |
| `workflow-upload.png` | 操作流程 — 笔记上传 → 解析 → 知识图谱 | 800×400 |
| `workflow-chat.png` | 操作流程 — 提问 → AI 引用笔记回答 | 800×400 |
| `workflow-grading.png` | 操作流程 — 提交试卷 → 批改报告 → 诊断 | 800×400 |

## 如何制作截图

### Android 截屏
- 运行 App 后使用 Android Studio 的 Logcat 截屏，或 `adb shell screencap`
- 推荐使用 [Device Art Generator](https://developer.android.com/distribute/marketing-tools/device-art-generator) 加设备边框

### Web 原型截屏
- 启动原型：`cd ui-prototype-v3 && npx serve .`
- 用 Chrome DevTools Device Mode (360×780) 截取各 Tab 界面
- 或用 Firefox Responsive Design Mode

### 封面图
- 用 Figma / Canva 制作，包含 logo + 标题 + 副标题
- 配色参考：橙色渐变（#FF6B35 → #F7C948）
