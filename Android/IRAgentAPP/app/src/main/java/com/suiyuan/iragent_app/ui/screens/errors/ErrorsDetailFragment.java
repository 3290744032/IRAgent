package com.suiyuan.iragent_app.ui.screens.errors;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;

import java.util.List;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisItem;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisJson;
import com.suiyuan.iragent_app.data.model.v3.ErrorDetail;
import com.suiyuan.iragent_app.data.model.v3.SimilarQuestion;

public class ErrorsDetailFragment extends Fragment {

    private ErrorsDetailViewModel viewModel;
    private LinearLayout rootLayout;
    private Button btnMaster;

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
            if (result != null) {
                if (result) {
                    btnMaster.setText("取消掌握");
                    btnMaster.setEnabled(true);
                    btnMaster.setBackgroundColor(Color.WHITE);
                    btnMaster.setTextColor(getResources().getColor(R.color.primary_color, null));
                    btnMaster.setAlpha(1f);
                    Toast.makeText(getContext(), "已标记为已掌握", Toast.LENGTH_SHORT).show();
                } else {
                    btnMaster.setText("标记为已掌握");
                    btnMaster.setEnabled(true);
                    btnMaster.setBackgroundColor(Color.WHITE);
                    btnMaster.setTextColor(getResources().getColor(R.color.primary_color, null));
                    btnMaster.setAlpha(1f);
                    Toast.makeText(getContext(), "已取消掌握，错题将重新进入复习队列", Toast.LENGTH_SHORT).show();
                }
            }
        });
        viewModel.getSimilarQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && !questions.isEmpty()) {
                showSimilarQuestionsSheet(questions);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG)
                        .setAction("重试", v -> {
                            String eid = getArguments() != null ? getArguments().getString("error_id", "") : "";
                            if (!eid.isEmpty()) viewModel.loadErrorDetail(eid);
                        })
                        .show();
            }
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

        btnMaster = new Button(requireContext());
        btnMaster.setBackgroundColor(Color.WHITE);
        btnMaster.setTextColor(getResources().getColor(R.color.primary_color, null));
        if (detail.isMastered()) {
            btnMaster.setText("取消掌握");
            btnMaster.setOnClickListener(v -> viewModel.unmarkMastered(detail.getId()));
        } else {
            btnMaster.setText("标记为已掌握");
            btnMaster.setOnClickListener(v -> {
                btnMaster.setAlpha(0.5f);
                viewModel.markMastered(detail.getId());
            });
        }
        LinearLayout.LayoutParams mp1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp1.setMargins(0, 0, 8, 0);
        btnMaster.setLayoutParams(mp1);
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

    private void showSimilarQuestionsSheet(List<SimilarQuestion> questions) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        LinearLayout sheet = new LinearLayout(requireContext());
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(16, 24, 16, 32);

        TextView title = new TextView(requireContext());
        title.setText("同类题推荐 (" + questions.size() + ")");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#1F2937"));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        sheet.addView(title);

        for (SimilarQuestion q : questions) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_white);
            card.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(cp);

            TextView tvText = new TextView(requireContext());
            tvText.setText(q.getText());
            tvText.setTextSize(13);
            tvText.setTextColor(Color.parseColor("#1F2937"));
            tvText.setMaxLines(3);
            card.addView(tvText);

            if (q.getTags() != null && !q.getTags().isEmpty()) {
                LinearLayout tagRow = new LinearLayout(requireContext());
                tagRow.setOrientation(LinearLayout.HORIZONTAL);
                tagRow.setPadding(0, 6, 0, 0);
                for (String tag : q.getTags()) {
                    TextView tvTag = new TextView(requireContext());
                    tvTag.setText(tag);
                    tvTag.setTextSize(10);
                    tvTag.setTextColor(Color.parseColor("#6366F1"));
                    tvTag.setBackgroundResource(R.drawable.btn_quick_reply);
                    tvTag.setPadding(8, 2, 8, 2);
                    tvTag.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    tagRow.addView(tvTag);
                    if (q.getTags().indexOf(tag) < q.getTags().size() - 1) {
                        ((LinearLayout.LayoutParams) tvTag.getLayoutParams()).setMargins(0, 0, 6, 0);
                    }
                }
                card.addView(tagRow);
            }

            // Bottom row: tags + practice button
            LinearLayout bottomRow = new LinearLayout(requireContext());
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setPadding(0, 8, 0, 0);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);

            Button btnPractice = new Button(requireContext());
            btnPractice.setText("去练习");
            btnPractice.setTextSize(12);
            btnPractice.setTextColor(Color.WHITE);
            btnPractice.setBackgroundColor(Color.parseColor("#6366F1"));
            btnPractice.setPadding(16, 4, 16, 4);
            btnPractice.setAllCaps(false);
            btnPractice.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btnPractice.setOnClickListener(vv -> {
                dialog.dismiss();
                Bundle args = new Bundle();
                if (q.getText() != null) {
                    args.putString("subject", "");
                }
                if (q.getTags() != null && !q.getTags().isEmpty()) {
                    args.putString("knowledge_points", String.join(",", q.getTags()));
                }
                try {
                    Navigation.findNavController(requireView()).navigate(R.id.nav_daily_practice, args);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "请先返回刷题页面", Toast.LENGTH_SHORT).show();
                }
            });
            bottomRow.addView(btnPractice);

            card.addView(bottomRow);
            sheet.addView(card);
        }

        dialog.setContentView(sheet);
        dialog.show();
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
