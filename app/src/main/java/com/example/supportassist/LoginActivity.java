package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    @Inject TokenManager tokenManager;
    private ApiService apiService;
    private ProgressBar progressBar;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = ApiClient.getApiService(this);
        progressBar = findViewById(R.id.pb_login);
        loginBtn = findViewById(R.id.btn_login);

        // 5. Modern UI: Biometric Authentication
        if (tokenManager.getAccessToken() != null) {
            if (BiometricHelper.isBiometricAvailable(this)) {
                BiometricHelper.showBiometricPrompt(this, new BiometricHelper.BiometricCallback() {
                    @Override
                    public void onAuthenticationSucceeded() {
                        navigateToMain();
                    }

                    @Override
                    public void onAuthenticationError(String error) {
                        Toast.makeText(LoginActivity.this, "Biometric failed: " + error, Toast.LENGTH_SHORT).show();
                        // Stay on login screen to allow password entry
                    }
                });
            } else {
                navigateToMain();
            }
            return;
        }

        final EditText emailInput = findViewById(R.id.et_email);
        final EditText passwordInput = findViewById(R.id.et_password);
        TextView tvSignup = findViewById(R.id.tv_signup);
        findViewById(R.id.tv_forgot_password).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

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

    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (loginBtn != null) loginBtn.setEnabled(!isLoading);
    }

    private void loginUser(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        apiService.login(credentials).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call, Response<ApiResponse<LoginResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    tokenManager.saveTokens(data.getAccessToken(), data.getRefreshToken());
                    tokenManager.saveUser(data.getUser());
                    
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    String message = ApiErrors.extractMessage(response, "Login failed. Please try again.");
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_LONG).show();
            }
        });
    }
}
