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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import io.reactivex.rxjava3.subjects.PublishSubject;

@AndroidEntryPoint
public class TicketsFragment extends Fragment {

    private static final String TAG = "TicketsFragment";

    private RecyclerView rvTickets;
    private TicketPagingAdapter pagingAdapter;
    private TextView tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed;
    private EditText etSearch;
    private Spinner spCategory;
    private View llEmptyState;
    private TicketViewModel viewModel;
    
    private final CompositeDisposable pagingDisposables = new CompositeDisposable();
    private final CompositeDisposable uiDisposables = new CompositeDisposable();
    private final PublishSubject<String> searchSubject = PublishSubject.create();

    private String currentStatus = null;
    private Integer currentCategoryId = null;
    private String currentSearch = null;
    private final List<Category> categoriesList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tickets, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        etSearch = view.findViewById(R.id.et_search);
        spCategory = view.findViewById(R.id.sp_category_filter);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        tvFilterAll = view.findViewById(R.id.tv_filter_all);
        tvFilterOpen = view.findViewById(R.id.tv_filter_open);
        tvFilterProgress = view.findViewById(R.id.tv_filter_progress);
        tvFilterClosed = view.findViewById(R.id.tv_filter_closed);

        rvTickets = view.findViewById(R.id.rv_tickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        
        pagingAdapter = new TicketPagingAdapter();
        rvTickets.setAdapter(pagingAdapter);

        pagingAdapter.addLoadStateListener(loadStates -> {
            boolean isEmpty = loadStates.getRefresh() instanceof LoadState.NotLoading && pagingAdapter.getItemCount() == 0;
            llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            return null;
        });

        setupFilters();
        setupSearch();
        initCategorySpinner();
        observeCategories();
        loadTickets();

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        view.findViewById(R.id.btn_clear_filters).setOnClickListener(v -> clearFilters());

        return view;
    }

    private void clearFilters() {
        etSearch.setText("");
        currentSearch = null;
        currentStatus = null;
        currentCategoryId = null;
        if (spCategory != null) spCategory.setSelection(0);
        updateFilterUI(tvFilterAll);
        loadTickets();
    }

    private void loadTickets() {
        pagingDisposables.clear();
        pagingDisposables.add(viewModel.getTicketsPaging(currentStatus, null, currentCategoryId, currentSearch, null)
                .subscribe(pagingData -> pagingAdapter.submitData(getLifecycle(), pagingData)));
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
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(this::loadTickets);
                    }
                }));
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
                .subscribe(this::updateCategoryList, throwable -> Log.e(TAG, "Error observing categories", throwable)));
        
        viewModel.refreshCategories();
    }

    private void updateCategoryList(List<Category> categoryList) {
        this.categoriesList.clear();
        this.categoriesList.addAll(categoryList);
        
        categoryNames.clear();
        categoryNames.add("All Categories");
        for (Category c : categoriesList) {
            categoryNames.add(c.getName() != null ? c.getName() : "Unknown");
        }

        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
        
        if (currentCategoryId != null) {
            for (int i = 0; i < categoriesList.size(); i++) {
                if (categoriesList.get(i).getId() == currentCategoryId.intValue()) {
                    spCategory.setSelection(i + 1, false);
                    break;
                }
            }
        }
    }

    private void setupFilters() {
        tvFilterAll.setOnClickListener(v -> { updateFilterUI(tvFilterAll); currentStatus = null; loadTickets(); });
        tvFilterOpen.setOnClickListener(v -> { updateFilterUI(tvFilterOpen); currentStatus = "OPEN"; loadTickets(); });
        tvFilterProgress.setOnClickListener(v -> { updateFilterUI(tvFilterProgress); currentStatus = "IN_PROGRESS"; loadTickets(); });
        tvFilterClosed.setOnClickListener(v -> { updateFilterUI(tvFilterClosed); currentStatus = "CLOSED"; loadTickets(); });
    }

    private void updateFilterUI(TextView activeFilter) {
        TextView[] filters = {tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed};
        for (TextView tv : filters) {
            tv.setBackgroundResource(0);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_grey));
        }
        activeFilter.setBackgroundResource(R.color.primary_blue);
        activeFilter.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pagingDisposables.dispose();
        uiDisposables.dispose();
    }
}
