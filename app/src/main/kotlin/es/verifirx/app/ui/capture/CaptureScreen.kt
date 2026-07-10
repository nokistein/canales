package es.verifirx.app.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import es.verifirx.app.R
import es.verifirx.app.ui.processing.ProcessingScreen
import java.io.File

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    onNavigateToResults: (String) -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.onImageReady(context, it) } }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CaptureEvent.NavigateToResults -> onNavigateToResults(event.sessionId)
            }
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (hasCameraPermission) {
                CameraCapturePane(
                    createOutputFile = { viewModel.createCaptureOutputFile(context) },
                    onImageCaptured = { uri -> viewModel.onImageReady(context, uri) },
                    onPickFromGallery = { galleryLauncher.launch("image/*") },
                )
            } else {
                PermissionRationale(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onPickFromGallery = { galleryLauncher.launch("image/*") },
                )
            }

            if (uiState is CaptureUiState.Processing) {
                ProcessingScreen()
            }
        }
    }

    val errorState = uiState as? CaptureUiState.Error
    if (errorState != null) {
        val messageRes = when (errorState.reason) {
            CaptureErrorReason.NO_ROWS_DETECTED -> R.string.results_empty
            CaptureErrorReason.PROCESSING_FAILED -> R.string.processing_error
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            confirmButton = { Button(onClick = viewModel::dismissError) { Text("OK") } },
            text = { Text(stringResource(messageRes)) },
        )
    }
}

@Composable
private fun CameraCapturePane(
    createOutputFile: () -> Pair<File, Uri>,
    onImageCaptured: (Uri) -> Unit,
    onPickFromGallery: () -> Unit,
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = { imageCapture = it },
            )

            // Guide frame showing where to align the dispensation sheet.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(0.71f)
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp)),
            )

            Text(
                text = stringResource(R.string.capture_hint),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPickFromGallery) {
                Icon(Icons.Filled.Photo, contentDescription = stringResource(R.string.capture_gallery_cd))
            }
            IconButton(
                onClick = {
                    val capture = imageCapture ?: return@IconButton
                    val (file, uri) = createOutputFile()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onImageCaptured(uri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.processing_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                },
                modifier = Modifier.size(72.dp),
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = stringResource(R.string.capture_shutter_cd),
                    modifier = Modifier.size(48.dp),
                )
            }
            // Balances the row visually against the gallery button on the other side.
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun CameraPreview(modifier: Modifier = Modifier, onImageCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    onImageCaptureReady(imageCapture)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit, onPickFromGallery: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.capture_permission_denied), textAlign = TextAlign.Center)
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.capture_grant_permission))
        }
        Button(onClick = onPickFromGallery, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.home_upload_scan))
        }
    }
}
