package com.suiyuan.iragent_app.ui.screens.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.ui.screens.auth.AuthActivity;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private TextView tvAccount, tvEmail, tvStudyCount, tvMessageCount;
    private Button btnLogout;
    private LinearLayout llSettings;
    private ImageView ivBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews(view);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupListeners();

        return view;
    }

    private void initViews(View view) {
        ivBack = view.findViewById(R.id.iv_back);
        tvAccount = view.findViewById(R.id.tv_account);
        tvEmail = view.findViewById(R.id.tv_email);
        tvStudyCount = view.findViewById(R.id.tv_study_count);
        tvMessageCount = view.findViewById(R.id.tv_message_count);
        btnLogout = view.findViewById(R.id.btn_logout);
        llSettings = view.findViewById(R.id.ll_settings);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> getActivity().onBackPressed());
        btnLogout.setOnClickListener(v -> logout());
        llSettings.setOnClickListener(v -> showSettings());
    }

    private void logout() {
        viewModel.logout();
        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showSettings() {
        // Navigate to settings activity
    }
}