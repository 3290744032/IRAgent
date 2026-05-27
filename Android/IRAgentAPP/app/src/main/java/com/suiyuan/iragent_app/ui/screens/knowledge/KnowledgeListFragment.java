package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.NoteItem;

import java.util.Arrays;
import java.util.List;

public class KnowledgeListFragment extends Fragment {

    private KnowledgeListViewModel viewModel;
    private RecyclerView rvNotes;
    private LinearLayout llSubjectTabs;
    private LinearLayout llStats;
    private EditText etSearch;
    private NoteCardAdapter adapter;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final List<String> SUBJECTS = Arrays.asList("全部", "数学", "物理", "化学", "英语", "政治", "历史");

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.uploadNote(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge_list, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(KnowledgeListViewModel.class);

        rvNotes = view.findViewById(R.id.rv_notes);
        llSubjectTabs = view.findViewById(R.id.ll_subject_tabs);
        llStats = view.findViewById(R.id.ll_stats);
        etSearch = view.findViewById(R.id.et_search);

        setupSubjectTabs();
        setupRecyclerView();
        setupSearch();
        setupObservers();

        view.findViewById(R.id.fab_upload).setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        // Setup knowledge graph WebView
        setupKnowledgeGraph(view);

        viewModel.listNotes("", 0, 20);
    }

    private void setupKnowledgeGraph(View view) {
        android.webkit.WebView kgWebView = view.findViewById(R.id.wv_knowledge_graph);
        if (kgWebView == null) {
            // Fallback: use ImageView if WebView not in layout
            return;
        }
        kgWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        kgWebView.getSettings().setJavaScriptEnabled(false);
        kgWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        kgWebView.loadUrl("file:///android_asset/knowledge_graph.html");
    }

    private void setupSubjectTabs() {
        llSubjectTabs.removeAllViews();
        for (String subject : SUBJECTS) {
            TextView chip = new TextView(requireContext());
            chip.setText(subject);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(subject.equals("全部") ?
                    Color.WHITE : getResources().getColor(R.color.gray_text, null));
            chip.setBackgroundResource(subject.equals("全部") ?
                    R.drawable.bg_btn_primary : R.drawable.bg_quick_chip);
            int paddingH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14,
                    getResources().getDisplayMetrics());
            int paddingV = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6,
                    getResources().getDisplayMetrics());
            chip.setPadding(paddingH, paddingV, paddingH, paddingV);
            chip.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics()), 0);
            chip.setLayoutParams(params);

            chip.setOnClickListener(v -> {
                for (int i = 0; i < llSubjectTabs.getChildCount(); i++) {
                    View child = llSubjectTabs.getChildAt(i);
                    if (child == chip) {
                        child.setBackgroundResource(R.drawable.bg_btn_primary);
                        ((TextView) child).setTextColor(Color.WHITE);
                    } else {
                        child.setBackgroundResource(R.drawable.bg_quick_chip);
                        ((TextView) child).setTextColor(getResources().getColor(R.color.gray_text, null));
                    }
                }
                String filter = subject.equals("全部") ? "" : subject;
                viewModel.listNotes(filter, 0, 20);
            });

            llSubjectTabs.addView(chip);
        }
    }

    private void setupRecyclerView() {
        adapter = new NoteCardAdapter(noteItem -> {
            Bundle args = new Bundle();
            args.putString("note_id", noteItem.getId());
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.nav_knowledge_detail, args);
        });
        rvNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotes.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (!query.isEmpty()) {
                        viewModel.searchNotes(query, 5);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void setupObservers() {
        viewModel.getNotesList().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                adapter.setNotes(notes);
                updateStats(notes);
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (results != null && !results.isEmpty() && adapter != null) {
                // Show search results in-place by converting to note items
            }
        });

        viewModel.getUploadResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Toast.makeText(getContext(),
                        "上传成功，解析了 " + result.getChunkCount() + " 个知识点",
                        Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats(List<NoteItem> notes) {
        llStats.removeAllViews();
        int linkedCount = notes.stream().mapToInt(NoteItem::getLinkedQuestionCount).sum();
        int[] stats = {notes.size(), countUniqueSubjects(notes), countKnowledgePoints(notes), linkedCount};
        String[] labels = {"份笔记", "个科目", "个考点", "关联题目"};

        for (int i = 0; i < stats.length; i++) {
            TextView stat = new TextView(requireContext());
            stat.setText(String.valueOf(stats[i]) + " " + labels[i]);
            stat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            stat.setTextColor(getResources().getColor(R.color.gray_text, null));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stat.setLayoutParams(params);
            llStats.addView(stat);
        }
    }

    private int countUniqueSubjects(List<NoteItem> notes) {
        return (int) notes.stream().map(NoteItem::getSubject).distinct().count();
    }

    private int countKnowledgePoints(List<NoteItem> notes) {
        return notes.stream().mapToInt(NoteItem::getChunkCount).sum();
    }

    private static class NoteCardAdapter extends RecyclerView.Adapter<NoteCardAdapter.ViewHolder> {
        private List<NoteItem> notes = java.util.Collections.emptyList();
        private final OnNoteClickListener listener;

        interface OnNoteClickListener {
            void onNoteClick(NoteItem note);
        }

        NoteCardAdapter(OnNoteClickListener listener) { this.listener = listener; }

        void setNotes(List<NoteItem> notes) {
            this.notes = notes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_note_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NoteItem note = notes.get(position);
            holder.tvChapter.setText(note.getSubject() + " · " + note.getChapter());
            holder.tvTitle.setText(note.getTitle());
            holder.tvDate.setText(formatDate(note.getCreatedAt()));

            // Preview from tags
            holder.tvPreview.setText(note.getTags());

            String linked = "关联 " + note.getLinkedQuestionCount() + " 道题目";
            holder.tvLinkedCount.setText(linked);

            // Tags
            holder.llTags.removeAllViews();
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                for (String tag : note.getTags().split(",")) {
                    TextView tagView = new TextView(holder.itemView.getContext());
                    tagView.setText(tag.trim());
                    tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    tagView.setTextColor(holder.itemView.getContext().getResources()
                            .getColor(R.color.primary_color, null));
                    tagView.setBackgroundResource(R.drawable.btn_quick_reply);
                    int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6,
                            holder.itemView.getContext().getResources().getDisplayMetrics());
                    tagView.setPadding(p, p/2, p, p/2);
                    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tp.setMargins(0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6,
                            holder.itemView.getContext().getResources().getDisplayMetrics()), 0);
                    tagView.setLayoutParams(tp);
                    holder.llTags.addView(tagView);
                }
            }

            holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
        }

        @Override
        public int getItemCount() { return notes.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvChapter, tvTitle, tvPreview, tvDate, tvLinkedCount;
            LinearLayout llTags;

            ViewHolder(View v) {
                super(v);
                tvChapter = v.findViewById(R.id.tv_chapter);
                tvTitle = v.findViewById(R.id.tv_title);
                tvPreview = v.findViewById(R.id.tv_preview);
                tvDate = v.findViewById(R.id.tv_date);
                tvLinkedCount = v.findViewById(R.id.tv_linked_count);
                llTags = v.findViewById(R.id.ll_tags);
            }
        }
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr;
        return dateStr.substring(5, 10);
    }
}
