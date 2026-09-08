package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private ApiService apiService;
    private TokenManager tokenManager;
    private ProgressBar progressBar;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        apiService = ApiClient.getApiService(this);
        tokenManager = new TokenManager(this);
        progressBar = findViewById(R.id.pb_signup);

        final EditText etName = findViewById(R.id.et_name);
        final EditText etEmail = findViewById(R.id.et_email_signup);
        final EditText etPhone = findViewById(R.id.et_phone);
        final EditText etPassword = findViewById(R.id.et_password_signup);
        final EditText etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);

        // Views to animate
        View[] viewsToAnimate = {
                findViewById(R.id.btn_back),
                findViewById(R.id.tv_create_account),
                findViewById(R.id.tv_join_subtitle),
                findViewById(R.id.tv_label_name),
                etName,
                findViewById(R.id.tv_label_email),
                etEmail,
                findViewById(R.id.tv_label_phone),
                etPhone,
                findViewById(R.id.tv_label_password),
                findViewById(R.id.til_password_signup),
                findViewById(R.id.tv_label_confirm_password),
                findViewById(R.id.til_confirm_password),
                btnSignup,
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
        btnBack.setOnClickListener(v -> finish());

        TextView tvLoginLink = findViewById(R.id.tv_login_link);
        tvLoginLink.setOnClickListener(v -> finish());

        btnSignup.setOnClickListener(v -> {
            Log.d(TAG, "Signup button clicked");
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            signupUser(name, email, phone, password);
        });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnSignup != null) btnSignup.setEnabled(!isLoading);
    }

    private void signupUser(String name, String email, String phone, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("phone", phone);
        body.put("password", password);

        Log.d(TAG, "Attempting signup at: " + ApiClient.getBaseUrl());

        apiService.signup(body).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    tokenManager.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    
                    Toast.makeText(SignupActivity.this, "Signup Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    String message = "Signup Failed";
                    if (response.body() != null && response.body().getMessage() != null) {
                        message = response.body().getMessage();
                    } else if (response.errorBody() != null) {
                        try {
                            message = response.errorBody().string();
                        } catch (Exception e) {
                            message = "Error code: " + response.code();
                        }
                    }
                    Log.e(TAG, "Signup failed: " + message);
                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Network Error: " + t.getMessage(), t);
                Toast.makeText(SignupActivity.this, "Connection Error: Check if backend is running on " + ApiClient.getBaseUrl(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
