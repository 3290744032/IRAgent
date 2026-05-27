package com.suiyuan.iragent_app.ui.screens.errors;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisItem;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisJson;
import com.suiyuan.iragent_app.data.model.v3.ErrorDetail;

public class ErrorsDetailFragment extends Fragment {

    private ErrorsDetailViewModel viewModel;
    private LinearLayout rootLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_errors_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootLayout = view.findViewById(R.id.root_container);
        viewModel = new ViewModelProvider(this).get(ErrorsDetailViewModel.class);

        viewModel.getErrorDetail().observe(getViewLifecycleOwner(), this::renderErrorDetail);
        viewModel.getMarkMasteredResult().observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(getContext(), "已标记为已掌握", Toast.LENGTH_SHORT).show();
        });
        viewModel.getSimilarQuestions().observe(getViewLifecycleOwner(), questions -> {
            Toast.makeText(getContext(), "已添加 " + questions.size() + " 道同类题", Toast.LENGTH_SHORT).show();
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        String errorId = getArguments() != null ? getArguments().getString("error_id", "") : "";
        if (!errorId.isEmpty()) {
            viewModel.loadErrorDetail(errorId);
        }
    }

    private void renderErrorDetail(ErrorDetail detail) {
        rootLayout.removeAllViews();
        if (detail == null) return;

        // Question header
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#FEF2F2"));
        header.setPadding(16, 16, 16, 16);
        int margin = (int)(8 * getResources().getDisplayMetrics().density);

        TextView tvSubject = new TextView(requireContext());
        tvSubject.setText(detail.getSubject() + " · " + detail.getKnowledgePoint());
        tvSubject.setTextSize(12);
        tvSubject.setTextColor(getResources().getColor(R.color.gray_text, null));
        header.addView(tvSubject);

        TextView tvQuestion = new TextView(requireContext());
        tvQuestion.setText(detail.getQuestionText());
        tvQuestion.setTextSize(16);
        tvQuestion.setTextColor(getResources().getColor(R.color.on_surface, null));
        tvQuestion.setTypeface(Typeface.DEFAULT_BOLD);
        tvQuestion.setPadding(0, 8, 0, 12);
        tvQuestion.setLineSpacing(4, 1f);
        header.addView(tvQuestion);

        LinearLayout answers = new LinearLayout(requireContext());
        answers.setOrientation(LinearLayout.VERTICAL);

        LinearLayout wrongRow = new LinearLayout(requireContext());
        wrongRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView wrongLabel = new TextView(requireContext());
        wrongLabel.setText("你的答案  ");
        wrongLabel.setTextSize(12);
        wrongLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        wrongRow.addView(wrongLabel);
        TextView wrongVal = new TextView(requireContext());
        wrongVal.setText(detail.getStudentAnswer());
        wrongVal.setTextSize(14);
        wrongVal.setTextColor(Color.parseColor("#EF4444"));
        wrongVal.setPaintFlags(wrongVal.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        wrongRow.addView(wrongVal);
        answers.addView(wrongRow);

        LinearLayout correctRow = new LinearLayout(requireContext());
        correctRow.setOrientation(LinearLayout.HORIZONTAL);
        correctRow.setPadding(0, 4, 0, 0);
        TextView correctLabel = new TextView(requireContext());
        correctLabel.setText("正确答案  ");
        correctLabel.setTextSize(12);
        correctLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        correctRow.addView(correctLabel);
        TextView correctVal = new TextView(requireContext());
        correctVal.setText(detail.getCorrectAnswer());
        correctVal.setTextSize(14);
        correctVal.setTextColor(Color.parseColor("#10B981"));
        correctVal.setTypeface(Typeface.DEFAULT_BOLD);
        correctRow.addView(correctVal);
        answers.addView(correctRow);

        header.addView(answers);
        rootLayout.addView(header);

        // Three-way diagnosis
        if (detail.getDiagnosis() != null) {
            addSectionTitle("AI 三维诊断");

            DiagnosisJson d = detail.getDiagnosis();
            addDiagnosisCard("考点漏缺", d.getPrerequisiteCheck(),
                    Color.parseColor("#3B82F6"), "📚", detail.getId());
            addDiagnosisCard("公式混淆", d.getFormulaConfusion(),
                    Color.parseColor("#8B5CF6"), "📐", detail.getId());
            addDiagnosisCard("计算失误", d.getCalculationError(),
                    Color.parseColor("#F59E0B"), "🔢", detail.getId());
        }

        // Action buttons
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 24, 0, 0);

        Button btnMaster = new Button(requireContext());
        btnMaster.setText("标记为已掌握");
        btnMaster.setBackgroundColor(Color.WHITE);
        btnMaster.setTextColor(getResources().getColor(R.color.primary_color, null));
        LinearLayout.LayoutParams mp1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp1.setMargins(0, 0, 8, 0);
        btnMaster.setLayoutParams(mp1);
        btnMaster.setOnClickListener(v -> {
            viewModel.markMastered(detail.getId());
        });
        actions.addView(btnMaster);

        Button btnSimilar = new Button(requireContext());
        btnSimilar.setText("练同类题");
        btnSimilar.setBackgroundColor(getResources().getColor(R.color.primary_color, null));
        btnSimilar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams mp2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp2.setMargins(8, 0, 0, 0);
        btnSimilar.setLayoutParams(mp2);
        btnSimilar.setOnClickListener(v -> {
            viewModel.loadSimilarQuestions(detail.getId());
        });
        actions.addView(btnSimilar);

        rootLayout.addView(actions);
    }

    private void addSectionTitle(String title) {
        TextView tv = new TextView(requireContext());
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTextColor(getResources().getColor(R.color.on_surface, null));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, 24, 0, 12);
        rootLayout.addView(tv);
    }

    private void addDiagnosisCard(String title, DiagnosisItem item, int borderColor, String icon, String questionId) {
        if (item == null) return;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins((int)(4 * getResources().getDisplayMetrics().density), 0,
                (int)(4 * getResources().getDisplayMetrics().density),
                (int)(8 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(cp);

        // Left border via a 4dp view
        View border = new View(requireContext());
        border.setLayoutParams(new LinearLayout.LayoutParams(
                (int)(4 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.MATCH_PARENT));
        border.setBackgroundColor(borderColor);

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(12, 0, 0, 0);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView header = new TextView(requireContext());
        header.setText(icon + " " + title);
        header.setTextSize(14);
        header.setTextColor(getResources().getColor(R.color.on_surface, null));
        header.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(header);

        TextView body = new TextView(requireContext());
        body.setText(item.getAnalysis() != null ? item.getAnalysis() : "");
        body.setTextSize(13);
        body.setTextColor(getResources().getColor(R.color.gray_text, null));
        body.setPadding(0, 8, 0, 0);
        body.setLineSpacing(4, 1f);
        content.addView(body);

        // Feedback button at bottom of diagnosis card
        Button btnFeedback = new Button(requireContext());
        btnFeedback.setText("题目有误？");
        btnFeedback.setTextSize(11);
        btnFeedback.setTextColor(Color.parseColor("#8B5CF6"));
        btnFeedback.setBackgroundResource(R.drawable.bg_option_unselected);
        btnFeedback.setPadding(8, 4, 8, 4);
        btnFeedback.setAllCaps(false);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, 8, 0, 0);
        btnFeedback.setLayoutParams(flp);
        btnFeedback.setOnClickListener(v -> {
            btnFeedback.setVisibility(View.GONE);
            viewModel.submitFeedback(questionId, "wrong_diagnosis", title);
        });
        content.addView(btnFeedback);

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.addView(border);
        wrapper.addView(content);
        card.addView(wrapper);

        rootLayout.addView(card);
    }
}
