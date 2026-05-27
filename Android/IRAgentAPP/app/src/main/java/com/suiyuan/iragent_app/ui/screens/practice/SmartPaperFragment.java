package com.suiyuan.iragent_app.ui.screens.practice;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.PracticeQuestion;
import com.suiyuan.iragent_app.data.model.v3.SmartPaper;
import com.suiyuan.iragent_app.data.model.v3.SmartPaperRequest;
import com.suiyuan.iragent_app.data.model.v3.SubmitAnswerResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class SmartPaperFragment extends Fragment {

    private SmartPaperViewModel viewModel;

    private View layoutConfig, layoutQuiz, layoutLoading, layoutResult;
    private EditText etTitle;
    private Spinner spCount, spDifficulty, spSubject;
    private Button btnGenerate;
    private TextView tvProgress, tvQuestion, tvResultScore;
    private LinearLayout llOptions, llResultStats, llResultDetails;
    private Button btnNext, btnBackHub;

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

        layoutConfig = view.findViewById(R.id.layout_config);
        layoutQuiz = view.findViewById(R.id.layout_quiz);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutResult = view.findViewById(R.id.layout_result);
        etTitle = view.findViewById(R.id.et_title);
        spCount = view.findViewById(R.id.sp_count);
        spDifficulty = view.findViewById(R.id.sp_difficulty);
        spSubject = view.findViewById(R.id.sp_subject);
        btnGenerate = view.findViewById(R.id.btn_generate);
        tvProgress = view.findViewById(R.id.tv_progress);
        tvQuestion = view.findViewById(R.id.tv_question);
        llOptions = view.findViewById(R.id.ll_options);
        btnNext = view.findViewById(R.id.btn_next);
        tvResultScore = view.findViewById(R.id.tv_result_score);
        llResultStats = view.findViewById(R.id.ll_result_stats);
        llResultDetails = view.findViewById(R.id.ll_result_details);
        btnBackHub = view.findViewById(R.id.btn_back_hub);

        setupSpinners();
        setupClickListeners();
        setupObservers();

        String subjectArg = getArguments() != null ? getArguments().getString(ARG_SUBJECT, "") : "";
        if (!subjectArg.isEmpty()) {
            setSpinnerSelection(spSubject, subjectArg);
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"5", "10", "15", "20"});
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCount.setAdapter(countAdapter);

        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"1 (最简单)", "2", "3 (中等)", "4", "5 (最难)"});
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDifficulty.setAdapter(difficultyAdapter);

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"数学", "物理", "化学", "英语", "政治", "历史"});
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subjectAdapter);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        btnGenerate.setOnClickListener(v -> generatePaper());
        btnNext.setOnClickListener(v -> onNextClick());
        btnBackHub.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void generatePaper() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "请输入试卷标题", Toast.LENGTH_SHORT).show();
            return;
        }

        int count;
        try {
            count = Integer.parseInt(spCount.getSelectedItem().toString());
        } catch (Exception e) {
            count = 10;
        }

        int difficulty = spDifficulty.getSelectedItemPosition() + 1;
        String subject = spSubject.getSelectedItem().toString();

        SmartPaperRequest req = new SmartPaperRequest(
                subject, "", title, count, difficulty,
                new ArrayList<String>(), true);

        showPhase(layoutConfig, false);
        showPhase(layoutLoading, true);
        viewModel.generatePaper(req);
    }

    private void onNextClick() {
        SmartPaper paper = viewModel.getPaper().getValue();
        if (paper == null) return;

        int idx = viewModel.getQuestionIndex().getValue() != null
                ? viewModel.getQuestionIndex().getValue() : 0;
        String qId = paper.getQuestions().get(idx).getId();
        viewModel.nextQuestion(qId);

        if (viewModel.isLastQuestion()) {
            viewModel.submitAll(paper.getPaperId());
        }
    }

    private void setupObservers() {
        viewModel.getPaper().observe(getViewLifecycleOwner(), paper -> {
            if (paper != null) {
                showPhase(layoutLoading, false);
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

        updateOptionHighlights(q.getOptions(), viewModel.getSelectedOption().getValue());

        boolean last = viewModel.isLastQuestion();
        btnNext.setText(last ? "提交" : "下一题");
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
