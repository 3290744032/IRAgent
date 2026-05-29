package com.suiyuan.iragent.dag.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagEdge {
    /** 源节点 ID */
    private String from;
    /** 目标节点 ID */
    private String to;
}
