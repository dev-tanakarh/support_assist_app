package com.example.supportassist;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_logo);
        TextView title = findViewById(R.id.tv_title);
        TextView subtitle = findViewById(R.id.tv_subtitle);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        // Pre-fetch categories in background to avoid long loading times in form
        DataRepository.getInstance(this).syncCategories();

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);
        title.startAnimation(fadeIn);
        subtitle.startAnimation(fadeIn);

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(2000); // Faster splash
        animator.addUpdateListener(animation -> progressBar.setProgress((int) animation.getAnimatedValue()));
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                checkLoginStatus();
            }
        });

        animator.start();
    }

    private void checkLoginStatus() {
        TokenManager tokenManager = new TokenManager(this);
        if (tokenManager.getAccessToken() != null) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }
}
