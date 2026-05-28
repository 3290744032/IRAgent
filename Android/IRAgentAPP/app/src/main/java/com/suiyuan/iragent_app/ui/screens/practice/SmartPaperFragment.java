package com.suiyuan.iragent_app.ui.screens.practice;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.PracticeQuestion;
import com.suiyuan.iragent_app.data.model.v3.SmartPaper;
import com.suiyuan.iragent_app.data.model.v3.SubmitAnswerResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmartPaperFragment extends Fragment {

    private SmartPaperViewModel viewModel;

    private View layoutConfig, layoutQuiz, layoutLoading, layoutResult;
    private TextView tvStreamLabel, tvProgress, tvQuestion, tvResultScore;
    private EditText etChatInput;
    private ImageButton btnSend;
    private ImageView ivBack;
    private TextView btnPdf, btnStartQuiz, btnAnswerKey;
    private LinearLayout llOptions, llResultStats, llResultDetails;
    private Button btnNext, btnBackHub;
    private WebView wvStreamContent;

    private String mMathTemplate;
    private boolean mWebViewReady;
    private String mLastRenderedText = "";
    private boolean isPdfPreviewMode;
    private int savedStartQuizVisibility = View.GONE;
    private int savedAnswerKeyVisibility = View.GONE;

    private static final String ARG_SUBJECT = "subject";

    public static SmartPaperFragment newInstance(String subject) {
        SmartPaperFragment f = new SmartPaperFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT, subject);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_smart_paper, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SmartPaperViewModel.class);

        loadMathTemplate();

        layoutConfig = view.findViewById(R.id.layout_config);
        layoutQuiz = view.findViewById(R.id.layout_quiz);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutResult = view.findViewById(R.id.layout_result);

        wvStreamContent = view.findViewById(R.id.wv_stream_content);
        tvStreamLabel = view.findViewById(R.id.tv_stream_label);
        etChatInput = view.findViewById(R.id.et_chat_input);
        btnSend = view.findViewById(R.id.btn_send);
        ivBack = view.findViewById(R.id.iv_back);
        btnPdf = view.findViewById(R.id.btn_pdf);
        btnStartQuiz = view.findViewById(R.id.btn_start_quiz);
        btnAnswerKey = view.findViewById(R.id.btn_answer_key);

        tvProgress = view.findViewById(R.id.tv_progress);
        tvQuestion = view.findViewById(R.id.tv_question);
        llOptions = view.findViewById(R.id.ll_options);
        btnNext = view.findViewById(R.id.btn_next);
        tvResultScore = view.findViewById(R.id.tv_result_score);
        llResultStats = view.findViewById(R.id.ll_result_stats);
        llResultDetails = view.findViewById(R.id.ll_result_details);
        btnBackHub = view.findViewById(R.id.btn_back_hub);

        setupWebView();
        setupClickListeners();
        setupObservers();
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
        WebSettings settings = wvStreamContent.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        wvStreamContent.setBackgroundColor(Color.TRANSPARENT);
        wvStreamContent.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        wvStreamContent.setVerticalScrollBarEnabled(false);
        wvStreamContent.setHorizontalScrollBarEnabled(false);
        wvStreamContent.setOverScrollMode(View.OVER_SCROLL_NEVER);

        wvStreamContent.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mWebViewReady = true;
                String content = viewModel.getStreamContent().getValue();
                if (content != null && !content.isEmpty()) {
                    renderInWebView(content);
                }
            }
        });
    }

    private void renderInWebView(String content) {
        if (!mWebViewReady || content == null) return;
        if (content.equals(mLastRenderedText)) return;
        String escaped = escapeJsString(content);
        wvStreamContent.evaluateJavascript("renderMathContent('" + escaped + "')", null);
        mLastRenderedText = content;
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendPrompt());
        ivBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        btnPdf.setOnClickListener(v -> {
            if (!isPdfPreviewMode) {
                previewPdf();
            } else {
                sharePdf();
            }
        });
        btnStartQuiz.setOnClickListener(v -> {
            SmartPaper paper = viewModel.getStreamedPaper();
            if (paper != null && paper.getQuestions() != null && !paper.getQuestions().isEmpty()) {
                showPhase(layoutConfig, false);
                showPhase(layoutQuiz, true);
                renderQuestion(paper);
            } else {
                Toast.makeText(getContext(), "试卷数据异常，请重新生成", Toast.LENGTH_SHORT).show();
            }
        });
        btnAnswerKey.setOnClickListener(v -> {
            viewModel.toggleAnswerKey();
        });
        btnNext.setOnClickListener(v -> onNextClick());
        btnBackHub.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private String buildPdfContent() {
        if (viewModel.hasAnswerKey() && viewModel.getAnswerKeyContent() != null) {
            return viewModel.getPaperBodyContent()
                    + "\n\n<div class='page-break'>&nbsp;</div>\n\n# 参考答案\n\n"
                    + viewModel.getAnswerKeyContent();
        }
        return viewModel.getPaperBodyContent();
    }

    private void previewPdf() {
        String content = buildPdfContent();
        if (content == null || content.isEmpty()) {
            Toast.makeText(getContext(), "没有可导出的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String html = buildExamHtml(content);
        wvStreamContent.loadDataWithBaseURL("https://cdn.jsdelivr.net", html,
                "text/html", "UTF-8", null);
        wvStreamContent.setVisibility(View.VISIBLE);
        tvStreamLabel.setText("预览模式 — 点击下方「分享 PDF」导出");

        isPdfPreviewMode = true;
        btnPdf.setText("分享 PDF");
        savedStartQuizVisibility = btnStartQuiz.getVisibility();
        savedAnswerKeyVisibility = btnAnswerKey.getVisibility();
        btnStartQuiz.setVisibility(View.GONE);
        btnAnswerKey.setVisibility(View.GONE);
        View bottomBar = getView().findViewById(R.id.bottom_input_container);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
    }

    private void sharePdf() {
        String content = buildPdfContent();
        if (content == null || content.isEmpty()) return;

        String html = buildExamHtml(content);

        new AlertDialog.Builder(requireContext())
                .setTitle("导出 PDF")
                .setMessage("系统将打开打印界面，请选择「保存为 PDF」即可导出印刷级试卷文件。")
                .setPositiveButton("确定", (dialog, which) -> startPdfPrint(html))
                .setNegativeButton("取消", null)
                .show();
    }

    private void startPdfPrint(String html) {
        Toast.makeText(getContext(), "正在生成 PDF...", Toast.LENGTH_SHORT).show();

        WebView printView = new WebView(requireContext());
        printView.setBackgroundColor(Color.WHITE);
        WebSettings ws = printView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        printView.setVisibility(View.GONE);

        ViewGroup root = (ViewGroup) requireView().getRootView();
        root.addView(printView, new ViewGroup.LayoutParams(1, 1));

        printView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) requireContext()
                        .getSystemService(Context.PRINT_SERVICE);
                PrintAttributes attrs = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build();
                pm.print("智能组卷",
                        printView.createPrintDocumentAdapter("SmartPaper"),
                        attrs);

                isPdfPreviewMode = false;
                btnPdf.setText("预览 PDF");
                btnStartQuiz.setVisibility(savedStartQuizVisibility);
                btnAnswerKey.setVisibility(savedAnswerKeyVisibility);
                tvStreamLabel.setText("生成完成");
                View bottomBar = getView().findViewById(R.id.bottom_input_container);
                if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);

                String orig = Boolean.TRUE.equals(viewModel.getAnswerKeyVisible().getValue())
                        ? viewModel.getFullContent()
                        : viewModel.getPaperBodyContent();
                if (orig != null) renderInWebView(orig);
            }
        });

        printView.loadDataWithBaseURL("https://cdn.jsdelivr.net", html,
                "text/html", "UTF-8", null);
    }

    private String buildExamHtml(String content) {
        String escaped = escapeJsString(content);
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
                "<script src='https://cdn.jsdelivr.net/npm/marked@4.3.0/marked.min.js'>" +
                "</script>" +
                "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/katex.min.css'>" +
                "<script src='https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/katex.min.js'>" +
                "</script>" +
                "<script src='https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/contrib/auto-render.min.js'>" +
                "</script>" +
                "<style>" +
                "@media print { @page { size: A4; margin: 2.5cm 2cm 2.5cm 2cm; } body { font-family: 'SimSun','STSong',serif; color: #000; background: #fff; } }" +
                "body { font-family: 'SimSun','STSong',serif; font-size: 12pt; line-height: 1.6; color: #000; background: #fff; padding: 10px; margin: 0; }" +
                ".sheet-header { font-family: 'SimHei','STHeiti',sans-serif; font-size: 24pt; text-align: center; margin: 0 0 10px; font-weight: bold; letter-spacing: 4px; }" +
                ".sheet-sub { text-align: center; font-size: 11pt; color: #333; margin-bottom: 30px; border-bottom: 2px solid #000; padding-bottom: 8px; }" +
                "h1 { font-family: 'SimHei','STHeiti',sans-serif; font-size: 16pt; text-align: center; margin: 0 0 20px; letter-spacing: 2px; }" +
                "h2 { font-family: 'SimHei','STHeiti',sans-serif; font-size: 14pt; margin: 25px 0 12px; padding-bottom: 4px; border-bottom: 1px solid #ccc; page-break-after: avoid; }" +
                "h3 { font-family: 'SimHei','STHeiti',sans-serif; font-size: 12pt; margin: 14px 0 6px; page-break-after: avoid; }" +
                "p { margin: 4px 0; text-align: justify; }" +
                ".q { page-break-inside: avoid !important; margin-bottom: 20px; }" +
                ".q-body { margin-bottom: 4px; }" +
                ".opts { padding-left: 20px; display: block; }" +
                ".opt { margin: 4px 0; display: block; }" +
                "pre { background: #f5f5f5; padding: 10px; border: 1px solid #ddd; font-size: 10pt; white-space: pre-wrap; }" +
                "code { font-size: 10pt; font-family: 'Courier New',monospace; }" +
                "table { border-collapse: collapse; width: 100%; margin: 10px 0; }" +
                "th,td { border: 1px solid #222; padding: 5px 8px; text-align: left; font-size: 11pt; }" +
                "th { background: #ececec; font-family: 'SimHei','STHeiti',sans-serif; }" +
                "blockquote { border-left: 3px solid #888; padding-left: 12px; margin: 8px 0; color: #444; }" +
                "ul,ol { margin: 4px 0; padding-left: 28px; }" +
                "li { margin: 2px 0; }" +
                "hr { border: none; border-top: 1px solid #ccc; margin: 16px 0; }" +
                ".katex { font-size: 1.1em; }" +
                ".katex-display { margin: 6px 0; }" +
                ".page-break { page-break-before: always; height: 0; margin: 0; padding: 0; border: none; }" +
                "</style></head><body>" +
                "<div class='sheet-header'>智能组卷</div>" +
                "<div class='sheet-sub'>AI 生成试卷 &nbsp;·&nbsp; 共若干题 &nbsp;·&nbsp; 满分 100 分</div>" +
                "<div id='content'></div>" +
                "<script>marked.setOptions({breaks:true,gfm:true,headerIds:false,mangle:false});" +
                "function r(){var c=document.getElementById('content');" +
                "c.innerHTML=marked.parse(contentStr);" +
                "renderMathInElement(c,{delimiters:[{left:'$$',right:'$$',display:true},{left:'$',right:'$',display:false},{left:'\\\\(',right:'\\\\)',display:false},{left:'\\\\[',right:'\\\\]',display:true}],throwOnError:false,trust:true});}" +
                "var contentStr='" + escaped + "';" +
                "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',r);else r();" +
                "</script></body></html>";
    }

    private void sendPrompt() {
        String prompt = etChatInput.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(getContext(), "请输入你的需求", Toast.LENGTH_SHORT).show();
            return;
        }
        etChatInput.setText("");
        wvStreamContent.setVisibility(View.VISIBLE);
        mLastRenderedText = "";
        mWebViewReady = false;
        tvStreamLabel.setText("AI 正在生成试卷...");
        btnPdf.setVisibility(View.GONE);
        btnStartQuiz.setVisibility(View.GONE);
        btnAnswerKey.setVisibility(View.GONE);
        btnAnswerKey.setText("查看解析");
        showPhase(layoutConfig, true);

        wvStreamContent.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate,
                "text/html", "UTF-8", null);

        viewModel.streamGeneratePaper(prompt);
    }

    private void setupObservers() {
        viewModel.getStreamContent().observe(getViewLifecycleOwner(), content -> {
            if (content != null) {
                renderInWebView(content);
            }
        });

        viewModel.getIsStreaming().observe(getViewLifecycleOwner(), streaming -> {
            if (streaming != null) {
                btnSend.setEnabled(!streaming);
                btnSend.setAlpha(streaming ? 0.5f : 1f);
                if (!streaming) {
                    tvStreamLabel.setText("生成完成");
                }
            }
        });

        viewModel.getPdfVisible().observe(getViewLifecycleOwner(), visible -> {
            if (visible != null) {
                if (visible) {
                    isPdfPreviewMode = false;
                    btnPdf.setText("预览 PDF");
                    View bottomBar = getView().findViewById(R.id.bottom_input_container);
                    if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
                }
                btnPdf.setVisibility(visible ? View.VISIBLE : View.GONE);
                btnStartQuiz.setVisibility(visible && viewModel.getStreamedPaper() != null
                        && !isPdfPreviewMode ? View.VISIBLE : View.GONE);
                btnAnswerKey.setVisibility(visible && viewModel.hasAnswerKey()
                        && !isPdfPreviewMode ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAnswerKeyVisible().observe(getViewLifecycleOwner(), visible -> {
            if (isPdfPreviewMode) return;
            if (visible != null && visible) {
                String full = viewModel.getFullContent();
                if (full != null) {
                    renderInWebView(full);
                }
                btnAnswerKey.setText("收起解析");
            } else if (visible != null) {
                String body = viewModel.getPaperBodyContent();
                if (body != null) {
                    renderInWebView(body);
                }
                btnAnswerKey.setText("查看解析");
            }
        });

        viewModel.getStreamError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                Toast.makeText(getContext(), err, Toast.LENGTH_LONG).show();
                tvStreamLabel.setText("生成失败，请重试");
            }
        });

        viewModel.getPaper().observe(getViewLifecycleOwner(), paper -> {
            if (paper != null && viewModel.getIsLoading().getValue() == Boolean.TRUE) {
                showPhase(layoutLoading, false);
                if (paper.getQuestions() == null || paper.getQuestions().isEmpty()) {
                    showPhase(layoutQuiz, false);
                    Toast.makeText(getContext(), "未找到符合条件的题目，请调整筛选条件", Toast.LENGTH_LONG).show();
                    showPhase(layoutConfig, true);
                    return;
                }
                showPhase(layoutQuiz, true);
                renderQuestion(paper);
            }
        });

        viewModel.getQuestionIndex().observe(getViewLifecycleOwner(), idx -> {
            SmartPaper paper = viewModel.getPaper().getValue();
            if (paper != null && idx != null) {
                renderQuestion(paper);
            }
        });

        viewModel.getSelectedOption().observe(getViewLifecycleOwner(), sel -> {
            SmartPaper paper = viewModel.getPaper().getValue();
            if (paper != null) {
                int idx = viewModel.getQuestionIndex().getValue() != null
                        ? viewModel.getQuestionIndex().getValue() : 0;
                updateOptionHighlights(paper.getQuestions().get(idx).getOptions(), sel);
                if (viewModel.isLastQuestion()) {
                    btnNext.setEnabled(sel != null && !sel.isEmpty());
                    btnNext.setAlpha(btnNext.isEnabled() ? 1f : 0.4f);
                }
            }
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                showPhase(layoutQuiz, false);
                showPhase(layoutResult, true);
                renderResult(result);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && !loading) {
                if (viewModel.getResult().getValue() != null) {
                    showPhase(layoutResult, true);
                } else if (viewModel.getPaper().getValue() != null) {
                    showPhase(layoutQuiz, true);
                }
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showPhase(layoutLoading, false);
                showPhase(layoutConfig, true);
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onNextClick() {
        SmartPaper paper = viewModel.getPaper().getValue();
        if (paper == null || paper.getQuestions() == null || paper.getQuestions().isEmpty()) return;

        int idx = viewModel.getQuestionIndex().getValue() != null
                ? viewModel.getQuestionIndex().getValue() : 0;
        if (idx >= paper.getQuestions().size()) return;
        String sel = viewModel.getSelectedOption().getValue();
        if (viewModel.isLastQuestion() && (sel == null || sel.isEmpty())) {
            return;
        }
        String qId = paper.getQuestions().get(idx).getId();
        viewModel.nextQuestion(qId);

        if (viewModel.isLastQuestion()) {
            viewModel.submitAll(paper.getPaperId());
        }
    }

    private void renderQuestion(SmartPaper paper) {
        int idx = viewModel.getQuestionIndex().getValue() != null
                ? viewModel.getQuestionIndex().getValue() : 0;
        int total = paper.getQuestions().size();

        tvProgress.setText((idx + 1) + "/" + total);

        PracticeQuestion q = paper.getQuestions().get(idx);
        tvQuestion.setText(q.getQuestionText());

        llOptions.removeAllViews();
        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            for (final String option : q.getOptions()) {
                Button btn = new Button(requireContext());
                btn.setText(option);
                btn.setTextSize(14);
                btn.setTypeface(Typeface.DEFAULT);
                btn.setPadding(16, 12, 16, 12);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 8);
                btn.setLayoutParams(lp);
                btn.setBackgroundResource(R.drawable.bg_quick_chip);
                btn.setTextColor(Color.parseColor("#1F2937"));
                btn.setAllCaps(false);
                btn.setOnClickListener(v -> viewModel.selectOption(option));
                llOptions.addView(btn);
            }
        }

        String selected = viewModel.getSelectedOption().getValue();
        updateOptionHighlights(q.getOptions(), selected);

        boolean last = viewModel.isLastQuestion();
        btnNext.setText(last ? "提交" : "下一题");
        if (last) {
            btnNext.setEnabled(selected != null && !selected.isEmpty());
            btnNext.setAlpha(btnNext.isEnabled() ? 1f : 0.4f);
        } else {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1f);
        }
    }

    private void updateOptionHighlights(java.util.List<String> options, String selected) {
        if (options == null) return;
        for (int i = 0; i < llOptions.getChildCount() && i < options.size(); i++) {
            View child = llOptions.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                if (options.get(i).equals(selected)) {
                    btn.setBackgroundColor(Color.parseColor("#6366F1"));
                    btn.setTextColor(Color.WHITE);
                } else {
                    btn.setBackgroundResource(R.drawable.bg_quick_chip);
                    btn.setTextColor(Color.parseColor("#1F2937"));
                }
            }
        }
    }

    private void renderResult(SubmitAnswerResult result) {
        tvResultScore.setText(result.getCorrectCount() + "/" + result.getTotalCount());

        llResultStats.removeAllViews();
        String[] labels = {"正确", "错误", "正确率"};
        int[] values = {result.getCorrectCount(), result.getWrongCount(),
                (int) (result.getAccuracy() * 100)};
        int[] colors = {Color.parseColor("#10B981"), Color.parseColor("#EF4444"),
                Color.parseColor("#6366F1")};

        for (int i = 0; i < 3; i++) {
            LinearLayout stat = new LinearLayout(requireContext());
            stat.setOrientation(LinearLayout.VERTICAL);
            stat.setGravity(android.view.Gravity.CENTER);
            stat.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stat.setLayoutParams(sp);

            TextView tvVal = new TextView(requireContext());
            tvVal.setText(String.valueOf(values[i]) + (i == 2 ? "%" : ""));
            tvVal.setTextSize(20);
            tvVal.setTextColor(colors[i]);
            tvVal.setGravity(android.view.Gravity.CENTER);
            tvVal.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            stat.addView(tvVal);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(11);
            tvLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            tvLabel.setGravity(android.view.Gravity.CENTER);
            stat.addView(tvLabel);

            llResultStats.addView(stat);
        }

        llResultDetails.removeAllViews();
        if (result.getDetails() != null) {
            for (SubmitAnswerResult.AnswerDetail d : result.getDetails()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(16, 12, 16, 12);
                row.setBackgroundResource(R.drawable.bg_quick_chip);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0, 0, 0, 8);
                row.setLayoutParams(rlp);

                TextView tvIdx = new TextView(requireContext());
                tvIdx.setText("第" + (d.getQuestionId()) + "题");
                tvIdx.setTextSize(12);
                tvIdx.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                tvIdx.setMinWidth(48);
                row.addView(tvIdx);

                LinearLayout content = new LinearLayout(requireContext());
                content.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                content.setLayoutParams(cp);

                TextView tvSelected = new TextView(requireContext());
                tvSelected.setText("你的答案: " + d.getSelectedAnswer());
                tvSelected.setTextSize(12);
                tvSelected.setTextColor(d.isCorrect()
                        ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                content.addView(tvSelected);

                if (!d.isCorrect() && d.getCorrectAnswer() != null) {
                    TextView tvCorrect = new TextView(requireContext());
                    tvCorrect.setText("正确答案: " + d.getCorrectAnswer());
                    tvCorrect.setTextSize(12);
                    tvCorrect.setTextColor(Color.parseColor("#10B981"));
                    content.addView(tvCorrect);
                }

                row.addView(content);

                TextView tvResult = new TextView(requireContext());
                tvResult.setText(d.isCorrect() ? "正确" : "错误");
                tvResult.setTextSize(12);
                tvResult.setTextColor(d.isCorrect()
                        ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                tvResult.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.addView(tvResult);

                llResultDetails.addView(row);
            }
        }
    }

    private void showPhase(View phase, boolean show) {
        phase.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String escapeJsString(String content) {
        if (content == null || content.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\u2028': sb.append("\\u2028"); break;
                case '\u2029': sb.append("\\u2029"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
