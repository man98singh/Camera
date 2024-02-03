package com.example.camera4k






import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


// MainActivity inherits from ComponentActivity, which is a base class for activities using Jetpack Compose
class MainActivity : ComponentActivity() {

    // This annotation indicates that we are using experimental API from the Accompanist library
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Standard call to the parent class's onCreate method
        setContent { // This sets the content view for the activity using Compose
            CameraPreview() // Calls a composable function to create the camera preview UI
        }
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

        AndroidView(
            factory = { ctx -> // The factory lambda is used to create the Android View
                val previewView = PreviewView(ctx) // Creates a PreviewView for the camera
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx) // Gets an instance of the camera provider

                // Listener to set up the camera once the camera provider is available
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get() // Gets the camera provider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider) // Sets the surface provider for the preview
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // Selects the back camera
                    cameraProvider.unbindAll() // Unbinds any previous use-cases
                    cameraProvider.bindToLifecycle( // Binds use-cases to the lifecycle owner
                        lifecycleOwner, cameraSelector, preview
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView // Returns the preview view to be displayed
            },
            modifier = Modifier.fillMaxSize() // Modifier to make the view fill the maximum available size
        )
    } else {
        // This is displayed if the user denies the camera permission
        Text("Camera permission denied. Please grant permission to use the camera.")
    }

}

