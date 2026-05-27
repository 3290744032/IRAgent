package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.LinkedKnowledgePoint;
import com.suiyuan.iragent_app.data.model.v3.LinkedQuestion;
import com.suiyuan.iragent_app.data.model.v3.NoteDetail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class KnowledgeDetailFragment extends Fragment {

    private KnowledgeDetailViewModel viewModel;
    private WebView wvContent;
    private LinearLayout llKnowledgePoints;
    private LinearLayout llLinkedQuestions;
    private TextView tvChapter, tvDate, tvTitle;
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
        tvChapter = view.findViewById(R.id.tv_detail_chapter);
        tvDate = view.findViewById(R.id.tv_detail_date);
        tvTitle = view.findViewById(R.id.tv_detail_title);

        loadMathTemplate();
        setupWebView();

        viewModel.getNoteDetail().observe(getViewLifecycleOwner(), this::renderNoteDetail);
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        String noteId = getArguments() != null ? getArguments().getString("note_id", "") : "";
        if (!noteId.isEmpty()) {
            viewModel.loadNoteDetail(noteId);
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

    private void setupWebView() {
        WebSettings settings = wvContent.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(false);
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

        // Chunks / knowledge points
        llKnowledgePoints.removeAllViews();
        if (detail.getChunks() != null) {
            for (com.suiyuan.iragent_app.data.model.v3.NoteChunk chunk : detail.getChunks()) {
                addKnowledgePointRow(chunk.getKnowledgePoint(), "知识点");
            }
        }
        if (detail.getLinkedKnowledgePoints() != null) {
            for (LinkedKnowledgePoint kp : detail.getLinkedKnowledgePoints()) {
                addKnowledgePointRow(kp.getName(), "相似度 " + (int)(kp.getSimilarity() * 100) + "%");
            }
        }

        // Linked questions
        llLinkedQuestions.removeAllViews();
        if (detail.getLinkedQuestions() != null) {
            for (LinkedQuestion q : detail.getLinkedQuestions()) {
                TextView tv = new TextView(requireContext());
                tv.setText(q.getText());
                tv.setTextSize(14);
                tv.setTextColor(getResources().getColor(R.color.gray_text, null));
                tv.setPadding(0, 12, 0, 12);
                tv.setBackgroundResource(android.R.color.white);
                llLinkedQuestions.addView(tv);
            }
        }
    }

    private void addKnowledgePointRow(String name, String meta) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);
        row.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 4);
        row.setLayoutParams(rowParams);

        View dot = new View(requireContext());
        dot.setLayoutParams(new LinearLayout.LayoutParams(24, 24));
        dot.setBackgroundColor(getResources().getColor(R.color.primary_color, null));
        row.addView(dot);

        TextView tvName = new TextView(requireContext());
        tvName.setText(name);
        tvName.setTextSize(14);
        tvName.setTextColor(getResources().getColor(R.color.on_surface, null));
        tvName.setPadding(12, 0, 0, 0);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameParams);
        row.addView(tvName);

        TextView tvMeta = new TextView(requireContext());
        tvMeta.setText(meta);
        tvMeta.setTextSize(11);
        tvMeta.setTextColor(getResources().getColor(R.color.text_tertiary, null));
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
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr;
        return dateStr.substring(5, 10);
    }
}
