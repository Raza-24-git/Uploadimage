package com.cloudinary.cloudinaryquickstart

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream
import android.util.Base64

class MainImagekit : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectButton: Button
    private lateinit var uploadButton: Button
    private var imageUri: Uri? = null
    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activityimagekit)

        imageView = findViewById(R.id.imageView)
        selectButton = findViewById(R.id.selectButton)
        uploadButton = findViewById(R.id.uploadButton)

        selectButton.setOnClickListener { pickImage() }
        uploadButton.setOnClickListener { uploadSelectedImage() }
    }

    // Image Picker
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                imageView.setImageURI(it)
                imageFile = uriToFile(it)
            }
        }

    private fun pickImage() {
        imagePickerLauncher.launch("image/*")
    }

    // Convert URI to File
    private fun uriToFile(uri: Uri): File? {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            val file = File(cacheDir, "upload_image.jpg")
            file.outputStream().use { output ->
                input.copyTo(output)
            }
            return file
        }
        return null
    }

    private fun uploadSelectedImage() {
        imageFile?.let { file ->
            uploadImageToImageKit(file)
        } ?: Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show()
    }

    // Upload Image to ImageKit.io
    private fun uploadImageToImageKit(file: File) {
        val uploadApiKey = "your_upload_api_key_here" // Use the Upload API Key (not private key)

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create("image/*".toMediaTypeOrNull(), file))
            .addFormDataPart("fileName", file.name)
            .addFormDataPart("useUniqueFileName", "true")
            .addFormDataPart("folder", "/uploads")
            .build()

        val request = Request.Builder()
            .url("https://upload.imagekit.io/api/v1/files/upload")
            .post(requestBody)
            .addHeader("Authorization", "Basic $uploadApiKey") // Use the upload key
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ImageKit Upload", "Upload Failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d("ImageKit Upload", "Upload Successful: ${response.body?.string()}")
                    } else {
                        Log.e("ImageKit Upload", "Upload Failed: ${response.body?.string()}")
                    }
                }
            }
        })
    }
}
