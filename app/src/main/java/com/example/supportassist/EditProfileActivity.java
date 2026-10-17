package com.example.supportassist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail;
    private ImageView ivProfilePicture;
    private MaterialButton btnSave;
    private ApiService apiService;
    private DataRepository repository;
    private Uri selectedImageUri;
    private User currentUser;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProfilePicture.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        apiService = ApiClient.getApiService(this);
        repository = DataRepository.getInstance(this);

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        btnSave = findViewById(R.id.btn_save);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        currentUser = repository.getCachedProfile();
        if (currentUser != null) {
            etName.setText(currentUser.getName());
            etPhone.setText(currentUser.getPhone());
            etEmail.setText(currentUser.getEmail());
            if (currentUser.getProfileImageUrl() != null) {
                Glide.with(this).load(currentUser.getProfileImageUrl()).placeholder(R.drawable.ic_profile).into(ivProfilePicture);
            }
        }

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("phone", phone);

        apiService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    repository.cacheProfile(response.body().getData());
                    if (selectedImageUri != null) {
                        compressAndUploadImage();
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void compressAndUploadImage() {
        btnSave.setText("Uploading Image...");
        Data inputData = new Data.Builder()
                .putString("image_uri", selectedImageUri.toString())
                .build();

        OneTimeWorkRequest compressionRequest = new OneTimeWorkRequest.Builder(ImageCompressionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(compressionRequest);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(compressionRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        String compressedPath = workInfo.getOutputData().getString("compressed_path");
                        uploadImage(compressedPath);
                    } else if (workInfo != null && workInfo.getState() == WorkInfo.State.FAILED) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                        Toast.makeText(this, "Image compression failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImage(String path) {
        File file = new File(path);
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        apiService.updateProfilePicture(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    repository.cacheProfile(response.body().getData());
                    Toast.makeText(EditProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                    Toast.makeText(EditProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
