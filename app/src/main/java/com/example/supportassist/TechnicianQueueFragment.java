package com.example.supportassist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.content.ContextCompat;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

@AndroidEntryPoint
public class TechnicianQueueFragment extends Fragment {

    private ProgressBar progressBar;
    private RecyclerView rvQueue;
    private View llEmptyState;
    private Spinner spStatus, spCategory;
    private EditText etSearch;
    
    private TicketPagingAdapter pagingAdapter;
    private TicketViewModel viewModel;
    
    @Inject CategoryRepository categoryRepository;
    
    private String[] statusValues;
    private List<Category> categories = new ArrayList<>();
    
    private final CompositeDisposable pagingDisposables = new CompositeDisposable();
    private final CompositeDisposable uiDisposables = new CompositeDisposable();
    private final PublishSubject<String> searchSubject = PublishSubject.create();

    private String currentStatus = null;
    private Integer currentCategoryId = null;
    private String currentSearch = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_technician_queue, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        rvQueue = view.findViewById(R.id.rv_queue);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        spStatus = view.findViewById(R.id.sp_status_filter);
        spCategory = view.findViewById(R.id.sp_category_filter);
        etSearch = view.findViewById(R.id.et_search);

        rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        pagingAdapter = new TicketPagingAdapter();
        rvQueue.setAdapter(pagingAdapter);

        pagingAdapter.addLoadStateListener(loadStates -> {
            boolean isLoading = loadStates.getRefresh() instanceof LoadState.Loading;
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            
            boolean isEmpty = loadStates.getRefresh() instanceof LoadState.NotLoading && pagingAdapter.getItemCount() == 0;
            llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            return null;
        });

        setupFilters();
        setupSearch();
        observeCategories();
        loadQueue();

        view.findViewById(R.id.btn_clear_filters).setOnClickListener(v -> clearFilters());

        return view;
    }

    private void clearFilters() {
        etSearch.setText("");
        currentSearch = null;
        currentStatus = null;
        currentCategoryId = null;
        spStatus.setSelection(0);
        spCategory.setSelection(0);
        loadQueue();
    }

    private void setupFilters() {
        statusValues = getResources().getStringArray(R.array.status_filter_values);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.status_filter_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter);
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String status = statusValues[position].isEmpty() ? null : statusValues[position];
                if (!Objects.equals(status, currentStatus)) {
                    currentStatus = status;
                    loadQueue();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void observeCategories() {
        uiDisposables.add(categoryRepository.getCategories()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateCategorySpinner, throwable -> updateCategorySpinner(new ArrayList<>())));
        
        categoryRepository.syncCategories();
    }

    private void updateCategorySpinner(List<Category> categoryList) {
        this.categories = categoryList;
        List<String> names = new ArrayList<>();
        names.add("All Categories");
        for (Category c : categories) names.add(c.getName());

        if (getContext() == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer newCatId = (position == 0) ? null : categories.get(position - 1).getId();
                if (!Objects.equals(newCatId, currentCategoryId)) {
                    currentCategoryId = newCatId;
                    loadQueue();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        if (currentCategoryId != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == currentCategoryId.intValue()) {
                    spCategory.setSelection(i + 1);
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
                    if (getActivity() != null) getActivity().runOnUiThread(this::loadQueue);
                }));
    }

    private void loadQueue() {
        pagingDisposables.clear();
        pagingDisposables.add(viewModel.getTicketsPaging(currentStatus, null, currentCategoryId, currentSearch, null)
                .subscribe(pagingData -> pagingAdapter.submitData(getLifecycle(), pagingData)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pagingDisposables.dispose();
        uiDisposables.dispose();
    }
}
