package com.app.beb.bebapp.user_manager;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnCanceledListener;
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
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;

// Class to read and write to Realtime Database
class FBUser {
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

public class UserManager {

    // Firebase stuff
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorageRef;
    private DatabaseReference mDBRef;

    private User _currentUser;

    private UserManagerDataListener dataListener;
    private UserManagerLoginListener loginListener;
    private UserManagerUserDataUpload dataUploadListener;

    private int uploadSuccessesNeeded = 2;
    private int currentSucceeded = 0;

    // Object lifecycle
    public static class UserManagerHolder {
        static final UserManager HOLDER_INSTANCE = new UserManager();
    }

    public static UserManager getInstance() {
        return UserManagerHolder.HOLDER_INSTANCE;
    }

    private UserManager() {
        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance();
        mDBRef = FirebaseDatabase.getInstance().getReference("users");

        if (mAuth.getCurrentUser() != null) {
            fetchUserData();
        }
    }


    // Public
    public void loginWithCredentials(String login, String password) {
        mAuth.signInWithEmailAndPassword(login, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
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
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
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

    public void logout() {
        mAuth.signOut();
        _currentUser = null;
    }

    // Private
    private void fetchUserData() {
        _currentUser = new User();
        _currentUser.setEmail(mAuth.getCurrentUser().getEmail());
        _currentUser.setId(mAuth.getUid());


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

        StorageReference profileRef = mStorageRef.getReference(String.format("/users/%s/profile.jpg",
                mAuth.getCurrentUser().getUid()));
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

    private void writeToDatabaseNewData(User user) {
        FBUser fbUser = new FBUser();
        fbUser.setName(user.getName());
        fbUser.setSurname(user.getSurname());
        fbUser.setPhone(user.getPhone());

        mDBRef.child(mAuth.getCurrentUser().getUid()).setValue(fbUser)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        tryToNotifyOnUpload();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        tryToNotifyOnFailure(exception.getMessage());
                    }
                })
                .addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                tryToNotifyOnFailure("Canceled");
            }
        });
    }

    private void writeToAuthentication(User user) {
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (!user.getEmail().equals(_currentUser.getEmail())) {
                mAuth.getCurrentUser().updateEmail(user.getEmail())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                tryToNotifyOnUpload();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("ERROR", e.getMessage());
                                tryToNotifyOnFailure(e.getMessage());
                            }
                        });
            }
        }
    }

    private void writeToStorage(User user) {
        Uri file = Uri.fromFile(new File(user.getProfilePicPath()));
        StorageReference profileRef = mStorageRef.getReference(String.format("/users/%s/profile.jpg",
                mAuth.getCurrentUser().getUid()));

        profileRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        tryToNotifyOnUpload();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        tryToNotifyOnFailure(exception.getMessage());
                    }
                })
                .addOnCanceledListener(new OnCanceledListener() {
                    @Override
                    public void onCanceled() {
                        tryToNotifyOnFailure("Canceled");
                    }
                });

    }

    private void tryToNotifyOnUpload() {
        currentSucceeded++;
        if (currentSucceeded == uploadSuccessesNeeded) {
            if (dataUploadListener != null) {
                dataUploadListener.onSuccess();
            }
        }
    }

    private void tryToNotifyOnFailure(String message) {
        if (dataUploadListener != null) {
            dataUploadListener.onFailure(message);
        }
    }

    // Accessors
    public User getCurrentUser() {
        return _currentUser;
    }

    public void overrideCurrentUser(User user) {
        currentSucceeded = 0;
        uploadSuccessesNeeded = 1;
        if (user.getProfilePicPath() != null) {
            if (!user.getProfilePicPath().equals(_currentUser.getProfilePicPath())) {
                writeToStorage(user);
                this._currentUser.setProfilePicPath(user.getProfilePicPath());
                uploadSuccessesNeeded++;
            }
        }
        writeToDatabaseNewData(user);

// TODO: Implement email editing
//        writeToAuthentication(user);

        this._currentUser.setName(user.getName());
        this._currentUser.setSurname(user.getSurname());
        this._currentUser.setPhone(user.getPhone());
    }

    public void setDataListener(UserManagerDataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void setLoginListener(UserManagerLoginListener loginListener) {
        this.loginListener = loginListener;
    }

    public void setDataUploadListener(UserManagerUserDataUpload dataUploadListener) {
        this.dataUploadListener = dataUploadListener;
    }
}
