package com.suiyuan.iragent_app.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * P1 测试工具：模拟后端 SSE 推送 JSON segment
 *
 * 用法（在 StudyFragment/DeepLearnFragment 中）：
 *   MockSseSource mock = new MockSseSource(callback);
 *   mock.start(); // 开始推送 mock 数据
 *
 * 回调类型与 SseParser.Callback 一致：
 *   "start" → onMessage("start", "...", null)
 *   "text"  → onMessage("text", "内容", null)
 *   "plot"  → onMessage("plot", null, "f(x)=x^2")
 *   "plot3d" → onMessage("plot3d", "{\"expr\":...}", null)
 *   "done"  → onMessage("done", "...", null)
 */
public class MockSseSource {

    private static final String TAG = "MockSseSource";
    private static final long INTERVAL_MS = 600;

    private final SseParser.Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> events = new ArrayList<>();
    private int index = 0;

    public MockSseSource(SseParser.Callback callback) {
        this.callback = callback;
        buildMockSequence();
    }

    private void buildMockSequence() {
        events.add(() -> callback.onMessage("start", "start", null));

        events.add(() -> callback.onMessage("text",
                "首先，我们来看一元二次方程的标准形式：$ax^2+bx+c=0$", null));

        events.add(() -> callback.onMessage("text",
                "其中 $a \\neq 0$。求根公式为：$x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$", null));

        // 2D plot — 单个表达式
        events.add(() -> callback.onMessage("plot", null, "f(x)=x^2-2x+1"));

        events.add(() -> callback.onMessage("text",
                "函数 $f(x)=x^2-2x+1$ 的图像如上所示。顶点坐标为 $(1,0)$。", null));

        // 2D plot — 多条曲线
        events.add(() -> callback.onMessage("plot", null, "f(x)=sin(x)"));
        events.add(() -> callback.onMessage("plot", null, "g(x)=cos(x)"));

        events.add(() -> callback.onMessage("text",
                "正弦和余弦函数的周期为 $2\\pi$。", null));

        // 3D plot
        events.add(() -> callback.onMessage("plot3d",
                "{\"expr\":\"sin(sqrt(x^2+y^2))\",\"range\":[[-6,6],[-6,6]],\"samples\":80}", null));

        events.add(() -> callback.onMessage("text",
                "这是三维曲面 $z = \\sin(\\sqrt{x^2+y^2})$ 的图像。", null));

        events.add(() -> callback.onMessage("done", "done", null));
    }

    public void start() {
        Log.d(TAG, "=== Mock SSE 开始推送 (" + events.size() + " 条事件) ===");
        index = 0;
        pushNext();
    }

