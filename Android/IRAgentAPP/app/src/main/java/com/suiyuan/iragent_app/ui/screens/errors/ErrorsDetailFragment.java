package com.suiyuan.iragent_app.ui.screens.errors;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisItem;
import com.suiyuan.iragent_app.data.model.v3.DiagnosisJson;
import com.suiyuan.iragent_app.data.model.v3.ErrorDetail;
import com.suiyuan.iragent_app.data.model.v3.SimilarQuestion;

import java.util.List;

public class ErrorsDetailFragment extends Fragment {

    private ErrorsDetailViewModel viewModel;
    private LinearLayout rootContainer;
    private LinearLayout llDiagnosis;
    private LinearLayout llSimilarContainer;
    private LinearLayout llSimilarHeader;
    private ProgressBar pbSimilarLoading;
    private TextView tvSimilarCount;
    private TextView tvSimilarEmpty;
    private MaterialButton btnMaster;
    private MaterialButton btnSimilar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_errors_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rootContainer = view.findViewById(R.id.root_container);
        llDiagnosis = view.findViewById(R.id.ll_diagnosis_container);
        llSimilarContainer = view.findViewById(R.id.ll_similar_container);
        llSimilarHeader = view.findViewById(R.id.ll_similar_header);
        pbSimilarLoading = view.findViewById(R.id.pb_similar_loading);
        tvSimilarCount = view.findViewById(R.id.tv_similar_count);
        tvSimilarEmpty = view.findViewById(R.id.tv_similar_empty);

        viewModel = new ViewModelProvider(this).get(ErrorsDetailViewModel.class);

        viewModel.getErrorDetail().observe(getViewLifecycleOwner(), detail -> {
            if (detail != null) {
                renderContent(detail);
                // 自动加载同类题
                viewModel.loadSimilarQuestions(detail.getId());
            }
        });

