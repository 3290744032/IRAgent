package com.suiyuan.iragent.config;

/**
 * 模拟ChatClient实现
 * 用于替代Spring AI的ChatClient，提供基本的聊天功能
 */
public class MockChatClient {

    /**
     * 模拟AI聊天响应
     * @param message 用户消息
     * @return AI回复
     */
    public String chat(String message) {
        // 简单的模拟回复逻辑
        if (message.contains("数学") || message.contains("方程") || message.contains("计算")) {
            return "我是智研Agent，专注于数学解题。\n\n" +
                    "对于数学问题，我可以提供详细的解题步骤和讲解。\n" +
                    "请提供具体的数学题目，我会为你解答。";
        } else if (message.contains("英语") || message.contains("阅读") || message.contains("语法")) {
            return "我是智研Agent，专注于英语学习。\n\n" +
                    "对于英语问题，我可以提供阅读理解、语法解析和词汇讲解。\n" +
                    "请提供具体的英语题目，我会为你解答。";
        } else if (message.contains("你好") || message.contains("Hello")) {
            return "你好！我是智研Agent，基于多智能体的AI精准解题与讲解系统。\n\n" +
                    "我可以帮助你解决数学、英语等学科的问题，提供详细的解题步骤和讲解。\n" +
                    "请问有什么我可以帮助你的吗？";
        } else {
            return "我是智研Agent，基于多智能体的AI精准解题与讲解系统。\n\n" +
                    "我可以帮助你解决数学、英语等学科的问题，提供详细的解题步骤和讲解。\n" +
                    "请提供具体的题目，我会为你解答。";
        }
    }

    /**
     * 模拟流式聊天响应
     * @param message 用户消息
     * @return 流式回复的字符串数组
     */
    public String[] streamChat(String message) {
        String response = chat(message);
        // 简单的流式模拟，将回复分割成多个片段
        return response.split("\\n");
    }
}