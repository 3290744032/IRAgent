package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.LinkedKnowledgePoint;
import com.suiyuan.iragent_app.data.model.v3.LinkedQuestion;
import com.suiyuan.iragent_app.data.model.v3.NoteDetail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class KnowledgeDetailFragment extends Fragment {

    private KnowledgeDetailViewModel viewModel;
    private WebView wvContent;
    private LinearLayout llKnowledgePoints, llLinkedQuestions, llDetailTags;
    private TextView tvChapter, tvDate, tvTitle;
    private android.widget.EditText etTitle, etSubject, etChapter, etTags, etContent;
    private View llEditMeta;
    private Button btnEdit, btnAI, btnSave, btnCancel;
    private boolean isEditing = false;
    private String noteId;
    private String mMathTemplate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_knowledge_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(KnowledgeDetailViewModel.class);

        wvContent = view.findViewById(R.id.wv_note_content);
        llKnowledgePoints = view.findViewById(R.id.ll_knowledge_points);
        llLinkedQuestions = view.findViewById(R.id.ll_linked_questions);
        llDetailTags = view.findViewById(R.id.ll_detail_tags);
        tvChapter = view.findViewById(R.id.tv_detail_chapter);
        tvDate = view.findViewById(R.id.tv_detail_date);
        tvTitle = view.findViewById(R.id.tv_detail_title);

        etTitle = view.findViewById(R.id.et_detail_title);
        etSubject = view.findViewById(R.id.et_detail_subject);
        etChapter = view.findViewById(R.id.et_detail_chapter);
        etTags = view.findViewById(R.id.et_detail_tags);
        etContent = view.findViewById(R.id.et_detail_content);
        llEditMeta = view.findViewById(R.id.ll_edit_meta);

        btnEdit = view.findViewById(R.id.btn_edit_note);
        btnAI = view.findViewById(R.id.btn_ai_optimize);
        btnSave = view.findViewById(R.id.btn_save_note);
        btnCancel = view.findViewById(R.id.btn_cancel_edit);

        loadMathTemplate();
        setupWebView();

        viewModel.getNoteDetail().observe(getViewLifecycleOwner(), this::renderNoteDetail);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                btnAI.setText("优化中...");
                btnAI.setEnabled(false);
            } else {
                btnAI.setText("AI 优化");
                btnAI.setEnabled(true);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getOptimizedContent().observe(getViewLifecycleOwner(), content -> {
            if (content != null) {
                String escaped = escapeJsString(content);
                wvContent.evaluateJavascript("renderMathContent('" + escaped + "')", null);
                if (isEditing) {
                    etContent.setText(content);
                }
                viewModel.clearOptimizedContent();
            }
        });

        noteId = getArguments() != null ? getArguments().getString("note_id", "") : "";
        if (!noteId.isEmpty()) {
            viewModel.loadNoteDetail(noteId);
        }

        btnEdit.setOnClickListener(v -> enterEditMode());
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> exitEditMode());
        btnAI.setOnClickListener(v -> {
            if (noteId.isEmpty()) return;
            com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
            View sheet = getLayoutInflater().inflate(R.layout.dialog_ai_optimize, null);
            dialog.setContentView(sheet);
            android.widget.EditText et = sheet.findViewById(R.id.et_ai_instruction);
            Button btnCancel = sheet.findViewById(R.id.btn_ai_cancel);
            Button btnStart = sheet.findViewById(R.id.btn_ai_start);
            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
            btnStart.setOnClickListener(v1 -> {
                String inst = et.getText().toString().trim();
                viewModel.optimizeNote(noteId, inst.isEmpty() ? "美化排版，统一格式" : inst);
                dialog.dismiss();
            });
            dialog.show();
        });
    }

    private void enterEditMode() {
        NoteDetail d = viewModel.getNoteDetail().getValue();
        if (d == null) return;
        isEditing = true;

        tvTitle.setVisibility(View.GONE);
        llDetailTags.setVisibility(View.GONE);
        wvContent.setVisibility(View.GONE);
        etTitle.setVisibility(View.VISIBLE);
        llEditMeta.setVisibility(View.VISIBLE);
        etTags.setVisibility(View.VISIBLE);
        etContent.setVisibility(View.VISIBLE);

        etTitle.setText(d.getTitle());
        etSubject.setText(d.getSubject() != null ? d.getSubject() : "");
        etChapter.setText(d.getChapter() != null ? d.getChapter() : "");
        etTags.setText(d.getTags() != null ? d.getTags() : "");
        etContent.setText(d.getContent() != null ? d.getContent() : "");

        btnEdit.setVisibility(View.GONE);
        btnAI.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
    }

    private void exitEditMode() {
        isEditing = false;
        tvTitle.setVisibility(View.VISIBLE);
        llDetailTags.setVisibility(View.VISIBLE);
        wvContent.setVisibility(View.VISIBLE);
        etTitle.setVisibility(View.GONE);
        llEditMeta.setVisibility(View.GONE);
        etTags.setVisibility(View.GONE);
        etContent.setVisibility(View.GONE);

        btnEdit.setVisibility(View.VISIBLE);
        btnAI.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);

        NoteDetail d = viewModel.getNoteDetail().getValue();
        if (d != null) renderNoteDetail(d);
    }

    private void saveChanges() {
        if (noteId.isEmpty()) return;
        String title = etTitle.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String chapter = etChapter.getText().toString().trim();
        String tags = etTags.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Snackbar.make(requireView(), "标题不能为空", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("title", title);
        body.put("subject", subject);
        body.put("chapter", chapter);
        body.put("tags", tags);
        body.put("content", content);

        viewModel.updateNote(noteId, body);
        exitEditMode();
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

    private void setupWebView() {
        WebSettings settings = wvContent.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(false);
        settings.setDefaultTextEncodingName("UTF-8");
        wvContent.setBackgroundColor(Color.TRANSPARENT);
        wvContent.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void renderNoteDetail(NoteDetail detail) {
        if (detail == null) return;

        tvChapter.setText(detail.getSubject() + " · " + detail.getChapter());
        tvDate.setText(formatDate(detail.getCreatedAt()));
        tvTitle.setText(detail.getTitle());

        // Render content via WebView
        if (detail.getContent() != null) {
            String escaped = escapeJsString(detail.getContent());
            wvContent.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    view.evaluateJavascript("renderMathContent('" + escaped + "')", null);
                }
            });
            wvContent.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate,
                    "text/html", "UTF-8", null);
        }

        // Tags — modern outline chips
        llDetailTags.removeAllViews();
        if (detail.getTags() != null && !detail.getTags().isEmpty()) {
            for (String tag : detail.getTags().split(",")) {
                TextView tv = new TextView(requireContext());
                tv.setText(tag.trim());
                tv.setTextSize(12);
                tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                tv.setTextColor(Color.WHITE);
                tv.setBackgroundResource(R.drawable.bg_tag_chip);
                int p = (int) (getResources().getDisplayMetrics().density * 10);
                tv.setPadding(p, (int)(p * 0.5f), p, (int)(p * 0.5f));
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tp.setMargins(0, 0, (int) (getResources().getDisplayMetrics().density * 6), 4);
                tv.setLayoutParams(tp);
                llDetailTags.addView(tv);
            }
        }

        // Chunks / knowledge points
        llKnowledgePoints.removeAllViews();
        if (detail.getChunks() != null) {
            for (com.suiyuan.iragent_app.data.model.v3.NoteChunk chunk : detail.getChunks()) {
                addKnowledgePointRow(chunk.getKnowledgePoint(), "知识点", true);
            }
        }
        if (detail.getLinkedKnowledgePoints() != null) {
            for (LinkedKnowledgePoint kp : detail.getLinkedKnowledgePoints()) {
                addKnowledgePointRow(kp.getName(), "相似度 " + (int)(kp.getSimilarity() * 100) + "%", false);
            }
        }

        // Linked questions
        llLinkedQuestions.removeAllViews();
        if (detail.getLinkedQuestions() != null) {
            for (LinkedQuestion q : detail.getLinkedQuestions()) {
                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackgroundResource(R.drawable.bg_card_white);
                card.setPadding(16, 14, 16, 14);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cp.setMargins(0, 0, 0, 8);
                card.setLayoutParams(cp);

                View accent = new View(requireContext());
                LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.MATCH_PARENT);
                alp.setMargins(0, 0, 12, 0);
                accent.setLayoutParams(alp);
                accent.setBackgroundColor(0xFF6366F1);
                card.addView(accent);

                TextView tv = new TextView(requireContext());
                tv.setText(stripLatex(q.getText()));
                tv.setTextSize(14);
                tv.setTextColor(0xFF1F2937);
                tv.setLineSpacing(6f, 1f);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tv.setLayoutParams(tp);
                card.addView(tv);

                llLinkedQuestions.addView(card);
            }
        }
    }

    private void addKnowledgePointRow(String name, String meta, boolean isOwn) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(14, 12, 14, 12);
        row.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 6);
        row.setLayoutParams(rowParams);

        // Colored accent bar on the left
        View accent = new View(requireContext());
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(3, 24);
        alp.setMargins(0, 0, 12, 0);
        accent.setLayoutParams(alp);
        accent.setBackgroundColor(isOwn ? 0xFF8B5CF6 : 0xFF6366F1);
        row.addView(accent);

        TextView tvName = new TextView(requireContext());
        tvName.setText(stripLatex(name));
        tvName.setTextSize(14);
        tvName.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        tvName.setTextColor(0xFF1F2937);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);
        row.addView(tvName);

        TextView tvMeta = new TextView(requireContext());
        tvMeta.setText(meta);
        tvMeta.setTextSize(11);
        tvMeta.setTextColor(0xFF6B7280);
        tvMeta.setPadding((int)(8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        row.addView(tvMeta);

        llKnowledgePoints.addView(row);
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
                case '\u2028': sb.append("\\u2028"); break;
                case '\u2029': sb.append("\\u2029"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr;
        return dateStr.substring(5, 10);
    }

    private static String stripLatex(String s) {
        if (s == null) return "";
        String result = s;
        // 1. Remove $$...$$ blocks (handle missing closing $$)
        result = result.replaceAll("\\$\\$[\\s\\S]*?(?:\\$\\$|$)", "");
        // 2. Remove $...$ inline blocks (non-greedy, handle missing closing $)
        result = result.replaceAll("\\$[^$\\n]{1,300}?(?:\\$|$)", "");
        // 3. Remove \begin{...}...\end{...} environments
        result = result.replaceAll("\\\\begin\\{[^}]+\\}[\\s\\S]*?\\\\end\\{[^}]+\\}", "");
        // 4. Remove remaining LaTeX commands with brace arguments
        result = result.replaceAll("\\\\[a-zA-Z]+\\{[^}]*\\}", "");
        // 5. Remove remaining bare LaTeX commands
        result = result.replaceAll("\\\\[a-zA-Z]+", "");
        // 6. Remove orphaned braces
        result = result.replaceAll("[{}]", "");
        // 7. Replace \n with space for single-line display
        result = result.replace('\n', ' ');
        return result.trim();
    }
}
