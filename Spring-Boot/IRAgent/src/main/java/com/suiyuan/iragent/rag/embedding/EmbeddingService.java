package com.suiyuan.iragent.rag.embedding;

import java.util.List;

public interface EmbeddingService {

    /**
     * 单个文本转向量
     * @return 向量（float 数组）
     */
    float[] embed(String text);

    /**
     * 批量文本转向量
     */
    List<float[]> embedBatch(List<String> texts);
}
