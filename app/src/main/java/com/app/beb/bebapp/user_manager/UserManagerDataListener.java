package com.app.beb.bebapp.user_manager;

public interface UserManagerDataListener {
    void onUserTextDataLoaded(User user);
    void onUserProfilePicDataLoaded(User user);
    void onUserDataFullyLoaded(User user);
}
