package com.suiyuan.iragent_app.ui.screens.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.suiyuan.iragent_app.R;
import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.ui.screens.onboarding.OnboardingActivity;

public class MainActivity extends AppCompatActivity {

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 首次启动检查引导流程
        PreferencesManager pm = new PreferencesManager(this);
        if (!pm.isOnboardingCompleted()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (navController != null) {
            // 自定义 Tab 切换：点击 Tab 时回到该 Tab 的根 Fragment，而不是留在子页面
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                // 如果当前就在这个 Tab 的根页面，不做任何事
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == itemId) {
                    return true;
                }
                // popBackStack 回到该 Tab 的根，再导航过去
                navController.popBackStack(itemId, false);
                navController.navigate(itemId);
                return true;
            });
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    // V2 backward compat: old StudyFragment calls this to switch to profile tab
    public void switchToProfile() {
        if (navController != null) {
            navController.navigate(R.id.nav_profile);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
