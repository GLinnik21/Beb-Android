package com.app.beb.bebapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Calendar;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;


public class EditProfile extends Fragment implements UserManagerUserDataUpload {
    private static final String IMAGE_DIRECTORY = "/beb_app";
    private int GALLERY = 1, CAMERA = 2;

    // GUI
    private TextInputLayout nameTextInput;
    private TextInputLayout surnameTextInput;
    private TextInputLayout phoneTextInput;
    private ImageView imageView;
    private FloatingActionButton okButton;

    private ProgressDialog dialog;

    private String newPicAbsolutePath;

    public EditProfile() {
        // Required empty public constructor
    }

    public static EditProfile newInstance() {
        EditProfile fragment = new EditProfile();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestMultiplePermissions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageView = getView().findViewById(R.id.profile_imageView);
        nameTextInput = getView().findViewById(R.id.name_textInputLayout);
        surnameTextInput = getView().findViewById(R.id.surname_textInputLayout);
        phoneTextInput = getView().findViewById(R.id.phone_textInputLayout);
        okButton = getView().findViewById(R.id.done_actionButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUser();
            }
        });

        final int kTakePictureIndex = 0;
        final int kPickFromGalleryIndex = 1;

        View editPhotoView = getActivity().findViewById(R.id.change_pic_view);
        editPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(R.array.pick_image_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case kTakePictureIndex:
                                takePhotoFromCamera();
                                break;
                            case kPickFromGalleryIndex:
                                choosePhotoFromGallary();
                                break;
                            default:
                                break;
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        setDataWithUser(UserManager.getInstance().getCurrentUser());
    }

    private void setDataWithUser(User user) {
        if (user != null) {
            if (user.getName() != null) {
                nameTextInput.getEditText().setText(user.getName());
            }
            if (user.getSurname() != null) {
                surnameTextInput.getEditText().setText(user.getSurname());
            }
            if (user.getEmail() != null) {
                phoneTextInput.getEditText().setText(user.getPhone());
            }
            if (user.getProfilePicPath() != null) {
                Bitmap myBitmap = BitmapFactory.decodeFile(user.getProfilePicPath());
                imageView.setImageBitmap(myBitmap);
            }
        }
    }

    private void saveUser() {
        User user = new User();
        user.setName(nameTextInput.getEditText().getText().toString());
        user.setSurname(surnameTextInput.getEditText().getText().toString());
        user.setPhone(phoneTextInput.getEditText().getText().toString());
        user.setProfilePicPath(newPicAbsolutePath);
        User current = UserManager.getInstance().getCurrentUser();

        Boolean changed = false;
        changed = changed || !user.getName().equals(current.getName());
        changed = changed || !user.getSurname().equals(current.getSurname());
        changed = changed || !user.getPhone().equals(current.getPhone());
        changed = changed || user.getProfilePicPath() != null;

        if (changed) {
            UserManager.getInstance().setDataUploadListener(this);
            UserManager.getInstance().overrideCurrentUser(user);
            dialog = ProgressDialog.show(getActivity(), "",
                    "Loading. Please wait...", true);
        } else {
            Navigation.findNavController(getView()).navigateUp();
        }
    }

    private void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == getActivity().RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), contentURI);
                    newPicAbsolutePath = saveImage(bitmap);
                    Toast.makeText(getContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
                    imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Failed!", Toast.LENGTH_SHORT).show();
                }
            }

        } else if (requestCode == CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(thumbnail);
            newPicAbsolutePath = saveImage(thumbnail);
            Toast.makeText(getContext(), "Image Saved!", Toast.LENGTH_SHORT).show();
        }
    }

    private String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(getContext(),
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::---&gt;" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    private void  requestMultiplePermissions(){
        Dexter.withActivity(getActivity())
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check for permanent denial of any permission
                        if (!report.areAllPermissionsGranted()) {
                            if (report.getDeniedPermissionResponses().size() > 1) {
                                Toast.makeText(getContext().getApplicationContext(),
                                        "All permissions were denied!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext().getApplicationContext(),
                                        String.format("%s was denied", report.getDeniedPermissionResponses().get(0).getPermissionName()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getContext().getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    @Override
    public void onSuccess() {
        dialog.dismiss();
        Navigation.findNavController(getView()).navigateUp();
    }

    @Override
    public void onFailure(String errorMessage) {
        dialog.dismiss();
        Toast.makeText(getContext().getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
    }
}
