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
public class DagNode {
    private String id;
    private NodeType type;
    /** 节点描述 */
    private String description;
    /** 依赖的前置节点 ID 列表 */
    private java.util.List<String> dependsOn;
    /** 节点配置参数 */
    private Map<String, Object> config;
}
