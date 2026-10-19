package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supportassist.viewmodel.UserViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class AdminUsersFragment extends Fragment {

    private static final String TAG = "AdminUsersFragment";

    @Inject ApiService apiService;

    private ProgressBar progressBar;
    private RecyclerView rvUsers;
    private Spinner spTypeFilter;
    private UserAdapter adapter;
    private String[] typeValues;
    private UserViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        rvUsers = view.findViewById(R.id.rv_users);
        spTypeFilter = view.findViewById(R.id.sp_user_type_filter);

        adapter = new UserAdapter(new ArrayList<>(), this::confirmRemoveTechnician, this::toggleActive);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        typeValues = getResources().getStringArray(R.array.user_type_filter_values);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.user_type_filter_labels, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTypeFilter.setAdapter(spinnerAdapter);
        spTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                observeUsers(typeValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        view.findViewById(R.id.btn_add_technician).setOnClickListener(v -> showAddTechnicianDialog());
        view.findViewById(R.id.btn_broadcast_alert).setOnClickListener(v -> showBroadcastAlertDialog());

        return view;
    }

    private void observeUsers(String type) {
        disposables.clear();
        progressBar.setVisibility(View.VISIBLE);
        
        disposables.add(viewModel.getUsersByType(type)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.updateUsers(users != null ? users : new ArrayList<>());
                }, throwable -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error observing users", throwable);
                }));
        
        viewModel.refreshUsers(type);
    }

    @Override
    public void onResume() {
        super.onResume();
        int pos = spTypeFilter.getSelectedItemPosition();
        if (pos >= 0) {
            observeUsers(typeValues[pos]);
        }
    }

    private void toggleActive(User user, boolean makeActive) {
        Map<String, Object> body = new HashMap<>();
        body.put("isActive", makeActive);

        apiService.setUserStatus(user.getId(), body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful()) {
                    Toast.makeText(getContext(), ApiErrors.extractMessage(response, getString(R.string.error_generic)), Toast.LENGTH_LONG).show();
                    int pos = spTypeFilter.getSelectedItemPosition();
                    viewModel.refreshUsers(typeValues[pos]);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmRemoveTechnician(User technician) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(technician.getName())
                .setMessage(R.string.admin_confirm_remove_technician)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> removeTechnician(technician))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void removeTechnician(User technician) {
        apiService.deleteTechnician(technician.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    int pos = spTypeFilter.getSelectedItemPosition();
                    viewModel.refreshUsers(typeValues[pos]);
                } else {
                    Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddTechnicianDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_technician, null);
        EditText etName = form.findViewById(R.id.et_tech_name);
        EditText etEmail = form.findViewById(R.id.et_tech_email);
        EditText etPhone = form.findViewById(R.id.et_tech_phone);
        EditText etPassword = form.findViewById(R.id.et_tech_password);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_add_technician)
                .setView(form)
                .setPositiveButton(R.string.admin_add_technician, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String password = etPassword.getText().toString();
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Toast.makeText(getContext(), R.string.error_fill_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createTechnician(name, email, phone, password);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void createTechnician(String name, String email, String phone, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("phone", phone);
        body.put("password", password);

        apiService.createTechnician(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(), R.string.admin_technician_created, Toast.LENGTH_SHORT).show();
                    viewModel.refreshUsers("TECHNICIAN");
                    spTypeFilter.setSelection(0);
                } else {
                    Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBroadcastAlertDialog() {
        View form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_broadcast_alert, null);
        EditText etTitle = form.findViewById(R.id.et_alert_title);
        EditText etMessage = form.findViewById(R.id.et_alert_message);
        View btnSend = form.findViewById(R.id.btn_send);
        View btnCancel = form.findViewById(R.id.btn_cancel);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(form)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSend.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(getContext(), R.string.error_fill_required, Toast.LENGTH_SHORT).show();
                return;
            }
            sendBroadcastAlert(title, etMessage.getText().toString().trim());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void sendBroadcastAlert(String title, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        if (!message.isEmpty()) body.put("message", message);

        apiService.sendAdminAlert(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (!isAdded()) return;
                Toast.makeText(getContext(),
                        response.isSuccessful() ? R.string.admin_alert_sent : R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                if (isAdded()) Toast.makeText(getContext(), R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
    }
}
