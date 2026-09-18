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

    public TicketPagingSource(ApiService apiService, String status) {
        this.apiService = apiService;
        this.status = status;
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

        return apiService.getTickets(status, null, null, null, page, 10)
                .subscribeOn(Schedulers.io())
                .map(this::toLoadResult)
                .onErrorReturn(LoadResult.Error::new);
    }

    private LoadResult<Integer, Ticket> toLoadResult(ApiResponse<TicketsResponse> response) {
        if (response.isSuccess()) {
            List<Ticket> tickets = response.getData().getTickets();
            int currentPage = response.getData().getPagination().getPage();
            int totalPages = response.getData().getPagination().getTotalPages();

            return new LoadResult.Page<>(
                    tickets,
                    currentPage == 1 ? null : currentPage - 1,
                    currentPage >= totalPages ? null : currentPage + 1
            );
        } else {
            return new LoadResult.Error<>(new Exception(response.getMessage()));
        }
    }
}
