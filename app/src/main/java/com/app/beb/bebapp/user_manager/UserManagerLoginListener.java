package com.app.beb.bebapp.user_manager;

public interface UserManagerLoginListener {
    void onLogin(Boolean successfully);
    void onRegister(Boolean successfully);
}