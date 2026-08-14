package com.example.supportassist;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Views to animate
        View[] viewsToAnimate = {
                findViewById(R.id.btn_back),
                findViewById(R.id.tv_create_account),
                findViewById(R.id.tv_join_subtitle),
                findViewById(R.id.tv_label_name),
                findViewById(R.id.et_name),
                findViewById(R.id.tv_label_email),
                findViewById(R.id.et_email_signup),
                findViewById(R.id.tv_label_phone),
                findViewById(R.id.et_phone),
                findViewById(R.id.tv_label_password),
                findViewById(R.id.til_password_signup),
                findViewById(R.id.tv_label_confirm_password),
                findViewById(R.id.til_confirm_password),
                findViewById(R.id.btn_signup),
                findViewById(R.id.ll_footer_signup)
        };

        long delay = 0;
        for (View view : viewsToAnimate) {
            if (view != null) {
                Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                animation.setStartOffset(delay);
                view.startAnimation(animation);
                delay += 50;
            }
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView tvLoginLink = findViewById(R.id.tv_login_link);
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}