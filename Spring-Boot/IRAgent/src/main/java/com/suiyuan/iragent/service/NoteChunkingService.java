package com.suiyuan.iragent.service;

import java.util.List;

public interface NoteChunkingService {
    record Chunk(String knowledgePoint, String content) {}

    List<Chunk> chunk(String content);
}
