package com.example.supportassist;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTicketActivity extends AppCompatActivity {

    private Spinner spCategory;
    private EditText etSubject, etDescription, etDepartment, etRoom, etDevice;
    private TextView tvLow, tvMedium, tvHigh;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryNames = new ArrayList<>();
    private ApiService apiService;
    private DataRepository repository;
    private String selectedPriority = "MEDIUM";
    private int preselectedCategoryId = -1;
    private Button btnSubmit;

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
        
        tvLow = findViewById(R.id.tv_priority_low);
        tvMedium = findViewById(R.id.tv_priority_medium);
        tvHigh = findViewById(R.id.tv_priority_high);
        
        setupPrioritySelection();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        // Fix: Load from cache first for instant loading
        loadCategoriesFromCache();
        fetchCategories();

        btnSubmit.setOnClickListener(v -> submitTicket());
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
        updatePriorityUI("MEDIUM"); // Initial state
    }

    private void updatePriorityUI(String priority) {
        selectedPriority = priority;
        
        // Reset all styles
        resetPriorityStyle(tvLow);
        resetPriorityStyle(tvMedium);
        resetPriorityStyle(tvHigh);

        // Highlight selected with specific colors
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

    private void submitTicket() {
        String subject = etSubject.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        
        if (subject.isEmpty() || description.isEmpty() || spCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button immediately to prevent double submission
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        int categoryId = categoryList.get(spCategory.getSelectedItemPosition()).getId();
        Map<String, Object> body = new HashMap<>();
        body.put("subject", subject);
        body.put("description", description);
        body.put("category_id", categoryId);
        body.put("priority", selectedPriority);
        body.put("department", etDepartment.getText().toString());
        body.put("device_type", etDevice.getText().toString());

        apiService.createTicket(body).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CreateTicketActivity.this, "Ticket Created!", Toast.LENGTH_SHORT).show();
                    repository.syncDataSilently(); // Immediate silent background sync
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
                Toast.makeText(CreateTicketActivity.this, "Network Error: Please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
