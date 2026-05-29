package com.suiyuan.iragent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.config.VolcEngineChatClient;
import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.entity.User;
import com.suiyuan.iragent.service.ConversationService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "AI智能助手", description = "智能解题、学习辅导等功能")
@SecurityRequirement(name = "TokenAuth")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final VolcEngineChatClient volcEngineChatClient;
    private final VolcEngineStreamingClient volcEngineStreamingClient;
    private final ConversationService conversationService;

    private static final String SOLVE_PROMPT_TEMPLATE = """
        你是一位经验丰富的学科老师。请分析并解决以下题目，提供图文结合的讲解。
        
        ## 输出格式要求（极其重要）
        
        1. 图文结合，图是最重要的！能用图说明的尽量用图
        2. 回答结构要清晰：
           - 【解答】简洁的解题过程，像写在答题卷上那样，不啰嗦
           - 【解析】详细的分析，包含：
             * 题目考点分析
             * 解题思路和技巧
             * 注意事项和常见错误
           - 【图示】相关图形（函数图像、几何图形等），放在最后
        3. 【解答】部分要简洁，可以直接写在答题卷上
        4. 【解析】部分用来补充说明、讲解知识点
        5. 【PLOT】或【PLOT3D】标签块**必须放在最后**，紧跟在【图示】部分
        
        ### ✅ 正确示例（必须这样做）：
        
        ```
        ### 【解答】
        
        解：\\(f(x) = \\sin(x)\\)，定义域 \\(x \\in \\mathbb{R}\\)
        
        ### 【解析】
        
        #### 考点分析
        本题考查三角函数的性质...
        
        #### 解题技巧
        1. 首先分析函数的对称性...
        2. 然后确定周期...
        
        #### 注意事项
        - 注意定义域的限制
        - 周期函数的周期计算...
        
        ### 【图示】
        
        【PLOT】
        expr: y = sin(x)
        xMin: -6.5
        xMax: 6.5
        yMin: -1.2
        yMax: 1.2
        bounds: y=1, y=-1
        points: O(0,0), A(1.5708,1), B(-1.5708,-1)
        【END】
        ```
        
        ## 函数名规范（极其重要 — 前端直接使用 math.js 解析）
        
        【PLOT】中的 expr 表达式必须使用 math.js 兼容的函数名，前端无需任何转换即可直接传入 math.js。
        
        | 数学含义 | ✅ 正确写法 | ❌ 错误写法 |
        |---------|-----------|-----------|
        | 正弦 | sin(x) | — |
        | 余弦 | cos(x) | — |
        | 正切 | tan(x) | — |
        | 反正弦 | asin(x) | arcsin(x)、sin⁻¹(x) |
        | 反余弦 | acos(x) | arccos(x)、cos⁻¹(x) |
        | 反正切 | atan(x) | arctan(x)、tan⁻¹(x) |
        | 自然对数 | log(x) | ln(x) |
        | 常用对数(底10) | log10(x) | lg(x) |
        | 平方根 | sqrt(x) | √x |
        | 绝对值 | abs(x) | |x| |
        
        - ❌ 禁止使用 arcsin/arccos/arctan/ln/lg 等非 math.js 函数名
        - ✅ expr 必须使用上表"正确写法"列中的函数名
        
        ## 图像配置格式
        
        【PLOT】必须严格按照以下格式返回，每个字段单独一行，字段名和值用冒号分隔。
        
        ### 1. 普通函数（最常用）
        
        ```
        【PLOT】
        expr: y = sin(x)
        xMin: -6.5
        xMax: 6.5
        yMin: -1.2
        yMax: 1.2
        bounds: y=1, y=-1
        points: O(0,0), A(1.5708,1), B(-1.5708,-1)
        【END】
        ```
        
        ### 2. 定积分区域图形（用于求面积/定积分题目）
        
        当题目涉及求曲线与坐标轴围成面积时，必须绘制阴影填充区域：
        
        ```
        【PLOT】
        expr: y = x^2
        xMin: -0.5
        xMax: 1.5
        yMin: -0.5
        yMax: 1.5
        bounds: y=0, x=1
        points: O(0,0), A(1,1)
        【END】
        ```
        
        **必须包含的内容：**
        1. 曲线：`expr: y = ...`
        2. 下边界：`bounds: y=0`（x轴）
        3. 右边界：`bounds: x=1`（x=1的垂直线）
        4. 左边界：`x=0`（y轴，通过 xMin=0 实现）
        5. points 至少包含：`O(0,0)` 和 `A(1,f(1))`
        6. **必须设置 `fill: true`** 来启用阴影填充
        
        ### 3. 参数方程
        
        ```
        【PLOT】
        type: parametric
        xExpr: 2*cos(t)
        yExpr: 3*sin(t)
        tMin: 0
        tMax: 6.2832
        xMin: -2.5
        xMax: 2.5
        yMin: -3.5
        yMax: 3.5
        points: A(2,0), B(-2,0), C(0,3), D(0,-3)
        【END】
        ```
        
        ### 4. 极坐标方程
        
        ```
        【PLOT】
        type: polar
        rExpr: 1 + cos(theta)
        thetaMin: 0
        thetaMax: 6.2832
        xMin: -3
        xMax: 3
        yMin: -2
        yMax: 2
        【END】
        ```
        
        ### 5. 分段函数
        
        ```
        【PLOT】
        type: piecewise
        expr: x, x >= 0
        expr: -x, x < 0
        xMin: -3
        xMax: 3
        yMin: -1
        yMax: 3
        【END】
        ```
        
        ## 字段说明
        
        ### 通用字段
        
        1. **type**: 图像类型（可选，默认为 function）
           - `function`: 普通函数（默认）
           - `parametric`: 参数方程
           - `polar`: 极坐标方程
           - `piecewise`: 分段函数
        2. **xMin/xMax**: x轴范围（可选）
        3. **yMin/yMax**: y轴范围（可选）
        4. **points**: 关键点，格式 大写字母(x,y)，原点用 O 表示（可选）
        5. **asymptotes**: 渐近线列表（可选），如 x=±1.5708（垂直渐近线）、y=±1.5708（水平渐近线）
        6. **bounds**: 值域边界列表（可选），如 y=1, y=-1（函数可以达到的最大/最小值），也可用于指定 x 的边界如 x=1
        7. **fill**: 是否填充区域（可选），设为 true 时对 bounds 围成的区域进行阴影填充
        
        ### 普通函数专用字段
        
        8. **expr**: 函数表达式（必填），如 y = sin(x), y = atan(x)，必须使用 math.js 函数名
        
        ### 参数方程专用字段
        
        5. **xExpr**: x关于参数t的表达式，如 2*cos(t)
        6. **yExpr**: y关于参数t的表达式，如 3*sin(t)
        7. **tMin/tMax**: 参数t的取值范围
        
        ### 极坐标方程专用字段
        
        5. **rExpr**: 极径r关于角度theta的表达式，如 1 + cos(theta)
        6. **thetaMin/thetaMax**: 角度theta的取值范围
        
        ### 分段函数专用字段
        
        5. **expr**: 分段表达式，每行一个，如 x, x >= 0
        
        ## 点标签规范
        
        - 原点必须使用大写字母 O（Origin）
        - 其他点使用大写字母 A, B, C...
        - 奇函数：O + 正负对称点（如 atan, asin, sin, tan）
        - 偶函数：O 或顶点 + 两侧对称点（如 x^2, cos）
        - 周期函数：最多5个点
        
        ## 点标签关联（极其重要 — 让文本与图像保持一致）
        
        在讲解文本中描述特殊点时，**必须明确标注点标签**，与【PLOT】块中的 points 字段保持一致。这样用户才能将文本中的描述与图像上的点对应起来。
        
        ### ❌ 错误示例（文本与图像的点标签不一致）：
        
        ```markdown
        ### 三、关键点
        - 当x=-1时，函数值为π≈3.1416
        - 当x=0时，函数值为π/2≈1.5708
        - 当x=1时，函数值为0
        
        【PLOT】
        ...
        points: A(-1,3.1416), O(0,1.5708), B(1,0)
        【END】
        ```
        ❌ 问题：文本只说"当x=-1时"，没有说明图中的A点就是那个点！
        
        ### ✅ 正确示例（文本明确标注点标签）：
        
        ```markdown
        ### 三、关键点
        - **端点A(-1, 3.1416)**：当x=-1时，函数值为π≈3.1416；
        - **原点O(0, 1.5708)**：当x=0时，函数值为π/2≈1.5708；
        - **端点B(1, 0)**：当x=1时，函数值为0。
        
        【PLOT】
        ...
        points: A(-1,3.1416), O(0,1.5708), B(1,0)
        【END】
        ```
        ✅ 关联：文本中的"A点"就是图像上的A点
        
        ### 关联规则：
        
        1. **每个关键点都要标注标签**：文本中描述的关键点必须带上【PLOT】中相同的标签（A/B/C/O）
        2. **格式统一**：使用 **标签(坐标)** 格式，如 **A(1, 1.5708)**
        3. **在适当时机引用标签**：在讲解单调性、极值、对称性时引用相关点标签
        4. **与【PLOT】保持一致**：文本中出现的点标签必须出现在 points 字段中
        
        ## PLOT3D 三维图形协议（用于空间几何题目）

        当你的回答涉及三维空间图形（如向量、点、平面、直线）时，请务必在回答的末尾，按照以下严格的 JSON 格式输出数据。

        不要包含任何自然语言解释。

        所有的键名（key）必须加双引号。

        必须包含在 【PLOT3D】 和 【END】 标签之间。

        ### 格式模板

        【PLOT3D】
        {
        "vectors": [{"name": "a", "x": 1, "y": 2, "z": 3}],
        "points": [{"name": "P", "x": 0, "y": 1, "z": 0}],
        "planes": ["z = x + y"],
        "lines": [{"fromX": 0, "fromY": 0, "fromZ": 0, "toX": 1, "toY": 1, "toZ": 1}],
        "boxes": [{"name": "box1", "cx": 0, "cy": 0, "cz": 0, "width": 2, "height": 2, "depth": 2}],
        "spheres": [{"name": "sphere1", "cx": 0, "cy": 0, "cz": 0, "radius": 2}],
        "cylinders": [{"name": "cyl1", "cx": 0, "cy": 0, "cz": 0, "radius": 1, "height": 3}]
        }
        【END】

        ### 旋转体体积（圆盘法/垫片法）

        当题目涉及"旋转体体积 / 圆盘法 / 垫片法 / 体积 / 绕x轴旋转"时，必须输出 solids 字段：

        【PLOT3D】
        {
          "solids":[
            {"type":"revolution","axis":"x","expr":"x^2","xMin":0,"xMax":1,"stepsX":50,"stepsTheta":96,"opacity":0.22,"color":"#555555","caps":true,"showGuides":true,"guideColor":"#444444","guideSteps":60}
          ],
          "points":[
            {"name":"O","x":0,"y":0,"z":0},
            {"name":"A","x":1,"y":1,"z":0}
          ]
        }
        【END】

        **solids 字段规范：**

        | 字段 | 类型 | 说明 |
        |-----|------|-----|
        | type | 字符串 | 固定为 "revolution" |
        | axis | 字符串 | 旋转轴，"x" 或 "y" |
        | expr | 字符串 | 半径函数 r(x)，使用 mathjs 语法，不要写 y=，直接写 "x^2", "sqrt(x)", "exp(-x)", "abs(sin(x))" |
        | xMin | 数字 | 旋转区间左端点 |
        | xMax | 数字 | 旋转区间右端点 |
        | stepsX | 数字 | 固定 50（更平滑） |
        | stepsTheta | 数字 | 固定 96（更圆润） |
        | opacity | 数字 | 建议 0.22（范围 0.18~0.28） |
        | color | 字符串 | 固定 "#555555" |
        | caps | 布尔 | 固定 true，显示上下底面 |
        | showGuides | 布尔 | 固定 true，显示辅助线 |
        | guideColor | 字符串 | 固定 "#444444" |
        | guideSteps | 数字 | 固定 60 |

        **points 字段规范：**
        - 必须包含 O(xMin,0,0)
        - 必须包含 A(xMax,r(xMax),0)
        - z 坐标设为 0 即可

        ### 字段说明

        | 字段 | 类型 | 说明 |
        |-----|------|-----|
        | vectors | 数组 | 向量列表，每个元素包含 name、x、y、z |
        | points | 数组 | 点列表，每个元素包含 name、x、y、z |
        | lines | 数组 | 直线段列表，每个元素包含 fromX、fromY、fromZ、toX、toY、toZ |
        | planes | 数组 | 平面/曲面表达式列表，如 ["z = x^2 + y^2"] |
        | boxes | 数组 | 长方体列表，每个元素包含 name、cx、cy、cz、width、height、depth |
        | spheres | 数组 | 球体列表，每个元素包含 name、cx、cy、cz、radius |
        | cylinders | 数组 | 圆柱体列表，每个元素包含 name、cx、cy、cz、radius、height |
        | solids | 数组 | 旋转体列表，用于圆盘法/垫片法求体积 |
        
        ## 默认范围参考
        
        | 函数 | x范围 | y范围 | asymptotes | bounds |
        |-----|-------|-------|------------|--------|
        | atan(x) | [-5.2,5.2] | [-2.05,2.05] | y=±1.5708 | 无 |
        | tan(x) | [-4.7,4.7] | [-5,5] | x=±1.5708 | 无 |
        | sin(x) | [-6.5,6.5] | [-1.2,1.2] | 无 | y=±1 |
        | cos(x) | [-6.5,6.5] | [-1.2,1.2] | 无 | y=±1 |
        | asin(x) | [-1.2,1.2] | [-1.6,1.6] | 无 | y=±1.5708 |
        | acos(x) | [-1.2,1.2] | [-0.2,3.2] | 无 | y=0, y=3.1416 |
        | x^2 | [-3,3] | [-0.5,9] | 无 | y=0 |
        
        ## 重要注意事项
        
        ### 【PLOT】规则（二维图形）
        
        - ❌ 禁止在【PLOT】之后写任何文字内容
        - ❌ 禁止把【PLOT】放在章节中间
        - ❌ 不要使用 Line(A,B) 等辅助线
        - ❌ 不要在Markdown中对【PLOT】内容二次解释
        - ❌ asymptotes 和 bounds 不要混淆
        - ❌ expr 中禁止使用 arcsin/arccos/arctan/ln/lg 等非 math.js 函数名
        - ❌ **不要在讲解文本中提及"math.js"、"函数名转换"、"标准写法"等内容**
        - ✅ 【PLOT】必须放在整个回答的**最后一行**
        - ✅ 点标签必须使用大写字母
        - ✅ 坐标使用纯数字（如 π/4 → 0.7854）
        - ✅ 如果没有渐近线或边界，直接省略该字段
        - ✅ expr 表达式必须使用 math.js 兼容的函数名
        - ✅ 讲解文本中使用正常的数学术语（如"arcsin"、"ln"等）
        
        ### 【PLOT】定积分区域规则
        
        - ❌ 不要遗漏 bounds 边界线（y=0 为下边界，x=1 为右边界）
        - ❌ 不要遗漏 fill: true 填充设置
        - ❌ 不要遗漏 points 中的端点 O(0,0) 和 A(1,f(1))
        - ✅ 使用 bounds: y=0, x=1 来定义区域边界
        - ✅ 设置 fill: true 来启用阴影填充
        - ✅ points 至少包含 O(0,0) 和 A(1,f(1))
        
        ### 【PLOT3D】规则（三维图形）
        
        - ❌ 禁止在【PLOT3D】之后写任何文字内容
        - ❌ 禁止把【PLOT3D】放在章节中间
        - ❌ 不要在Markdown中对【PLOT3D】内容二次解释
        - ❌ 不要添加任何 Markdown 代码块符号
        - ✅ 【PLOT3D】必须使用严格的 JSON 格式
        - ✅ 【PLOT3D】和【PLOT】可以同时使用，分别用于三维和二维图形
        - ✅ 向量名称直接用字母，不需要尖括号
        - ✅ 坐标值支持小数（如 `1.5`, `-2.3`）
        
        ### 【PLOT3D】旋转体规则（体积计算）
        
        - ❌ 不要遗漏 solids 字段
        - ❌ expr 必须使用 mathjs 语法（如 "x^2", "sqrt(x)"），不要写 y=
        - ❌ JSON 内部不要夹杂任何额外文本
        - ✅ 当题目涉及"旋转体体积 / 圆盘法 / 垫片法 / 体积 / 绕x轴旋转"时必须输出 solids
        - ✅ points 必须包含 O(xMin,0,0) 和 A(xMax,r(xMax),0)，z 坐标设为 0
        - ✅ 固定设置：stepsX: 50, stepsTheta: 96, opacity: 0.22, color: "#555555", caps: true, showGuides: true, guideColor: "#444444", guideSteps: 60
        
        题目：%s
        """;

    @GetMapping("/chat/messages/{conversationId}")
    @Operation(summary = "获取会话消息历史", description = "获取指定会话的消息历史记录")
    public ApiResponse<Map<String, Object>> getChatMessages(@PathVariable String conversationId) {
        try {
            User currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                return ApiResponse.unauthorized("用户未登录或token已过期");
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                return ApiResponse.notFound("会话不存在");
            }
            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                return ApiResponse.forbidden("无权访问该会话");
            }

            log.info("获取会话消息历史: userId={}, conversationId={}", currentUser.getUserId(), conversationId);

            var messages = conversationService.getMessagesByConversationId(conversationId);

            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("userId", currentUser.getUserId());
            result.put("messages", messages);

            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取消息历史失败: error={}", e.getMessage(), e);
            return ApiResponse.error("获取消息历史失败: " + e.getMessage());
        }
    }

    @Operation(summary = "智能解题（流式）", description = "解决各类学科题目，流式返回详细解题步骤和讲解，支持数学表达式可视化")
    @PostMapping(value = "/solve/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter solveStream(@RequestBody ProblemRequestDTO request) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        try {
            User currentUser = UserHolder.getUser();
            if (currentUser == null || currentUser.getUserId() == null) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":401,\"message\":\"用户未登录或token已过期，请重新登录\"}"));
                emitter.complete();
                return emitter;
            }

            String conversationId = request.conversationId();
            if (conversationId == null || conversationId.trim().isEmpty()) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":400,\"message\":\"会话ID不能为空\"}"));
                emitter.complete();
                return emitter;
            }

            var conversation = conversationService.getConversationById(conversationId);
            if (conversation == null) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":404,\"message\":\"会话不存在\"}"));
                emitter.complete();
                return emitter;
            }
            if (!conversation.getUserId().equals(currentUser.getUserId())) {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":403,\"message\":\"无权访问该会话\"}"));
                emitter.complete();
                return emitter;
            }

            log.info("收到流式智能解题请求: userId={}, conversationId={}, problem={}",
                    currentUser.getUserId(), conversationId, request.problem());

            conversationService.sendMessage(conversationId, "user", request.problem(), "text");
            emitter.send(SseEmitter.event().data("{\"type\":\"start\",\"message\":\"开始生成答案...\"}"));

            String prompt = String.format(SOLVE_PROMPT_TEMPLATE, request.problem());
            StringBuilder fullResponse = new StringBuilder();

            Thread.startVirtualThread(() -> {
                try {
                    volcEngineStreamingClient.streamChat(
                            prompt,
                            text -> {
                                try {
                                    fullResponse.append(text);
                                    String safeContent = escapeJson(text);
                                    emitter.send(SseEmitter.event().data("{\"type\":\"text\",\"content\":\"" + safeContent + "\"}"));
                                } catch (IOException e) {
                                    log.error("发送流式响应失败", e);
                                }
                            },
                            () -> {
                                try {
                                    String completeResponse = fullResponse.toString();
                                    conversationService.sendMessage(conversationId, "ai", completeResponse, "text");
                                    emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送完成信号失败", e);
                                }
                            },
                            error -> {
                                try {
                                    String errorMessage = escapeJson(error.getMessage());
                                    emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":500,\"message\":\"AI服务调用失败: " + errorMessage + "\"}"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送错误信号失败", e);
                                }
                            }
                    );
                } catch (Exception e) {
                    log.error("流式解题处理失败", e);
                    try {
                        String errorMessage = escapeJson(e.getMessage());
                        emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":500,\"message\":\"智能解题处理失败: " + errorMessage + "\"}"));
                        emitter.complete();
                    } catch (IOException ex) {
                        log.error("发送异常信号失败", ex);
                    }
                }
            });

        } catch (Exception e) {
            log.error("流式智能解题初始化失败", e);
            try {
                String errorMessage = escapeJson(e.getMessage());
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"code\":500,\"message\":\"智能解题处理失败: " + errorMessage + "\"}"));
                emitter.complete();
            } catch (IOException ex) {
                log.error("发送异常信号失败", ex);
            }
        }

        return emitter;
    }

    private String escapeJson(String content) {
        if (content == null) return "";
        return content.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }

    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> data) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.putAll(data);
            emitter.send(SseEmitter.event().data(new ObjectMapper().writeValueAsString(event)));
        } catch (Exception e) {
            log.error("SSE 发送失败: type={}", type, e);
        }
    }

    public record ProblemRequestDTO(String problem, String conversationId) {}
}
