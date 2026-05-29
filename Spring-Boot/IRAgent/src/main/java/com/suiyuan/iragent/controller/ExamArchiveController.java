package com.suiyuan.iragent.controller;

import com.suiyuan.iragent.config.VolcEngineStreamingClient;
import com.suiyuan.iragent.service.ExamArchiveService;
import com.suiyuan.iragent.utils.ApiResponse;
import com.suiyuan.iragent.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v3/exam-archive")
@Tag(name = "真题库", description = "学生上传试卷 + AI 组卷，个人题库管理")
@SecurityRequirement(name = "TokenAuth")
public class ExamArchiveController {

    private final ExamArchiveService service;
    private final VolcEngineStreamingClient streamingClient;

    public ExamArchiveController(ExamArchiveService service, VolcEngineStreamingClient streamingClient) {
        this.service = service;
        this.streamingClient = streamingClient;
    }

    @Operation(summary = "真题列表")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String knowledgePoint,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listQuestions(subject, year, examType, knowledgePoint, difficulty, page, size));
    }

    @Operation(summary = "筛选选项")
    @GetMapping("/filters")
    public ApiResponse<Map<String, Object>> filters() {
        return ApiResponse.success(service.getFilters());
    }

    @Operation(summary = "AI 真题模拟", description = "AI 生成符合指定考纲风格的模拟题，覆盖薄弱考点")
    @PostMapping("/simulate")
    public ApiResponse<List<Map<String, Object>>> simulate(@RequestBody Map<String, Object> body) {
        String subject = (String) body.getOrDefault("subject", "数学");
        String examType = (String) body.get("examType");
        int count = body.get("count") instanceof Number n ? n.intValue() : 10;
        return ApiResponse.success(service.simulateQuestions(subject, examType, count));
    }

    @Operation(summary = "上传试卷（学生自传）",
            description = "拍照/PDF 上传试卷，豆包多模态 OCR 识别题目，自动入库个人题库")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "数学") String subject) {
        var user = UserHolder.getUser();
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String prompt = "识别试卷中每道题，输出严格JSON数组（不要任何其他文字）：[{\"questionText\":\"题目\",\"questionType\":\"single_choice|fill_blank|calculation\",\"options\":[],\"correctAnswer\":\"答案\",\"knowledgePoint\":\"考点\",\"difficulty\":3}]";
            java.util.concurrent.atomic.AtomicReference<String> ref = new java.util.concurrent.atomic.AtomicReference<>("");
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            streamingClient.streamChatWithImage(prompt, base64,
                    t -> ref.updateAndGet(s -> s + t),
                    latch::countDown, e -> latch.countDown());
            latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
            return ApiResponse.success(service.ingestQuestions(ref.get(), subject, user.getUserId()));
        } catch (Exception e) {
            return ApiResponse.success(Map.of("ingested", 0, "error", e.getMessage()));
        }
    }
}
