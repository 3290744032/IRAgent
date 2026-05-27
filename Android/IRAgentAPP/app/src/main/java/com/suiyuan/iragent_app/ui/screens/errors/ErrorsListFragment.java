package com.suiyuan.iragent_app.ui.screens.errors;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.ErrorItem;
import com.suiyuan.iragent_app.data.model.v3.ReviewItem;

import java.util.List;

public class ErrorsListFragment extends Fragment {

    private ErrorsListViewModel viewModel;
    private RecyclerView rvErrors;
    private ErrorCardAdapter adapter;
    private LinearLayout reviewBanner;
    private View loadingView, emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_errors_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ErrorsListViewModel.class);

        rvErrors = view.findViewById(R.id.rv_errors);
        reviewBanner = view.findViewById(R.id.review_banner);
        reviewBanner.setOnClickListener(v -> { applyReviewFilter(); });

        adapter = new ErrorCardAdapter(errorItem -> {
            Bundle args = new Bundle();
            args.putString("error_id", errorItem.getId());
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.nav_errors_detail, args);
        });
        rvErrors.setLayoutManager(new LinearLayoutManager(getContext()));
        rvErrors.setAdapter(adapter);

        setupLoadingView(view);
        setupEmptyView(view);

        viewModel.getErrorsList().observe(getViewLifecycleOwner(), errors -> {
            if (errors != null) {
                adapter.setErrors(errors);
                boolean empty = errors.isEmpty();
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvErrors.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading != null) loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getReviewQueue().observe(getViewLifecycleOwner(), queue -> {
            if (queue != null && !queue.isEmpty() && reviewBanner != null) {
                reviewBanner.setVisibility(View.VISIBLE);
                TextView tv = (TextView) reviewBanner.getChildAt(0);
                tv.setText("今天有 " + queue.size() + " 道错题需要复习");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(view, error, Snackbar.LENGTH_LONG)
                        .setAction("重试", v -> {
                            viewModel.listErrors("", "", 0, 20);
                            viewModel.loadReviewQueue();
                        })
                        .show();
                viewModel.clearError();
            }
        });

        viewModel.listErrors("", "", 0, 20);
        viewModel.loadReviewQueue();
    }

    private void applyReviewFilter() {
        List<ReviewItem> queue = viewModel.getReviewQueue().getValue();
        if (queue != null && !queue.isEmpty()) {
            Bundle args = new Bundle();
            args.putString("error_id", queue.get(0).getErrorId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_errors_detail, args);
        } else {
            Snackbar.make(requireView(), "暂无待复习的错题", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void setupLoadingView(View view) {
        loadingView = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyle);
        ((ProgressBar) loadingView).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        int rvIdx = ((ViewGroup) rvErrors.getParent()).indexOfChild(rvErrors);
        ((ViewGroup) rvErrors.getParent()).addView(loadingView, rvIdx + 1, lp);
    }

    private void setupEmptyView(View view) {
        emptyView = new LinearLayout(requireContext());
        ((LinearLayout) emptyView).setOrientation(LinearLayout.VERTICAL);
        ((LinearLayout) emptyView).setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        TextView icon = new TextView(requireContext());
        icon.setText("📭");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        ((LinearLayout) emptyView).addView(icon);

        TextView tv = new TextView(requireContext());
        tv.setText("暂无错题数据");
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#9CA3AF"));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 12, 0, 0);
        ((LinearLayout) emptyView).addView(tv);

        int rvIdx = ((ViewGroup) rvErrors.getParent()).indexOfChild(rvErrors);
        ((ViewGroup) rvErrors.getParent()).addView(emptyView, rvIdx + 2, lp);
    }

    private static class ErrorCardAdapter extends RecyclerView.Adapter<ErrorCardAdapter.ViewHolder> {
        private List<ErrorItem> errors = java.util.Collections.emptyList();
        private final OnErrorClickListener listener;

        interface OnErrorClickListener { void onErrorClick(ErrorItem error); }
        ErrorCardAdapter(OnErrorClickListener listener) { this.listener = listener; }

        void setErrors(List<ErrorItem> errors) {
            this.errors = errors;
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
            ErrorItem e = errors.get(position);
            holder.card.removeAllViews();

            // Source row
            LinearLayout header = new LinearLayout(holder.card.getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            TextView source = new TextView(holder.card.getContext());
            source.setText(e.getSubject() + " · " + e.getKnowledgePoint());
            source.setTextSize(12);
            source.setTextColor(holder.card.getContext().getResources().getColor(R.color.text_tertiary, null));
            header.addView(source);
            holder.card.addView(header);

            // Question
            TextView question = new TextView(holder.card.getContext());
            question.setText(e.getQuestionText());
            question.setTextSize(14);
            question.setTextColor(holder.card.getContext().getResources().getColor(R.color.on_surface, null));
            question.setTypeface(Typeface.DEFAULT_BOLD);
            question.setMaxLines(2);
            question.setPadding(0, 8, 0, 12);
            holder.card.addView(question);

            // Answer comparison
            LinearLayout answers = new LinearLayout(holder.card.getContext());
            answers.setOrientation(LinearLayout.HORIZONTAL);
            TextView wrong = new TextView(holder.card.getContext());
            wrong.setText(e.getStudentAnswer());
            wrong.setTextSize(12);
            wrong.setTextColor(Color.parseColor("#EF4444"));
            wrong.setPaintFlags(wrong.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            answers.addView(wrong);
            TextView arrow = new TextView(holder.card.getContext());
            arrow.setText(" → ");
            arrow.setTextSize(12);
            arrow.setTextColor(holder.card.getContext().getResources().getColor(R.color.text_tertiary, null));
            answers.addView(arrow);
            TextView correct = new TextView(holder.card.getContext());
            correct.setText(e.getCorrectAnswer());
            correct.setTextSize(12);
            correct.setTextColor(Color.parseColor("#10B981"));
            answers.addView(correct);
            holder.card.addView(answers);

            holder.card.setOnClickListener(v -> listener.onErrorClick(e));
        }

        @Override
        public int getItemCount() { return errors.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout card;
            ViewHolder(LinearLayout card) { super(card); this.card = card; }
        }
    }
}