        viewModel.getMarkMasteredResult().observe(getViewLifecycleOwner(), result -> {
            if (btnMaster == null) return;
            if (result != null && result) {
                updateMasteredButton(true);
                Toast.makeText(getContext(), "已标记为已掌握", Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                updateMasteredButton(false);
                Toast.makeText(getContext(), "已取消掌握", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getSimilarQuestions().observe(getViewLifecycleOwner(), questions -> {
            pbSimilarLoading.setVisibility(View.GONE);
            if (questions != null && !questions.isEmpty()) {
                renderSimilarQuestions(questions);
            } else {
                tvSimilarEmpty.setVisibility(View.VISIBLE);
                tvSimilarCount.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        String errorId = getArguments() != null ? getArguments().getString("error_id", "") : "";
        if (!errorId.isEmpty()) {
            viewModel.loadErrorDetail(errorId);
        }
    }

    // ==================== 渲染内容 ====================

    private void renderContent(ErrorDetail detail) {
        if (detail == null) return;

        // 题目头部
        TextView tvSubjectKp = requireView().findViewById(R.id.tv_subject_kp);
        tvSubjectKp.setText(detail.getSubject() + " · " + detail.getKnowledgePoint());

        TextView tvQuestion = requireView().findViewById(R.id.tv_question);
        tvQuestion.setText(detail.getQuestionText());

        TextView tvWrong = requireView().findViewById(R.id.tv_wrong_answer);
        tvWrong.setText(detail.getStudentAnswer());
        tvWrong.setPaintFlags(tvWrong.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        TextView tvCorrect = requireView().findViewById(R.id.tv_correct_answer);
        tvCorrect.setText(detail.getCorrectAnswer());

        // 诊断卡片
        llDiagnosis.removeAllViews();
        if (detail.getDiagnosis() != null) {
            DiagnosisJson d = detail.getDiagnosis();
            addDiagnosisCard("📚 考点漏缺", d.getPrerequisiteCheck(),
                    Color.parseColor("#3B82F6"), detail.getId());
            addDiagnosisCard("📐 公式混淆", d.getFormulaConfusion(),
                    Color.parseColor("#8B5CF6"), detail.getId());
            addDiagnosisCard("🔢 计算失误", d.getCalculationError(),
                    Color.parseColor("#F59E0B"), detail.getId());
        }

        // 操作按钮（在 XML 布局末尾动态添加）
        addActionButtons(detail);
    }

    private void updateMasteredButton(boolean mastered) {
        if (mastered) {
            btnMaster.setText("✓ 已掌握");
            btnMaster.setStrokeColorResource(R.color.success);
            btnMaster.setTextColor(getResources().getColor(R.color.success, null));
        } else {
            btnMaster.setText("标记为已掌握");
            btnMaster.setStrokeColorResource(R.color.primary_color);
            btnMaster.setTextColor(getResources().getColor(R.color.primary_color, null));
        }
    }

    private void addDiagnosisCard(String title, DiagnosisItem item, int accentColor, String questionId) {
        if (item == null || item.getAnalysis() == null || item.getAnalysis().isEmpty()) return;

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.WHITE);
        int pd = dp(14);
        card.setPadding(pd, pd, pd, pd);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(dp(4), 0, dp(4), dp(8));
        card.setLayoutParams(cp);

        // 左侧色条
        View accent = new View(requireContext());
        accent.setLayoutParams(new LinearLayout.LayoutParams(dp(4),
                ViewGroup.LayoutParams.MATCH_PARENT));
        accent.setBackgroundColor(accentColor);
        card.addView(accent);

        // 内容
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), 0, 0, 0);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(getResources().getColor(R.color.on_surface, null));
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(tvTitle);

        TextView tvBody = new TextView(requireContext());
        tvBody.setText(item.getAnalysis());
        tvBody.setTextSize(13);
        tvBody.setTextColor(getResources().getColor(R.color.gray_text, null));
        tvBody.setPadding(0, dp(6), 0, 0);
        tvBody.setLineSpacing(dp(2), 1f);
        content.addView(tvBody);

        // 反馈按钮
        MaterialButton btnFb = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonStyle);
        btnFb.setText("题目有误？");
        btnFb.setTextSize(11);
        btnFb.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        btnFb.setPadding(0, 0, 0, 0);
        btnFb.setMinimumHeight(0);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, dp(6), 0, 0);
        btnFb.setLayoutParams(flp);
        btnFb.setOnClickListener(v -> {
            btnFb.setVisibility(View.GONE);
            viewModel.submitFeedback(questionId, "wrong_diagnosis", title);
        });
        content.addView(btnFb);

        card.addView(content);
        llDiagnosis.addView(card);
    }

    private void addActionButtons(ErrorDetail detail) {
        // 移除旧按钮（重新渲染时避免重复）
        int childCount = rootContainer.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = rootContainer.getChildAt(i);
            if (child.getTag() != null && "action_buttons".equals(child.getTag().toString())) {
                rootContainer.removeView(child);
            }
        }

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 24, 0, 8);
        actions.setTag("action_buttons");

        btnMaster = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnMaster.setCornerRadius(dp(24));
        btnMaster.setStrokeWidth(dp(2));
        if (detail.isMastered()) {
            updateMasteredButton(true);
            btnMaster.setOnClickListener(v -> viewModel.unmarkMastered(detail.getId()));
        } else {
            updateMasteredButton(false);
            btnMaster.setOnClickListener(v -> {
                btnMaster.setEnabled(false);
                viewModel.markMastered(detail.getId());
            });
        }
        LinearLayout.LayoutParams mp1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp1.setMargins(0, 0, 8, 0);
        btnMaster.setLayoutParams(mp1);
        actions.addView(btnMaster);

        btnSimilar = new MaterialButton(requireContext());
        btnSimilar.setText("练同类题");
        btnSimilar.setCornerRadius(24);
        btnSimilar.setBackgroundColor(getResources().getColor(R.color.primary_color, null));
        btnSimilar.setTextColor(Color.WHITE);
        btnSimilar.setStrokeWidth(0);
        LinearLayout.LayoutParams mp2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mp2.setMargins(8, 0, 0, 0);
        btnSimilar.setLayoutParams(mp2);
        btnSimilar.setOnClickListener(v -> {
            // 滚动到同类题区域 + 刷新加载
            llSimilarHeader.setVisibility(View.VISIBLE);
            pbSimilarLoading.setVisibility(View.VISIBLE);
            tvSimilarEmpty.setVisibility(View.GONE);
            viewModel.loadSimilarQuestions(detail.getId());
            // 滚动到底部
            requireView().findViewById(R.id.ll_similar_container)
                    .getParent().requestChildFocus(
                            requireView().findViewById(R.id.ll_similar_container),
                            requireView().findViewById(R.id.ll_similar_container));
        });
        actions.addView(btnSimilar);

        rootContainer.addView(actions);
    }

    // ==================== 同类题渲染 ====================

    private void renderSimilarQuestions(List<SimilarQuestion> questions) {
        llSimilarHeader.setVisibility(View.VISIBLE);
        tvSimilarCount.setVisibility(View.VISIBLE);
        tvSimilarEmpty.setVisibility(View.GONE);
        tvSimilarCount.setText(questions.size() + " 题");
        llSimilarContainer.removeAllViews();

        for (SimilarQuestion q : questions) {
            View card = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_similar_question, llSimilarContainer, false);

            TextView tvSimilarity = card.findViewById(R.id.tv_similarity);
            Double sim = q.getSimilarity();
            if (sim != null) {
                int pct = (int)(sim * 100);
                tvSimilarity.setText("相似 " + pct + "%");
            } else {
                tvSimilarity.setText("推荐");
            }

            if (q.getDifficulty() != null) {
                TextView tvDiff = card.findViewById(R.id.tv_difficulty);
                tvDiff.setText(q.getDifficulty());
                switch (q.getDifficulty()) {
                    case "简单": tvDiff.setTextColor(Color.parseColor("#10B981")); break;
                    case "中等": tvDiff.setTextColor(Color.parseColor("#F59E0B")); break;
                    case "困难": tvDiff.setTextColor(Color.parseColor("#EF4444")); break;
                }
            }

            TextView tvText = card.findViewById(R.id.tv_question_text);
            tvText.setText(q.getText());

            // 知识点标签
            LinearLayout llTags = card.findViewById(R.id.ll_tags);
            if (q.getTags() != null && !q.getTags().isEmpty()) {
                for (String tag : q.getTags()) {
                    TextView tvTag = new TextView(requireContext());
                    tvTag.setText(tag);
                    tvTag.setTextSize(11);
                    tvTag.setTextColor(getResources().getColor(R.color.primary_color, null));
                    tvTag.setBackgroundResource(R.drawable.btn_quick_reply);
                    tvTag.setPadding(dp(8), dp(2), dp(8), dp(2));
                    LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tagLp.setMargins(0, 0, dp(6), 0);
                    tvTag.setLayoutParams(tagLp);
                    llTags.addView(tvTag);
                }
            }

            // 题型
            if (q.getQuestionType() != null) {
                TextView tvType = card.findViewById(R.id.tv_question_type);
                tvType.setText(q.getQuestionType());
            }

            // 开始练习按钮
            MaterialButton btnStart = card.findViewById(R.id.btn_start_practice);
            btnStart.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("subject", "");
                if (q.getTags() != null && !q.getTags().isEmpty()) {
                    args.putString("knowledge_points", String.join(",", q.getTags()));
                }
                if (q.getText() != null) {
                    args.putString("question_text", q.getText());
                }
                try {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.nav_daily_practice, args);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "请先返回刷题页面", Toast.LENGTH_SHORT).show();
                }
            });

            llSimilarContainer.addView(card);
        }
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}
