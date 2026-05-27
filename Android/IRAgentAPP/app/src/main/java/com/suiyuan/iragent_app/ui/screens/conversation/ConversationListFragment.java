package com.suiyuan.iragent_app.ui.screens.conversation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.ConversationEntity;

import java.util.ArrayList;
import java.util.List;

public class ConversationListFragment extends Fragment {

    private ConversationListViewModel viewModel;
    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private EditText etSearch;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation_list, container, false);

        initViews(view);
        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupObservers();

        viewModel.loadConversations();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        etSearch = view.findViewById(R.id.et_search);
        emptyView = view.findViewById(R.id.empty_view);
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter(new ArrayList<>(), conversation -> {
            Bundle args = new Bundle();
            args.putString("conversation_id", conversation.getId());
            Navigation.findNavController(requireView()).navigate(R.id.nav_chat, args);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchConversations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupObservers() {
        viewModel.getConversationsLiveData().observe(getViewLifecycleOwner(), conversations -> {
            adapter.updateConversations(conversations);
            emptyView.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

        private List<ConversationEntity> conversations;
        private OnConversationClickListener listener;

        public ConversationAdapter(List<ConversationEntity> conversations, OnConversationClickListener listener) {
            this.conversations = conversations;
            this.listener = listener;
        }

        public void updateConversations(List<ConversationEntity> newConversations) {
            this.conversations = newConversations;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conversation, parent, false);
            return new ConversationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
            ConversationEntity conversation = conversations.get(position);
            holder.tvName.setText(conversation.name);
            holder.tvDescription.setText(conversation.description);
            holder.tvTime.setText(formatTime(conversation.updatedAt));

            if (conversation.isPinned) {
                holder.ivPin.setVisibility(View.VISIBLE);
            } else {
                holder.ivPin.setVisibility(View.GONE);
            }

            if (conversation.unreadCount > 0) {
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
                holder.tvUnreadCount.setText(String.valueOf(conversation.unreadCount));
            } else {
                holder.tvUnreadCount.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(conversation));
        }

        @Override
        public int getItemCount() {
            return conversations.size();
        }

        private String formatTime(String timestamp) {
            return timestamp;
        }

        public static class ConversationViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDescription, tvTime, tvUnreadCount;
            ImageView ivPin;

            public ConversationViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvDescription = itemView.findViewById(R.id.tv_description);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
                ivPin = itemView.findViewById(R.id.iv_pin);
            }
        }
    }

    public interface OnConversationClickListener {
        void onClick(ConversationEntity conversation);
    }
}
