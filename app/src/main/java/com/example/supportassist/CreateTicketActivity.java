package com.example.supportassist;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class CreateTicketActivity extends AppCompatActivity {

    private Spinner spCategory;
    private EditText etSubject, etDescription, etDepartment, etRoom, etDevice;
    private TextView tvLow, tvMedium, tvHigh, tvAttachmentStatus;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryNames = new ArrayList<>();
    private ApiService apiService;
    private DataRepository repository;
    private String selectedPriority = "MEDIUM";
    private int preselectedCategoryId = -1;
    private Button btnSubmit;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    tvAttachmentStatus.setText("Image selected");
                    tvAttachmentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_open_text));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);

        apiService = ApiClient.getApiService(this);
        repository = DataRepository.getInstance(this);

        preselectedCategoryId = getIntent().getIntExtra("category_id", -1);
        String prefilledSubject = getIntent().getStringExtra("subject_hint");

        etSubject = findViewById(R.id.et_subject);
        if (prefilledSubject != null) etSubject.setText(prefilledSubject);
        
        etDescription = findViewById(R.id.et_description);
        etDepartment = findViewById(R.id.et_department);
        etRoom = findViewById(R.id.et_room);
        etDevice = findViewById(R.id.et_device);
        spCategory = findViewById(R.id.sp_category);
        btnSubmit = findViewById(R.id.btn_submit);
        tvAttachmentStatus = findViewById(R.id.tv_label_attachments);
        
        tvLow = findViewById(R.id.tv_priority_low);
        tvMedium = findViewById(R.id.tv_priority_medium);
        tvHigh = findViewById(R.id.tv_priority_high);
        
        setupPrioritySelection();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.ll_add_attachment).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        loadCategoriesFromCache();
        fetchCategories();

        btnSubmit.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                compressAndSubmit();
            } else {
                submitTicket(null);
            }
        });
    }

    private void compressAndSubmit() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Compressing Image...");

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
                        submitTicket(compressedPath);
                    } else if (workInfo != null && workInfo.getState() == WorkInfo.State.FAILED) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Ticket");
                        Toast.makeText(this, "Compression failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCategoriesFromCache() {
        List<Category> cached = repository.getCachedCategories();
        if (cached != null && !cached.isEmpty()) {
            updateCategoryUI(cached);
        }
    }

    private void setupPrioritySelection() {
        tvLow.setOnClickListener(v -> updatePriorityUI("LOW"));
        tvMedium.setOnClickListener(v -> updatePriorityUI("MEDIUM"));
        tvHigh.setOnClickListener(v -> updatePriorityUI("HIGH"));
        updatePriorityUI("MEDIUM");
    }

    private void updatePriorityUI(String priority) {
        selectedPriority = priority;
        resetPriorityStyle(tvLow);
        resetPriorityStyle(tvMedium);
        resetPriorityStyle(tvHigh);

        if (priority.equals("LOW")) {
            highlightPriority(tvLow, R.color.status_open_bg, R.color.status_open_text);
        } else if (priority.equals("MEDIUM")) {
            highlightPriority(tvMedium, R.color.priority_medium_bg, R.color.priority_medium_text);
        } else if (priority.equals("HIGH")) {
            highlightPriority(tvHigh, R.color.priority_high_bg, R.color.priority_high_text);
        }
    }

    private void resetPriorityStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.edit_text_bg);
        tv.setBackgroundTintList(null);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_grey));
    }

    private void highlightPriority(TextView tv, int bgColorRes, int textColorRes) {
        tv.setBackgroundResource(R.drawable.edit_text_bg);
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes)));
        tv.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private void fetchCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> freshCategories = response.body().getData();
                    repository.cacheCategories(freshCategories);
                    updateCategoryUI(freshCategories);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {}
        });
    }

    private void updateCategoryUI(List<Category> categories) {
        categoryList.clear();
        categoryList.addAll(categories);
        categoryNames.clear();
        int selectionIndex = 0;
        for (int i = 0; i < categoryList.size(); i++) {
            Category c = categoryList.get(i);
            categoryNames.add(c.getName());
            if (c.getId() == preselectedCategoryId) selectionIndex = i;
        }
        categoryAdapter.notifyDataSetChanged();
        if (preselectedCategoryId != -1) spCategory.setSelection(selectionIndex);
    }

    private void submitTicket(String compressedImagePath) {
        String subject = etSubject.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        
        if (subject.isEmpty() || description.isEmpty() || spCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Ticket");
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        int categoryId = categoryList.get(spCategory.getSelectedItemPosition()).getId();

        // Every non-file field of a multipart request is still its own part —
        // Retrofit has no "@Body inside @Multipart" escape hatch, so this can't
        // be a single Map the way a JSON request could.
        RequestBody subjectBody = textPart(subject);
        RequestBody descriptionBody = textPart(description);
        RequestBody categoryBody = textPart(String.valueOf(categoryId));
        RequestBody priorityBody = textPart(selectedPriority);
        RequestBody departmentBody = textPart(etDepartment.getText().toString());
        RequestBody deviceBody = textPart(etDevice.getText().toString());
        RequestBody roomBody = textPart(etRoom.getText().toString());

        MultipartBody.Part attachmentPart = null;
        if (compressedImagePath != null) {
            File imageFile = new File(compressedImagePath);
            RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
            // Field name here MUST be "attachment" — it's what the backend's
            // FileUpload::storeImage() reads via $request->file('attachment').
            attachmentPart = MultipartBody.Part.createFormData("attachment", imageFile.getName(), fileBody);
        }

        apiService.createTicket(subjectBody, descriptionBody, categoryBody, priorityBody, departmentBody, deviceBody, roomBody, attachmentPart)
                .enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CreateTicketActivity.this, "Ticket Created!", Toast.LENGTH_SHORT).show();
                    repository.syncDataSilently();
                    finish();
                } else {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Ticket");
                    Toast.makeText(CreateTicketActivity.this, "Failed to create ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Ticket");
                Toast.makeText(CreateTicketActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static RequestBody textPart(String value) {
        return RequestBody.create(value != null ? value : "", MediaType.parse("text/plain"));
    }
}
