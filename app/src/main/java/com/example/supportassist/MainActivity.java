package com.example.supportassist;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AlertPoller alertPoller;
    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject WebSocketManager webSocketManager;
    
    private BottomNavigationView bottomNav;
    private boolean doubleBackToExitPressedOnce = false;
    private int homeItemId = R.id.nav_home;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestNotificationPermissionIfNeeded();
        FcmTokenSync.registerCurrentToken(apiService);

        bottomNav = findViewById(R.id.bottomNavigationView);

        setUpNavForRole();

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(homeItemId);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNav.getSelectedItemId() != homeItemId) {
                    bottomNav.setSelectedItemId(homeItemId);
                } else {
                    if (doubleBackToExitPressedOnce) {
                        finish();
                        return;
                    }
                    doubleBackToExitPressedOnce = true;
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
                }
            }
        });

        // Initialize Real-Time Connection
        if (tokenManager.getAccessToken() != null) {
            webSocketManager.connect(tokenManager.getAccessToken());
        }
    }

    private void setUpNavForRole() {
        String role = tokenManager.getUserType();

        if ("TECHNICIAN".equalsIgnoreCase(role)) {
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_technician);
            homeItemId = R.id.nav_tech_dashboard;
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_admin);
            homeItemId = R.id.nav_admin_dashboard;
        } else {
            homeItemId = R.id.nav_home;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_create_ticket) {
                startActivity(new Intent(MainActivity.this, CreateTicketActivity.class));
                return false; 
            }
            Fragment selectedFragment = fragmentFor(itemId);
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    @Nullable
    private Fragment fragmentFor(int itemId) {
        if (itemId == R.id.nav_home) return new HomeFragment();
        if (itemId == R.id.nav_tickets) return new TicketsFragment();
        if (itemId == R.id.nav_alerts) return new AlertsFragment();
        if (itemId == R.id.nav_profile) return new ProfileFragment();
        
        if (itemId == R.id.nav_tech_dashboard) return new TechnicianDashboardFragment();
        if (itemId == R.id.nav_tech_queue) return new TechnicianQueueFragment();
        if (itemId == R.id.nav_tech_profile) return new ProfileFragment();
        
        if (itemId == R.id.nav_admin_dashboard) return new AdminDashboardFragment();
        if (itemId == R.id.nav_admin_tickets) return new AdminTicketsFragment();
        if (itemId == R.id.nav_admin_users) return new AdminUsersFragment();
        if (itemId == R.id.nav_admin_profile) return new ProfileFragment();
        return null;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        alertPoller.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        alertPoller.stop();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToTickets() {
        bottomNav.setSelectedItemId(R.id.nav_tickets);
    }

    public void navigateToAdminTickets() {
        bottomNav.setSelectedItemId(R.id.nav_admin_tickets);
    }

    public void navigateToTechQueue() {
        bottomNav.setSelectedItemId(R.id.nav_tech_queue);
    }
}
