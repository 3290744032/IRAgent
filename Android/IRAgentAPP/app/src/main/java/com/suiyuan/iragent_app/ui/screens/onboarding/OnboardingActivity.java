package com.suiyuan.iragent_app.ui.screens.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.ui.screens.main.MainActivity;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private int currentStep = 0;
    private static final int TOTAL_STEPS = 3;

    private LinearLayout llProgressDots;
    private FrameLayout flStepContainer;
    private Button btnSkip, btnNext;

    private String selectedExamType = "";
    private int targetScore = 130;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom);
            return insets;
        });

        llProgressDots = findViewById(R.id.ll_progress_dots);
        flStepContainer = findViewById(R.id.fl_step_container);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> advanceStep());

        showStep(0);
    }

    private void showStep(int step) {
        currentStep = step;
        updateProgressDots();

        btnSkip.setVisibility(step < TOTAL_STEPS - 1 ? View.VISIBLE : View.GONE);
        btnNext.setText(step == TOTAL_STEPS - 1 ? "开始使用" : "下一步");

        flStepContainer.removeAllViews();

        switch (step) {
            case 0: flStepContainer.addView(buildStep1()); break;
            case 1: flStepContainer.addView(buildStep2()); break;
            case 2: flStepContainer.addView(buildStep3()); break;
        }
    }

    private void advanceStep() {
        if (currentStep < TOTAL_STEPS - 1) {
            showStep(currentStep + 1);
        } else {
            finishOnboarding();
        }
    }

    private void finishOnboarding() {
        PreferencesManager pm = new PreferencesManager(this);
        pm.setOnboardingCompleted(true);
        pm.setExamType(selectedExamType);
        pm.setTargetScore(targetScore);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void updateProgressDots() {
        llProgressDots.removeAllViews();
        for (int i = 0; i < TOTAL_STEPS; i++) {
            View dot = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == currentStep ? (int)(24 * getResources().getDisplayMetrics().density) : size,
                    size);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(i <= currentStep ?
                    getResources().getColor(R.color.primary_color, null) :
                    getResources().getColor(R.color.gray_light, null));
            llProgressDots.addView(dot);
        }
    }

    private View buildStep1() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView icon = new TextView(this);
        icon.setText("📚");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("选择你的考试");
        title.setTextSize(24);
        title.setTextColor(getResources().getColor(R.color.on_surface, null));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 24, 0, 8);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("AI 将根据你的考试类型，智能适配考纲和学习路径");
        desc.setTextSize(14);
        desc.setTextColor(getResources().getColor(R.color.gray_text, null));
        desc.setGravity(Gravity.CENTER);
        layout.addView(desc);

        // Exam type cards in a grid
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, 32, 0, 0);

        List<String[]> exams = Arrays.asList(
                new String[]{"🎓", "考研"},
                new String[]{"📝", "高考"},
                new String[]{"📖", "中考"},
                new String[]{"🏛", "公务员"},
                new String[]{"🌍", "雅思/托福"},
                new String[]{"📋", "其他"}
        );

        LinearLayout row = null;
        for (int i = 0; i < exams.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row);
            }

            String[] exam = exams.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(24, 16, 24, 16);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cp.setMargins(4, 4, 4, 4);
            card.setLayoutParams(cp);
            card.setBackgroundColor(Color.WHITE);

            TextView cardIcon = new TextView(this);
            cardIcon.setText(exam[0]);
            cardIcon.setTextSize(36);
            cardIcon.setGravity(Gravity.CENTER);
            card.addView(cardIcon);

            TextView cardLabel = new TextView(this);
            cardLabel.setText(exam[1]);
            cardLabel.setTextSize(16);
            cardLabel.setTextColor(getResources().getColor(R.color.on_surface, null));
            cardLabel.setGravity(Gravity.CENTER);
            card.addView(cardLabel);

            String examName = exam[1];
            card.setOnClickListener(v -> {
                for (int j = 0; j < ((LinearLayout)card.getParent()).getChildCount(); j++) {
                    View child = ((LinearLayout)card.getParent()).getChildAt(j);
                    child.setBackgroundColor(Color.WHITE);
                }
                card.setBackgroundColor(getResources().getColor(R.color.primary_light, null));
                selectedExamType = examName;
            });

            if (row != null) row.addView(card);
        }

        layout.addView(grid);
        return layout;
    }

    private View buildStep2() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView icon = new TextView(this);
        icon.setText("🎯");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("设定目标分数");
        title.setTextSize(24);
        title.setTextColor(getResources().getColor(R.color.on_surface, null));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 24, 0, 8);
        layout.addView(title);

        TextView scoreDisplay = new TextView(this);
        scoreDisplay.setText(String.valueOf(targetScore));
        scoreDisplay.setTextSize(48);
        scoreDisplay.setTextColor(getResources().getColor(R.color.primary_color, null));
        scoreDisplay.setGravity(Gravity.CENTER);
        scoreDisplay.setPadding(0, 32, 0, 16);
        layout.addView(scoreDisplay);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(90); // 60 to 150
        seekBar.setProgress(targetScore - 60);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                targetScore = progress + 60;
                scoreDisplay.setText(String.valueOf(targetScore));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        layout.addView(seekBar);

        LinearLayout rangeRow = new LinearLayout(this);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView minLabel = new TextView(this);
        minLabel.setText("60");
        minLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        rangeRow.addView(minLabel);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        rangeRow.addView(spacer);
        TextView maxLabel = new TextView(this);
        maxLabel.setText("150");
        maxLabel.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        rangeRow.addView(maxLabel);
        layout.addView(rangeRow);

        return layout;
    }

    private View buildStep3() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 40, 32, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView icon = new TextView(this);
        icon.setText("📤");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        layout.addView(icon);

        TextView title = new TextView(this);
        title.setText("上传你的笔记");
        title.setTextSize(24);
        title.setTextColor(getResources().getColor(R.color.on_surface, null));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 24, 0, 8);
        layout.addView(title);

        TextView desc = new TextView(this);
        desc.setText("上传课堂笔记或错题本，AI 将提取知识点并建立你的专属知识库");
        desc.setTextSize(14);
        desc.setTextColor(getResources().getColor(R.color.gray_text, null));
        desc.setGravity(Gravity.CENTER);
        layout.addView(desc);

        LinearLayout uploadZone = new LinearLayout(this);
        uploadZone.setOrientation(LinearLayout.VERTICAL);
        uploadZone.setGravity(Gravity.CENTER);
        uploadZone.setPadding(32, 48, 32, 48);
        uploadZone.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams uzParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        uzParams.setMargins(0, 48, 0, 0);
        uploadZone.setLayoutParams(uzParams);

        TextView uploadIcon = new TextView(this);
        uploadIcon.setText("📄");
        uploadIcon.setTextSize(48);
        uploadZone.addView(uploadIcon);

        TextView uploadText = new TextView(this);
        uploadText.setText("点击上传笔记");
        uploadText.setTextSize(16);
        uploadText.setTextColor(getResources().getColor(R.color.on_surface, null));
        uploadText.setPadding(0, 12, 0, 4);
        uploadZone.addView(uploadText);

        TextView uploadHint = new TextView(this);
        uploadHint.setText("支持 PDF、Word、图片、Markdown");
        uploadHint.setTextSize(12);
        uploadHint.setTextColor(getResources().getColor(R.color.text_tertiary, null));
        uploadZone.addView(uploadHint);

        uploadZone.setOnClickListener(v -> {
            // Launch file picker to upload notes directly
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 100);
        });

        layout.addView(uploadZone);

        return layout;
    }
}
