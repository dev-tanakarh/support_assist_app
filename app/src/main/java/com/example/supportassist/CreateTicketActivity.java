package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
    private String selectedPriority = "MEDIUM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);

        apiService = ApiClient.getApiService(this);

        etSubject = findViewById(R.id.et_subject);
        etDescription = findViewById(R.id.et_description);
        etDepartment = findViewById(R.id.et_department);
        etRoom = findViewById(R.id.et_room);
        etDevice = findViewById(R.id.et_device);
        spCategory = findViewById(R.id.sp_category);
        
        // Fixed: Using direct IDs for the priority TextViews
        tvLow = findViewById(R.id.tv_priority_low);
        tvMedium = findViewById(R.id.tv_priority_medium);
        tvHigh = findViewById(R.id.tv_priority_high);
        
        setupPrioritySelection();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        fetchCategories();

        findViewById(R.id.btn_submit).setOnClickListener(v -> submitTicket());
    }

    private void setupPrioritySelection() {
        // Use the initialized member variables instead of getChildAt
        tvLow.setOnClickListener(v -> updatePriorityUI("LOW", tvLow, tvMedium, tvHigh));
        tvMedium.setOnClickListener(v -> updatePriorityUI("MEDIUM", tvLow, tvMedium, tvHigh));
        tvHigh.setOnClickListener(v -> updatePriorityUI("HIGH", tvLow, tvMedium, tvHigh));
        
        // Initial state
        updatePriorityUI("MEDIUM", tvLow, tvMedium, tvHigh);
    }

    private void updatePriorityUI(String priority, TextView low, TextView medium, TextView high) {
        selectedPriority = priority;
        
        // Reset backgrounds
        low.setBackgroundResource(R.drawable.edit_text_bg);
        medium.setBackgroundResource(R.drawable.edit_text_bg);
        high.setBackgroundResource(R.drawable.edit_text_bg);

        // Highlight selected
        if (priority.equals("LOW")) low.setBackgroundResource(R.drawable.priority_low_bg);
        else if (priority.equals("MEDIUM")) medium.setBackgroundResource(R.drawable.priority_medium_bg);
        else if (priority.equals("HIGH")) high.setBackgroundResource(R.drawable.priority_high_bg);
    }

    private void fetchCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    categoryList.clear();
                    categoryList.addAll(response.body().getData());
                    
                    categoryNames.clear();
                    for (Category c : categoryList) {
                        categoryNames.add(c.getName());
                    }
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Log.e("CreateTicket", "Error fetching categories: " + t.getMessage());
            }
        });
    }

    private void submitTicket() {
        String subject = etSubject.getText().toString();
        String description = etDescription.getText().toString();
        String department = etDepartment.getText().toString();
        String device = etDevice.getText().toString();
        
        if (subject.isEmpty() || description.isEmpty() || spCategory.getSelectedItem() == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int categoryId = categoryList.get(spCategory.getSelectedItemPosition()).getId();

        Map<String, Object> body = new HashMap<>();
        body.put("subject", subject);
        body.put("description", description);
        body.put("category_id", categoryId);
        body.put("priority", selectedPriority);
        body.put("department", department);
        body.put("device_type", device);

        apiService.createTicket(body).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CreateTicketActivity.this, "Ticket Submitted Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateTicketActivity.this, "Failed to submit ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                Toast.makeText(CreateTicketActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
