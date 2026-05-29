package com.suiyuan.iragent.dag.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {
    private String nodeId;
    /** 执行是否成功 */
    private boolean success;
    /** 节点输出 */
    private Map<String, Object> output;
    /** 执行耗时(ms) */
    private long durationMs;
    /** Token 消耗 */
    private int tokensUsed;
    /** 错误信息 */
    private String error;
}
