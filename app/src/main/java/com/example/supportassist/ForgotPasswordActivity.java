package com.example.supportassist;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText etEmail, etToken, etNewPassword;
    private MaterialButton btnSendCode, btnReset;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        apiService = ApiClient.getApiService(this);

        etEmail = findViewById(R.id.et_email);
        etToken = findViewById(R.id.et_token);
        etNewPassword = findViewById(R.id.et_new_password);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnReset = findViewById(R.id.btn_reset);
        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> sendResetCode());
        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void sendResetCode() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.forgotPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setLoading(false);
                // Always show the same message regardless of outcome — the
                // backend deliberately never reveals whether the email
                // matched an account (see AuthController::forgotPassword).
                Toast.makeText(ForgotPasswordActivity.this, R.string.reset_code_sent, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetPassword() {
        String token = etToken.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString();
        if (token.isEmpty() || newPassword.length() < 8) {
            Toast.makeText(this, R.string.error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("newPassword", newPassword);

        apiService.resetPassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, R.string.reset_password_success, Toast.LENGTH_LONG).show();
                    finish(); // back to the LoginActivity that launched this — no need for a new instance
                } else {
                    String message = ApiErrors.extractMessage(response, getString(R.string.error_generic));
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendCode.setEnabled(!loading);
        btnReset.setEnabled(!loading);
    }
}
