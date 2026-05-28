package com.suiyuan.iragent_app.ui.screens.practice;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.PracticeQuestion;
import com.suiyuan.iragent_app.data.model.v3.SubmitAnswerResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DailyPracticeFragment extends Fragment {

    private DailyPracticeViewModel viewModel;

    private View layoutQuiz, layoutLoading, layoutResult;
    private View layoutModeBanner;
    private TextView tvTargetKp;
    private LinearLayout llQuestionContainer;
    private MaterialButton btnSubmitAll;
    private MaterialButton btnSkip;
    private TextView tvResultScore;
    private TextView tvResultLabel;
    private LinearLayout llResultStats;
    private TextView tvErrorHint;
    private LinearLayout llResultDetails;
    private MaterialButton btnBackHub;

    private List<PracticeQuestion> questions;
    private Map<String, PracticeQuestion> questionMap;
    private String sessionId;
    private String currentQuestionIdForCamera;
    private Uri cameraPhotoUri;
    private final Set<String> hiddenFeedbackQuestions = new HashSet<>();

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && currentQuestionIdForCamera != null) {
                    viewModel.setPhotoUri(currentQuestionIdForCamera, uri);
                    updateCameraIcon(currentQuestionIdForCamera, true);
                    currentQuestionIdForCamera = null;
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null && currentQuestionIdForCamera != null) {
                    viewModel.setPhotoUri(currentQuestionIdForCamera, cameraPhotoUri);
                    updateCameraIcon(currentQuestionIdForCamera, true);
                }
                currentQuestionIdForCamera = null;
                cameraPhotoUri = null;
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_practice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DailyPracticeViewModel.class);

        layoutQuiz = view.findViewById(R.id.layout_quiz);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutResult = view.findViewById(R.id.layout_result);
        layoutModeBanner = view.findViewById(R.id.layout_mode_banner);
        tvTargetKp = view.findViewById(R.id.tv_target_kp);
        llQuestionContainer = view.findViewById(R.id.ll_question_container);
        btnSubmitAll = view.findViewById(R.id.btn_submit_all);
        btnSkip = view.findViewById(R.id.btn_skip);
        tvResultScore = view.findViewById(R.id.tv_result_score);
        tvResultLabel = view.findViewById(R.id.tv_result_label);
        llResultStats = view.findViewById(R.id.ll_result_stats);
        tvErrorHint = view.findViewById(R.id.tv_error_hint);
        llResultDetails = view.findViewById(R.id.ll_result_details);
        btnBackHub = view.findViewById(R.id.btn_back_hub);

        Bundle args = getArguments();
        String subject = args != null ? args.getString("subject", "") : "";
        String knowledgePoints = args != null ? args.getString("knowledge_points", "") : "";

        // 显示同类题模式横幅
        if (knowledgePoints != null && !knowledgePoints.isEmpty()) {
            layoutModeBanner.setVisibility(View.VISIBLE);
            tvTargetKp.setText(knowledgePoints.length() > 30
                    ? knowledgePoints.substring(0, 30) + "..." : knowledgePoints);
        }

        viewModel.loadPractice(subject, 5,
                knowledgePoints.isEmpty() ? null : knowledgePoints);

        btnSubmitAll.setOnClickListener(v -> onSubmitAll());
        btnSkip.setOnClickListener(v -> {
            if (sessionId != null) viewModel.submitAll(sessionId, "daily_practice");
        });
        btnBackHub.setOnClickListener(v -> requireActivity().onBackPressed());

        setupObservers();
    }

    private void onSubmitAll() {
        if (questions != null) {
            for (PracticeQuestion q : questions) {
                String answer = viewModel.getAnswerMap().get(q.getId());
                boolean hasPhoto = viewModel.getPhotoUriMap().containsKey(q.getId());
                if ((answer == null || answer.trim().isEmpty()) && !hasPhoto) {
                    Toast.makeText(getContext(),
                            "请回答第 " + (q.getIndex() + 1) + " 题", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        viewModel.submitAll(sessionId != null ? sessionId : "", "daily_practice");
    }

    private void setupObservers() {
        viewModel.getSession().observe(getViewLifecycleOwner(), session -> {
            if (session != null) {
                questions = session.getQuestions();
                sessionId = session.getSessionId();
                questionMap = new HashMap<>();
                for (PracticeQuestion q : questions) questionMap.put(q.getId(), q);
                buildQuestionCards();
                showState(0);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) showState(1);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), this::renderResult);

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void showState(int state) {
        layoutQuiz.setVisibility(state == 0 ? View.VISIBLE : View.GONE);
        layoutLoading.setVisibility(state == 1 ? View.VISIBLE : View.GONE);
        layoutResult.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
    }

    // ==================== 题目卡片 ====================

    private void buildQuestionCards() {
        llQuestionContainer.removeAllViews();
        if (questions == null) return;

        for (int i = 0; i < questions.size(); i++) {
            PracticeQuestion q = questions.get(i);
            llQuestionContainer.addView(createQuestionCard(q, i));
        }
    }

    private View createQuestionCard(PracticeQuestion q, int index) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cp);

        // 题号 + 来源标签
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvIndex = new TextView(requireContext());
        tvIndex.setText("第 " + (index + 1) + " 题");
        tvIndex.setTextSize(13);
        tvIndex.setTextColor(getResources().getColor(R.color.primary_color, null));
        tvIndex.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(tvIndex);

        if (q.getQuestionType() != null) {
            TextView tvType = new TextView(requireContext());
            tvType.setText(" · " + q.getQuestionType());
            tvType.setTextSize(12);
            tvType.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            header.addView(tvType);
        }

        // Source tag
        if (q.getSource() != null) {
            TextView sourceTag = sourceTagView(q.getSource());
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(dp(10), 0, 0, 0);
            sourceTag.setLayoutParams(slp);
            header.addView(sourceTag);
        }

        card.addView(header);

        // 题目文本
        TextView tvQ = new TextView(requireContext());
        tvQ.setText(q.getQuestionText());
        tvQ.setTextSize(15);
        tvQ.setTextColor(getResources().getColor(R.color.on_surface, null));
        tvQ.setLineSpacing(dp(3), 1f);
        tvQ.setPadding(0, dp(10), 0, 0);
        card.addView(tvQ);

        // 答案输入行
        LinearLayout inputRow = buildAnswerInput(q);
        card.addView(inputRow);

        // 反馈按钮
        if ("ai-generated".equals(q.getSource()) && !hiddenFeedbackQuestions.contains(q.getId())) {
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
            String qid = q.getId();
            btnFb.setOnClickListener(v -> {
                hiddenFeedbackQuestions.add(qid);
                viewModel.submitDailyFeedback(qid, "wrong_question", "");
                btnFb.setVisibility(View.GONE);
            });
            card.addView(btnFb);
        }

        return card;
    }

    private TextView sourceTagView(String source) {
        TextView tag = new TextView(requireContext());
        tag.setTextSize(10);
        tag.setPadding(dp(8), dp(3), dp(8), dp(3));
        tag.setBackgroundResource(R.drawable.bg_option_unselected);
        switch (source) {
            case "official":
                tag.setText("真题");
                tag.setTextColor(Color.parseColor("#6B7280"));
                break;
            case "ai-generated":
                tag.setText("AI 生成");
                tag.setTextColor(Color.parseColor("#8B5CF6"));
                break;
            case "user-contributed":
                tag.setText("用户上传");
                tag.setTextColor(Color.parseColor("#3B82F6"));
                break;
            default:
                tag.setText(source);
                tag.setTextColor(Color.parseColor("#6B7280"));
        }
        return tag;
    }

    private LinearLayout buildAnswerInput(PracticeQuestion q) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);

        EditText et = new EditText(requireContext());
        et.setHint("请输入答案...");
        et.setTextSize(14);
        et.setTextColor(getResources().getColor(R.color.on_surface, null));
        et.setHintTextColor(getResources().getColor(R.color.text_tertiary, null));
        et.setPadding(dp(14), dp(10), dp(14), dp(10));
        et.setBackgroundResource(R.drawable.bg_option_unselected);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ep.setMargins(0, 0, dp(8), 0);
        et.setLayoutParams(ep);
        et.setMinHeight(dp(44));

        String existing = viewModel.getAnswerMap().get(q.getId());
        if (existing != null) et.setText(existing);

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                viewModel.setAnswer(q.getId(), s.toString());
            }
        });
        row.addView(et);

        // 拍照按钮
        LinearLayout camBtn = new LinearLayout(requireContext());
        camBtn.setOrientation(LinearLayout.VERTICAL);
        camBtn.setGravity(Gravity.CENTER);
        camBtn.setPadding(dp(8), dp(6), dp(8), dp(6));
        camBtn.setBackgroundResource(R.drawable.bg_option_unselected);
        camBtn.setTag("camera_" + q.getId());

        ImageView ivCam = new ImageView(requireContext());
        ivCam.setImageResource(android.R.drawable.ic_menu_camera);
        ivCam.setColorFilter(getResources().getColor(R.color.primary_color, null));
        ivCam.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView tvCam = new TextView(requireContext());
        tvCam.setText("拍照");
        tvCam.setTextSize(10);
        tvCam.setTextColor(getResources().getColor(R.color.primary_color, null));
        tvCam.setGravity(Gravity.CENTER);

        camBtn.addView(ivCam);
        camBtn.addView(tvCam);

        if (viewModel.getPhotoUriMap().containsKey(q.getId())) {
            tvCam.setText("已拍");
            ivCam.setColorFilter(Color.parseColor("#10B981"));
        }

        camBtn.setOnClickListener(v -> {
            currentQuestionIdForCamera = q.getId();
            openCamera();
        });

        row.addView(camBtn);
        return row;
    }

    // ==================== 结果渲染 ====================

    private void renderResult(SubmitAnswerResult result) {
        showState(2);

        int accuracy = (int)(result.getAccuracy() * 100);
        tvResultScore.setText(String.valueOf(accuracy));
        tvResultLabel.setText("正确率");
        if (accuracy >= 80) {
            tvResultScore.setTextColor(getResources().getColor(R.color.success, null));
        } else if (accuracy >= 60) {
            tvResultScore.setTextColor(getResources().getColor(R.color.warning, null));
        } else {
            tvResultScore.setTextColor(getResources().getColor(R.color.error, null));
        }

        // 统计栏
        llResultStats.removeAllViews();
        addStat("正确", String.valueOf(result.getCorrectCount()),
                getResources().getColor(R.color.success, null));
        addStat("错误", String.valueOf(result.getWrongCount()),
                getResources().getColor(R.color.error, null));
        addStat("正确率", accuracy + "%",
                getResources().getColor(R.color.primary_color, null));

        // 错题提示
        if (result.getWrongCount() > 0) {
            tvErrorHint.setText("错题已自动收录到错题本，可随时复习巩固");
            tvErrorHint.setVisibility(View.VISIBLE);
        } else {
            tvErrorHint.setVisibility(View.GONE);
        }

        // 详情
        llResultDetails.removeAllViews();
        if (result.getDetails() != null) {
            for (SubmitAnswerResult.AnswerDetail d : result.getDetails()) {
                llResultDetails.addView(buildResultRow(d));
            }
        }
    }

    private void addStat(String label, String value, int color) {
        LinearLayout stat = new LinearLayout(requireContext());
        stat.setOrientation(LinearLayout.VERTICAL);
        stat.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stat.setLayoutParams(sp);

        TextView tvVal = new TextView(requireContext());
        tvVal.setText(value);
        tvVal.setTextSize(22);
        tvVal.setTextColor(color);
        tvVal.setTypeface(Typeface.DEFAULT_BOLD);
        tvVal.setGravity(Gravity.CENTER);
        stat.addView(tvVal);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(11);
        tvLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setPadding(0, dp(2), 0, 0);
        stat.addView(tvLabel);

        llResultStats.addView(stat);
    }

    private LinearLayout buildResultRow(SubmitAnswerResult.AnswerDetail d) {
        PracticeQuestion q = questionMap != null ? questionMap.get(d.getQuestionId()) : null;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackgroundResource(R.drawable.bg_card_white);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rp);

        // 状态 + 题目
        TextView tvStatus = new TextView(requireContext());
        tvStatus.setText(d.isCorrect() ? "✓ 正确" : "✗ 错误");
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(d.isCorrect()
                ? getResources().getColor(R.color.success, null)
                : getResources().getColor(R.color.error, null));
        tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(tvStatus);

        if (q != null) {
            TextView tvQText = new TextView(requireContext());
            tvQText.setText(q.getQuestionText());
            tvQText.setTextSize(13);
            tvQText.setTextColor(getResources().getColor(R.color.on_surface, null));
            tvQText.setPadding(0, dp(6), 0, 0);
            tvQText.setLineSpacing(dp(2), 1f);
            tvQText.setMaxLines(3);
            row.addView(tvQText);
        }

        // 答案对比
        if (!d.isCorrect()) {
            LinearLayout answerRow = new LinearLayout(requireContext());
            answerRow.setOrientation(LinearLayout.HORIZONTAL);
            answerRow.setPadding(0, dp(8), 0, 0);

            TextView yourLabel = labelTextView("你的答案 ");
            answerRow.addView(yourLabel);

            TextView yourVal = valueTextView(d.getSelectedAnswer(),
                    getResources().getColor(R.color.error, null));
            yourVal.setPaintFlags(yourVal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            answerRow.addView(yourVal);

            TextView arrow = labelTextView("  →  ");
            answerRow.addView(arrow);

            TextView correctVal = valueTextView(d.getCorrectAnswer(),
                    getResources().getColor(R.color.success, null));
            correctVal.setTypeface(Typeface.DEFAULT_BOLD);
            answerRow.addView(correctVal);

            row.addView(answerRow);
        }

        // 解析
        if (d.getExplanation() != null && !d.getExplanation().isEmpty()) {
            TextView tvExp = new TextView(requireContext());
            tvExp.setText(d.getExplanation());
            tvExp.setTextSize(12);
            tvExp.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvExp.setLineSpacing(dp(2), 1f);
            tvExp.setPadding(0, dp(8), 0, 0);
            row.addView(tvExp);
        }

        return row;
    }

    private TextView labelTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        return tv;
    }

    private TextView valueTextView(String text, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(color);
        return tv;
    }

    // ==================== 拍照 ====================

    private void openCamera() {
        try {
            File photoFile = createTempImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (Exception e) {
            imagePickerLauncher.launch("image/*");
        }
    }

    private File createTempImageFile() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return File.createTempFile("IMG_" + ts + "_", ".jpg", requireContext().getCacheDir());
    }

    private void updateCameraIcon(String questionId, boolean hasPhoto) {
        for (int i = 0; i < llQuestionContainer.getChildCount(); i++) {
            View card = llQuestionContainer.getChildAt(i);
            View camBtn = card.findViewWithTag("camera_" + questionId);
            if (camBtn instanceof LinearLayout) {
                ImageView iv = (ImageView)((LinearLayout) camBtn).getChildAt(0);
                TextView tv = (TextView)((LinearLayout) camBtn).getChildAt(1);
                if (hasPhoto) {
                    tv.setText("已拍");
                    iv.setColorFilter(Color.parseColor("#10B981"));
                } else {
                    tv.setText("拍照");
                    iv.setColorFilter(getResources().getColor(R.color.primary_color, null));
                }
            }
        }
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}
