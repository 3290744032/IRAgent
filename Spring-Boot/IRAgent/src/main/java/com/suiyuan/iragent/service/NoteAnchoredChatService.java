package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dto.Plot3DConfig;
import com.suiyuan.iragent.dto.PlotConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface NoteAnchoredChatService {
    void chat(long userId, String conversationId, String question,
              Consumer<String> onChunk,
              Consumer<List<Map<String, Object>>> onNoteRefs,
              Consumer<PlotConfig> onPlot, Consumer<Plot3DConfig> onPlot3d,
              Runnable onComplete, Consumer<Throwable> onError);
}
