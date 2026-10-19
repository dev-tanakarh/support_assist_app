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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class CreateTicketActivity extends AppCompatActivity {

    private static final String TAG = "CreateTicket";
    
    @Inject ApiService apiService;
    @Inject CategoryRepository categoryRepository;
    
    private Spinner spCategory;
    private EditText etSubject, etDescription, etDepartment, etRoom, etDevice;
    private TextView tvLow, tvMedium, tvHigh, tvAttachmentStatus;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryNames = new ArrayList<>();
    
    private String selectedPriority = "MEDIUM";
    private int preselectedCategoryId = -1;
    private Button btnSubmit;
    private Uri selectedImageUri;
    
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    tvAttachmentStatus.setText(R.string.attachments);
                    tvAttachmentStatus.append(": Selected");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);

        preselectedCategoryId = getIntent().getIntExtra("category_id", -1);
        
        initViews();
        setupPrioritySelection();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.ll_add_attachment).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        observeCategories();
        btnSubmit.setOnClickListener(v -> handleSubmission());
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

    private void initViews() {
        etSubject = findViewById(R.id.et_subject);
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
    }

    private void observeCategories() {
        // Reactive lazy loading: Show cached categories instantly, then update when sync completes
        disposables.add(categoryRepository.getCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCategoryUI, throwable -> Log.e(TAG, "Error loading categories", throwable)));
        
        categoryRepository.syncCategories();
    }

    private void updateCategoryUI(List<Category> categories) {
        if (categories == null) return;
        categoryList.clear();
        categoryList.addAll(categories);
        categoryNames.clear();
        int selection = 0;
        for (int i = 0; i < categoryList.size(); i++) {
            categoryNames.add(categoryList.get(i).getName());
            if (categoryList.get(i).getId() == preselectedCategoryId) selection = i;
        }
        categoryAdapter.notifyDataSetChanged();
        spCategory.setSelection(selection);
    }

    private void handleSubmission() {
        if (etSubject.getText().toString().trim().isEmpty() || etDescription.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            compressAndSubmit();
        } else {
            submitTicketJson();
        }
    }

    private void submitTicketJson() {
        setLoading(true);
        Map<String, Object> body = new HashMap<>();
        body.put("subject", etSubject.getText().toString().trim());
        body.put("description", etDescription.getText().toString().trim());
        body.put("category_id", categoryList.get(spCategory.getSelectedItemPosition()).getId());
        body.put("priority", selectedPriority);
        body.put("department", etDepartment.getText().toString().trim());
        body.put("device_type", etDevice.getText().toString().trim());
        body.put("room", etRoom.getText().toString().trim());

        apiService.createTicketJson(body).enqueue(createCallback());
    }

    private void compressAndSubmit() {
        setLoading(true);
        Data inputData = new Data.Builder()
                .putString("image_uri", selectedImageUri.toString())
                .putInt("target_width", 1280)
                .putInt("target_height", 720)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ImageCompressionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(request);
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        submitTicketMultipart(workInfo.getOutputData().getString("compressed_path"));
                    } else if (workInfo != null && workInfo.getState() == WorkInfo.State.FAILED) {
                        setLoading(false);
                        Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitTicketMultipart(String imagePath) {
        RequestBody subject = textPart(etSubject.getText().toString().trim());
        RequestBody description = textPart(etDescription.getText().toString().trim());
        RequestBody category = textPart(String.valueOf(categoryList.get(spCategory.getSelectedItemPosition()).getId()));
        RequestBody priority = textPart(selectedPriority);
        RequestBody dept = textPart(etDepartment.getText().toString().trim());
        RequestBody device = textPart(etDevice.getText().toString().trim());
        RequestBody room = textPart(etRoom.getText().toString().trim());

        File file = new File(imagePath);
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part attachment = MultipartBody.Part.createFormData("attachment", file.getName(), fileBody);

        apiService.createTicketMultipart(subject, description, category, priority, dept, device, room, attachment)
                .enqueue(createCallback());
    }

    private Callback<ApiResponse<Ticket>> createCallback() {
        return new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CreateTicketActivity.this, "Ticket Created!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String msg = ApiErrors.extractMessage(response, getString(R.string.error_generic));
                    Toast.makeText(CreateTicketActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreateTicketActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Submitting..." : getString(R.string.submit_ticket));
    }

    private void setupPrioritySelection() {
        tvLow.setOnClickListener(v -> updatePriorityUI("LOW"));
        tvMedium.setOnClickListener(v -> updatePriorityUI("MEDIUM"));
        tvHigh.setOnClickListener(v -> updatePriorityUI("HIGH"));
        updatePriorityUI("MEDIUM");
    }

    private void updatePriorityUI(String priority) {
        selectedPriority = priority;
        TextView[] views = {tvLow, tvMedium, tvHigh};
        for (TextView tv : views) {
            tv.setBackgroundResource(R.drawable.edit_text_bg);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_grey));
            tv.setBackgroundTintList(null);
        }
        TextView selected = priority.equals("LOW") ? tvLow : priority.equals("HIGH") ? tvHigh : tvMedium;
        int color = priority.equals("LOW") ? R.color.status_open_text : priority.equals("HIGH") ? R.color.priority_high_text : R.color.priority_medium_text;
        int bg = priority.equals("LOW") ? R.color.status_open_bg : priority.equals("HIGH") ? R.color.priority_high_bg : R.color.priority_medium_bg;
        selected.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, bg)));
        selected.setTextColor(ContextCompat.getColor(this, color));
    }

    private static RequestBody textPart(String value) {
        return RequestBody.create(value != null ? value : "", MediaType.parse("text/plain"));
    }
}
