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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AlertPoller alertPoller;
    @Inject ApiService apiService;
    private BottomNavigationView bottomNav;
    private FloatingActionButton fab;
    private TokenManager tokenManager;
    private boolean doubleBackToExitPressedOnce = false;
    private int homeItemId = R.id.nav_home;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Nothing to do either way — FCM still registers the token and
                // delivers silently in the background if this is denied, the
                // user just won't see a system notification for it.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);

        requestNotificationPermissionIfNeeded();
        FcmTokenSync.registerCurrentToken(apiService);

        bottomNav = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

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
    }

    /**
     * The layout's default menu (bottom_nav_menu.xml) is the END_USER one —
     * for the other two roles we swap it at runtime, per the role saved at
     * login (see TokenManager.saveUser). Only END_USER creates tickets, so
     * the FAB is hidden entirely for technician/admin, matching the web app
     * where neither role has a "new ticket" action.
     */
    private void setUpNavForRole() {
        String role = tokenManager.getUserType();

        if ("TECHNICIAN".equals(role)) {
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_technician);
            fab.setVisibility(View.GONE);
            homeItemId = R.id.nav_tech_dashboard;
        } else if ("ADMIN".equals(role)) {
            bottomNav.getMenu().clear();
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_admin);
            fab.setVisibility(View.GONE);
            homeItemId = R.id.nav_admin_dashboard;
        } else {
            // END_USER, or role somehow missing — the layout's default menu already fits.
            fab.setVisibility(View.VISIBLE);
            homeItemId = R.id.nav_home;
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentFor(item.getItemId());
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });

        fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CreateTicketActivity.class)));
    }

    @Nullable
    private Fragment fragmentFor(int itemId) {
        // END_USER
        if (itemId == R.id.nav_home) return new HomeFragment();
        if (itemId == R.id.nav_tickets) return new TicketsFragment();
        if (itemId == R.id.nav_alerts) return new AlertsFragment();
        if (itemId == R.id.nav_profile) return new ProfileFragment();
        // TECHNICIAN
        if (itemId == R.id.nav_tech_dashboard) return new TechnicianDashboardFragment();
        if (itemId == R.id.nav_tech_queue) return new TechnicianQueueFragment();
        if (itemId == R.id.nav_tech_profile) return new ProfileFragment();
        // ADMIN
        if (itemId == R.id.nav_admin_dashboard) return new AdminDashboardFragment();
        if (itemId == R.id.nav_admin_tickets) return new AdminTicketsFragment();
        if (itemId == R.id.nav_admin_users) return new AdminUsersFragment();
        if (itemId == R.id.nav_admin_profile) return new ProfileFragment();
        return null;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return; // pre-Android 13: no runtime prompt needed
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Real-time updates only while the app is actually in front of the
        // user — see AlertPoller's docblock for why this isn't a background loop.
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

    /** Only meaningful for END_USER, whose menu has a tickets tab — the only role HomeFragment (which calls this) is ever shown for. */
    public void navigateToTickets() {
        bottomNav.setSelectedItemId(R.id.nav_tickets);
    }
}
