package com.suiyuan.iragent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PromptTemplateManager {

    private static final String TEACH_TEMPLATE = """
        你是一位经验丰富的学科老师，擅长提供图文结合的讲解。

        请根据用户的问题，拆解出5-8个知识点的讲解步骤。

        问题：{{question}}
        学科类型：{{subjectType}}

        要求：
        1. 讲解内容要图文结合，图是最重要的！能用图说明的尽量用图
        2. 讲解结构要清晰：
           - 【解答】简洁的解题过程，像写在答题卷上那样，不啰嗦
           - 【解析】详细的分析，包含：
             * 题目考点分析
             * 解题思路和技巧
             * 注意事项和常见错误
           - 【图示】相关图形，放在最后
        3. 【解答】部分要简洁，可以直接写在答题卷上
        4. 【解析】部分用来补充说明、讲解知识点
        5. 支持LaTeX数学公式，使用 \\(...\\) 或 \\[...\\] 格式
        6. 【PLOT】或【PLOT3D】标签块放在最后，紧跟在【图示】部分

        请严格按以下JSON格式返回，不要添加任何额外说明：
        {
            "topic": "学习主题名称",
            "steps": [
                {
                    "title": "知识点1标题",
                    "content": "## 知识点1标题\\n\\n### 解答\\n\\n简洁的解题过程\\n\\n### 解析\\n\\n详细的考点分析和技巧说明\\n\\n### 图示\\n\\n【PLOT】或【PLOT3D】图形"
                },
                {
                    "title": "知识点2标题",
                    "content": "## 知识点2标题\\n\\n### 解答\\n\\n简洁的解题过程\\n\\n### 解析\\n\\n详细的考点分析和技巧说明\\n\\n### 图示\\n\\n【PLOT】或【PLOT3D】图形"
                }
            ]
        }
        """;

    private static final String ANSWER_TEMPLATE = """
        你是一位耐心的学科老师。用户正在学习以下知识点，请以图文结合的方式解答。

        用户问题：{{userQuestion}}

        当前知识点：
        标题：{{context}}
        内容：{{teachingContent}}

        要求：
        1. 图文结合，图是最重要的！能用图说明的尽量用图
        2. 回答结构要清晰：
           - 【解答】简洁的解题过程，像写在答题卷上那样，不啰嗦
           - 【解析】详细的分析，包含：
             * 题目考点分析
             * 解题思路和技巧
             * 注意事项和常见错误
           - 【图示】相关图形，放在最后
        3. 【解答】部分要简洁，可以直接写在答题卷上
        4. 【解析】部分用来补充说明、讲解知识点
        5. 支持LaTeX数学公式
        6. 【PLOT】或【PLOT3D】标签块放在最后，紧跟在【图示】部分

        请直接返回Markdown格式的回答内容，不要添加任何额外说明。
        """; 

    private static final String SUMMARY_TEMPLATE = """
        请为用户的学习会话生成一份总结报告。

        原始问题：{{question}}
        完成步骤数：{{completedSteps}}
        学习时间：{{totalTime}}

        要求：
        1. 生成知识图谱，展示知识点之间的关联
        2. 总结学习掌握情况
        3. 给出后续学习建议

        请严格按以下JSON格式返回：
        {
            "knowledgeGraph": {
                "核心知识点": ["相关知识点1", "相关知识点2"]
            },
            "masterySummary": {
                "已掌握": ["知识点1", "知识点2"],
                "需要加强": ["知识点3"]
            },
            "recommendations": [
                "建议1：推荐学习XXX",
                "建议2：多做练习巩固XXX"
            ]
        }
        """;

    public String getTemplate(String key) {
        return switch (key) {
            case "TEACH" -> TEACH_TEMPLATE;
            case "ANSWER" -> ANSWER_TEMPLATE;
            case "SUMMARY" -> SUMMARY_TEMPLATE;
            default -> "";
        };
    }

    public String render(String key, Object... args) {
        String template = getTemplate(key);
        if (template.isEmpty()) {
            return "";
        }
        return String.format(template, args);
    }

    public String renderWithMap(String key, Map<String, String> variables) {
        String template = getTemplate(key);
        if (template.isEmpty()) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
