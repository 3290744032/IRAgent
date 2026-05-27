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
import android.widget.Button;
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
    private LinearLayout llQuestionContainer;
    private Button btnSubmitAll;
    private TextView tvResultScore;
    private LinearLayout llResultStats, llResultDetails;
    private Button btnBackHub;

    private List<PracticeQuestion> questions;
    private Map<String, PracticeQuestion> questionMap;
    private String sessionId;

    private String currentQuestionIdForCamera;
    private Uri cameraPhotoUri;
    private Set<String> hiddenFeedbackQuestions = new HashSet<>();

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
        llQuestionContainer = view.findViewById(R.id.ll_question_container);
        btnSubmitAll = view.findViewById(R.id.btn_submit_all);
        tvResultScore = view.findViewById(R.id.tv_result_score);
        llResultStats = view.findViewById(R.id.ll_result_stats);
        llResultDetails = view.findViewById(R.id.ll_result_details);
        btnBackHub = view.findViewById(R.id.btn_back_hub);

        String subject = getArguments() != null ? getArguments().getString("subject", "") : "";
        viewModel.loadPractice(subject, 5);

        btnSubmitAll.setOnClickListener(v -> onSubmitAll());

        Button btnSkip = new Button(requireContext());
        btnSkip.setText("跳过，部分提交");
        btnSkip.setTextSize(14);
        btnSkip.setBackgroundResource(R.drawable.bg_quick_chip);
        btnSkip.setTextColor(Color.parseColor("#6B7280"));
        btnSkip.setAllCaps(false);
        LinearLayout.LayoutParams skipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skipLp.setMargins(0, 12, 0, 0);
        btnSkip.setLayoutParams(skipLp);
        btnSkip.setOnClickListener(v -> {
            if (sessionId != null) {
                viewModel.submitAll(sessionId, "daily_practice");
            }
        });
        ((LinearLayout) btnSubmitAll.getParent()).addView(btnSkip);

        btnBackHub.setOnClickListener(v -> requireActivity().onBackPressed());

        setupObservers();
    }

    private void onSubmitAll() {
        boolean hasEmpty = false;
        if (questions != null) {
            for (PracticeQuestion q : questions) {
                String answer = viewModel.getAnswerMap().get(q.getId());
                boolean hasPhoto = viewModel.getPhotoUriMap().containsKey(q.getId());
                if ((answer == null || answer.trim().isEmpty()) && !hasPhoto) {
                    hasEmpty = true;
                    Toast.makeText(getContext(), "请回答第 " + (q.getIndex() + 1) + " 题", Toast.LENGTH_SHORT).show();
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
                for (PracticeQuestion q : questions) {
                    questionMap.put(q.getId(), q);
                }
                buildQuestionCards();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                layoutQuiz.setVisibility(View.GONE);
                layoutResult.setVisibility(View.GONE);
                layoutLoading.setVisibility(View.VISIBLE);
            } else {
                layoutLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                renderResult(result);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void buildQuestionCards() {
        llQuestionContainer.removeAllViews();
        if (questions == null) return;

        for (int i = 0; i < questions.size(); i++) {
            PracticeQuestion q = questions.get(i);
            LinearLayout card = createQuestionCard(q, i);
            llQuestionContainer.addView(card);
        }

        layoutQuiz.setVisibility(View.VISIBLE);
        layoutLoading.setVisibility(View.GONE);
        layoutResult.setVisibility(View.GONE);
    }

    private LinearLayout createQuestionCard(PracticeQuestion q, int index) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cp);

        // Question header (index + type + source tag)
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvIndex = new TextView(requireContext());
        tvIndex.setText("第 " + (index + 1) + " 题");
        tvIndex.setTextSize(13);
        tvIndex.setTextColor(Color.parseColor("#6366F1"));
        tvIndex.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(tvIndex);

        if (q.getQuestionType() != null) {
            TextView tvType = new TextView(requireContext());
            tvType.setText("  ·  " + q.getQuestionType());
            tvType.setTextSize(12);
            tvType.setTextColor(Color.parseColor("#9CA3AF"));
            headerRow.addView(tvType);
        }

        // Source tag
        if (q.getSource() != null) {
            TextView sourceTag = new TextView(requireContext());
            sourceTag.setTextSize(10);
            sourceTag.setPadding(6, 2, 6, 2);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(8, 0, 0, 0);
            sourceTag.setLayoutParams(slp);
            if ("official".equals(q.getSource())) {
                sourceTag.setText("真题");
                sourceTag.setTextColor(Color.parseColor("#6B7280"));
                sourceTag.setBackgroundResource(R.drawable.bg_option_unselected);
            } else if ("ai-generated".equals(q.getSource())) {
                sourceTag.setText("AI 生成");
                sourceTag.setTextColor(Color.parseColor("#8B5CF6"));
                sourceTag.setBackgroundResource(R.drawable.bg_option_unselected);
            } else if ("user-contributed".equals(q.getSource())) {
                sourceTag.setText("用户上传");
                sourceTag.setTextColor(Color.parseColor("#3B82F6"));
                sourceTag.setBackgroundResource(R.drawable.bg_option_unselected);
            }
            headerRow.addView(sourceTag);
        }

        card.addView(headerRow);

        // Question text
        TextView tvQuestion = new TextView(requireContext());
        tvQuestion.setText(q.getQuestionText());
        tvQuestion.setTextSize(15);
        tvQuestion.setTextColor(Color.parseColor("#1F2937"));
        tvQuestion.setLineSpacing(4, 1f);
        tvQuestion.setPadding(0, 8, 0, 0);
        card.addView(tvQuestion);

        // Answer input row
        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(0, 12, 0, 0);

        EditText etAnswer = new EditText(requireContext());
        etAnswer.setHint("请输入答案...");
        etAnswer.setTextSize(14);
        etAnswer.setTextColor(Color.parseColor("#1F2937"));
        etAnswer.setHintTextColor(Color.parseColor("#9CA3AF"));
        etAnswer.setPadding(12, 8, 12, 8);
        etAnswer.setBackgroundResource(R.drawable.bg_option_unselected);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ep.setMargins(0, 0, 8, 0);
        etAnswer.setLayoutParams(ep);
        etAnswer.setMinHeight(40);

        final String questionId = q.getId();
        // Restore existing answer if any
        String existing = viewModel.getAnswerMap().get(questionId);
        if (existing != null) {
            etAnswer.setText(existing);
        }

        etAnswer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setAnswer(questionId, s.toString());
            }
        });
        inputRow.addView(etAnswer);

        // Camera button
        LinearLayout cameraBtn = new LinearLayout(requireContext());
        cameraBtn.setOrientation(LinearLayout.VERTICAL);
        cameraBtn.setGravity(Gravity.CENTER);
        cameraBtn.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams camp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cameraBtn.setLayoutParams(camp);

        ImageView ivCamera = new ImageView(requireContext());
        ivCamera.setImageResource(android.R.drawable.ic_menu_camera);
        ivCamera.setColorFilter(Color.parseColor("#6366F1"));
        ivCamera.setLayoutParams(new LinearLayout.LayoutParams(32, 32));

        TextView tvCameraLabel = new TextView(requireContext());
        tvCameraLabel.setText("拍照");
        tvCameraLabel.setTextSize(10);
        tvCameraLabel.setTextColor(Color.parseColor("#6366F1"));
        tvCameraLabel.setGravity(Gravity.CENTER);

        cameraBtn.addView(ivCamera);
        cameraBtn.addView(tvCameraLabel);
        cameraBtn.setTag("camera_" + questionId);

        cameraBtn.setOnClickListener(v -> {
            currentQuestionIdForCamera = questionId;
            showCameraOrGalleryPicker();
        });

        // Check if photo already exists
        if (viewModel.getPhotoUriMap().containsKey(questionId)) {
            tvCameraLabel.setText("已拍照");
            ivCamera.setColorFilter(Color.parseColor("#10B981"));
        }

        inputRow.addView(cameraBtn);
        card.addView(inputRow);

        // Feedback button for ai-generated questions
        if ("ai-generated".equals(q.getSource()) && !hiddenFeedbackQuestions.contains(q.getId())) {
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
                hiddenFeedbackQuestions.add(questionId);
                viewModel.submitDailyFeedback(questionId, "wrong_question", "");
                btnFeedback.setVisibility(View.GONE);
            });
            card.addView(btnFeedback);
        }

        return card;
    }

    private void showCameraOrGalleryPicker() {
        // Simple: launch camera directly. Could show a bottom sheet like StudyFragmentV3
        try {
            File photoFile = createTempImageFile();
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (Exception e) {
            // Fallback to gallery picker
            imagePickerLauncher.launch("image/*");
        }
    }

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File cacheDir = requireContext().getCacheDir();
        return File.createTempFile("IMG_" + timeStamp + "_", ".jpg", cacheDir);
    }

    private void updateCameraIcon(String questionId, boolean hasPhoto) {
        for (int i = 0; i < llQuestionContainer.getChildCount(); i++) {
            View card = llQuestionContainer.getChildAt(i);
            View cameraBtn = card.findViewWithTag("camera_" + questionId);
            if (cameraBtn instanceof LinearLayout) {
                ImageView iv = (ImageView) ((LinearLayout) cameraBtn).getChildAt(0);
                TextView tv = (TextView) ((LinearLayout) cameraBtn).getChildAt(1);
                if (hasPhoto) {
                    tv.setText("已拍照");
                    iv.setColorFilter(Color.parseColor("#10B981"));
                } else {
                    tv.setText("拍照");
                    iv.setColorFilter(Color.parseColor("#6366F1"));
                }
            }
        }
    }

    private void renderResult(SubmitAnswerResult result) {
        layoutQuiz.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutResult.setVisibility(View.VISIBLE);

        int accuracy = (int) (result.getAccuracy() * 100);
        int score = (int) (result.getAccuracy() * 100);
        tvResultScore.setText(String.valueOf(score));
        tvResultScore.setTextColor(accuracy >= 80 ? Color.parseColor("#10B981") :
                accuracy >= 60 ? Color.parseColor("#F59E0B") : Color.parseColor("#EF4444"));

        llResultStats.removeAllViews();
        String[] labels = {"正确", "错误", "正确率"};
        int[] values = {result.getCorrectCount(), result.getWrongCount(), accuracy};
        int[] colors = {Color.parseColor("#10B981"), Color.parseColor("#EF4444"), Color.parseColor("#6366F1")};
        for (int i = 0; i < 3; i++) {
            LinearLayout stat = new LinearLayout(requireContext());
            stat.setOrientation(LinearLayout.VERTICAL);
            stat.setGravity(Gravity.CENTER);
            stat.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stat.setLayoutParams(sp);
            TextView tvVal = new TextView(requireContext());
            tvVal.setText(String.valueOf(values[i]) + (i == 2 ? "%" : ""));
            tvVal.setTextSize(20);
            tvVal.setTextColor(colors[i]);
            tvVal.setGravity(Gravity.CENTER);
            stat.addView(tvVal);
            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(11);
            tvLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            tvLabel.setGravity(Gravity.CENTER);
            stat.addView(tvLabel);
            llResultStats.addView(stat);
        }

        // Hint that errors are auto-recorded
        TextView tvHint = new TextView(requireContext());
        tvHint.setText("错题已自动收录到错题本");
        tvHint.setTextSize(12);
        tvHint.setTextColor(Color.parseColor("#6B7280"));
        tvHint.setGravity(Gravity.CENTER);
        tvHint.setPadding(0, 8, 0, 16);
        llResultStats.addView(tvHint);

        llResultDetails.removeAllViews();
        if (result.getDetails() != null) {
            for (SubmitAnswerResult.AnswerDetail detail : result.getDetails()) {
                PracticeQuestion q = questionMap != null ? questionMap.get(detail.getQuestionId()) : null;

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(16, 12, 16, 12);
                row.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.setMargins(0, 0, 0, 8);
                row.setLayoutParams(rp);

                TextView tvQText = new TextView(requireContext());
                tvQText.setText(q != null ? q.getQuestionText() : detail.getQuestionId());
                tvQText.setTextSize(14);
                tvQText.setTextColor(Color.parseColor("#1F2937"));
                tvQText.setTypeface(Typeface.DEFAULT_BOLD);
                tvQText.setLineSpacing(4, 1f);
                row.addView(tvQText);

                LinearLayout answerRow = new LinearLayout(requireContext());
                answerRow.setOrientation(LinearLayout.HORIZONTAL);
                answerRow.setPadding(0, 8, 0, 0);

                TextView tvStatus = new TextView(requireContext());
                tvStatus.setText(detail.isCorrect() ? "\u2713 正确" : "\u2717 错误");
                tvStatus.setTextSize(12);
                tvStatus.setTextColor(detail.isCorrect() ?
                        Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                tvStatus.setPadding(0, 0, 12, 0);
                answerRow.addView(tvStatus);

                if (!detail.isCorrect()) {
                    TextView tvYourLabel = new TextView(requireContext());
                    tvYourLabel.setText("你的答案: ");
                    tvYourLabel.setTextSize(12);
                    tvYourLabel.setTextColor(Color.parseColor("#9CA3AF"));
                    answerRow.addView(tvYourLabel);

                    TextView tvYourVal = new TextView(requireContext());
                    tvYourVal.setText(detail.getSelectedAnswer());
                    tvYourVal.setTextSize(12);
                    tvYourVal.setTextColor(Color.parseColor("#EF4444"));
                    tvYourVal.setPaintFlags(tvYourVal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    tvYourVal.setPadding(0, 0, 12, 0);
                    answerRow.addView(tvYourVal);

                    TextView tvCorrectLabel = new TextView(requireContext());
                    tvCorrectLabel.setText("正确答案: ");
                    tvCorrectLabel.setTextSize(12);
                    tvCorrectLabel.setTextColor(Color.parseColor("#9CA3AF"));
                    answerRow.addView(tvCorrectLabel);

                    TextView tvCorrectVal = new TextView(requireContext());
                    tvCorrectVal.setText(detail.getCorrectAnswer());
                    tvCorrectVal.setTextSize(12);
                    tvCorrectVal.setTextColor(Color.parseColor("#10B981"));
                    tvCorrectVal.setTypeface(Typeface.DEFAULT_BOLD);
                    answerRow.addView(tvCorrectVal);
                }

                row.addView(answerRow);

                if (detail.getExplanation() != null && !detail.getExplanation().isEmpty()) {
                    TextView tvExplanation = new TextView(requireContext());
                    tvExplanation.setText("解析: " + detail.getExplanation());
                    tvExplanation.setTextSize(12);
                    tvExplanation.setTextColor(Color.parseColor("#6B7280"));
                    tvExplanation.setLineSpacing(4, 1f);
                    tvExplanation.setPadding(0, 8, 0, 0);
                    row.addView(tvExplanation);
                }

                llResultDetails.addView(row);
            }
        }
    }
}
