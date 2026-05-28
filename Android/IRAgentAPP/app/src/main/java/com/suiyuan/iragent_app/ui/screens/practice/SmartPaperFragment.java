package com.suiyuan.iragent_app.ui.screens.practice;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import com.suiyuan.iragent_app.data.model.v3.PracticeQuestion;
import com.suiyuan.iragent_app.data.model.v3.SmartPaper;
import com.suiyuan.iragent_app.data.model.v3.SubmitAnswerResult;

public class SmartPaperFragment extends Fragment {

    private SmartPaperViewModel viewModel;

    private View layoutConfig, layoutQuiz, layoutLoading, layoutResult;
    private TextView tvStreamContent, tvStreamLabel, tvProgress, tvQuestion, tvResultScore;
    private EditText etChatInput;
    private Button btnSend, btnPdf, btnStartQuiz, btnAnswerKey, btnGenerate;
    private LinearLayout llOptions, llResultStats, llResultDetails;
    private Button btnNext, btnBackHub;
    private NestedScrollView scrollStream;
    private Markwon markwon;

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

        float mathTextSize = getResources().getDisplayMetrics().scaledDensity * 15f;
        markwon = Markwon.builder(requireContext())
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(JLatexMathPlugin.create(mathTextSize, config -> {
                    config.inlinesEnabled(true);
                }))
                .build();

        layoutConfig = view.findViewById(R.id.layout_config);
        layoutQuiz = view.findViewById(R.id.layout_quiz);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutResult = view.findViewById(R.id.layout_result);

        tvStreamContent = view.findViewById(R.id.tv_stream_content);
        tvStreamLabel = view.findViewById(R.id.tv_stream_label);
        etChatInput = view.findViewById(R.id.et_chat_input);
        btnSend = view.findViewById(R.id.btn_send);
        btnPdf = view.findViewById(R.id.btn_pdf);
        btnStartQuiz = view.findViewById(R.id.btn_start_quiz);
        btnAnswerKey = view.findViewById(R.id.btn_answer_key);
        scrollStream = view.findViewById(R.id.scroll_stream);

        tvProgress = view.findViewById(R.id.tv_progress);
        tvQuestion = view.findViewById(R.id.tv_question);
        llOptions = view.findViewById(R.id.ll_options);
        btnNext = view.findViewById(R.id.btn_next);
        tvResultScore = view.findViewById(R.id.tv_result_score);
        llResultStats = view.findViewById(R.id.ll_result_stats);
        llResultDetails = view.findViewById(R.id.ll_result_details);
        btnBackHub = view.findViewById(R.id.btn_back_hub);

        setupClickListeners();
        setupObservers();
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendPrompt());
        btnPdf.setOnClickListener(v -> {
            Toast.makeText(getContext(), "PDF 导出功能待实现", Toast.LENGTH_SHORT).show();
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

    private void sendPrompt() {
        String prompt = etChatInput.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(getContext(), "请输入你的需求", Toast.LENGTH_SHORT).show();
            return;
        }
        etChatInput.setText("");
        tvStreamContent.setVisibility(View.VISIBLE);
        tvStreamContent.setText("");
        tvStreamLabel.setText("AI 正在生成试卷...");
        btnPdf.setVisibility(View.GONE);
        btnStartQuiz.setVisibility(View.GONE);
        btnAnswerKey.setVisibility(View.GONE);
        btnAnswerKey.setText("查看解析");
        showPhase(layoutConfig, true);
        viewModel.streamGeneratePaper(prompt);
    }

    private void setupObservers() {
        viewModel.getStreamContent().observe(getViewLifecycleOwner(), content -> {
            markwon.setMarkdown(tvStreamContent, content);
            if (scrollStream != null) {
                scrollStream.post(() -> scrollStream.fullScroll(View.FOCUS_DOWN));
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
                btnPdf.setVisibility(visible ? View.VISIBLE : View.GONE);
                btnStartQuiz.setVisibility(visible && viewModel.getStreamedPaper() != null
                        ? View.VISIBLE : View.GONE);
                btnAnswerKey.setVisibility(visible && viewModel.hasAnswerKey()
                        ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAnswerKeyVisible().observe(getViewLifecycleOwner(), visible -> {
            if (visible != null && visible) {
                String full = viewModel.getFullContent();
                if (full != null) {
                    markwon.setMarkdown(tvStreamContent, full);
                    if (scrollStream != null) {
                        scrollStream.post(() -> scrollStream.fullScroll(View.FOCUS_DOWN));
                    }
                }
                btnAnswerKey.setText("收起解析");
            } else if (visible != null) {
                String body = viewModel.getPaperBodyContent();
                if (body != null) {
                    markwon.setMarkdown(tvStreamContent, body);
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
            // Used for backward compatibility with non-streaming flow
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
}
