package com.suiyuan.iragent_app.ui.screens.knowledge;

import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.NoteFragment;
import com.suiyuan.iragent_app.data.model.v3.NoteItem;
import com.suiyuan.iragent_app.data.model.v3.UploadResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KnowledgeListFragment extends Fragment {

    private KnowledgeListViewModel viewModel;
    private RecyclerView rvNotes;
    private LinearLayout llSubjectTabs;
    private LinearLayout llStats;
    private EditText etSearch;
    private NoteCardAdapter adapter;
    private LinearLayout llSearchResults;
    private View loadingView, emptyView;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private MaterialCardView cardUpload;
    private LinearLayout llUploadTrigger, llUploadProgress;
    private TextView tvUploadStep, tvUploadProgress;
    private ProgressBar pbUpload;

    private static final List<String> SUBJECTS = Arrays.asList("全部", "数学", "物理", "化学", "英语", "政治", "历史");

    private String pendingNoteName;
    private Uri cameraPhotoUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) showNameDialog(uri);
            });

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) showNameDialog(uri);
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoUri != null) showNameDialog(cameraPhotoUri);
                cameraPhotoUri = null;
            });

    private void showNameDialog(Uri uri) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        android.widget.EditText etName = new android.widget.EditText(requireContext());
        etName.setText(pendingNoteName != null ? pendingNoteName : "我的笔记");
        etName.setHint("输入笔记名称");
        etName.setTextSize(15);
        etName.setBackgroundResource(R.drawable.bg_input_rounded);
        etName.setPadding(16, 12, 16, 12);
        layout.addView(etName);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("📝 笔记名称")
            .setView(layout)
            .setPositiveButton("开始上传", (d, w) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) name = "我的笔记";
                pendingNoteName = name;
                viewModel.uploadNote(uri, name);
            })
            .setNegativeButton("取消", null)
            .show();
    }

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

        cardUpload = view.findViewById(R.id.card_upload);
        llUploadTrigger = view.findViewById(R.id.ll_upload_trigger);
        llUploadProgress = view.findViewById(R.id.ll_upload_progress);
        tvUploadStep = view.findViewById(R.id.tv_upload_step);
        tvUploadProgress = view.findViewById(R.id.tv_upload_progress);
        pbUpload = view.findViewById(R.id.pb_upload);

        setupSubjectTabs();
        setupRecyclerView();
        setupSearchResultsContainer(view);
        setupLoadingView(view);
        setupEmptyView(view);
        setupSearch();
        setupObservers(view);

        cardUpload.setOnClickListener(v -> showAttachSheet());

        // Setup knowledge graph WebView
        setupKnowledgeGraph(view);

        viewModel.listNotes("", 0, 20);
    }

    private void showAttachSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_attach_sheet, null);
        sheet.setContentView(sheetView);

        sheetView.findViewById(R.id.ll_attach_camera).setOnClickListener(v -> {
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

        sheetView.findViewById(R.id.ll_attach_gallery).setOnClickListener(v -> {
            sheet.dismiss();
            imagePickerLauncher.launch("image/*");
        });

        sheetView.findViewById(R.id.ll_attach_file).setOnClickListener(v -> {
            sheet.dismiss();
            filePickerLauncher.launch("*/*");
        });

        sheet.show();
    }

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File cacheDir = requireContext().getCacheDir();
        return File.createTempFile("IMG_" + timeStamp + "_", ".jpg", cacheDir);
    }

    private void setupKnowledgeGraph(View view) {
        android.webkit.WebView kgWebView = view.findViewById(R.id.wv_knowledge_graph);
        if (kgWebView == null) {
            // Fallback: use ImageView if WebView not in layout
            return;
        }
        kgWebView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        kgWebView.getSettings().setJavaScriptEnabled(true);
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
                        ((TextView) child).setTextColor(getResources().getColor(R.color.gray_text, null));
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

    private void setupSearchResultsContainer(View view) {
        llSearchResults = new LinearLayout(requireContext());
        llSearchResults.setOrientation(LinearLayout.VERTICAL);
        llSearchResults.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llSearchResults.setLayoutParams(lp);
        int rvIndex = ((ViewGroup) rvNotes.getParent()).indexOfChild(rvNotes);
        ((ViewGroup) rvNotes.getParent()).addView(llSearchResults, rvIndex);
    }

    private void setupRecyclerView() {
        adapter = new NoteCardAdapter(noteItem -> {
            if (noteItem.getId() == null) return;
            Bundle args = new Bundle();
            args.putString("note_id", noteItem.getId());
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.nav_knowledge_detail, args);
        });
        adapter.setOnNoteDeleteListener(note -> viewModel.deleteNote(note.getId()));
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
                    } else {
                        llSearchResults.removeAllViews();
                        llSearchResults.setVisibility(View.GONE);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void setupLoadingView(View view) {
        loadingView = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyle);
        ((ProgressBar) loadingView).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        int rvIdx = ((ViewGroup) rvNotes.getParent()).indexOfChild(rvNotes);
        ((ViewGroup) rvNotes.getParent()).addView(loadingView, rvIdx + 1, lp);
    }

    private void setupEmptyView(View view) {
        emptyView = new LinearLayout(requireContext());
        ((LinearLayout) emptyView).setOrientation(LinearLayout.VERTICAL);
        ((LinearLayout) emptyView).setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        TextView icon = new TextView(requireContext());
        icon.setText("📂");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        ((LinearLayout) emptyView).addView(icon);

        TextView tv = new TextView(requireContext());
        tv.setText("暂无笔记数据");
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#9CA3AF"));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 12, 0, 0);
        ((LinearLayout) emptyView).addView(tv);

        int rvIdx = ((ViewGroup) rvNotes.getParent()).indexOfChild(rvNotes);
        ((ViewGroup) rvNotes.getParent()).addView(emptyView, rvIdx + 2, lp);
    }

    private void showUploadPreview(UploadResult result) {
        UploadResult.Classification cls = result.getClassification();
        String subject = cls != null && cls.getSubject() != null ? cls.getSubject() : "未分类";
        String chapter = cls != null && cls.getChapter() != null ? cls.getChapter() : "";
        String tags = cls != null && cls.getTags() != null ? cls.getTags() : "";

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 8);

        // AI 分类结果卡片
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_primary_light);
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(cardLp);

        addInfoRow(card, "📖 科目", subject.isEmpty() ? "未分类" : subject);
        addInfoRow(card, "📂 章节", chapter.isEmpty() ? "自动识别中" : chapter);
        addInfoRow(card, "🏷️ 标签", tags.isEmpty() ? "—" : tags);
        addInfoRow(card, "📦 知识点", result.getChunkCount() + " 个片段");

        layout.addView(card);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setIcon(null)
            .setTitle("✅ 上传成功，AI 已分类")
            .setView(layout)
            .setPositiveButton("查看笔记", (d, w) -> {
                if (result.getNoteId() != null) {
                    Bundle args = new Bundle();
                    args.putString("note_id", result.getNoteId());
                    Navigation.findNavController(requireView()).navigate(R.id.nav_knowledge_detail, args);
                }
            })
            .setNegativeButton("继续浏览", null)
            .show();
        viewModel.listNotes("", 0, 20);
    }

    private void addInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rlp);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(13);
        tvLabel.setTextColor(Color.parseColor("#6B7280"));
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(14);
        tvValue.setTextColor(Color.parseColor("#1F2937"));
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setPadding(12, 0, 0, 0);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvValue.setLayoutParams(vp);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);
    }

    private void showSearchResults(List<NoteFragment> results) {
        llSearchResults.removeAllViews();
        llSearchResults.setVisibility(View.VISIBLE);

        TextView header = new TextView(requireContext());
        header.setText("搜索结果 (" + results.size() + ")");
        header.setTextSize(14);
        header.setTextColor(Color.parseColor("#374151"));
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(16, 12, 16, 4);
        llSearchResults.addView(header);

        for (NoteFragment frag : results) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card_white);
            card.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(16, 0, 16, 8);
            card.setLayoutParams(cp);

            TextView tvContent = new TextView(requireContext());
            tvContent.setText(frag.getContent());
            tvContent.setTextSize(13);
            tvContent.setTextColor(Color.parseColor("#1F2937"));
            tvContent.setMaxLines(3);
            card.addView(tvContent);

            LinearLayout meta = new LinearLayout(requireContext());
            meta.setOrientation(LinearLayout.HORIZONTAL);
            meta.setPadding(0, 6, 0, 0);
            if (frag.getKnowledgePoint() != null) {
                TextView tvKp = new TextView(requireContext());
                tvKp.setText(frag.getKnowledgePoint());
                tvKp.setTextSize(11);
                tvKp.setTextColor(Color.parseColor("#6366F1"));
                meta.addView(tvKp);
            }
            TextView tvSim = new TextView(requireContext());
            tvSim.setText(String.format("相似度 %.0f%%", frag.getSimilarity() * 100));
            tvSim.setTextSize(11);
            tvSim.setTextColor(Color.parseColor("#9CA3AF"));
            tvSim.setPadding(8, 0, 0, 0);
            meta.addView(tvSim);
            card.addView(meta);

            llSearchResults.addView(card);
        }
    }

    private void setupObservers(View view) {
        viewModel.getNotesList().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                adapter.setNotes(notes);
                updateStats(notes);
                boolean empty = notes.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getUploadStep().observe(getViewLifecycleOwner(), step -> {
            if (step != null && !step.isEmpty()) {
                llUploadTrigger.setVisibility(View.GONE);
                llUploadProgress.setVisibility(View.VISIBLE);
                tvUploadStep.setText(step);
            } else {
                llUploadTrigger.setVisibility(View.VISIBLE);
                llUploadProgress.setVisibility(View.GONE);
            }
        });

        viewModel.getUploadProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                pbUpload.setProgress(progress);
                tvUploadProgress.setText(progress + "%");
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (results != null && !results.isEmpty()) {
                showSearchResults(results);
            } else {
                llSearchResults.removeAllViews();
                llSearchResults.setVisibility(View.GONE);
            }
        });

        viewModel.getUploadResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                llUploadTrigger.setVisibility(View.VISIBLE);
                llUploadProgress.setVisibility(View.GONE);
                showUploadPreview(result);
                viewModel.getUploadResult().setValue(null);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG)
                        .setAction("重试", v -> viewModel.listNotes("", 0, 20))
                        .show();
                viewModel.clearError();
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
        private OnNoteDeleteListener deleteListener;

        interface OnNoteClickListener {
            void onNoteClick(NoteItem note);
        }

        interface OnNoteDeleteListener {
            void onNoteDelete(NoteItem note);
        }

        NoteCardAdapter(OnNoteClickListener listener) { this.listener = listener; }

        void setOnNoteDeleteListener(OnNoteDeleteListener l) { this.deleteListener = l; }

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
            String chapter = note.getChapter() != null && !note.getChapter().isEmpty() ? note.getChapter() : "";
            String previewText = chapter.isEmpty() ? note.getTitle() : chapter + " · " + note.getTitle();
            holder.tvPreview.setText(previewText);

            String linked = "关联 " + note.getLinkedQuestionCount() + " 道题目";
            holder.tvLinkedCount.setText(linked);

            // Tags
            holder.llTags.removeAllViews();
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                for (String tag : note.getTags().split(",")) {
                    TextView tagView = new TextView(holder.itemView.getContext());
                    tagView.setText(tag.trim());
                    tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                    tagView.setTextColor(Color.WHITE);
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
            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("删除笔记")
                    .setMessage("确定删除「" + note.getTitle() + "」吗？删除后不可恢复。")
                    .setPositiveButton("删除", (d, w) -> {
                        if (deleteListener != null) deleteListener.onNoteDelete(note);
                    })
                    .setNegativeButton("取消", null)
                    .show();
                return true;
            });
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
