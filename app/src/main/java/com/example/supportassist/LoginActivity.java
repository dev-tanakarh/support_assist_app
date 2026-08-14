package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views to animate
        View logo = findViewById(R.id.iv_logo);
        View welcome = findViewById(R.id.tv_welcome);
        View subtitle = findViewById(R.id.tv_signin_subtitle);
        View emailLabel = findViewById(R.id.tv_label_email);
        View emailInput = findViewById(R.id.et_email);
        View passwordLabel = findViewById(R.id.tv_label_password);
        View passwordInput = findViewById(R.id.til_password);
        View forgotPassword = findViewById(R.id.tv_forgot_password);
        View loginBtn = findViewById(R.id.btn_login);
        View divider = findViewById(R.id.ll_divider);
        View googleBtn = findViewById(R.id.btn_google);
        View footer = findViewById(R.id.tv_signup).getParent() instanceof LinearLayout ? (View)findViewById(R.id.tv_signup).getParent() : null;

        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        
        // Apply animations with slight delays for a cascading effect
        logo.startAnimation(slideUp);
        welcome.startAnimation(slideUp);
        subtitle.startAnimation(slideUp);
        
        emailLabel.startAnimation(slideUp);
        emailInput.startAnimation(slideUp);
        passwordLabel.startAnimation(slideUp);
        passwordInput.startAnimation(slideUp);
        forgotPassword.startAnimation(slideUp);
        
        loginBtn.startAnimation(slideUp);
        divider.startAnimation(slideUp);
        googleBtn.startAnimation(slideUp);
        if (footer != null) footer.startAnimation(slideUp);

        TextView tvSignup = findViewById(R.id.tv_signup);
        tvSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });
    }
}