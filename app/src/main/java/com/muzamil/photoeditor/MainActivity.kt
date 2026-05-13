package com.muzamil.photoeditor

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.muzamil.photoeditor.ui.MyEditorScreen
import com.muzamil.photoeditor.ui.theme.PhotoEditorTheme
import com.muzamil.photoeditor.viewmodel.PhotoEditorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PhotoEditorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {


            PhotoEditorTheme {
                val pickMedia =
                    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        val bitmap = getBitmap(contentResolver, uri)
                        bitmap?.let {
                            viewModel.loadImage(bitmap = it)
                        }
                    }



                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                            MyEditorScreen(viewModel = viewModel){
                                pickMedia.launch(
                                    PickVisualMediaRequest(
                                        //Here we request only photos. Change this to .ImageAndVideo if
                                        //you want videos too.
                                        //Or use .VideoOnly if you only want videos.
                                        mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }



                    }
                }
            }
        }
    }
}

// Source - https://stackoverflow.com/a/71995972
// Posted by Rahul Tala
// Retrieved 2025-11-21, License - CC BY-SA 4.0

fun getBitmap(contentResolver: ContentResolver, fileUri: Uri?): Bitmap? {
    fileUri ?: return null
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, fileUri))
        } else {
            contentResolver.openInputStream(fileUri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) {
        null
    }
}