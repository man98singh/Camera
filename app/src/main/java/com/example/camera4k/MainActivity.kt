package com.example.camera4k






import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera4k.databinding.ActivityMainBinding
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


// MainActivity inherits from ComponentActivity, which is a base class for activities using Jetpack Compose
class MainActivity : ComponentActivity() {

    private var isRecording = mutableStateOf(false)

    private lateinit var viewBinding : ActivityMainBinding
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var imageCapture : ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService



    // This annotation indicates that we are using experimental API from the Accompanist library
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Standard call to the parent class's onCreate method
        setContent { // This sets the content view for the activity using Compose
            CameraPreview() // Calls a composable function to create the camera preview UI
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @ExperimentalPermissionsApi

    private fun  takePhoto() {
         val imageCapture = imageCapture?: return
        val name = SimpleDateFormat(FILENAME_FORMAT , Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                put(MediaStore.Images.Media.RELATIVE_PATH , "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback{
            override fun onError (exc : ImageCaptureException) {
                Log.e(TAG, "Photo capture failed : ${exc.message}", exc)
            }
            override fun onImageSaved(output : ImageCapture.OutputFileResults) {
                val msg = "Photo capture succeeded : ${output.savedUri}"
                Toast.makeText(baseContext,msg ,Toast.LENGTH_SHORT).show()
                Log.d(TAG,msg)

            }
        })
    }

    private fun captureVideo() {
        if (recording == null) {
            // Start recording
            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX")
                }
            }

            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()

            recording = videoCapture?.output?.prepareRecording(this, mediaStoreOutputOptions)
                ?.start(ContextCompat.getMainExecutor(this)) { videoRecordEvent ->
                    when (videoRecordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Recording started")
                            isRecording.value = true
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (videoRecordEvent.error == null) {
                                val savedUri = videoRecordEvent.outputResults.outputUri
                                Log.d(TAG, "Recording saved to $savedUri")
                            } else {
                                Log.e(TAG, "Recording error: ${videoRecordEvent.error}")
                            }
                            recording = null
                            isRecording.value = false
                        } }}

        } else {
            // Stop recording
            recording?.stop()
            recording = null
            isRecording.value = false
        }
    }

// Annotation to indicate the use of an experimental API for handling permissions

@ExperimentalPermissionsApi
@Composable // Marks this function as a Composable, meaning it can define UI elements
fun CameraPreview() {
    // rememberPermissionState handles the state of camera permission
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // LaunchedEffect is used to perform side effects in Compose (like requesting permissions)
    LaunchedEffect(key1 = true) {
        cameraPermissionState.launchPermissionRequest() // Requests camera permission
    }

    // Checks if the camera permission has been granted
    if (cameraPermissionState.status.isGranted) {
        val context = LocalContext.current // Gets the current context *
        val lifecycleOwner = LocalLifecycleOwner.current // Gets the current LifecycleOwner


        // AndroidView is a Composable that allows you to use Android Views
        Box(modifier = Modifier.fillMaxSize()) { Column {

        }


            AndroidView(
                factory = { ctx -> // The factory lambda is used to create the Android View
                    val previewView = PreviewView(ctx) // Creates a PreviewView for the camera
                    val cameraProviderFuture =
                        ProcessCameraProvider.getInstance(ctx) // Gets an instance of the camera provider

                    // Listener to set up the camera once the camera provider is available
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get() // Gets the camera provider
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider) // Sets the surface provider for the preview
                        }
                            imageCapture = ImageCapture.Builder().build()

                        val cameraSelector =
                            CameraSelector.DEFAULT_BACK_CAMERA // Selects the back camera
                        cameraProvider.unbindAll() // Unbinds any previous use-cases
                        cameraProvider.bindToLifecycle( // Binds use-cases to the lifecycle owner
                            lifecycleOwner, cameraSelector, preview , imageCapture
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView // Returns the preview view to be displayed
                },
                modifier = Modifier.fillMaxSize()// Modifier to make the view fill the maximum available size
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { takePhoto() }) {
                    Text("Capture Photo")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { captureVideo() }) {
                    Text(if (isRecording.value) "Stop Recording" else "Start Recording")

                }

            }
        }
    }
//                 Button(onClick = { takePhoto() },modifier = Modifier
//                     .fillMaxWidth()
//                     .align(Alignment.BottomCenter)
//                     .padding(bottom = 4.dp)) {
//                    Text("Capture photo")
//                }
//
//               Spacer(modifier = Modifier.height(8.dp))
//                Button(onClick = { captureVideo() }) {
//                Text("Record Video")
//            }
//        } }
            else {
            // This is displayed if the user denies the camera permission
            Text("Camera permission denied. Please grant permission to use the camera.")
        }
    

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val TAG = "CameraXAPP"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }
}
