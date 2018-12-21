package com.app.beb.bebapp.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beb.bebapp.R;
import com.app.beb.bebapp.user_manager.User;
import com.app.beb.bebapp.user_manager.UserManager;
import com.app.beb.bebapp.user_manager.UserManagerDataListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class Profile extends Fragment implements UserManagerDataListener {

    // GUI
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneTextView;
    private ImageView imageView;

    public Profile() {
        // Required empty public constructor
    }

    public static Profile newInstance() {
        Profile fragment = new Profile();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameTextView = getView().findViewById(R.id.name_textView);
        emailTextView = getView().findViewById(R.id.email_textView);
        phoneTextView = getView().findViewById(R.id.phone_textView);
        imageView = getView().findViewById(R.id.profile_pic_imageView);

        setDataWithUser(UserManager.getInstance().getCurrentUser());
        UserManager.getInstance().setDataListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_profile:
                NavHostFragment.findNavController(this).navigate(R.id.editProfile);
                return true;
        }
        return false;
    }

    private void setDataWithUser(User user) {
        if (user != null) {
            if (user.getName() != null) {
                nameTextView.setText(String.format("%s ", user.getName()));
            }
            if (user.getSurname() != null) {
                nameTextView.setText(String.format("%s%s", nameTextView.getText(), user.getSurname()));
            }
            if (user.getEmail() != null) {
                emailTextView.setText(user.getEmail());
            }
            if (user.getPhone() != null) {
                phoneTextView.setText(user.getPhone());
            }
            if (user.getProfilePicPath() != null) {
                Bitmap myBitmap = BitmapFactory.decodeFile(user.getProfilePicPath());
                imageView.setImageBitmap(myBitmap);
            }
        }
    }

    @Override
    public void onUserTextDataLoaded(User user) {
        setDataWithUser(user);
    }

    @Override
    public void onUserProfilePicDataLoaded(User user) {
        setDataWithUser(user);
    }

    @Override
    public void onUserDataFullyLoaded(User user) {
        setDataWithUser(user);
    }
}

