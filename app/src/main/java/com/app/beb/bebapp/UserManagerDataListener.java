package com.app.beb.bebapp;

public interface UserManagerDataListener {
    void onUserTextDataLoaded(User user);
    void onUserProfilePicDataLoaded(User user);
    void onUserDataFullyLoaded(User user);
}
