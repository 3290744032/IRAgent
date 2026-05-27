package com.suiyuan.iragent_app.ui.screens.practice;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.suiyuan.iragent_app.data.model.v3.DiagnosisJson;
import com.suiyuan.iragent_app.data.model.v3.GradedQuestion;
import com.suiyuan.iragent_app.data.model.v3.GradingReport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PracticeHubFragment extends Fragment {

    private PracticeHubViewModel viewModel;

    private View layoutHub, layoutGrading;
    private View layoutGradingInput, layoutGradingProgress, layoutGradingReport;
    private TextView tvGradingStatus;
    private ProgressBar pbGrading;
    private TextView tvReportScore, tvReportLabel;
    private LinearLayout llReportStats, llReportQuestions;
    private String mMathTemplate;

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
        tvGradingStatus = view.findViewById(R.id.tv_grading_status);
        pbGrading = view.findViewById(R.id.pb_grading);
        tvReportScore = view.findViewById(R.id.tv_report_score);
        tvReportLabel = view.findViewById(R.id.tv_report_label);
        llReportStats = view.findViewById(R.id.ll_report_stats);
        llReportQuestions = view.findViewById(R.id.ll_report_questions);

        loadMathTemplate();

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

            // 切换到批改视图，照片置顶显示
            layoutHub.setVisibility(View.GONE);
            layoutGrading.setVisibility(View.VISIBLE);
            layoutGradingInput.setVisibility(View.GONE);
            layoutGradingProgress.setVisibility(View.VISIBLE);
            layoutGradingReport.setVisibility(View.GONE);

            // 照片显示（复用或新建 ImageView）
            android.widget.ImageView iv = layoutGradingProgress.findViewWithTag("grading_image");
            if (iv == null) {
                iv = new android.widget.ImageView(requireContext());
                iv.setTag("grading_image");
                iv.setAdjustViewBounds(true);
                iv.setMaxHeight((int)(300 * getResources().getDisplayMetrics().density));
                iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                iv.setPadding(0, 0, 0, 16);
                ((LinearLayout)layoutGradingProgress).addView(iv, 0);
            }
            iv.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            iv.setVisibility(View.VISIBLE);

            tvGradingStatus.setText("豆包视觉批改中...");
            viewModel.submitImageGrading(bytes, "数学", 100);
        } catch (Exception e) {
            Toast.makeText(getContext(), "图片读取失败", Toast.LENGTH_SHORT).show();
        }
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
                LinearLayout card = new LinearLayout(requireContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 12, 16, 12);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cp.setMargins(0, 0, 0, 12);
                card.setLayoutParams(cp);
                card.setBackgroundResource(R.drawable.bg_ai_bubble);

                // Header: index + knowledge point + correct/wrong
                LinearLayout header = new LinearLayout(requireContext());
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setPadding(0, 0, 0, 8);
                header.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView tvIdx = new TextView(requireContext());
                tvIdx.setText(" #" + q.getIndex() + " ");
                tvIdx.setTextSize(13);
                tvIdx.setTypeface(null, android.graphics.Typeface.BOLD);
                tvIdx.setTextColor(getResources().getColor(R.color.on_surface, null));
                header.addView(tvIdx);

                TextView tvTopic = new TextView(requireContext());
                tvTopic.setText(q.getKnowledgePoint());
                tvTopic.setTextSize(13);
                tvTopic.setTextColor(getResources().getColor(R.color.gray_text, null));
                LinearLayout.LayoutParams topicLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tvTopic.setLayoutParams(topicLp);
                header.addView(tvTopic);

                TextView tvResult = new TextView(requireContext());
                tvResult.setText(q.isCorrect() ? "✓ 正确" : "✗ 错误");
                tvResult.setTextSize(12);
                tvResult.setTypeface(null, android.graphics.Typeface.BOLD);
                tvResult.setTextColor(q.isCorrect() ?
                        Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                header.addView(tvResult);

                card.addView(header);

                // Question text
                if (q.getQuestionText() != null && !q.getQuestionText().isEmpty()) {
                    TextView labelQ = new TextView(requireContext());
                    labelQ.setText("题目");
                    labelQ.setTextSize(11);
                    labelQ.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                    labelQ.setPadding(0, 4, 0, 2);
                    card.addView(labelQ);
                    WebView wvQ = createMathWebView();
                    renderMathInWebView(wvQ, q.getQuestionText());
                    card.addView(wvQ);
                }

                // Student answer
                if (q.getStudentAnswer() != null && !q.getStudentAnswer().isEmpty()) {
                    TextView labelS = new TextView(requireContext());
                    labelS.setText("你的答案");
                    labelS.setTextSize(11);
                    labelS.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                    labelS.setPadding(0, 4, 0, 2);
                    card.addView(labelS);
                    WebView wvS = createMathWebView();
                    renderMathInWebView(wvS, q.getStudentAnswer());
                    card.addView(wvS);
                }

                // Correct answer
                if (q.getCorrectAnswer() != null && !q.getCorrectAnswer().isEmpty()) {
                    TextView labelC = new TextView(requireContext());
                    labelC.setText("正确答案");
                    labelC.setTextSize(11);
                    labelC.setTextColor(getResources().getColor(R.color.text_tertiary, null));
                    labelC.setPadding(0, 4, 0, 2);
                    card.addView(labelC);
                    WebView wvC = createMathWebView();
                    renderMathInWebView(wvC, q.getCorrectAnswer());
                    card.addView(wvC);
                }

                // Score
                TextView tvScore = new TextView(requireContext());
                tvScore.setText("得分: " + q.getScore() + " / " + q.getMaxScore());
                tvScore.setTextSize(12);
                tvScore.setTextColor(getResources().getColor(R.color.gray_text, null));
                tvScore.setPadding(0, 6, 0, 2);
                card.addView(tvScore);

                // Diagnosis (if wrong and diagnosis exists)
                String analysis = getDiagnosisText(q.getDiagnosis());
                if (!q.isCorrect() && analysis != null && !analysis.isEmpty()) {
                    TextView labelD = new TextView(requireContext());
                    labelD.setText("解析");
                    labelD.setTextSize(11);
                    labelD.setTextColor(Color.parseColor("#6366F1"));
                    labelD.setPadding(0, 6, 0, 2);
                    card.addView(labelD);
                    WebView wvD = createMathWebView();
                    renderMathInWebView(wvD, analysis);
                    card.addView(wvD);
                }

                llReportQuestions.addView(card);
            }
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

    private WebView createMathWebView() {
        WebView wv = new WebView(requireContext());
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(false);
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.setPadding(0, 2, 0, 2);
        LinearLayout.LayoutParams wvp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wv.setLayoutParams(wvp);
        return wv;
    }

    private void renderMathInWebView(WebView wv, String content) {
        String escaped = escapeJsString(content);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("renderMathContent('" + escaped + "')", null);
            }
        });
        wv.loadDataWithBaseURL("https://cdn.jsdelivr.net", mMathTemplate,
                "text/html", "UTF-8", null);
    }

    private String getDiagnosisText(DiagnosisJson diag) {
        if (diag == null) return null;
        StringBuilder sb = new StringBuilder();
        if (diag.getFormulaConfusion() != null && diag.getFormulaConfusion().getAnalysis() != null)
            sb.append(diag.getFormulaConfusion().getAnalysis()).append("\n\n");
        if (diag.getCalculationError() != null && diag.getCalculationError().getAnalysis() != null)
            sb.append(diag.getCalculationError().getAnalysis()).append("\n\n");
        if (diag.getPrerequisiteCheck() != null && diag.getPrerequisiteCheck().getAnalysis() != null)
            sb.append(diag.getPrerequisiteCheck().getAnalysis());
        return sb.toString().trim();
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
}
