package com.suiyuan.iragent_app.ui.screens.study;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.ConversationEntity;
import com.suiyuan.iragent_app.data.model.Conversation;
import com.suiyuan.iragent_app.data.model.Message;
import com.suiyuan.iragent_app.data.model.v3.NoteRef;
import com.suiyuan.iragent_app.data.repository.v3.ConversationRepositoryV3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyFragmentV3 extends Fragment {

    private StudyViewModelV3 viewModel;
    private LinearLayout llMessages;
    private ScrollView svMessages;
    private EditText etInput;
    private TextView ivSend, ivAttach, tvHistory, tvTitle, ivNewConversation;

    private View mStreamingContainer;
    private WebView mStreamingWebView;
    private String mCurrentFullText = "";
    private String mLastRenderedText = "";
    private boolean mIsUserScrolling = false;
    private boolean mIsStreaming = false;
    private boolean mWebViewReady = false;
    private boolean mHasCompletedStreaming = false;
    private String mMathTemplate;

    private ConversationRepositoryV3 conversationRepoV3;

    private static final long RENDER_INTERVAL_MS = 200;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRenderRunnable = this::doSafeRender;

    // File picker launchers
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) chatWithImage(uri);
            });
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleFilePicked(uri, "文件");
            });

    // Camera launcher
    private Uri cameraPhotoUri;
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null) {
                    chatWithImage(cameraPhotoUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_study_v3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StudyViewModelV3.class);
        conversationRepoV3 = new ConversationRepositoryV3();

        llMessages = view.findViewById(R.id.ll_messages);
        svMessages = view.findViewById(R.id.sv_messages);
        etInput = view.findViewById(R.id.et_input);
        ivSend = view.findViewById(R.id.iv_send);
        ivAttach = view.findViewById(R.id.iv_attach);
        tvHistory = view.findViewById(R.id.tv_history);
        tvTitle = view.findViewById(R.id.tv_title);
        ivNewConversation = view.findViewById(R.id.iv_new_conversation);

        loadMathTemplate();
        setupListeners(view);
        setupScrollListener();
        setupObservers();

        String convId = getArguments() != null ? getArguments().getString("conversation_id", "") : "";
        viewModel.setConversationId(convId);
        if (!convId.isEmpty()) {
            loadConversationMessages(convId);
        } else {
            showWelcomeMessage();
        }
    }

    private void loadMathTemplate() {
        try {
            InputStream is = requireContext().getAssets().open("math_template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            mMathTemplate = sb.toString();
            reader.close();
            is.close();
        } catch (IOException e) {
            mMathTemplate = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body><div id='content'></div></body></html>";
        }
    }

    private void setupListeners(View view) {
        ivNewConversation.setOnClickListener(v -> {
            viewModel.startNewConversation();
            llMessages.removeAllViews();
            showWelcomeMessage();
        });

        ivSend.setOnClickListener(v -> sendMessage());

        ivAttach.setOnClickListener(v -> showAttachSheet());

        tvHistory.setOnClickListener(v -> showConversationHistory());

        view.findViewById(R.id.tv_deep_learn).setOnClickListener(v -> {
            NavController nc = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("topic", etInput.getText().toString().trim());
            nc.navigate(R.id.nav_deeplearn, args);
        });

        view.findViewById(R.id.tv_video_lesson).setOnClickListener(v -> {
            NavController nc = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("topic", etInput.getText().toString().trim());
            nc.navigate(R.id.nav_video, args);
        });

    }

    private void showConversationHistory() {
        conversationRepoV3.getConversations(1, 50, new ConversationRepositoryV3.ResultCallback<java.util.List<Conversation>>() {
            @Override
            public void onSuccess(java.util.List<Conversation> data) {
                if (isAdded()) showHistoryBottomSheet(data);
            }

            @Override
            public void onError(int code, String message) {
                if (isAdded()) Toast.makeText(getContext(), "加载失败: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onException(Exception e) {
                if (isAdded()) Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showHistoryBottomSheet(java.util.List<com.suiyuan.iragent_app.data.model.Conversation> conversations) {
        LinearLayout sheet = new LinearLayout(requireContext());
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(0, 24, 0, 48);

        TextView title = new TextView(requireContext());
        title.setText("历史对话");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#1F2937"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        sheet.addView(title);

        final com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        if (conversations == null || conversations.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂无历史对话");
            empty.setTextSize(14);
            empty.setTextColor(Color.parseColor("#9CA3AF"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, 32, 0, 32);
            sheet.addView(empty);
        } else {
            for (com.suiyuan.iragent_app.data.model.Conversation c : conversations) {
                LinearLayout item = new LinearLayout(requireContext());
                item.setOrientation(LinearLayout.HORIZONTAL);
                item.setPadding(16, 12, 16, 12);
                item.setGravity(Gravity.CENTER_VERTICAL);

                View dot = new View(requireContext());
                dot.setLayoutParams(new LinearLayout.LayoutParams(10, 10));
                dot.setBackgroundResource(R.drawable.bg_dot_blue);
                item.addView(dot);

                LinearLayout info = new LinearLayout(requireContext());
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(12, 0, 0, 0);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                info.setLayoutParams(ip);

                TextView name = new TextView(requireContext());
                name.setText(c.getName() != null ? c.getName() : "对话");
                name.setTextSize(14);
                name.setTextColor(Color.parseColor("#1F2937"));
                info.addView(name);

                TextView desc = new TextView(requireContext());
                desc.setText(c.getDescription() != null ? c.getDescription() : "");
                desc.setTextSize(11);
                desc.setTextColor(Color.parseColor("#9CA3AF"));
                info.addView(desc);

                item.addView(info);
                final String convId = c.getConversationId();
                item.setOnClickListener(v -> {
                    dialog.dismiss();
                    viewModel.setConversationId(convId);
                    loadConversationMessages(convId);
                });

                sheet.addView(item);
            }
        }

        dialog.setContentView(sheet);
        dialog.show();
    }

    private void loadConversationMessages(String conversationId) {
        conversationRepoV3.getMessages(conversationId, new ConversationRepositoryV3.ResultCallback<java.util.List<Message>>() {
            @Override
            public void onSuccess(java.util.List<Message> messages) {
                if (!isAdded()) return;
                llMessages.removeAllViews();
                for (Message msg : messages) {
                    if ("user".equals(msg.getSenderType())) {
                        addUserBubble(msg.getContent());
                    } else {
                        addAiBubble(msg.getContent());
                    }
                }
                Toast.makeText(getContext(), "已加载历史消息", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int code, String message) {
                if (isAdded()) Toast.makeText(getContext(), "加载消息失败: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onException(Exception e) {
                if (isAdded()) Toast.makeText(getContext(), "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========== Attach Sheet ==========

    private void showAttachSheet() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog);
        View sheet = LayoutInflater.from(getContext()).inflate(R.layout.dialog_attach_sheet, null);
        dialog.setContentView(sheet);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setWindowAnimations(android.R.style.Animation_InputMethod);
        }

        sheet.findViewById(R.id.ll_attach_camera).setOnClickListener(v -> {
            dialog.dismiss();
            launchCamera();
        });

        sheet.findViewById(R.id.ll_attach_gallery).setOnClickListener(v -> {
            dialog.dismiss();
            imagePickerLauncher.launch("image/*");
        });

        sheet.findViewById(R.id.ll_attach_file).setOnClickListener(v -> {
            dialog.dismiss();
            filePickerLauncher.launch("*/*");
        });

        dialog.show();
    }

    // ========== Camera ==========

    private void launchCamera() {
        try {
            File photoFile = createTempImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法启动相机：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File cacheDir = requireContext().getCacheDir();
        return File.createTempFile("IMG_" + timeStamp + "_", ".jpg", cacheDir);
    }

    // ========== Image Chat ==========

    private void chatWithImage(Uri uri) {
        if (uri == null || mIsStreaming) return;
        mHasCompletedStreaming = false;
        addUserBubble("📷 图片已发送，正在解答...");
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) { addErrorMessage("无法读取图片"); return; }
            viewModel.chatWithImage(is, "请帮我解答这道题");
        } catch (Exception e) {
            addErrorMessage("读取图片失败: " + e.getMessage());
        }
    }

    // ========== File Handler ==========

    private void handleFilePicked(Uri uri, String type) {
        if (uri == null) return;
        String preview = "[📎 " + type + "已选择，正在上传...]";
        addUserBubble(preview);

        // Actually upload the file
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is == null) { addErrorMessage("无法读取文件"); return; }
            java.io.File cacheDir = requireContext().getCacheDir();
            java.io.File tempFile = new java.io.File(cacheDir, "upload_" + System.currentTimeMillis());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            byte[] buf = new byte[4096];
            int read;
            while ((read = is.read(buf)) != -1) fos.write(buf, 0, read);
            fos.close(); is.close();

            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(tempFile,
                    okhttp3.MediaType.parse("application/octet-stream"));
            okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("file", "upload", reqFile);
            okhttp3.RequestBody titlePart = okhttp3.RequestBody.create("", okhttp3.MediaType.parse("text/plain"));

            com.suiyuan.iragent_app.data.repository.v3.KnowledgeRepository repo =
                    new com.suiyuan.iragent_app.data.repository.v3.KnowledgeRepository(
                            com.suiyuan.iragent_app.data.remote.v3.NetworkClientV3.getApiService());
            repo.uploadNote(part, titlePart, new com.suiyuan.iragent_app.data.repository.v3.KnowledgeRepository.ResultCallback<com.suiyuan.iragent_app.data.model.v3.UploadResult>() {
                @Override
                public void onSuccess(com.suiyuan.iragent_app.data.model.v3.UploadResult data) {
                    addAiBubble("✅ 文件已上传，已解析 " + data.getChunkCount() + " 个知识点。现在你可以针对笔记内容提问了！");
                }
                @Override
                public void onError(int code, String msg) { addErrorMessage("上传失败: " + msg); }
                @Override
                public void onException(Exception e) { addErrorMessage("上传异常: " + e.getMessage()); }
            });
        } catch (Exception e) {
            addErrorMessage("读取文件失败: " + e.getMessage());
        }
    }

    private void setupScrollListener() {
        svMessages.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    mIsUserScrolling = true;
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    mIsUserScrolling = false;
                    break;
            }
            return false;
        });
    }

    private void setupObservers() {
        viewModel.getStreamStart().observe(getViewLifecycleOwner(), started -> {
            if (started != null && started) startStreamingMessage();
        });
        viewModel.getStreamText().observe(getViewLifecycleOwner(), this::appendStreamingText);
        viewModel.getStreamNoteRefs().observe(getViewLifecycleOwner(), this::addNoteRefCards);
        viewModel.getStreamDone().observe(getViewLifecycleOwner(), done -> {
            if (done != null && done) finishStreamingMessage();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                finishStreamingMessage();
                addErrorMessage(error);
                viewModel.clearError();
            }
        });
        viewModel.getConversationTitle().observe(getViewLifecycleOwner(), title -> {
            if (tvTitle != null && title != null) tvTitle.setText(title);
        });
        viewModel.getCompletedAiResponse().observe(getViewLifecycleOwner(), response -> {
            if (response != null && !response.isEmpty()) {
                Boolean isStart = viewModel.getStreamStart().getValue();
                Boolean isDone = viewModel.getStreamDone().getValue();
                boolean streamingConsumed = (isStart == null || !isStart) && (isDone == null || !isDone);
                if (streamingConsumed && !mHasCompletedStreaming) {
                    addAiBubble(response);
                }
            }
        });
    }

    private void showWelcomeMessage() {
        addAiBubble("你好！我是你的 AI 备考助手。\n\n我已经学习了你的笔记，可以直接向我提问，我会结合你的笔记来解答。");
    }

    private void sendMessage() {
        String message = etInput.getText().toString().trim();
        if (message.isEmpty() || mIsStreaming) return;
        mHasCompletedStreaming = false;
        addUserBubble(message);
        etInput.setText("");
        viewModel.chat(message);
    }

    // ========== Message Bubbles ==========

    private void addUserBubble(String text) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        TextView avatar = new TextView(requireContext());
        avatar.setText("我");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(12);
        avatar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(36, 36);
        avp.setMargins(12, 2, 0, 0);
        avatar.setLayoutParams(avp);
        avatar.setBackgroundResource(R.drawable.bg_btn_primary);

        TextView bubble = new TextView(requireContext());
        bubble.setText(text);
        bubble.setTextSize(14);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(24, 14, 24, 14);
        bubble.setBackgroundResource(R.drawable.bg_user_bubble);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 0, 12, 0);
        bubble.setLayoutParams(bp);
        bubble.setMaxWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.75));

        row.addView(bubble);
        row.addView(avatar);
        llMessages.addView(row);
        scrollToBottom();
    }

    private void addAiBubble(String text) {
        // Extract PLOT/PLOT3D blocks before rendering text
        List<PlotBlock> plotBlocks = new java.util.ArrayList<>();
        String cleanText = text;

        // Extract 【PLOT】...【END】 blocks
        Pattern plotPattern = Pattern.compile("【PLOT】\\s*(.*?)【END】", Pattern.DOTALL);
        Matcher pm = plotPattern.matcher(cleanText);
        while (pm.find()) {
            plotBlocks.add(new PlotBlock("plot", pm.group(1).trim()));
        }
        cleanText = cleanText.replaceAll("(?s)【PLOT】\\s*.*?【END】", "").trim();

        // Extract 【PLOT3D】...【END】 blocks
        Pattern plot3dPattern = Pattern.compile("【PLOT3D】\\s*(.*?)【END】", Pattern.DOTALL);
        Matcher p3m = plot3dPattern.matcher(cleanText);
        while (p3m.find()) {
            plotBlocks.add(new PlotBlock("plot3d", p3m.group(1).trim()));
        }
        cleanText = cleanText.replaceAll("(?s)【PLOT3D】\\s*.*?【END】", "").trim();

        if (cleanText.isEmpty() && plotBlocks.isEmpty()) return;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        // Bubble row with avatar
        LinearLayout bubbleRow = new LinearLayout(requireContext());
        bubbleRow.setOrientation(LinearLayout.HORIZONTAL);
        bubbleRow.setGravity(Gravity.START);
        bubbleRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView avatar = new TextView(requireContext());
        avatar.setText("AI");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(12);
        avatar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(36, 36);
        avp.setMargins(0, 2, 12, 0);
        avatar.setLayoutParams(avp);
        avatar.setBackgroundResource(R.drawable.bg_logo_circle);
        bubbleRow.addView(avatar);

        final String finalCleanText = cleanText;
        if (!finalCleanText.isEmpty()) {
            WebView wv = createMathWebView();
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    String escaped = escapeJsString(finalCleanText);
                    view.evaluateJavascript("renderMathContent('" + escaped + "')", null);
                }
            });
            wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate,
                    "text/html", "UTF-8", null);
            bubbleRow.addView(wv);
        }
        row.addView(bubbleRow);

        // Add plot views below the bubble
        renderPlotBlocks(plotBlocks, row);

        llMessages.addView(row);
        scrollToBottom();
    }

    // ========== Plot Rendering ==========

    private static class PlotBlock {
        final String type;
        final String content;
        PlotBlock(String type, String content) { this.type = type; this.content = content; }
    }

    private void renderPlotBlocks(List<PlotBlock> blocks, LinearLayout parent) {
        // Merge consecutive PLOT blocks with same bounds/xMin/xMax/yMin/yMax
        List<PlotBlock> merged = new java.util.ArrayList<>();
        int i = 0;
        while (i < blocks.size()) {
            PlotBlock block = blocks.get(i);
            if (!"plot".equals(block.type)) {
                merged.add(block);
                i++;
                continue;
            }
            // Collect all expr: lines + config from the first block
            StringBuilder allExprs = new StringBuilder();
            StringBuilder config = new StringBuilder();
            parsePlotContent(block.content, allExprs, config);
            String baseConfig = config.toString();

            int j = i + 1;
            while (j < blocks.size()) {
                PlotBlock next = blocks.get(j);
                if (!"plot".equals(next.type)) break;
                StringBuilder nextExprs = new StringBuilder();
                StringBuilder nextConfig = new StringBuilder();
                parsePlotContent(next.content, nextExprs, nextConfig);
                if (!nextConfig.toString().equals(baseConfig)) break;
                // Same config — merge expressions
                allExprs.append(nextExprs);
                // Merge points too
                allExprs.append(extractPoints(next.content));
                j++;
            }

            String mergedContent = allExprs.toString() + "\n" + baseConfig;
            merged.add(new PlotBlock("plot", mergedContent));
            i = j;
        }

        for (PlotBlock block : merged) {
            if ("plot".equals(block.type)) {
                renderPlotBlock(block.content, parent);
            } else if ("plot3d".equals(block.type)) {
                renderPlot3dBlock(block.content, parent);
            }
        }
    }

    /** Parse a PLOT content string into expr lines (with prefix) and non-expr config */
    private void parsePlotContent(String content, StringBuilder outExprs, StringBuilder outConfig) {
        if (content == null) return;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("expr:") || trimmed.matches("expr\\s*:.*")) {
                outExprs.append(line).append("\n");
            } else {
                outConfig.append(line).append("\n");
            }
        }
    }

    /** Extract points: line from PLOT content */
    private String extractPoints(String content) {
        if (content == null) return "";
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("points:")) {
                return "\n" + line;
            }
        }
        return "";
    }

    private void renderPlotBlock(String content, LinearLayout parent) {
        if (content == null || content.isEmpty()) return;

        // 将完整PLOT内容（含expr:/bounds:/xMin:等所有参数）传递到JS，由PlotParser.parse()自行解析
        java.util.List<String> fullContent = new java.util.ArrayList<>();
        fullContent.add(content);

        TextView label = new TextView(requireContext());
        label.setText("📈 函数图像");
        label.setTextSize(11);
        label.setTextColor(Color.parseColor("#6B7280"));
        label.setPadding(48, 8, 16, 4);
        parent.addView(label);

        com.suiyuan.iragent_app.ui.geogebra.GeoGebraView ggb = new com.suiyuan.iragent_app.ui.geogebra.GeoGebraView(requireContext());
        int h = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h);
        gp.setMargins(48, 0, 16, 8);
        ggb.setLayoutParams(gp);
        ggb.setExpressions(fullContent);
        parent.addView(ggb);
    }

    private void renderPlot3dBlock(String content, LinearLayout parent) {
        if (content == null || content.isEmpty()) return;

        TextView label = new TextView(requireContext());
        label.setText("🧊 3D 图形");
        label.setTextSize(11);
        label.setTextColor(Color.parseColor("#6B7280"));
        label.setPadding(48, 8, 16, 4);
        parent.addView(label);

        FrameLayout container = new FrameLayout(requireContext());
        int h = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h);
        cp.setMargins(48, 0, 16, 8);
        container.setLayoutParams(cp);
        container.setBackgroundColor(Color.WHITE);

        // WebView 必须先加到底层（不GONE），获得正确布局尺寸，JS才能读到 window.innerWidth/Height
        WebView rwv = new WebView(requireContext());
        rwv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rwv.setBackgroundColor(Color.WHITE);
        rwv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings rs = rwv.getSettings();
        rs.setJavaScriptEnabled(true);
        rs.setDomStorageEnabled(true);
        rs.setAllowFileAccess(true);
        rs.setAllowContentAccess(true);
        rs.setAllowUniversalAccessFromFileURLs(true);
        rs.setAllowFileAccessFromFileURLs(true);
        rs.setCacheMode(WebSettings.LOAD_NO_CACHE);
        container.addView(rwv);

        // ImageView 放在上面一层，capture后显示结果
        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        container.addView(imageView);

        // 加载提示在最上层
        LinearLayout loadingView = new LinearLayout(requireContext());
        loadingView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        android.widget.ProgressBar pb = new android.widget.ProgressBar(requireContext());
        loadingView.addView(pb);
        TextView lt = new TextView(requireContext());
        lt.setText("正在生成3D图像...");
        lt.setTextColor(Color.GRAY);
        lt.setTextSize(14);
        lt.setPadding(0, 8, 0, 0);
        loadingView.addView(lt);
        container.addView(loadingView);

        rwv.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onRenderComplete(String base64) {
                mMainHandler.post(() -> {
                    try {
                        loadingView.setVisibility(View.GONE);
                        if (base64 == null || base64.trim().isEmpty()) {
                            return;
                        }
                        String pure = base64.contains(",") ? base64.substring(base64.indexOf(",") + 1) : base64;
                        byte[] decoded = android.util.Base64.decode(pure, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bmp != null) {
                            imageView.setImageBitmap(bmp);
                        }
                        mMainHandler.postDelayed(() -> {
                            if (rwv.getParent() == container) container.removeView(rwv);
                            rwv.destroy();
                        }, 500);
                    } catch (Exception e) {
                        // capture failed, just keep loadingView hidden
                    }
                });
            }
        }, "androidBridge");

        rwv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.postDelayed(() -> {
                    String jsCall;
                    if (content.trim().startsWith("{")) {
                        jsCall = "render3D(" + content + ")";
                    } else {
                        String esc = content.replace("\n", "\\n").replace("'", "\\'");
                        jsCall = "render3D('" + esc + "')";
                    }
                    view.evaluateJavascript(jsCall, null);
                }, 200);
            }
        });

        parent.addView(container);
        rwv.loadUrl("file:///android_asset/geogebra/3d_renderer.html");
    }

    private String stripPlotTags(String text) {
        if (text == null) return "";
        return text.replaceAll("(?s)【PLOT】\\s*.*?【END】\\s*", "")
                   .replaceAll("(?s)【PLOT3D】\\s*.*?【END】\\s*", "")
                   .replaceAll("(?s)noteRefs:\\s*\\[.*?\\]\\s*", "")
                   .trim();
    }

    /** 从文本中提取 noteRefs JSON，渲染为卡片并返回去除 noteRefs 后的文本 */
    private String extractAndRenderNoteRefs(String text, ViewGroup parent) {
        if (text == null || text.isEmpty() || parent == null) return text;
        Pattern p = Pattern.compile("noteRefs:\\s*(\\[.*?\\])", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                String json = m.group(1);
                org.json.JSONArray arr = new org.json.JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.getJSONObject(i);
                    String fragment = obj.optString("noteFragment", "");
                    if (fragment.isEmpty()) continue;
                    View card = LayoutInflater.from(getContext()).inflate(
                            R.layout.item_note_ref_card, parent, false);
                    TextView tvFragment = card.findViewById(R.id.tv_note_fragment);
                    TextView tvSimilarity = card.findViewById(R.id.tv_similarity);
                    tvFragment.setText(fragment);
                    tvSimilarity.setText("📖 笔记引用");
                    parent.addView(card);
                }
            } catch (Exception ignored) {}
        }
        return text.replaceAll("(?s)noteRefs:\\s*\\[.*?\\]\\s*", "").trim();
    }

    private void addErrorMessage(String error) {
        TextView tv = new TextView(requireContext());
        tv.setText(error);
        tv.setTextColor(Color.parseColor("#EF4444"));
        tv.setTextSize(14);
        tv.setPadding(48, 12, 16, 12);
        llMessages.addView(tv);
        scrollToBottom();
    }

    // ========== Streaming ==========

    private void startStreamingMessage() {
        if (mHasCompletedStreaming) return;
        mIsStreaming = true;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mWebViewReady = false;
        mMainHandler.removeCallbacksAndMessages(null);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 8, 0, 8);
        row.setLayoutParams(rowParams);

        TextView avatar = new TextView(requireContext());
        avatar.setText("AI");
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(12);
        avatar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(36, 36);
        avp.setMargins(0, 2, 12, 0);
        avatar.setLayoutParams(avp);
        avatar.setBackgroundResource(R.drawable.bg_logo_circle);
        row.addView(avatar);

        mStreamingContainer = row;
        mStreamingWebView = createMathWebView();
        mStreamingWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebViewReady = true;
                if (!mIsStreaming && mCurrentFullText != null && !mCurrentFullText.isEmpty()) {
                    finishStreamingMessage();
                } else {
                    doSafeRender();
                }
            }
        });
        mStreamingWebView.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate,
                "text/html", "UTF-8", null);
        row.addView(mStreamingWebView);
        llMessages.addView(row);
        scrollToBottom();
    }

    private void appendStreamingText(String fullText) {
        if (mStreamingWebView == null || fullText == null || !mIsStreaming) return;
        mCurrentFullText = fullText;
        mMainHandler.removeCallbacks(mRenderRunnable);
        mMainHandler.postDelayed(mRenderRunnable, RENDER_INTERVAL_MS);
    }

    private void doSafeRender() {
        if (mStreamingWebView == null || getContext() == null || !mWebViewReady) return;
        String textToRender = mCurrentFullText;
        if (textToRender.equals(mLastRenderedText)) return;
        // Strip plot/noteRefs tags during streaming — they'll be rendered after completion
        String textWithoutPlots = stripPlotTags(textToRender);
        textWithoutPlots = removeUnclosedMarkdownAndMath(textWithoutPlots);
        String escaped = escapeJsString(textWithoutPlots);
        mStreamingWebView.evaluateJavascript("renderMathContent('" + escaped + "')", null);
        mLastRenderedText = textToRender;
        scrollToBottom();
    }

    private void addNoteRefCards(List<NoteRef> noteRefs) {
        if (mStreamingContainer == null || noteRefs == null || noteRefs.isEmpty()) return;
        LinearLayout parent = (LinearLayout) mStreamingContainer.getParent();
        if (!(parent instanceof LinearLayout)) return;

        for (NoteRef ref : noteRefs) {
            View card = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_note_ref_card, parent, false);
            TextView tvFragment = card.findViewById(R.id.tv_note_fragment);
            TextView tvSimilarity = card.findViewById(R.id.tv_similarity);
            tvFragment.setText(ref.getNoteFragment());
            tvSimilarity.setText("相似度 " + (int)(ref.getSimilarity() * 100) + "%");
            parent.addView(card);
        }
    }

    private void finishStreamingMessage() {
        if (mHasCompletedStreaming) return;
        mIsStreaming = false;
        mMainHandler.removeCallbacksAndMessages(null);

        String finalText = mCurrentFullText;
        android.util.Log.d("StreamFinalText", "最终内容:\n" + finalText);

        // Parse PLOT/PLOT3D blocks from final text
        List<PlotBlock> plotBlocks = new java.util.ArrayList<>();
        Pattern plotPattern = Pattern.compile("【PLOT】\\s*(.*?)【END】", Pattern.DOTALL);
        Matcher pm = plotPattern.matcher(finalText);
        while (pm.find()) {
            plotBlocks.add(new PlotBlock("plot", pm.group(1).trim()));
        }
        Pattern plot3dPattern = Pattern.compile("【PLOT3D】\\s*(.*?)【END】", Pattern.DOTALL);
        Matcher p3m = plot3dPattern.matcher(finalText);
        while (p3m.find()) {
            plotBlocks.add(new PlotBlock("plot3d", p3m.group(1).trim()));
        }

        // Render clean text without PLOT/noteRefs tags
        String cleanText = stripPlotTags(finalText);

        // If WebView is not ready yet, defer — onPageFinished will re-trigger
        if (!mWebViewReady && mStreamingWebView != null) {
            android.util.Log.d("StreamFinalText", "WebView 尚未就绪，推迟渲染");
            return;
        }

        // Extract and render noteRefs cards (fallback when noteRefs come as text, not SSE event)
        if (mStreamingContainer != null) {
            LinearLayout parent = (LinearLayout) mStreamingContainer.getParent();
            if (parent instanceof LinearLayout) {
                cleanText = extractAndRenderNoteRefs(cleanText, parent);
            }
        }

        if (mStreamingWebView != null && !cleanText.isEmpty()) {
            String escaped = escapeJsString(cleanText);
            mStreamingWebView.evaluateJavascript("renderMathContent('" + escaped + "')", null);
        }

        // Render plot blocks below the streaming row
        if (mStreamingContainer != null && !plotBlocks.isEmpty()) {
            LinearLayout parent = (LinearLayout) mStreamingContainer.getParent();
            if (parent instanceof LinearLayout) {
                LinearLayout plotContainer = new LinearLayout(requireContext());
                plotContainer.setOrientation(LinearLayout.VERTICAL);
                plotContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                renderPlotBlocks(plotBlocks, plotContainer);
                parent.addView(plotContainer);
            }
        }

        mHasCompletedStreaming = true;
        mStreamingContainer = null;
        mStreamingWebView = null;
        mCurrentFullText = "";
        mLastRenderedText = "";
        mWebViewReady = false;
        scrollToBottom();
        viewModel.consumeStreamDone();
    }

    private WebView createMathWebView() {
        WebView wv = new WebView(requireContext());
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(false);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.setPadding(24, 14, 24, 14);
        wv.setBackgroundResource(R.drawable.bg_ai_bubble);
        int maxW = (int)(getResources().getDisplayMetrics().widthPixels * 0.75);
        wv.setMinimumWidth(maxW / 2);
        LinearLayout.LayoutParams wvp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wv.setLayoutParams(wvp);
        return wv;
    }

    private String escapeJsString(String content) {
        if (content == null || content.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : content.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private String removeUnclosedMarkdownAndMath(String text) {
        if (text == null || text.isEmpty()) return text;
        String processed = text;
        int dollarCount = 0;
        for (char c : processed.toCharArray()) if (c == '$') dollarCount++;
        if (dollarCount % 2 != 0) {
            int last = processed.lastIndexOf('$');
            if (last != -1) processed = processed.substring(0, last);
        }
        int boldCount = 0, lastBold = -1;
        Matcher bm = Pattern.compile("\\*\\*").matcher(processed);
        while (bm.find()) { boldCount++; lastBold = bm.start(); }
        if (boldCount % 2 != 0 && lastBold != -1) processed = processed.substring(0, lastBold);
        return processed;
    }

    private void scrollToBottom() {
        if (!mIsUserScrolling && svMessages != null) {
            svMessages.post(() -> svMessages.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMainHandler.removeCallbacksAndMessages(null);
        mIsStreaming = false;
        mWebViewReady = false;
        if (mStreamingWebView != null) {
            mStreamingWebView.stopLoading();
            mStreamingWebView.destroy();
            mStreamingWebView = null;
        }
    }
}
