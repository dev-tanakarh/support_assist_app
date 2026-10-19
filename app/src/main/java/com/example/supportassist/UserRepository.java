package com.example.supportassist;

import android.util.Log;
import com.example.supportassist.db.UserDao;
import com.example.supportassist.db.UserEntity;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class UserRepository {
    private static final String TAG = "UserRepository";
    private final ApiService apiService;
    private final UserDao userDao;
    private final DataRepository legacyDataRepository;
    private final TokenManager tokenManager;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public UserRepository(ApiService apiService, UserDao userDao, 
                          DataRepository legacyDataRepository, TokenManager tokenManager) {
        this.apiService = apiService;
        this.userDao = userDao;
        this.legacyDataRepository = legacyDataRepository;
        this.tokenManager = tokenManager;
    }

    public Flowable<List<User>> getUsersByType(String type) {
        return userDao.getUsersFlowable(type)
                .map(entities -> {
                    List<User> users = new ArrayList<>();
                    for (UserEntity e : entities) {
                        users.add(mapToDomain(e));
                    }
                    return users;
                })
                .subscribeOn(Schedulers.io());
    }

    public void syncUsers(String type) {
        apiService.getUsers(type).enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<User> users = response.body().getData();
                    if (users != null) {
                        saveUsersLocally(type, users);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                Log.e(TAG, "Failed to sync users for type: " + type, t);
            }
        });
    }

    private void saveUsersLocally(String type, List<User> users) {
        List<UserEntity> entities = new ArrayList<>();
        for (User u : users) {
            entities.add(mapToEntity(u));
        }
        dbExecutor.execute(() -> {
            userDao.clearUsersByType(type);
            userDao.insertAll(entities);
        });
    }

    /**
     * Unified method to save profile data across all cache layers.
     */
    public void saveProfileLocally(User user) {
        if (user == null) return;
        tokenManager.saveUser(user);
        legacyDataRepository.cacheProfile(user);
        dbExecutor.execute(() -> userDao.insert(mapToEntity(user)));
    }

    public User getCurrentUserSync() {
        User user = tokenManager.getUser();
        if (user == null) {
            user = legacyDataRepository.getCachedProfile();
        }
        return user;
    }

    public Flowable<User> getProfile() {
        // Instant data from TokenManager (populated at Login/Signup)
        User cached = getCurrentUserSync();
        Flowable<User> initialFlow = cached != null ? Flowable.just(cached) : Flowable.empty();
        
        String currentUserId = tokenManager.getUserId();
        if (currentUserId == null) return initialFlow;

        // Reactive data from local Room database
        Flowable<User> dbFlow = userDao.getUserById(currentUserId)
                .map(this::mapToDomain)
                .filter(Objects::nonNull); // Prevent blanking out if DB row is missing

        return Flowable.concat(initialFlow, dbFlow)
                .distinctUntilChanged((u1, u2) -> {
                    if (u1 == null || u2 == null) return u1 == u2;
                    return Objects.equals(u1.getId(), u2.getId()) &&
                           Objects.equals(u1.getName(), u2.getName()) &&
                           Objects.equals(u1.getEmail(), u2.getEmail()) &&
                           Objects.equals(u1.getUserType(), u2.getUserType()) &&
                           Objects.equals(u1.getProfileImageUrl(), u2.getProfileImageUrl());
                })
                .subscribeOn(Schedulers.io());
    }

    public void syncProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        saveProfileLocally(user);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e(TAG, "Profile sync failed", t);
            }
        });
    }

    private User mapToDomain(UserEntity e) {
        if (e == null) return null;
        User u = new User();
        u.setId(e.id);
        u.setName(e.name);
        u.setEmail(e.email);
        u.setPhone(e.phone);
        u.setUserType(e.userType);
        u.setActive(e.isActive);
        u.setProfileImageUrl(e.profileImageUrl);
        u.setAvatarThumbnailUrl(e.avatarThumbnailUrl);
        u.setRating(e.rating);
        u.setReviewCount(e.reviewCount);
        return u;
    }

    private UserEntity mapToEntity(User u) {
        return new UserEntity(
                u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getUserType(), u.isActive(), u.getProfileImageUrl(),
                u.getAvatarThumbnailUrl(), u.getRating(), u.getReviewCount()
        );
    }
}
