package com.cloudinary.cloudinaryquickstart;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.cloudinary.Transformation;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.cloudinary.cloudinaryquickstart.databinding.ActivityImageUploadBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainCloudinary extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    private final String cloudName = "nimg";
    private String url;
    private final String publicId = "9532de8dbf5eea9a359456ceb2ec79";
    private final String uploadPreset = "selimN";

    private ActivityImageUploadBinding binding;
    private Uri imageUri;
    private String currentPhotoPath;

    // Permission request launcher
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    captureImage(); // Open camera only if permission is granted
                } else {
                    Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityImageUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle("Cloudinary Quickstart");
        setSupportActionBar(binding.toolbar);

        initCloudinary();
        generateUrl();

        binding.selectImageButton.setOnClickListener(v -> selectImage());
        binding.captureImageButton.setOnClickListener(v -> checkCameraPermission());
        Button btnTs = findViewById(R.id.next);
        btnTs.setOnClickListener(view -> {
            Intent intent = new Intent(MainCloudinary.this, MainImagekit.class);
            startActivity(intent);
        });
    }

    // Check Camera Permission Before Capturing Image
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            captureImage();
        } else {
            requestCameraPermission();
        }
    }

    // Request Camera Permission
    private void requestCameraPermission() {
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void initCloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        MediaManager.init(this, config);
    }

    private void generateUrl() {
        url = MediaManager.get().url().transformation(new Transformation().effect("sepia")).generate(publicId);
        Glide.with(this).load(url).into(binding.generatedImageview);
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Cloudinary Quickstart", "Error occurred while creating the file");
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, "com.cloudinary.cloudinaryquickstart.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                imageUri = data.getData();
            } else if (requestCode == CAMERA_REQUEST) {
                // Use `imageUri` directly because `data.getData()` is null when using FileProvider
                Log.d("Cloudinary Quickstart", "Captured image path: " + currentPhotoPath);
            }
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri == null) {
            Log.e("Cloudinary Quickstart", "No image selected");
            return;
        }

        String fileName = getFileName(imageUri);
        binding.fileNameTextView.setText("File: " + fileName);

        MediaManager.get().upload(imageUri).unsigned(uploadPreset).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d("Cloudinary Quickstart", "Upload started");
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
                Log.d("Cloudinary Quickstart", "Upload progress: " + bytes + "/" + totalBytes);
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                Log.d("Cloudinary Quickstart", "Upload success");
                String uploadedUrl = (String) resultData.get("secure_url");

                Log.d("Cloudinary uploadedUrl", uploadedUrl);
                Glide.with(getApplicationContext()).load(uploadedUrl).into(binding.uploadedImageview);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.e("Cloudinary Quickstart", "Upload failed: " + error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
                Log.e("Cloudinary Quickstart", "Upload rescheduled: " + error.getDescription());
            }
        }).dispatch();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
