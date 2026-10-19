package com.example.supportassist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supportassist.paging.TicketPagingAdapter;
import com.example.supportassist.viewmodel.TicketViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class AdminTicketsFragment extends Fragment {

    private static final String TAG = "AdminTicketsFragment";

    private ProgressBar progressBar;
    private RecyclerView rvTickets;
    private View emptyStateContainer;
    private Spinner spStatus, spPriority, spCategory;
    private EditText etSearch;
    private TicketPagingAdapter pagingAdapter;
    private TicketViewModel viewModel;
    
    private final List<Category> categoriesList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    
    private String[] statusValues, priorityValues;
    
    private final CompositeDisposable pagingDisposables = new CompositeDisposable();
    private final CompositeDisposable uiDisposables = new CompositeDisposable();
    private final PublishSubject<String> searchSubject = PublishSubject.create();

    private String currentStatus = null;
    private String currentPriority = null;
    private Integer currentCategoryId = null;
    private String currentSearch = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_tickets, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        rvTickets = view.findViewById(R.id.rv_tickets);
        emptyStateContainer = view.findViewById(R.id.ll_empty_state);
        spStatus = view.findViewById(R.id.sp_status_filter);
        spPriority = view.findViewById(R.id.sp_priority_filter);
        spCategory = view.findViewById(R.id.sp_category_filter);
        etSearch = view.findViewById(R.id.et_search);

        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        pagingAdapter = new TicketPagingAdapter();
        rvTickets.setAdapter(pagingAdapter);

        pagingAdapter.addLoadStateListener(loadStates -> {
            boolean isLoading = loadStates.getRefresh() instanceof LoadState.Loading;
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            
            boolean isNotLoading = loadStates.getRefresh() instanceof LoadState.NotLoading;
            boolean isEmpty = isNotLoading && pagingAdapter.getItemCount() == 0;
            if (emptyStateContainer != null) {
                emptyStateContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
            return null;
        });

        setupFilters();
        setupSearch();
        initCategorySpinner();
        observeCategories();
        loadTickets();

        view.findViewById(R.id.btn_clear_filters).setOnClickListener(v -> clearFilters());

        return view;
    }

    private void clearFilters() {
        etSearch.setText("");
        currentSearch = null;
        currentStatus = null;
        currentPriority = null;
        currentCategoryId = null;
        if (spStatus != null) spStatus.setSelection(0);
        if (spPriority != null) spPriority.setSelection(0);
        if (spCategory != null) spCategory.setSelection(0);
        loadTickets();
    }

    private void setupFilters() {
        // Status Filter
        String[] statusLabels = getResources().getStringArray(R.array.status_filter_labels);
        statusValues = getResources().getStringArray(R.array.status_filter_values);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statusLabels);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (statusValues == null || position >= statusValues.length) return;
                String status = statusValues[position].isEmpty() ? null : statusValues[position];
                if ((status == null && currentStatus != null) || (status != null && !status.equals(currentStatus))) {
                    currentStatus = status;
                    loadTickets();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Priority Filter
        String[] priorityLabels = getResources().getStringArray(R.array.priority_filter_labels);
        priorityValues = getResources().getStringArray(R.array.priority_filter_values);
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, priorityLabels);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);
        spPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (priorityValues == null || position >= priorityValues.length) return;
                String val = priorityValues[position];
                String actualPriority = val.isEmpty() ? null : val;
                if ((actualPriority == null && currentPriority != null) || (actualPriority != null && !actualPriority.equals(currentPriority))) {
                    currentPriority = actualPriority;
                    loadTickets();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void initCategorySpinner() {
        categoryNames.clear();
        categoryNames.add("All Categories");
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryNames);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
        
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer newCatId = (position == 0) ? null : (categoriesList.size() > position - 1 ? categoriesList.get(position - 1).getId() : null);
                if ((newCatId == null && currentCategoryId != null) || (newCatId != null && !newCatId.equals(currentCategoryId))) {
                    currentCategoryId = newCatId;
                    loadTickets();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observeCategories() {
        uiDisposables.add(viewModel.getCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (list != null) {
                        updateCategoryList(list);
                    }
                }, throwable -> Log.e(TAG, "Error observing categories", throwable)));
        
        viewModel.refreshCategories();
    }

    private void updateCategoryList(List<Category> categoryList) {
        this.categoriesList.clear();
        this.categoriesList.addAll(categoryList);
        
        categoryNames.clear();
        categoryNames.add("All Categories");
        for (Category c : categoriesList) {
            String name = c.getName();
            categoryNames.add(name != null ? name : "Unknown");
        }

        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
        
        // Restore selection if currentCategoryId exists
        if (currentCategoryId != null) {
            for (int i = 0; i < categoriesList.size(); i++) {
                if (categoriesList.get(i).getId() == currentCategoryId.intValue()) {
                    spCategory.setSelection(i + 1, false);
                    break;
                }
            }
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchSubject.onNext(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        uiDisposables.add(searchSubject
                .debounce(400, TimeUnit.MILLISECONDS)
                .distinctUntilChanged()
                .subscribe(query -> {
                    currentSearch = query.trim().isEmpty() ? null : query.trim();
                    if (getActivity() != null) getActivity().runOnUiThread(this::loadTickets);
                }));
    }

    private void loadTickets() {
        pagingDisposables.clear();
        pagingDisposables.add(viewModel.getTicketsPaging(currentStatus, currentPriority, currentCategoryId, currentSearch, null)
                .subscribe(pagingData -> pagingAdapter.submitData(getLifecycle(), pagingData)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pagingDisposables.dispose();
        uiDisposables.dispose();
    }
}
