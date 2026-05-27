package com.suiyuan.iragent_app.ui.screens.practice;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.GradedQuestion;
import com.suiyuan.iragent_app.data.model.v3.GradingReport;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PracticeHubFragment extends Fragment {

    private PracticeHubViewModel viewModel;

    private View layoutHub, layoutGrading;
    private View layoutGradingInput, layoutGradingProgress, layoutGradingReport;
    private EditText etContent, etMaxScore;
    private Spinner spSubject;
    private TextView tvGradingStatus;
    private ProgressBar pbGrading;
    private TextView tvReportScore, tvReportLabel;
    private LinearLayout llReportStats, llReportQuestions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_practice_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PracticeHubViewModel.class);

        layoutHub = view.findViewById(R.id.layout_hub);
        layoutGrading = view.findViewById(R.id.layout_grading);
        layoutGradingInput = view.findViewById(R.id.layout_grading_input);
        layoutGradingProgress = view.findViewById(R.id.layout_grading_progress);
        layoutGradingReport = view.findViewById(R.id.layout_grading_report);
        etContent = view.findViewById(R.id.et_grading_content);
        etMaxScore = view.findViewById(R.id.et_max_score);
        spSubject = view.findViewById(R.id.sp_subject);
        tvGradingStatus = view.findViewById(R.id.tv_grading_status);
        pbGrading = view.findViewById(R.id.pb_grading);
        tvReportScore = view.findViewById(R.id.tv_report_score);
        tvReportLabel = view.findViewById(R.id.tv_report_label);
        llReportStats = view.findViewById(R.id.ll_report_stats);
        llReportQuestions = view.findViewById(R.id.ll_report_questions);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"数学", "物理", "化学", "英语", "政治", "历史"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(adapter);

        view.findViewById(R.id.btn_start_grading).setOnClickListener(v -> startGrading());
        view.findViewById(R.id.btn_pick_image).setOnClickListener(v -> pickImageForGrading());
        view.findViewById(R.id.btn_grading_back).setOnClickListener(v -> backToHub());
        view.findViewById(R.id.btn_grading).setOnClickListener(v -> enterGrading());
        NavController navController = Navigation.findNavController(view);
        view.findViewById(R.id.btn_smart_paper).setOnClickListener(v ->
                navController.navigate(R.id.nav_smart_paper));
        view.findViewById(R.id.btn_daily_practice).setOnClickListener(v ->
                navController.navigate(R.id.nav_daily_practice));
        view.findViewById(R.id.btn_exam_archive).setOnClickListener(v ->
                navController.navigate(R.id.nav_exam_archive));

        setupObservers();
    }

    private void enterGrading() {
        layoutHub.setVisibility(View.GONE);
        layoutGrading.setVisibility(View.VISIBLE);
        layoutGradingInput.setVisibility(View.VISIBLE);
        layoutGradingProgress.setVisibility(View.GONE);
        layoutGradingReport.setVisibility(View.GONE);
    }

    private void backToHub() {
        layoutGrading.setVisibility(View.GONE);
        layoutHub.setVisibility(View.VISIBLE);
    }

    private Uri cameraPhotoUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) startImageGrading(uri);
        });

    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraPhotoUri != null) startImageGrading(cameraPhotoUri);
            cameraPhotoUri = null;
        });

    private void pickImageForGrading() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 24, 16, 32);

        TextView title = new TextView(requireContext());
        title.setText("选择图片来源");
        title.setTextSize(16);
        title.setTextColor(Color.parseColor("#1F2937"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        Button btnCamera = new Button(requireContext());
        btnCamera.setText("📷 拍照");
        btnCamera.setTextSize(14);
        btnCamera.setAllCaps(false);
        btnCamera.setBackgroundResource(R.drawable.bg_quick_chip);
        btnCamera.setTextColor(Color.parseColor("#1F2937"));
        btnCamera.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btnCamera.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 8);
        btnCamera.setLayoutParams(cp);
        btnCamera.setOnClickListener(v -> {
            sheet.dismiss();
            try {
                File photoFile = createTempImageFile();
                cameraPhotoUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(cameraPhotoUri);
            } catch (Exception e) {
                Toast.makeText(getContext(), "无法启动相机", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnCamera);

        Button btnGallery = new Button(requireContext());
        btnGallery.setText("🖼 从相册选择");
        btnGallery.setTextSize(14);
        btnGallery.setAllCaps(false);
        btnGallery.setBackgroundResource(R.drawable.bg_quick_chip);
        btnGallery.setTextColor(Color.parseColor("#1F2937"));
        btnGallery.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btnGallery.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnGallery.setLayoutParams(gp);
        btnGallery.setOnClickListener(v -> {
            sheet.dismiss();
            imagePickerLauncher.launch("image/*");
        });
        layout.addView(btnGallery);

        sheet.setContentView(layout);
        sheet.show();
    }

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File cacheDir = requireContext().getCacheDir();
        return File.createTempFile("IMG_" + timeStamp + "_", ".jpg", cacheDir);
    }

    private void startImageGrading(android.net.Uri uri) {
        try {
            java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            is.close();
            viewModel.submitImageGrading(bytes, spSubject.getSelectedItem().toString(),
                    Integer.parseInt(etMaxScore.getText().toString()));
        } catch (Exception e) {
            Toast.makeText(getContext(), "图片读取失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void startGrading() {
        String content = etContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "请输入试卷内容", Toast.LENGTH_SHORT).show();
            return;
        }
        String subject = spSubject.getSelectedItem().toString();
        int maxScore = 100;
        try { maxScore = Integer.parseInt(etMaxScore.getText().toString()); } catch (Exception ignored) {}

        layoutGradingInput.setVisibility(View.GONE);
        layoutGradingProgress.setVisibility(View.VISIBLE);
        viewModel.submitGrading(content, subject, maxScore);
    }

    private void setupObservers() {
        viewModel.getGradingStep().observe(getViewLifecycleOwner(), step -> {
            if (step != null) tvGradingStatus.setText(step);
        });

        viewModel.getGradingProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) pbGrading.setProgress(progress);
        });

        viewModel.getGradingReport().observe(getViewLifecycleOwner(), report -> {
            layoutGradingProgress.setVisibility(View.GONE);
            layoutGradingReport.setVisibility(View.VISIBLE);
            renderReport(report);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void renderReport(GradingReport report) {
        tvReportScore.setText(String.valueOf(report.getTotalScore()));
        tvReportLabel.setText("总分 / " + report.getMaxScore());

        llReportStats.removeAllViews();
        String[] labels = {"正确", "错误", "正确率"};
        int[] values = {report.getCorrectCount(), report.getWrongCount(),
                (int)(report.getAccuracy() * 100)};
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
            tvVal.setFontFeatureSettings("sans-serif-medium");
            stat.addView(tvVal);
            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(11);
            tvLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            tvLabel.setGravity(android.view.Gravity.CENTER);
            stat.addView(tvLabel);
            llReportStats.addView(stat);
        }

        llReportQuestions.removeAllViews();
        if (report.getQuestions() != null) {
            for (GradedQuestion q : report.getQuestions()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(16, 12, 16, 12);
                row.setOnClickListener(v -> Toast.makeText(getContext(),
                        q.getQuestionText(), Toast.LENGTH_SHORT).show());

                TextView tvIdx = new TextView(requireContext());
                tvIdx.setText(String.valueOf(q.getIndex()));
                tvIdx.setTextSize(12);
                tvIdx.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                tvIdx.setMinWidth(36);
                row.addView(tvIdx);

                TextView tvTopic = new TextView(requireContext());
                tvTopic.setText(q.getKnowledgePoint());
                tvTopic.setTextSize(13);
                tvTopic.setTextColor(getResources().getColor(R.color.gray_text, null));
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tvTopic.setLayoutParams(tp);
                row.addView(tvTopic);

                TextView tvResult = new TextView(requireContext());
                tvResult.setText(q.isCorrect() ? "✓ 正确" : "✗ 错误");
                tvResult.setTextSize(12);
                tvResult.setTextColor(q.isCorrect() ?
                        Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                row.addView(tvResult);

                llReportQuestions.addView(row);
            }
        }
    }
}
