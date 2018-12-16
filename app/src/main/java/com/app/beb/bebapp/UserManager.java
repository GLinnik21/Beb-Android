package com.app.beb.bebapp;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public class UserManager {

    // Class to read and write to Realtime Database
    private class FBUser {
        private String name;
        private String surname;
        private String phone;

        public FBUser() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    // Firebase stuff
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private DatabaseReference mDBRef;

    private User _currentUser;

    private UserManagerDataListener dataListener;
    private UserManagerLoginListener loginListener;

    // Object lifecycle
    public static class UserManagerHolder {
        static final UserManager HOLDER_INSTANCE = new UserManager();
    }

    public static UserManager getInstance() {
        return UserManagerHolder.HOLDER_INSTANCE;
    }

    private UserManager() {
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference().
                getRoot().
                child("users").
                child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
        mDBRef = FirebaseDatabase.getInstance().getReference("users");
    }


    // Public
    public void loginWithCredentials(String login, String password) {
        mAuth.signInWithEmailAndPassword(login, password)
                .addOnCompleteListener((Executor) this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (loginListener != null) {
                            loginListener.onLogin(task.isSuccessful());
                        }
                        if (task.isSuccessful()) {
                            fetchUserData();
                        }
                    }
                });
    }

    public void registerWithCredentials(String login, String password) {
        mAuth.createUserWithEmailAndPassword(login, password)
                .addOnCompleteListener((Executor) this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (loginListener != null) {
                            loginListener.onRegister(task.isSuccessful());
                        }
                        if (task.isSuccessful()) {
                            fetchUserData();
                        }
                    }
                });
    }


    // Private
    private void fetchUserData() {
        _currentUser = new User();
        _currentUser.setEmail(mAuth.getCurrentUser().getEmail());

        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        final Boolean[] firstPartLoaded = {false};

        mDBRef.child(userId).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        FBUser value = dataSnapshot.getValue(FBUser.class);
                        Log.d("DATABASE", "User is: " + value);

                        if (value != null) {
                            _currentUser.setName(value.getName());
                            _currentUser.setSurname(value.getSurname());
                            _currentUser.setPhone(value.getPhone());
                        }

                        if (dataListener != null) {
                            dataListener.onUserTextDataLoaded(_currentUser);
                            if (firstPartLoaded[0]) {
                                dataListener.onUserDataFullyLoaded(_currentUser);
                            } else {
                                firstPartLoaded[0] = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.w("DATABASE", "Failed to read user.", error.toException());
                    }
                });

        StorageReference profileRef = mStorageRef.child("profile.jpg");
        try {
            final File localFile = File.createTempFile("profile" + UUID.randomUUID(), "jpg");
            profileRef.getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Log.d("DATABASE", "Successfully loaded profile pic");

                            _currentUser.setProfilePicPath(localFile.getAbsolutePath());

                            if (dataListener != null) {
                                dataListener.onUserProfilePicDataLoaded(_currentUser);
                                if (firstPartLoaded[0]) {
                                    dataListener.onUserDataFullyLoaded(_currentUser);
                                } else {
                                    firstPartLoaded[0] = true;
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("DATABASE", "Could not load profile pic");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Accessors
    public User getCurrentUser() {
        return _currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this._currentUser = currentUser;
    }

    public void setDataListener(UserManagerDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void setLoginListener(UserManagerLoginListener loginListener) {
        this.loginListener = loginListener;
    }
}
