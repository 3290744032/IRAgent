package com.suiyuan.iragent_app.ui.screens.errors;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.model.v3.ErrorItem;

import java.util.List;

public class ErrorsListFragment extends Fragment {

    private ErrorsListViewModel viewModel;
    private RecyclerView rvErrors;
    private ErrorCardAdapter adapter;
    private LinearLayout reviewBanner;

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

        adapter = new ErrorCardAdapter(errorItem -> {
            Bundle args = new Bundle();
            args.putString("error_id", errorItem.getId());
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.nav_errors_detail, args);
        });
        rvErrors.setLayoutManager(new LinearLayoutManager(getContext()));
        rvErrors.setAdapter(adapter);

        viewModel.getErrorsList().observe(getViewLifecycleOwner(), errors -> {
            if (errors != null) adapter.setErrors(errors);
        });

        viewModel.getReviewQueue().observe(getViewLifecycleOwner(), queue -> {
            if (queue != null && !queue.isEmpty() && reviewBanner != null) {
                reviewBanner.setVisibility(View.VISIBLE);
                TextView tv = (TextView) reviewBanner.getChildAt(0);
                tv.setText("今天有 " + queue.size() + " 道错题需要复习");
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        viewModel.listErrors("", "", 0, 20);
        viewModel.loadReviewQueue();
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
