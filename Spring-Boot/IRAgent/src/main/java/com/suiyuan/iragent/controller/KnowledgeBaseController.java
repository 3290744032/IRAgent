package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.rag.retrieval.PersonalNoteRetriever.NoteFragment;
import com.suiyuan.iragent.service.GraphDataService;
import com.suiyuan.iragent.service.KnowledgeBaseService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v3/kb")
@Tag(name = "知识库", description = "个人笔记上传、检索、管理 — per-user Milvus 向量库")
@SecurityRequirement(name = "TokenAuth")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final GraphDataService graphDataService;

    public KnowledgeBaseController(KnowledgeBaseService kbService, GraphDataService graphDataService) {
        this.kbService = kbService;
        this.graphDataService = graphDataService;
    }

    @Operation(summary = "上传文件笔记", description = "接收文件（txt/md），解析后向量化存入用户专属 Milvus Collection")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) throws IOException {
        var user = UserHolder.getUser();
        var result = kbService.uploadFile(user.getUserId(), file, title);
        return ApiResponse.success(result);
    }

    @Operation(summary = "笔记列表", description = "返回用户笔记列表，支持按科目筛选和分页")
    @GetMapping("/notes")
    public ApiResponse<List<Map<String, Object>>> listNotes(
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var user = UserHolder.getUser();
        var notes = kbService.listNotes(user.getUserId(), subject, page, size);
        return ApiResponse.success(notes);
    }

    @Operation(summary = "笔记详情", description = "返回笔记全文 + 所有分块")
    @GetMapping("/notes/{id}")
    public ApiResponse<Map<String, Object>> getNote(@PathVariable String id) {
        var user = UserHolder.getUser();
        return ApiResponse.success(kbService.getNoteDetail(user.getUserId(), id));
    }

    @Operation(summary = "语义搜索笔记", description = "在用户个人笔记中做向量检索，返回 Top-K 相关片段")
    @PostMapping("/search")
    public ApiResponse<List<NoteFragment>> search(
            @RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        int topK = body.get("topK") instanceof Number n ? n.intValue() : 5;
        var user = UserHolder.getUser();
        var results = kbService.searchNotes(user.getUserId(), query, topK);
        return ApiResponse.success(results);
    }

    @Operation(summary = "编辑笔记", description = "修改标题/内容/科目/章节/标签")
    @PutMapping("/notes/{id}")
    public ApiResponse<Map<String, Object>> updateNote(@PathVariable String id,
                                                        @RequestBody Map<String, String> body) {
        var user = UserHolder.getUser();
        kbService.updateNote(user.getUserId(), id, body);
        return ApiResponse.success(Map.of("updated", true));
    }

    @Operation(summary = "删除笔记", description = "删除指定笔记及其所有分块（PG），Milvus 向量异步清理")
    @DeleteMapping("/notes/{id}")
    public ApiResponse<Map<String, Object>> deleteNote(@PathVariable String id) {
        var user = UserHolder.getUser();
        kbService.deleteNote(user.getUserId(), id);
        return ApiResponse.success(Map.of("deleted", true));
    }

    @Operation(summary = "知识图谱数据", description = "返回考点→笔记→题目三元关系图谱 JSON，供 ECharts 渲染")
    @GetMapping("/graph-data")
    public ApiResponse<Map<String, Object>> getGraphData() {
        var user = UserHolder.getUser();
        return ApiResponse.success(graphDataService.getGraphData(user.getUserId()));
    }

    @Operation(summary = "AI 优化笔记", description = "用 AI 美化笔记格式（Markdown 排版优化）")
    @PostMapping("/notes/{id}/optimize")
    public ApiResponse<Map<String, Object>> optimizeNote(@PathVariable String id,
                                                          @RequestBody Map<String, String> body) {
        var user = UserHolder.getUser();
        String instruction = body != null ? body.getOrDefault("instruction", "美化排版，统一格式") : "美化排版，统一格式";
        var result = kbService.optimizeNote(user.getUserId(), id, instruction);
        return ApiResponse.success(result);
    }
}
