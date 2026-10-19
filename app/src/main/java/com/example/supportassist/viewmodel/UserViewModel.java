package com.example.supportassist.viewmodel;

import androidx.lifecycle.ViewModel;
import com.example.supportassist.User;
import com.example.supportassist.UserRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;

@HiltViewModel
public class UserViewModel extends ViewModel {

    private final UserRepository repository;

    @Inject
    public UserViewModel(UserRepository repository) {
        this.repository = repository;
    }

    public Flowable<List<User>> getUsersByType(String type) {
        return repository.getUsersByType(type);
    }

    public void refreshUsers(String type) {
        repository.syncUsers(type);
    }

    public Flowable<User> getProfile() {
        return repository.getProfile();
    }

    public void refreshProfile() {
        repository.syncProfile();
    }
}