    private void pushNext() {
        if (index >= events.size()) {
            Log.d(TAG, "=== Mock SSE 推送完成 ===");
            return;
        }
        Runnable event = events.get(index);
        index++;

        handler.postDelayed(() -> {
            Log.d(TAG, "Mock event " + index + "/" + events.size());
            event.run();
            pushNext();
        }, INTERVAL_MS);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * P3 全序列联调 Mock：二次函数顶点式完整课堂教学演示
     *
     * 动作类型一览（可与后端对接）：
     *   title          - 课程标题（居中大标题）
     *   show_grid      - 显示坐标网格
     *   write_text     - 逐字书写文本（支持 $LaTeX$）
     *   write_formula  - 居中显示 LaTeX 公式
     *   draw_graph     - 绘制函数曲线（expr 为 mathjs 表达式）
     *   annotate       - 在坐标上标注文字（coord:[x,y] 或 target:"vertex"）
     *   highlight      - 高亮关键点
     *   draw_step      - 推导步骤（step:编号, text:内容）
     *   example        - 例题框（text/latex）
     *   summary        - 总结框（text）
     *   clear_overlay  - 仅清除板书文字（保留网格和曲线）
     *   clear_board    - 清除全部（标题+文字+曲线+网格）
     *
     *  所有 audioTrigger:true 的动作需要 text 字段给 TTS 朗读
     */
    public static String buildTimelineJson() {
        return "{"
            + "\"lessonTitle\":\"\u4e8c\u6b21\u51fd\u6570\u9876\u70b9\u5f0f\","
            + "\"topic\":\"y = a(x-h)\u00b2 + k \u7684\u56fe\u50cf\u4e0e\u6027\u8d28\","
            + "\"durationSeconds\":44,"
            + "\"viewBox\":{\"xRange\":6,\"yRange\":4},"
            + "\"timeline\":["
            // 1. 课程标题
            + "{\"id\":\"a0\",\"time\":0.0,\"action\":\"title\",\"text\":\"**\u4e8c\u6b21\u51fd\u6570 y = a(x-h)\u00b2 + k**\",\"duration\":1.8,\"audioTrigger\":true,\"tts\":\"\u4e8c\u6b21\u51fd\u6570 y = a(x-h)\u7684\u5e73\u65b9 + k\"},"
            // 2. 显示坐标系
            + "{\"id\":\"a1\",\"time\":2.0,\"action\":\"show_grid\",\"duration\":0.8},"
            // 3. 板书标准形式 (text 保留数学符号, tts 给口语化文本)
            + "{\"id\":\"a2\",\"time\":3.0,\"action\":\"write_text\",\"text\":\"\u6807\u51c6\u5f62\u5f0f\uff1a**y = a(x-h)\u00b2 + k**\uff0c\u5176\u4e2d **a \u2260 0**\",\"duration\":3.0,\"audioTrigger\":true,\"tts\":\"\u6807\u51c6\u5f62\u5f0f\uff1ay = a(x-h)\u7684\u5e73\u65b9 + k\uff0c\u5176\u4e2d a \u4e0d\u7b49\u4e8e 0\"},"
            // 4. 展示公式
            + "{\"id\":\"a3\",\"time\":6.0,\"action\":\"write_formula\",\"latex\":\"y = a(x-h)^2 + k\",\"duration\":2.0,\"audioTrigger\":false},"
            // 5. 画基础图 y = x²
            + "{\"id\":\"a4\",\"time\":8.0,\"action\":\"draw_graph\",\"expr\":\"x^2\",\"duration\":2.5,\"audioTrigger\":true,\"text\":\"\u5148\u770b\u57fa\u7840\u51fd\u6570 **y = x\u00b2**\uff0c\u9876\u70b9\u5728\u539f\u70b9 **(0,0)**\",\"tts\":\"\u5148\u770b\u57fa\u7840\u51fd\u6570 y = x\u7684\u5e73\u65b9\uff0c\u9876\u70b9\u5728\u539f\u70b9 (0,0)\"},"
            // 6. 标注顶点 (0,0)
            + "{\"id\":\"a5\",\"time\":10.5,\"action\":\"annotate\",\"coord\":[0,0],\"label\":\"(0,0)\",\"duration\":1.0,\"audioTrigger\":false},"
            // 7. 画 y = (x-1)² | h > 0 右移
            + "{\"id\":\"a6\",\"time\":12.0,\"action\":\"write_text\",\"text\":\"\u5f53 **h = 1** \u65f6\uff0c\u56fe\u50cf\u5411\u53f3\u5e73\u79fb 1 \u4e2a\u5355\u4f4d\",\"duration\":2.5,\"audioTrigger\":false},"
            + "{\"id\":\"a7\",\"time\":14.5,\"action\":\"draw_graph\",\"expr\":\"(x-1)^2\",\"duration\":2.5,\"audioTrigger\":false},"
            + "{\"id\":\"a8\",\"time\":17.0,\"action\":\"annotate\",\"target\":\"vertex\",\"expr\":\"(x-1)^2\",\"label\":\"(1,0)\",\"duration\":1.0,\"audioTrigger\":true,\"text\":\"\u9876\u70b9\u53d8\u4e3a **(1,0)**\uff0c\u56fe\u50cf\u5411\u53f3\u79fb\u52a8\u4e86\u4e00\u4e2a\u5355\u4f4d\",\"tts\":\"\u9876\u70b9\u53d8\u4e3a (1,0)\uff0c\u56fe\u50cf\u5411\u53f3\u79fb\u52a8\u4e86\u4e00\u4e2a\u5355\u4f4d\"},"
            // 9. 画 y = (x+1)² | h < 0 左移
            + "{\"id\":\"a9\",\"time\":18.5,\"action\":\"write_text\",\"text\":\"\u5f53 **h = -1** \u65f6\uff0c\u56fe\u50cf\u5411\u5de6\u5e73\u79fb 1 \u4e2a\u5355\u4f4d\",\"duration\":2.5,\"audioTrigger\":false},"
            + "{\"id\":\"a10\",\"time\":21.0,\"action\":\"draw_graph\",\"expr\":\"(x+1)^2\",\"duration\":2.5,\"audioTrigger\":false},"
            + "{\"id\":\"a11\",\"time\":23.5,\"action\":\"annotate\",\"target\":\"vertex\",\"expr\":\"(x+1)^2\",\"label\":\"(-1,0)\",\"duration\":1.0,\"audioTrigger\":true,\"text\":\"\u9876\u70b9\u53d8\u4e3a **(-1,0)**\uff0c\u56fe\u50cf\u5411\u5de6\u79fb\u52a8\u4e86\u4e00\u4e2a\u5355\u4f4d\",\"tts\":\"\u9876\u70b9\u53d8\u4e3a (-1,0)\uff0c\u56fe\u50cf\u5411\u5de6\u79fb\u52a8\u4e86\u4e00\u4e2a\u5355\u4f4d\"},"
            // 10. 清空板书，进入推导环节
            + "{\"id\":\"a12\",\"time\":25.0,\"action\":\"clear_overlay\",\"duration\":0.5},"
            // 11. 推导步骤
            + "{\"id\":\"a13\",\"time\":26.0,\"action\":\"draw_step\",\"step\":1,\"text\":\"\u786e\u5b9a\u9876\u70b9\u5750\u6807 **(h, k)**\",\"duration\":1.5,\"audioTrigger\":true,\"tts\":\"\u7b2c\u4e00\u6b65\u786e\u5b9a\u9876\u70b9\u5750\u6807 (h,k)\"},"
            + "{\"id\":\"a14\",\"time\":27.5,\"action\":\"draw_step\",\"step\":2,\"text\":\"\u6839\u636e **a** \u7684\u6b63\u8d1f\u5224\u65ad\u5f00\u53e3\u65b9\u5411\uff1a**a > 0** \u5411\u4e0a\uff0c**a < 0** \u5411\u4e0b\",\"duration\":2.5,\"audioTrigger\":true,\"tts\":\"\u7b2c\u4e8c\u6b65\u6839\u636e a \u7684\u6b63\u8d1f\u5224\u65ad\u5f00\u53e3\u65b9\u5411\uff0c a \u5927\u4e8e 0 \u5411\u4e0a\uff0c a \u5c0f\u4e8e 0 \u5411\u4e0b\"},"
            + "{\"id\":\"a15\",\"time\":30.5,\"action\":\"draw_step\",\"step\":3,\"text\":\"\u753b\u5bf9\u79f0\u8f74 **x = h**\uff0c\u63cf\u70b9\u8fde\u7ebf\u5b8c\u6210\u56fe\u50cf\",\"duration\":2.0,\"audioTrigger\":true,\"tts\":\"\u7b2c\u4e09\u6b65\u753b\u5bf9\u79f0\u8f74 x = h\uff0c\u63cf\u70b9\u8fde\u7ebf\u5b8c\u6210\u56fe\u50cf\"},"
            // 12. 例题
            + "{\"id\":\"a16\",\"time\":33.0,\"action\":\"example\",\"text\":\"\u6c42\u51fd\u6570 **y = 2(x-3)\u00b2 + 1** \u7684\u9876\u70b9\u5750\u6807\u548c\u5f00\u53e3\u65b9\u5411\",\"latex\":\"y = 2(x-3)^2 + 1\",\"duration\":3.5,\"audioTrigger\":true,\"tts\":\"\u6c42\u51fd\u6570 y = 2(x-3)\u7684\u5e73\u65b9 + 1 \u7684\u9876\u70b9\u5750\u6807\u548c\u5f00\u53e3\u65b9\u5411\"},"
            // 13. 画例题函数图 + 标注顶点
            + "{\"id\":\"a17\",\"time\":36.5,\"action\":\"draw_graph\",\"expr\":\"2*(x-3)^2+1\",\"duration\":2.5},"
            + "{\"id\":\"a18\",\"time\":39.0,\"action\":\"annotate\",\"coord\":[3,1],\"label\":\"(3,1)\",\"duration\":1.0},"
            // 14. 总结
            + "{\"id\":\"a19\",\"time\":40.5,\"action\":\"summary\",\"text\":\"**a** \u63a7\u5236\u5f00\u53e3\u65b9\u5411\u548c\u5927\u5c0f\uff0c**h** \u63a7\u5236\u5de6\u53f3\u5e73\u79fb\uff0c**k** \u63a7\u5236\u4e0a\u4e0b\u5e73\u79fb\",\"duration\":3.5,\"audioTrigger\":true,\"tts\":\"\u603b\u7ed3\u4e00\u4e0b\uff0ca \u63a7\u5236\u5f00\u53e3\u65b9\u5411\u548c\u5927\u5c0f\uff0ch \u63a7\u5236\u5de6\u53f3\u5e73\u79fb\uff0ck \u63a7\u5236\u4e0a\u4e0b\u5e73\u79fb\"}"
            + "]}";
    }
}
