package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private TokenManager tokenManager;
    private ApiService apiService;
    private ProgressBar progressBar;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getApiService(this);
        progressBar = findViewById(R.id.pb_login);
        loginBtn = findViewById(R.id.btn_login);

        if (tokenManager.getAccessToken() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        final EditText emailInput = findViewById(R.id.et_email);
        final EditText passwordInput = findViewById(R.id.et_password);
        TextView tvSignup = findViewById(R.id.tv_signup);

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            setLoading(true);
            loginUser(email, password);
        });

        tvSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (loginBtn != null) loginBtn.setEnabled(!isLoading);
    }

    private void loginUser(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Log.d(TAG, "Attempting login at: " + ApiClient.getBaseUrl());

        apiService.login(credentials).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    tokenManager.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String message = "Login Failed";
                    if (response.body() != null && response.body().getMessage() != null) {
                        message = response.body().getMessage();
                    } else if (response.code() == 401) {
                        message = "Invalid email or password";
                    }
                    Log.e(TAG, "Login failed: " + message + " (Code: " + response.code() + ")");
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                setLoading(false);
                Log.e(TAG, "Network Error: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Connection Error: Check if backend is running on " + ApiClient.getBaseUrl(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
