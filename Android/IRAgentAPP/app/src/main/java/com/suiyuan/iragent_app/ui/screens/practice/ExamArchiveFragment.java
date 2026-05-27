package com.suiyuan.iragent_app.ui.screens.practice;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.ExamFilterData;
import com.suiyuan.iragent_app.data.model.v3.ExamQuestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExamArchiveFragment extends Fragment {

    private ExamArchiveViewModel viewModel;
    private RecyclerView rvQuestions;
    private TextView tvEmpty;
    private Spinner spSubject, spYear, spExamType;
    private Button btnSearch;
    private FloatingActionButton fabSimulate;
    private ExamCardAdapter adapter;

    private Set<String> hiddenFeedbackQuestions = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exam_archive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExamArchiveViewModel.class);

        spSubject = view.findViewById(R.id.sp_subject);
        spYear = view.findViewById(R.id.sp_year);
        spExamType = view.findViewById(R.id.sp_exam_type);
        btnSearch = view.findViewById(R.id.btn_search);
        rvQuestions = view.findViewById(R.id.rv_exam_questions);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabSimulate = view.findViewById(R.id.fab_simulate);

        adapter = new ExamCardAdapter(question -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("题目详情")
                    .setMessage("题目：" + question.getQuestionText()
                            + "\n\n解析：" + question.getExplanation())
                    .setPositiveButton("确定", null)
                    .show();
        });
        rvQuestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvQuestions.setAdapter(adapter);

        viewModel.getFilters().observe(getViewLifecycleOwner(), this::populateFilters);
        viewModel.getQuestions().observe(getViewLifecycleOwner(), questions -> {
            if (questions != null && !questions.isEmpty()) {
                adapter.setQuestions(questions);
                rvQuestions.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                adapter.setQuestions(new ArrayList<>());
                rvQuestions.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null && loading) {
                tvEmpty.setText("加载中...");
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        viewModel.loadFilters();

        btnSearch.setOnClickListener(v -> {
            String subject = spSubject.getSelectedItem() != null
                    ? spSubject.getSelectedItem().toString() : "";
            if ("全部".equals(subject)) subject = "";
            Integer year = null;
            if (spYear.getSelectedItem() != null) {
                String yearStr = spYear.getSelectedItem().toString();
                if (!"全部".equals(yearStr)) {
                    try { year = Integer.parseInt(yearStr); } catch (NumberFormatException ignored) {}
                }
            }
            String examType = spExamType.getSelectedItem() != null
                    ? spExamType.getSelectedItem().toString() : "";
            if ("全部".equals(examType)) examType = "";
            viewModel.search(subject, year, examType, null, null, 0, 20);
        });

        fabSimulate.setOnClickListener(v -> showArchiveMenu());
    }

    private final androidx.activity.result.ActivityResultLauncher<String> archiveUploadLauncher =
        registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) viewModel.uploadPaper(uri);
        });

    private void showArchiveMenu() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("个人题库")
            .setItems(new String[]{"📄 上传试卷", "🤖 AI 模拟真题"}, (d, which) -> {
                if (which == 0) archiveUploadLauncher.launch("image/*");
                else showSimulateDialog();
            })
            .show();
    }

    private void showSimulateDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        TextView tvHint = new TextView(requireContext());
        tvHint.setText("AI 将根据你选择的科目和考试类型生成模拟真题");
        tvHint.setTextSize(13);
        tvHint.setTextColor(Color.parseColor("#6B7280"));
        layout.addView(tvHint);

        Spinner spSubj = new Spinner(requireContext());
        List<String> subjList = new ArrayList<>();
        subjList.add("数学"); subjList.add("物理"); subjList.add("化学");
        subjList.add("英语"); subjList.add("政治"); subjList.add("历史");
        ArrayAdapter<String> subjAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, subjList);
        subjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubj.setAdapter(subjAdapter);
        spSubj.setPadding(0, 16, 0, 8);
        layout.addView(spSubj);

        Spinner spCount = new Spinner(requireContext());
        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[]{"5", "10", "15"});
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCount.setAdapter(countAdapter);
        spCount.setPadding(0, 8, 0, 8);
        layout.addView(spCount);

        new AlertDialog.Builder(requireContext())
                .setTitle("AI 模拟真题")
                .setView(layout)
                .setPositiveButton("生成", (dialog, which) -> {
                    String subject = spSubj.getSelectedItem().toString();
                    int count = Integer.parseInt(spCount.getSelectedItem().toString());
                    viewModel.simulate(subject, "", count);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void populateFilters(ExamFilterData filterData) {
        if (filterData == null) return;

        List<String> subjects = new ArrayList<>();
        subjects.add("全部");
        if (filterData.getSubjects() != null) subjects.addAll(filterData.getSubjects());
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, subjects);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subjectAdapter);

        List<String> years = new ArrayList<>();
        years.add("全部");
        if (filterData.getYears() != null) {
            for (Integer y : filterData.getYears()) years.add(String.valueOf(y));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);

        List<String> examTypes = new ArrayList<>();
        examTypes.add("全部");
        if (filterData.getExamTypes() != null) examTypes.addAll(filterData.getExamTypes());
        ArrayAdapter<String> examTypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, examTypes);
        examTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spExamType.setAdapter(examTypeAdapter);
    }

    private interface OnQuestionClickListener { void onQuestionClick(ExamQuestion question); }

    private class ExamCardAdapter extends RecyclerView.Adapter<ExamCardAdapter.ViewHolder> {
        private List<ExamQuestion> questions = new ArrayList<>();
        private final OnQuestionClickListener listener;

        ExamCardAdapter(OnQuestionClickListener listener) { this.listener = listener; }

        void setQuestions(List<ExamQuestion> questions) {
            this.questions = questions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(Color.WHITE);
            RecyclerView.LayoutParams cp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(16, 4, 16, 8);
            card.setLayoutParams(cp);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ExamQuestion q = questions.get(position);
            holder.card.removeAllViews();

            // Row 1: subject + year | source tag | difficulty stars
            LinearLayout header = new LinearLayout(holder.card.getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView subjectYear = new TextView(holder.card.getContext());
            subjectYear.setText(q.getSubject() + " · " + q.getYear());
            subjectYear.setTextSize(12);
            subjectYear.setTextColor(holder.card.getContext().getResources().getColor(R.color.text_tertiary, null));
            header.addView(subjectYear);

            // Source tag
            if (q.getSource() != null) {
                TextView sourceTag = new TextView(holder.card.getContext());
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
                header.addView(sourceTag);
            }

            TextView difficulty = new TextView(holder.card.getContext());
            StringBuilder stars = new StringBuilder();
            int d = q.getDifficulty();
            for (int i = 0; i < d; i++) stars.append("★");
            for (int i = d; i < 5; i++) stars.append("☆");
            difficulty.setText(stars.toString());
            difficulty.setTextSize(12);
            difficulty.setTextColor(Color.parseColor("#F59E0B"));
            header.addView(difficulty);

            holder.card.addView(header);

            // Row 2: question text (max 2 lines)
            TextView questionText = new TextView(holder.card.getContext());
            questionText.setText(q.getQuestionText());
            questionText.setTextSize(14);
            questionText.setTextColor(holder.card.getContext().getResources().getColor(R.color.on_surface, null));
            questionText.setTypeface(Typeface.DEFAULT_BOLD);
            questionText.setMaxLines(2);
            questionText.setPadding(0, 8, 0, 8);
            holder.card.addView(questionText);

            // Row 3: knowledge point (gray text)
            if (q.getKnowledgePoint() != null && !q.getKnowledgePoint().isEmpty()) {
                TextView kp = new TextView(holder.card.getContext());
                kp.setText(q.getKnowledgePoint());
                kp.setTextSize(12);
                kp.setTextColor(holder.card.getContext().getResources().getColor(R.color.text_tertiary, null));
                holder.card.addView(kp);
            }

            // Row 4: feedback button for ai-generated
            if ("ai-generated".equals(q.getSource()) && !hiddenFeedbackQuestions.contains(q.getId())) {
                Button btnFeedback = new Button(holder.card.getContext());
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
                    hiddenFeedbackQuestions.add(q.getId());
                    viewModel.submitFeedback(q.getId(), "wrong_question", "");
                    notifyItemChanged(position);
                });
                holder.card.addView(btnFeedback);
            }

            holder.card.setOnClickListener(v -> listener.onQuestionClick(q));
        }

        @Override
        public int getItemCount() { return questions.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout card;
            ViewHolder(LinearLayout card) { super(card); this.card = card; }
        }
    }
}
