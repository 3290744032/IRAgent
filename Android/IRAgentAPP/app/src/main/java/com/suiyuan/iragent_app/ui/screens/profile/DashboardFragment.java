package com.suiyuan.iragent_app.ui.screens.profile;

import android.content.Intent;
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

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.data.model.v3.DashboardOverview;
import com.suiyuan.iragent_app.data.model.v3.MasteryRadarData;
import com.suiyuan.iragent_app.data.model.v3.TaskItem;
import com.suiyuan.iragent_app.data.model.v3.WeeklyReport;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.ui.screens.auth.AuthActivity;
import com.suiyuan.iragent_app.ui.screens.onboarding.OnboardingActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private CoverageRingView coverageRing;
    private LinearLayout llCoverageStats, llTodayTasks, llWeeklyStats;
    private TextView tvGreeting, tvUserName, tvExamGoal;
    private RadarChart radarChart;
    private TextView tvRadarLabels, tvSettingsExam, tvSettingsNotes;
    private WeeklyReport lastReport;
    private DashboardOverview lastOverview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        coverageRing = view.findViewById(R.id.coverage_ring);
        llCoverageStats = view.findViewById(R.id.ll_coverage_stats);
        llTodayTasks = view.findViewById(R.id.ll_today_tasks);
        llWeeklyStats = view.findViewById(R.id.ll_weekly_stats);
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvExamGoal = view.findViewById(R.id.tv_exam_goal);
        radarChart = view.findViewById(R.id.radar_chart);
        tvRadarLabels = view.findViewById(R.id.tv_radar_labels);
        tvSettingsExam = view.findViewById(R.id.tv_settings_exam);
        tvSettingsNotes = view.findViewById(R.id.tv_settings_notes);

        setupRadarChart();

        PreferencesManager pm = new PreferencesManager(requireContext());
        // 从本地读取真实用户名，不硬编码
        String account = pm.getAccount();
        tvUserName.setText(account != null && !account.isEmpty() ? account : "同学");
        String examType = pm.getExamType();
        int targetScore = pm.getTargetScore();
        if (!examType.isEmpty()) {
            tvExamGoal.setText("目标：" + examType + " " + targetScore + "分");
            tvSettingsExam.setText(examType + " · " + targetScore + "分");
        }

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            pm.clearAll();
            NetworkClient.setToken(null);
            startActivity(new Intent(getContext(), AuthActivity.class));
            requireActivity().finish();
        });

        view.findViewById(R.id.ll_settings_exam).setOnClickListener(v ->
                Toast.makeText(getContext(), "考试目标：" + examType + " " + targetScore + "分", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.ll_settings_knowledge).setOnClickListener(v -> {
            androidx.navigation.NavController nc =
                    androidx.navigation.Navigation.findNavController(requireView());
            nc.navigate(R.id.nav_knowledge);
        });

        view.findViewById(R.id.ll_settings_export).setOnClickListener(v -> {
            DashboardOverview ov = viewModel.getOverview().getValue();
            StringBuilder csv = new StringBuilder("类别,数值\n");
            if (ov != null) {
                csv.append("笔记数量,").append(ov.getTotalNotes()).append("\n");
                csv.append("错题数量,").append(ov.getTotalErrors()).append("\n");
                csv.append("已掌握错题,").append(ov.getMasteredErrors()).append("\n");
                csv.append("错误率,").append(String.format("%.0f%%", ov.getErrorRate() * 100)).append("\n");
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_TEXT, csv.toString());
            share.putExtra(Intent.EXTRA_SUBJECT, "IRAgent 学习数据导出");
            startActivity(Intent.createChooser(share, "导出学习数据"));
        });

        view.findViewById(R.id.ll_settings_reonboard).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), OnboardingActivity.class));
        });

        setupObservers();
        viewModel.loadAllDashboard();
    }

    private void setupRadarChart() {
        radarChart.getDescription().setEnabled(false);
        radarChart.setWebLineWidth(1f);
        radarChart.setWebColor(Color.parseColor("#E5E7EB"));
        radarChart.setWebLineWidthInner(0.5f);
        radarChart.setWebColorInner(Color.parseColor("#E5E7EB"));
        radarChart.setWebAlpha(100);
        radarChart.setRotationEnabled(false);

        XAxis xAxis = radarChart.getXAxis();
        xAxis.setTextSize(10f);
        xAxis.setTextColor(getResources().getColor(R.color.gray_text, null));

        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setDrawLabels(false);
        yAxis.setAxisLineColor(Color.TRANSPARENT);
    }

    private void setupObservers() {
        viewModel.getOverview().observe(getViewLifecycleOwner(), overview -> {
            if (overview != null) {
                lastOverview = overview;
                coverageRing.setPercentage(calculateCoverage(overview));
                buildCoverageStats(overview);
                tvSettingsNotes.setText(overview.getTotalNotes() + " 份笔记");
            }
        });

        viewModel.getRadarData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) renderRadarChart(data);
        });

        viewModel.getTodayTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) buildTaskItems(tasks);
        });

        viewModel.getWeeklyReport().observe(getViewLifecycleOwner(), report -> {
            if (report != null) {
                lastReport = report;
                buildWeeklyReport(report);
                buildCoverageStats(lastOverview);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private float calculateCoverage(DashboardOverview overview) {
        int totalKps = overview.getMasteredErrors() + overview.getTotalErrors();
        int mastered = overview.getMasteredErrors();
        if (totalKps == 0) return 0f;
        return (float) overview.getMasteredErrors() / overview.getTotalErrors();
    }

    private void buildCoverageStats(DashboardOverview overview) {
        if (overview == null) return;
        llCoverageStats.removeAllViews();

        int totalKps = overview.getMasteredErrors() + overview.getTotalErrors();
        String masteredStr = overview.getMasteredErrors() + " / " + totalKps;

        String practiceCount = "0";
        String weeklyHours = "0";
        if (lastReport != null) {
            practiceCount = String.valueOf(lastReport.getWeekReports());
            weeklyHours = (lastReport.getTotalActivity() / 10f) + "h";
        }

        String[][] items = {
                {"已掌握考点", masteredStr, "#1F2937"},
                {"笔记数量", overview.getTotalNotes() + " 份", "#1F2937"},
                {"累计刷题", practiceCount + " 道", "#1F2937"},
                {"本周学习", weeklyHours, "#6366F1"}
        };

        for (String[] item : items) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 4, 0, 4);

            TextView label = new TextView(requireContext());
            label.setText(item[0]);
            label.setTextSize(12);
            label.setTextColor(getResources().getColor(R.color.gray_text, null));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lp);
            row.addView(label);

            TextView value = new TextView(requireContext());
            value.setText(item[1]);
            value.setTextSize(12);
            value.setTextColor(Color.parseColor(item[2]));
            value.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(value);

            llCoverageStats.addView(row);
        }
    }

    private void buildTaskItems(java.util.List<TaskItem> tasks) {
        llTodayTasks.removeAllViews();
        for (TaskItem task : tasks) {
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(16, 16, 16, 16);
            item.setBackgroundColor(Color.WHITE);
            item.setGravity(Gravity.CENTER_VERTICAL);

            TextView icon = new TextView(requireContext());
            icon.setText(getTaskIcon(task.getType()));
            icon.setTextSize(24);
            item.addView(icon);

            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(12, 0, 0, 0);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(ip);

            TextView title = new TextView(requireContext());
            title.setText(task.getTitle());
            title.setTextSize(14);
            title.setTextColor(getResources().getColor(R.color.on_surface, null));
            title.setTypeface(Typeface.DEFAULT_BOLD);
            info.addView(title);

            TextView desc = new TextView(requireContext());
            desc.setText(task.getDescription());
            desc.setTextSize(12);
            desc.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            info.addView(desc);

            item.addView(info);
            item.setOnClickListener(v -> {
                androidx.navigation.NavController nc =
                        androidx.navigation.Navigation.findNavController(requireView());
                switch (task.getType() != null ? task.getType() : "") {
                    case "review": nc.navigate(R.id.nav_errors); break;
                    case "practice": nc.navigate(R.id.nav_practice); break;
                    case "weakness": nc.navigate(R.id.nav_chat); break;
                    default: nc.navigate(R.id.nav_knowledge); break;
                }
            });

            llTodayTasks.addView(item);

            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(getResources().getColor(R.color.gray_light, null));
            llTodayTasks.addView(divider);
        }
    }

    private void buildWeeklyReport(WeeklyReport report) {
        llWeeklyStats.removeAllViews();

        // Set week range
        TextView tvRange = getView() != null ? getView().findViewById(R.id.tv_week_range) : null;
        if (tvRange != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("M月d日", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
            String start = sdf.format(cal.getTime());
            cal.add(java.util.Calendar.DAY_OF_MONTH, 6);
            String end = sdf.format(cal.getTime());
            tvRange.setText(start + " — " + end);
        }

        int totalPractice = report.getWeekReports();
        int errorCount = report.getWeekErrors();
        String correctRateStr;
        if (totalPractice > 0) {
            correctRateStr = Math.max(0, (totalPractice - errorCount) * 100 / totalPractice) + "%";
        } else {
            correctRateStr = "N/A";
        }

        String[][] items = {
                {"学习时长", report.getTotalActivity() + "h", "#1F2937"},
                {"做题数", String.valueOf(totalPractice), "#1F2937"},
                {"正确率", correctRateStr, "#10B981"},
                {"新掌握", String.valueOf(report.getWeekNotes()), "#6366F1"}
        };

        for (String[] item : items) {
            LinearLayout stat = new LinearLayout(requireContext());
            stat.setOrientation(LinearLayout.VERTICAL);
            stat.setGravity(Gravity.CENTER);
            stat.setPadding(16, 8, 16, 8);

            TextView value = new TextView(requireContext());
            value.setText(item[1]);
            value.setTextSize(20);
            value.setTextColor(Color.parseColor(item[2]));
            value.setTypeface(Typeface.DEFAULT_BOLD);
            value.setGravity(Gravity.CENTER);
            stat.addView(value);

            TextView label = new TextView(requireContext());
            label.setText(item[0]);
            label.setTextSize(10);
            label.setTextColor(getResources().getColor(R.color.text_tertiary, null));
            label.setGravity(Gravity.CENTER);
            stat.addView(label);

            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stat.setLayoutParams(sp);
            llWeeklyStats.addView(stat);
        }
    }

    private String getTaskIcon(String type) {
        switch (type != null ? type : "") {
            case "review": return "🔄";
            case "practice": return "📝";
            case "weakness": return "🎯";
            default: return "📋";
        }
    }

    private void renderRadarChart(MasteryRadarData data) {
        if (data.getLabels() == null || data.getValues() == null) return;

        List<String> labels = data.getLabels();
        List<Double> values = data.getValues();

        List<RadarEntry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new RadarEntry(values.get(i).floatValue()));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#6366F1"));
        dataSet.setFillColor(Color.parseColor("#6366F1"));
        dataSet.setFillAlpha(50);
        dataSet.setLineWidth(2f);
        dataSet.setDrawFilled(true);
        dataSet.setDrawHighlightCircleEnabled(true);
        dataSet.setDrawHighlightIndicators(false);

        RadarData radarData = new RadarData(dataSet);
        radarData.setDrawValues(false);

        radarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        radarChart.setData(radarData);
        radarChart.invalidate();

        // Update labels text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(labels.size(), values.size()); i++) {
            if (i > 0) sb.append(" · ");
            sb.append(labels.get(i)).append(" ").append(String.format("%.0f%%", values.get(i)));
        }
        tvRadarLabels.setText(sb.toString());
    }
}
