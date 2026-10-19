package com.example.supportassist.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingState;
import androidx.paging.rxjava3.RxPagingSource;
import com.example.supportassist.ApiResponse;
import com.example.supportassist.ApiService;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketsResponse;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;

public class TicketPagingSource extends RxPagingSource<Integer, Ticket> {

    private final ApiService apiService;
    private final String status;
    private final String priority;
    private final Integer categoryId;
    private final String query;
    private final Boolean unassigned;

    public TicketPagingSource(ApiService apiService, 
                              @Nullable String status,
                              @Nullable String priority,
                              @Nullable Integer categoryId,
                              @Nullable String query,
                              @Nullable Boolean unassigned) {
        this.apiService = apiService;
        this.status = status;
        this.priority = priority;
        this.categoryId = categoryId;
        this.query = query;
        this.unassigned = unassigned;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Ticket> pagingState) {
        return null;
    }

    @NonNull
    @Override
    public Single<LoadResult<Integer, Ticket>> loadSingle(@NonNull LoadParams<Integer> loadParams) {
        int page = loadParams.getKey() != null ? loadParams.getKey() : 1;
        int perPage = loadParams.getLoadSize();

        return apiService.getTickets(status, priority, categoryId, query, unassigned, null, page, perPage)
                .subscribeOn(Schedulers.io())
                .map(this::toLoadResult)
                .onErrorReturn(LoadResult.Error::new);
    }

    private LoadResult<Integer, Ticket> toLoadResult(ApiResponse<TicketsResponse> response) {
        if (response.isSuccess() && response.getData() != null) {
            List<Ticket> tickets = response.getData().getTickets();
            TicketsResponse.Pagination pagination = response.getData().getPagination();
            
            if (pagination == null) {
                return new LoadResult.Page<>(tickets, null, null);
            }

            int currentPage = pagination.getPage();
            int totalPages = pagination.getTotalPages();

            return new LoadResult.Page<>(
                    tickets,
                    currentPage == 1 ? null : currentPage - 1,
                    currentPage >= totalPages ? null : currentPage + 1
            );
        } else {
            String message = response.getMessage() != null ? response.getMessage() : "Unknown error";
            return new LoadResult.Error<>(new Exception(message));
        }
    }
}
