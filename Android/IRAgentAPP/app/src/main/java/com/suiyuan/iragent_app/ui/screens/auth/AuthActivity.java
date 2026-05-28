package com.suiyuan.iragent_app.ui.screens.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.ui.screens.main.MainActivity;

public class AuthActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private EditText etAccount, etPassword, etVerifiCode;
    private EditText etRegAccount, etRegPassword, etRegPasswordConfirm, etRegVerifiCode;
    private ProgressBar progressBar;
    private TextView tabLogin, tabRegister, btnLogin, btnRegisterSubmit;
    private View layoutLoginForm, layoutRegisterForm;
    private TextView btnRefreshCode, btnRegRefreshCode;
    private ImageView ivVerifiCode, ivRegVerifiCode;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager pm = new PreferencesManager(this);
        if (pm.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);

        // Handle status bar insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View root = findViewById(android.R.id.content);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), navBarHeight);
            return insets;
        });
        // Light status bar icons for light background
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // Tab switching
        tabLogin = findViewById(R.id.tab_login);
        tabRegister = findViewById(R.id.tab_register);
        layoutLoginForm = findViewById(R.id.layout_login_form);
        layoutRegisterForm = findViewById(R.id.layout_register_form);

        tabLogin.setOnClickListener(v -> switchToLogin());
        tabRegister.setOnClickListener(v -> switchToRegister());

        // Login form
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        etVerifiCode = findViewById(R.id.et_verifi_code);
        btnLogin = findViewById(R.id.btn_login);
        btnRefreshCode = findViewById(R.id.btn_refresh_code);
        ivVerifiCode = findViewById(R.id.iv_verifi_code);

        btnLogin.setOnClickListener(v -> handleAuth());
        btnRefreshCode.setOnClickListener(v -> loadVerifiCode());
        ivVerifiCode.setOnClickListener(v -> loadVerifiCode());

        // Register form
        etRegAccount = findViewById(R.id.et_reg_account);
        etRegPassword = findViewById(R.id.et_reg_password);
        etRegPasswordConfirm = findViewById(R.id.et_reg_password_confirm);
        etRegVerifiCode = findViewById(R.id.et_reg_verifi_code);
        btnRegisterSubmit = findViewById(R.id.btn_register_submit);
        btnRegisterSubmit.setOnClickListener(v -> handleAuth());
        btnRegRefreshCode = findViewById(R.id.btn_reg_refresh_code);
        ivRegVerifiCode = findViewById(R.id.iv_reg_verifi_code);
        btnRegRefreshCode.setOnClickListener(v -> loadVerifiCode());
        ivRegVerifiCode.setOnClickListener(v -> loadVerifiCode());

        progressBar = findViewById(R.id.progress_bar);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();
        loadVerifiCode();
    }

    private void switchToLogin() {
        isLoginMode = true;
        tabLogin.setBackgroundResource(R.drawable.bg_tab_active);
        tabLogin.setTextColor(getResources().getColor(R.color.primary_dark, null));
        tabRegister.setBackgroundResource(android.R.color.transparent);
        tabRegister.setTextColor(getResources().getColor(R.color.gray_text, null));
        layoutLoginForm.setVisibility(View.VISIBLE);
        layoutRegisterForm.setVisibility(View.GONE);
    }

    private void switchToRegister() {
        isLoginMode = false;
        tabRegister.setBackgroundResource(R.drawable.bg_tab_active);
        tabRegister.setTextColor(getResources().getColor(R.color.primary_dark, null));
        tabLogin.setBackgroundResource(android.R.color.transparent);
        tabLogin.setTextColor(getResources().getColor(R.color.gray_text, null));
        layoutLoginForm.setVisibility(View.GONE);
        layoutRegisterForm.setVisibility(View.VISIBLE);
    }

    private void setupObservers() {
        viewModel.getVerifiCodeLiveData().observe(this, this::updateVerifiCode);
        viewModel.getAuthResult().observe(this, result -> {
            hideProgress();
            if (result.isSuccess()) {
                Toast.makeText(this, isLoginMode ? "登录成功" : "注册成功", Toast.LENGTH_SHORT).show();
                com.suiyuan.iragent_app.IRAgentApplication.getInstance().resetRedirectGuard();
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading) showProgress();
            else hideProgress();
        });
    }

    private void loadVerifiCode() {
        viewModel.loadVerifiCode();
    }

    private void updateVerifiCode(Bitmap bitmap) {
        if (bitmap == null) return;
        if (ivVerifiCode != null) ivVerifiCode.setImageBitmap(bitmap);
        if (ivRegVerifiCode != null) ivRegVerifiCode.setImageBitmap(bitmap);
    }

    private void handleAuth() {
        String account, password, verifiCode;

        if (isLoginMode) {
            account = etAccount.getText().toString().trim();
            password = etPassword.getText().toString().trim();
            verifiCode = etVerifiCode.getText().toString().trim();
        } else {
            account = etRegAccount.getText().toString().trim();
            password = etRegPassword.getText().toString().trim();
            String confirm = etRegPasswordConfirm.getText().toString().trim();
            verifiCode = etRegVerifiCode.getText().toString().trim();
            if (!password.equals(confirm)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (TextUtils.isEmpty(account)) { Toast.makeText(this, "请输入账号", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(password)) { Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show(); return; }
        if (TextUtils.isEmpty(verifiCode)) { Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show(); return; }

        showProgress();
        if (isLoginMode) {
            viewModel.login(account, password, verifiCode);
        } else {
            viewModel.register(account, password, "", "", verifiCode);
        }
    }

    private void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        if (isLoginMode) btnLogin.setEnabled(false);
        else btnRegisterSubmit.setEnabled(false);
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        if (isLoginMode) btnLogin.setEnabled(true);
        else btnRegisterSubmit.setEnabled(true);
    }
}
