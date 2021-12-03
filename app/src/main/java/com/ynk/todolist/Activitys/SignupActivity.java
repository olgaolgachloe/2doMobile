package com.ynk.todolist.Activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.ynk.todolist.Database.AppDatabase;
import com.ynk.todolist.Database.DAO;
import com.ynk.todolist.Model.User;
import com.ynk.todolist.R;

import muyan.snacktoa.SnackToa;


public class SignupActivity extends AppCompatActivity implements View.OnClickListener {

    private DAO dao;

    private EditText etName, etUserName, etPassword, etMail;

    private ImageView imageViewClickToChooseProfilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dao = AppDatabase.getDb(this).getDAO();

        imageViewClickToChooseProfilePicture = findViewById(R.id.imageView_click_to_choose_profile_picture);
        imageViewClickToChooseProfilePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseProfilePicture();
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        initToolbar();
        initComponent();

    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initComponent() {
        etName = findViewById(R.id.etName);
        etUserName = findViewById(R.id.etUserName);
        etPassword = findViewById(R.id.etPassword);
        etMail = findViewById(R.id.etMail);

        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSubmit:
                boolean isError = false;
                if (TextUtils.isEmpty(etName.getText().toString().trim())) {
                    etName.setError(getString(R.string.signUpNameError));
                    isError = true;
                }
                if (TextUtils.isEmpty(etUserName.getText().toString().trim())) {
                    etUserName.setError(getString(R.string.signUpUserNameError));
                    isError = true;
                }
                if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
                    etPassword.setError(getString(R.string.signUpPasswordError));
                    isError = true;
                }
                if (TextUtils.isEmpty(etMail.getText().toString().trim())) {
                    etMail.setError(getString(R.string.signUpEmailError));
                    isError = true;
                }
                if (isError) return;

                if (dao.signUpControl(etUserName.getText().toString(), etMail.getText().toString()) == 0) {
                    User user = new User();
                    user.setUserMail(etMail.getText().toString());
                    user.setUserName(etUserName.getText().toString());
                    user.setUserNameSurname(etName.getText().toString());
                    user.setUserPassword(etPassword.getText().toString());
                    dao.insertUser(user);
                    SnackToa.toastSuccess(this, getString(R.string.signUpSuccessMessage));
                    finish();
                } else {
                    SnackToa.snackBarError(this, getString(R.string.signUpErrorMessage));
                }

                break;

        }
    }

    private void chooseProfilePicture(){
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.alert_dialog_profile_picture, null);
        builder.setCancelable(false);
        builder.setView(dialogView);

        ImageView imageViewCamera = dialogView.findViewById(R.id.imageView_camera);
        ImageView imageViewGallery = dialogView.findViewById(R.id.imageView_gallery);

        final AlertDialog alertDialogProfilePicture = builder.create();
        alertDialogProfilePicture.show();

        imageViewCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAndRequestPermission()){
                    takePictureFromCamera();
                    alertDialogProfilePicture.cancel();
                }
            }
        });

        imageViewGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureFromGallery();
                alertDialogProfilePicture.cancel();

            }
        });
    }

    private void takePictureFromGallery(){
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, 1);
    }


    private void takePictureFromCamera(){
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null){
            startActivityForResult(takePicture, 2);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case 1:
                if (resultCode == RESULT_OK){
                    Uri selectedImageUri = data.getData();
                    imageViewClickToChooseProfilePicture.setImageURI(selectedImageUri);
                }
                break;
            case 2:
                if (resultCode == RESULT_OK){
                    Bundle bundle = data.getExtras();
                    Bitmap bitmapImage = (Bitmap) bundle.get("data");
                    imageViewClickToChooseProfilePicture.setImageBitmap(bitmapImage);
                }
                break;

        }
    }

    private boolean checkAndRequestPermission(){
        if (Build.VERSION.SDK_INT >= 23){
            int cameraPermission = ActivityCompat.checkSelfPermission(SignupActivity.this, Manifest.permission.CAMERA);
            if (cameraPermission == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(SignupActivity.this, new String[]{Manifest.permission.CAMERA}, 20);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 20 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            takePictureFromCamera();

        }
        else{
            Toast.makeText(SignupActivity.this, "Permission not Granted", Toast.LENGTH_SHORT).show();
        }

    }



}
